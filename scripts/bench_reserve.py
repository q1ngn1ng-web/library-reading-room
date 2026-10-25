#!/usr/bin/env python3
"""
并发抢座压测脚本（可复现）。
用法（在服务器上）: sudo python3 scripts/bench_reserve.py [并发数] [轮次]
前提: /etc/reading-room.env 含 JWT_SECRET；数据库中存在 AVAILABLE 座位。
场景A: N 个并发请求抢【同一个】座位 —— 断言恰好 1 成功（防超卖回归）
场景B: N 个并发请求抢【各自不同】座位 —— 断言全部成功（吞吐正常）
输出: Markdown 表格（含成功数、P50/P95/错误分布），追加到 docs/benchmark-latest.md 由人工核对。
"""
import concurrent.futures as cf
import hmac, hashlib, base64, json, time, sys, urllib.request, subprocess

HOST = "http://127.0.0.1:9090"
CONCURRENCY = int(sys.argv[1]) if len(sys.argv) > 1 else 20
SEATS_POOL = int(sys.argv[2]) if len(sys.argv) > 2 else 40


def env(name):
    out = subprocess.run(["grep", f"^{name}=", "/etc/reading-room.env"],
                         capture_output=True, text=True).stdout.strip()
    return out.split("=", 1)[1]


def jwt_token(user_id, role, secret):
    b = lambda x: base64.urlsafe_b64encode(x).rstrip(b"=").decode()
    h = b(json.dumps({"alg": "HS256", "typ": "JWT"}).encode())
    p = b(json.dumps({"aud": [str(user_id)], "role": role, "exp": int(time.time()) + 900}).encode())
    s = b(hmac.new(secret.encode(), f"{h}.{p}".encode(), hashlib.sha256).digest())
    return f"{h}.{p}.{s}"


def post(path, token, body):
    data = json.dumps(body).encode()
    req = urllib.request.Request(HOST + path, data=data, method="POST",
                                 headers={"Content-Type": "application/json", "token": token})
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            payload = json.loads(resp.read())
            return time.time() - t0, payload.get("code"), payload.get("msg")
    except Exception as e:
        return time.time() - t0, "EXC", str(e)[:60]


def sql(q):
    return subprocess.run(["sudo", "mysql", "library_room", "-N", "-s", "-e", q],
                          capture_output=True, text=True).stdout.strip().splitlines()


def pct(lat, p):
    xs = sorted(lat)
    return xs[min(len(xs) - 1, int(len(xs) * p))] * 1000 if xs else -1


def main():
    secret = env("JWT_SECRET")
    # 准备：清场（把此前测试遗留的活跃预约结算掉），取一批空座
    sql("UPDATE seat SET status=0 WHERE status IN (1,2) AND id NOT IN "
        "(SELECT seat_id FROM (SELECT seat_id FROM reservation WHERE status IN (1,5)) t)")
    # 参与用户：避免"一人一预约"限制影响场景B，每个并发用不同测试用户（直接建号）
    for i in range(CONCURRENCY):
        sql(f"INSERT IGNORE INTO user (username,password,nickname,role,avatar,sex,age,phone,chat) "
            f"VALUES ('bench{i:03d}','x','bench{i:03d}','student','default.png','男',20,'',1)")
    uids = [r.split("\t")[0] for r in sql("SELECT id FROM user WHERE username LIKE 'bench%' ORDER BY id")]
    seats = [r for r in sql(f"SELECT id FROM seat WHERE status=0 LIMIT {SEATS_POOL}")]
    assert len(seats) >= 2, "没有可用座位"
    users = uids[:CONCURRENCY]
    while len(users) < CONCURRENCY:
        users.append(uids[0])

    print(f"| 场景 | 并发 | 成功 | 失败 | P50(ms) | P95(ms) |")
    print(f"| --- | --- | --- | --- | --- | --- |")

    # 场景A：同座竞争（防超卖）
    seat_a = seats[0]
    body = {"seatId": int(seat_a), "startTime": "2099-01-01 08:00:00", "endTime": "2099-01-01 21:00:00"}
    with cf.ThreadPoolExecutor(CONCURRENCY) as ex:
        futs = [ex.submit(post, "/reservations/reserve", jwt_token(int(u), "user", secret), body)
                for u in users]
        results = [f.result() for f in futs]
    ok = sum(1 for _, c, _ in results if c == "0")
    lat = [t for t, c, _ in results if c == "0"]
    print(f"| A: {CONCURRENCY}并发抢同一座位 | {CONCURRENCY} | {ok}（应为1） | {CONCURRENCY-ok} | "
          f"{pct(lat,0.5):.0f} | {pct(lat,0.95):.0f} |")

    # 清理场景A预约
    sql("UPDATE reservation SET status=4 WHERE status=1 AND end_time='2099-01-01 21:00:00'")
    sql(f"UPDATE seat SET status=0 WHERE id={seat_a}")

    # 场景B：各自抢不同座位（吞吐）
    pairs = list(zip(users, seats[1:1 + CONCURRENCY]))
    with cf.ThreadPoolExecutor(CONCURRENCY) as ex:
        futs = [ex.submit(post, "/reservations/reserve", jwt_token(int(u), "user", secret),
                          {"seatId": int(s), "startTime": "2099-01-01 08:00:00",
                           "endTime": "2099-01-01 21:00:00"}) for u, s in pairs]
        results = [f.result() for f in futs]
    ok = sum(1 for _, c, _ in results if c == "0")
    lat = [t for t, c, _ in results if c == "0"]
    print(f"| B: {CONCURRENCY}并发各抢不同座位 | {CONCURRENCY} | {ok}（应为{len(pairs)}） | {len(pairs)-ok} | "
          f"{pct(lat,0.5):.0f} | {pct(lat,0.95):.0f} |")

    # 清理
    sql("UPDATE reservation SET status=4 WHERE status IN (1,5) AND end_time='2099-01-01 21:00:00'")
    sql("UPDATE seat SET status=0 WHERE status IN (1,2) AND id NOT IN "
        "(SELECT seat_id FROM (SELECT seat_id FROM reservation WHERE status IN (1,5)) t)")
    print("\n（已清理测试数据）")


if __name__ == "__main__":
    main()
