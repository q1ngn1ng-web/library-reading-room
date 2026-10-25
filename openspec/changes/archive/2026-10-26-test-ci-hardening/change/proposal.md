# Proposal: test-ci-hardening（测试、CI 与参数校验加固）

## 为什么

两次 SDD 变更后功能已验证，但：① 零自动化测试，回归全靠手测；② 缺参请求返回"系统异常"500（验收中实测：reserve 缺 startTime 直接 DB 约束异常）；③ 限流器 `acquire()` 阻塞式排队，过载时拖慢而非拒绝；④ 无 CI，push 后不知道构建/测试是否通过。

## 改什么

| 模块 | 变更 |
| --- | --- |
| testing | 预约状态机单元测试（Mockito）：reserve 抢占成败/重复预约、checkIn、checkout、validateReservation 三分支（超时/到期/已退座）、结算时座位被改的兜底；UserService 的 BCrypt 三分支（哈希命中/存量明文自动升级/密码错误） |
| api-validation | 引入 spring-boot-starter-validation；Reservation 必填字段 @NotNull；GlobalExceptionHandler 增补 MethodArgumentNotValidException / MissingServletRequestParameterException 友好提示 |
| rate-limiter | acquire() → tryAcquire()：过载返回 429 JSON，不再排队 |
| ci | GitHub Actions：push 触发后端 mvn test + 前端 npm build（双 job） |
| benchmark | 自研并发压测脚本 scripts/bench_reserve.py（服务器执行），产出可复现的并发抢座数据入 docs/ |

## 非目标

- CI 自动部署到服务器（需要用户在 GitHub 仓库添加 SSH secrets，步骤写入 DEVELOPMENT.md 待用户执行）
- JMeter 图形化脚本（当前 Python 脚本覆盖同等场景，可复现；JMeter 列为可选后续）
- 前端测试（Vue2 项目改造成本高，非本期重点）

## 验收

1. `mvn test` 全绿（≥15 个用例），覆盖上述场景
2. reserve 缺 startTime/seatId → 返回友好中文提示而非"系统异常"
3. 并发压测脚本在服务器跑通，数据落 docs/benchmark-*.md
4. GitHub push 后 Actions 两个 job 绿勾
