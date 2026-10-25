# Proposal: reservation-integrity-redis（预约完整性修复 + 并发防护 + Redis 引入）

## 为什么

业务场景分析（2026-10-26）确认了三类问题：

1. **预约终态保证缺失（P0，线上正在发生）**：`validateReservation` 判断"预约时段结束"时把 `endTime` 写成了 `timeoutAt`（`now.after(reservation.getTimeoutAt())`），且 `checkIn` 不更新 `timeoutAt`——导致**用户签到满 30 分钟后界面"当前预约"消失，但预约与座位在库中永久占用，且可再次预约第二个座位（重复占座）**。定时任务只扫未签到超时（status=1），永远不管已签到过期的预约。
2. **整层禁用无联动**：管理员整层禁用座位时只改座位状态，该层已有活跃预约悬空、占座用户无感知。
3. **预约接口并发超卖**：check-then-act 无锁，并发预约同一座位可重复成功。
4. **验证码存内存 Map**：无过期、无消费即删、只增不减（内存泄漏）。

## 改什么

| 模块 | 变更 |
| --- | --- |
| reservation-lifecycle | 修复 endTime 比较对象；定时任务双扫描（未签到超时→TIMEOUT / 已签到过期→自动 checkout COMPLETED）；所有座位状态变更改条件更新 |
| seat-lockdown | 整层禁用时作废该层活跃预约（新状态 ADMIN_RELEASED）并 WS 通知在馆用户 |
| reservation-concurrency | `reserve()` 改为条件 UPDATE 抢占（`where status=AVAILABLE`，判影响行数），并发只允许一人成功 |
| redis-services | 引入 Redis：验证码 5 分钟 TTL + 一次性消费；getCurrentUser 用户缓存（写路径主动失效） |

## 非目标

- 举报释放机制（用户设想的"监督-限时确认-释放"）：独立后续变更实施，本变更不做
- RabbitMQ、分布式锁（单体 + 数据库条件更新已正确防超卖，多实例部署时再引入）
- 测试与 CI（独立变更）
- 聊天室开放/禁言与座位禁用的联动（聊天室有独立管理开关）

## 验收（部署后 http://124.223.80.145）

1. 签到后等 30+ 分钟（或 SQL 把 timeout_at 改到过去模拟），当前预约**不再消失**；把 end_time 改到过去后 ≤60s，预约自动 COMPLETED、座位回 AVAILABLE、用户被踢出聊天室
2. 并发 10 个请求抢同一座位：仅 1 个成功，其余收到明确失败提示
3. 整层禁用后：该层活跃预约全部变 ADMIN_RELEASED，在线用户收到 WS 通知
4. 验证码 5 分钟后失效；同一验证码二次使用失败；Redis 中 key 自动过期
5. 登录后访问受保护接口：第二次起用户信息来自 Redis 缓存（日志可证），改资料后缓存即时失效
