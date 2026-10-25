# Tasks: test-ci-hardening

- [ ] T1 pom：spring-boot-starter-test(test) + spring-boot-starter-validation
- [ ] T2 Reservation 实体 @NotNull（seatId/startTime/endTime，中文消息）；ReservationController.reserve 加 @Valid
- [ ] T3 GlobalExceptionHandler 增补参数校验/缺参两类异常的友好返回
- [ ] T4 RateLimiterInterceptor 改 tryAcquire + 429 JSON
- [ ] T5 ReservationServiceImplTest（≥10 用例）
- [ ] T6 UserServiceTest（BCrypt 三分支，≥4 用例）
- [ ] T7 服务器 mvn test 全绿
- [ ] T8 .github/workflows/ci.yml（backend test + frontend build）
- [ ] T9 scripts/bench_reserve.py + 服务器执行 + docs/benchmark-2026-10-26.md
- [ ] T10 DEVELOPMENT.md 详尽交接文档；架构文档/面试文档同步；部署验证；归档
