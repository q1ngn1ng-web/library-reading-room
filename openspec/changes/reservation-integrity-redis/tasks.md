# Tasks: reservation-integrity-redis

## 后端

- [ ] T1 `ReservationStatus` 新增 `ADMIN_RELEASED(7, "管理员作废")`
  验证：编译通过
- [ ] T2 `SeatDao.tryUpdateStatus(id, from, to)` 条件更新（返回 int）；`ReservationDao.findExpiredCheckedIn(now)`、`findActiveByRoomId(roomId)` 两个查询
  验证：编译通过 + SQL 人工核对
- [ ] T3 `validateReservation` 修复 endTime 比较；`checkTimeoutReservations` 双扫描（TIMEOUT + 自动 checkout COMPLETED）
  验证：SQL 模拟过期 → 60s 内自动结算（部署后验收 1）
- [ ] T4 `reserve()` 改条件抢占 + 影响行数判断；`checkIn/cancel/checkout` 的座位更新全部条件化
  验证：并发 curl ×10 抢同座仅 1 成功（部署后验收 2）
- [ ] T5 `WebSocketServer.sendToUser(userId, type, msg)`：遍历房间会话定点推送
  验证：日志/客户端收到通知
- [ ] T6 `SeatServiceImpl.batchUpdateSeatStatus` 整层禁用联动（作废预约 + 通知）
  验证：整层禁用后该层预约全部 ADMIN_RELEASED（验收 3）
- [ ] T7 pom 加 spring-boot-starter-data-redis；`RedisConfig`（StringRedisTemplate 直接用，不定制序列化）；yml 加 redis 配置（环境变量）
  验证：启动无错
- [ ] T8 验证码 Redis 化：`CaptureController` SET EX 300；`UserController.login` 校验后 DEL、错误即拒；删除 `CaptureConfig` 静态 Map
  验证：同 key 二次使用失败；5 分钟后失效（验收 4）
- [ ] T9 `getCurrentUser/getCurrentAdmin` 缓存（user:{id}/admin:{id}，TTL 30min）+ 写路径失效（update/delete/resetPassword/改密）
  验证：日志显示缓存命中；改资料后立即读到新值（验收 5）

## 部署与验收

- [ ] T10 服务器 Redis 设 requirepass + bind 127.0.0.1；`/etc/reading-room.env` 追加 REDIS_PASSWORD
- [ ] T11 构建、部署、按 proposal 五条验收逐项验证并记录
