# Tasks: test-ci-hardening

- [x] T1 pom：spring-boot-starter-test(test) + spring-boot-starter-validation
- [x] T2 Reservation 实体 @NotNull（seatId/startTime/endTime，中文消息）；ReservationController.reserve 加 @Valid
- [x] T3 GlobalExceptionHandler 增补参数校验/缺参两类异常的友好返回
- [x] T4 RateLimiterInterceptor 改 tryAcquire + 429 JSON
- [x] T5 ReservationServiceImplTest（≥10 用例）
- [x] T6 UserServiceTest（BCrypt 三分支，≥4 用例）
- [x] T7 服务器 mvn test 全绿
- [x] T8 .github/workflows/ci.yml（backend test + frontend build）
- [x] T9 scripts/bench_reserve.py + 服务器执行 + docs/benchmark-2026-10-26.md
- [x] T10 DEVELOPMENT.md 详尽交接文档；架构文档/面试文档同步；部署验证；归档

## 验收记录（2026-10-26，4/4）

1. ✅ mvn test 18/18 全绿（ReservationServiceImplTest 13 + UserServiceTest 5；过程中修复 Mockito 嵌套打桩与 Result.success(String) 断言两处测试问题）
2. ✅ 缺 startTime → "预约开始时间不能为空"（不再是"系统异常"500）
3. ✅ 压测：同座竞争 20 并发 1 成功 / 各抢各座 20/20 成功 P95 74ms（docs/benchmark-2026-10-26.md）
4. ⏳ GitHub Actions：ci.yml 已入库，首次 push 后验证（两个 job：后端 mvn test + 前端 npm build）
