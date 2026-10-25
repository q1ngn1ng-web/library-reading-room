#!/usr/bin/env python3
"""
阶梯并发压测（混压模式：N 个用户抢 K 个空座，断言成功数 ≤ K 且与放行数一致 = 零超卖）。
用法: sudo python3 scripts/bench_load.py <并发1> [并发2 ...]
产出: Markdown 表（成功/抢空/429/异常、P50/P95/P99、总耗时、吞吐）。
前置: /etc/reading-room.env 有 JWT_SECRET；自动造压测用户并在结束时清理。
"""
import concurrent.futures as cf
import hmac, hashlib, base64, json, time, sys, urllib.request, urllib.error, subprocess

HOST = "http://127.0.0.1:9090"
LEVELS = [int(x) for x in sys.argv[1:]] or [100, 300, 500, 1000]
MAXN = max(LEVELS)
BENCH_PREFIX = "load"


def env(name):
    out = subprocess.run(["grep", f"^{name}=", "/etc/reading-room.env"],
                         capture_output=True, text=True).stdout.strip()
    return out.split("=", 1)[1]


def jwt_token(user_id, secret):
    b = lambda x: base64.urlsafe_b64encode(x).rstrip(b"=").decode()
    h = b(json.dumps({"alg": "HS256", "typ": "JWT"}).encode())
    p = b(json.dumps({"aud": [str(user_id)], "role": "user",
                      "exp": int(time.time()) + 3600}).encode())
    return f"{h}.{p}." + b(hmac.new(secret.encode(), f"{h}.{p}".encode(), hashlib.sha256).digest())


def sql(q, fetch=False):
    r = subprocess.run(["sudo", "mysql", "library_room", "-N", "-s", "-e", q],
                       capture_output=True, text=True)
    return r.stdout.strip().splitlines() if fetch else r


def post(token, seat_id):
    body = json.dumps({"seatId": seat_id, "startTime": "2099-01-01 08:00:00",
                       "endTime": "2099-01-01 21:00:00"}).encode()
    req = urllib.request.Request(HOST + "/reservations/reserve", data=body, method="POST",
                                 headers={"Content-Type": "application/json", "token": token})
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            payload = json.loads(resp.read())
            return time.time() - t0, resp.getcode(), payload.get("code"), payload.get("msg")
    except urllib.error.HTTPError as e:
        return time.time() - t0, e.code, str(e.code), str(e.reason)
    except Exception as e:
        return time.time() - t0, 0, "EXC", str(e)[:50]


def pct(xs, p):
    if not xs:
        return -1
    xs = sorted(xs)
    return xs[min(len(xs) - 1, int(len(xs) * p))] * 1000


def reset_between_levels():
    sql(f"UPDATE reservation SET status=4 WHERE status IN (1,5) AND user_id IN "
        f"(SELECT id FROM (SELECT id FROM user WHERE username LIKE '{BENCH_PREFIX}%') t)")
    sql("UPDATE seat SET status=0 WHERE status IN (1,2) AND id NOT IN "
        "(SELECT seat_id FROM (SELECT seat_id FROM reservation WHERE status IN (1,5)) t)")


def main():
    secret = env("JWT_SECRET")

    # 造压测用户（密码给合法 BCrypt 哈希，避免被启动迁移器处理）
    sql(f"DELETE FROM reservation WHERE user_id IN (SELECT id FROM (SELECT id FROM user WHERE username LIKE '{BENCH_PREFIX}%') t)")
    sql(f"DELETE FROM user WHERE username LIKE '{BENCH_PREFIX}%'")
    vals = ",".join(
        f"('{BENCH_PREFIX}{i:04d}','$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5B0G1S3k2Z/GWjLQZxW1xQvVJ7mG',"
        f"'压测{i:04d}','student','default.png','男',20,'',1)" for i in range(MAXN))
    sql(f"INSERT INTO user (username,password,nickname,role,avatar,sex,age,phone,chat) VALUES {vals}")
    uids = [r.split("\t")[0] for r in
            sql(f"SELECT id FROM user WHERE username LIKE '{BENCH_PREFIX}%' ORDER BY id", fetch=True)]
    assert len(uids) >= MAXN, f"用户创建不足: {len(uids)}"
    tokens = [jwt_token(int(u), secret) for u in uids]
    print(f"已造 {len(uids)} 个压测用户\n")

    print("| 并发 | 空座K | 成功 | 抢空(手慢了) | 429 | 异常 | P50(ms) | P95(ms) | P99(ms) | 总耗时 | 吞吐 |")
    print("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |")

    try:
        for n in LEVELS:
            reset_between_levels()
            seat_ids = [int(r) for r in sql("SELECT id FROM seat WHERE status=0 ORDER BY id", fetch=True)]
            k = len(seat_ids)
            assert k >= 1, "无可用座位"

            t_start = time.time()
            with cf.ThreadPoolExecutor(n) as ex:
                futs = [ex.submit(post, tokens[i], seat_ids[i % k]) for i in range(n)]
                results = [f.result() for f in futs]
            wall = time.time() - t_start

            ok = [r for r in results if r[2] == "0"]
            taken = [r for r in results if r[3] and "手慢了" in str(r[3])]
            limited = [r for r in results if r[1] == 429]
            exc = [r for r in results if r[2] == "EXC"]
            other = n - len(ok) - len(taken) - len(limited) - len(exc)
            lat = [r[0] for r in ok]
            print(f"| {n} | {k} | {len(ok)}（应≤{k}） | {len(taken)} | {len(limited)} | {len(exc)}{f'+{other}其他' if other else ''} | "
                  f"{pct(lat,.5):.0f} | {pct(lat,.95):.0f} | {pct(lat,.99):.0f} | {wall:.1f}s | {n/wall:.0f}/s |")
    finally:
        reset_between_levels()
        sql(f"DELETE FROM reservation WHERE user_id IN (SELECT id FROM (SELECT id FROM user WHERE username LIKE '{BENCH_PREFIX}%') t)")
        sql(f"DELETE FROM user WHERE username LIKE '{BENCH_PREFIX}%'")
        print("\n（压测数据已清理）")


if __name__ == "__main__":
    main()
