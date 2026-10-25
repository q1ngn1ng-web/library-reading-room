# Design: reservation-integrity-redis

## 决策 1：预约终态保证 = 修复判断 + 双扫描定时任务

- `validateReservation`：`now.after(reservation.getTimeoutAt())` → `now.after(reservation.getEndTime())`（原行是笔误，本意就是"预约时段自然结束"）
- `checkTimeoutReservations()` 拆两类（每 60s）：
  - 未签到超时（status=1 且 timeout_at<now）→ TIMEOUT + 释放座位（现有行为）
  - 已签到过期（status=5 且 end_time<now）→ 自动 checkout：COMPLETED + real_end_time=now + 释放座位
- 释放座位一律走**条件更新** `updateSeatStatusConditionally(id, from, to)`（判影响行数）：座位已被管理员改为 MAINTENANCE 时放弃覆盖，避免"禁用座位被定时任务改回可约"

## 决策 2：并发抢占 = 数据库条件更新（不引入分布式锁）

```
UPDATE seat SET status = RESERVED WHERE id = ? AND status = AVAILABLE
→ 影响行数 1：抢占成功，继续插入预约（同一事务，插入失败整体回滚）
→ 影响行数 0：已被抢，返回"手慢了，该座位刚被别人约走"
```

理由：单体架构下数据库行级条件更新就是正确的防超卖方案（本质是乐观并发），Redis 分布式锁是多实例阶段的需求，提前引入只增加复杂度。checkIn（RESERVED→IN_USE）与各处释放同样条件化。

## 决策 3：整层禁用联动（新枚举 ADMIN_RELEASED=7）

`SeatServiceImpl.batchUpdateSeatStatus(roomId, status)`：当目标为 MAINTENANCE 时，先查该层全部活跃预约（join seat on room_id，status in 1,5），逐条置 ADMIN_RELEASED，再批量禁用座位。作废时通过 `WebSocketServer.sendToUser()` 给在线用户推 `{type:"reservationCancelled"}`（尽力而为，不在线者下次进首页自然可见）。恢复启用（→AVAILABLE）无逆向动作。

## 决策 4：Redis（spring-boot-starter-data-redis，Lettuce）

- 配置：`REDIS_HOST/REDIS_PORT/REDIS_PASSWORD` 环境变量；服务器 Redis 设 requirepass、仅监听 127.0.0.1
- **验证码**：`SET captcha:{key} {code} EX 300`；登录校验成功后 `DEL`（一次性）；替换原静态 Map（CaptureConfig 删除）
- **用户缓存**：`getCurrentUser()` 先查 `user:{id}`（JSON，TTL 30min），未命中查库回填；写路径（update/delete/resetPassword/改密）主动 `DEL`。管理员侧同理（`admin:{id}`）
- **降级策略**：所有 Redis 操作 try-catch，异常时验证码直接失败、用户缓存直查 DB——Redis 不可用不拖垮登录后的主流程

## 明确不做的

- 举报释放机制、MQ、分布式锁、聊天室-座位联动、测试基建
