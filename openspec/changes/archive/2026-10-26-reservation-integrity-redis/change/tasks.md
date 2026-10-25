# Tasks: reservation-integrity-redis

## 后端

- [x] T1 `ReservationStatus` 新增 `ADMIN_RELEASED(7, "管理员作废")`
  验证：编译通过
- [x] T2 `SeatDao.tryUpdateStatus(id, from, to)` 条件更新（返回 int）；`ReservationDao.findExpiredCheckedIn(now)`、`findActiveByRoomId(roomId)` 两个查询
  验证：编译通过 + SQL 人工核对
- [x] T3 `validateReservation` 修复 endTime 比较；`checkTimeoutReservations` 双扫描（TIMEOUT + 自动 checkout COMPLETED）
  验证：SQL 模拟过期 → 60s 内自动结算（部署后验收 1）
- [x] T4 `reserve()` 改条件抢占 + 影响行数判断；`checkIn/cancel/checkout` 的座位更新全部条件化
  验证：并发 curl ×10 抢同座仅 1 成功（部署后验收 2）
- [x] T5 `WebSocketServer.sendToUser(userId, type, msg)`：遍历房间会话定点推送
  验证：日志/客户端收到通知
- [x] T6 `SeatServiceImpl.batchUpdateSeatStatus` 整层禁用联动（作废预约 + 通知）
  验证：整层禁用后该层预约全部 ADMIN_RELEASED（验收 3）
- [x] T7 pom 加 spring-boot-starter-data-redis；`RedisConfig`（StringRedisTemplate 直接用，不定制序列化）；yml 加 redis 配置（环境变量）
  验证：启动无错
- [x] T8 验证码 Redis 化：`CaptureController` SET EX 300；`UserController.login` 校验后 DEL、错误即拒；删除 `CaptureConfig` 静态 Map
  验证：同 key 二次使用失败；5 分钟后失效（验收 4）
- [x] T9 `getCurrentUser/getCurrentAdmin` 缓存（user:{id}/admin:{id}，TTL 30min）+ 写路径失效（update/delete/resetPassword/改密）
  验证：日志显示缓存命中；改资料后立即读到新值（验收 5）

## 部署与验收

- [x] T10 服务器 Redis 设 requirepass + bind 127.0.0.1；`/etc/reading-room.env` 追加 REDIS_PASSWORD
- [x] T11 构建、部署、按 proposal 五条验收逐项验证并记录

## 验收记录（2026-10-26，5/5 通过）

1. ✅ 到期自动结算：已签到过期预约 60s 内 COMPLETED + 座位释放 + real_end_time 记录（顺带清掉了库存量 4 条陈年悬空预约、55 个悬空座位状态）
2. ✅ 并发防超卖：10 并发抢同座，1 成功 + 9 明确拒绝
3. ✅ 整层禁用联动：活跃预约 → ADMIN_RELEASED，座位 → MAINTENANCE
4. ✅ 验证码：TTL 299s，登录成功即删，重放被拒
5. ✅ 用户缓存：命中加速 + 写路径即时失效

## 过程中发现并修复的额外问题

- @Resource 按字段名注入 redisTemplate 与 Bean 类型冲突导致启动崩溃 → 改名 stringRedisTemplate
- 自定义 SQL select * 无列别名（未开驼峰映射）导致实体 seatId=null：**原 findTimeoutReservations 超时释放路径从未真正释放过座位**（存量老 bug），已全部加显式别名
