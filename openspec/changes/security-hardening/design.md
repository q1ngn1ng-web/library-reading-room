# Design: security-hardening

## 决策 1：BCrypt 迁移 = 启动迁移器 + 首登升级双保险

- 依赖只加 `spring-security-crypto`（不含 Security 框架本体），`BCryptPasswordEncoder` 注册为 Bean
- **启动迁移器**（保证"数据库里一定要变"）：`ApplicationRunner` 在启动时扫描 user/admin 表，凡非 `$2a$` 前缀的密码行就地 encode 回写——服务一旦重启，库里全部为哈希，不依赖任何人登录
- 登录流程：按用户名查 → BCrypt matches；保留存量明文比对分支作为防御性兜底（应对"导入种子数据后未重启"的窗口期），命中即自动升级
- 修改点：`UserService.login`、`AdminService.login`、`UserService.add/resetPassword`（新密码一律 encode 后入库）
- 种子 SQL 文件保持明文演示密码（可读性好），首次部署重启后由迁移器全部哈希化

**取舍**：过渡期库中新旧格式并存，靠前缀判断；相比"停机跑迁移脚本"，零运维成本且用户无感，代价是登录逻辑多一个分支（约 10 行）。

## 决策 2：JWT 独立密钥 + 角色声明

- `application.yml` 增加 `jwt.secret: ${JWT_SECRET:dev-only-secret-change-me}`、`jwt.expire-hours: 4`
- `JwtTokenUtils.genToken(userId, role)`：`HMAC256(secret)` 签名，Claim 放 `userId` + `role`（user/admin）
- `JwtInterceptor`：纯密钥验签（不再查库、不再用密码），解出 `userId/role` 放入 request attribute
- `getCurrentUser()/getCurrentAdmin()`：从 attribute 取 id 再查库（保留存在性校验，防止已删用户持旧 token）

**取舍**：
- 失去"改密自动失效 token"副作用 → 补偿：改密接口返回后前端清除本地 token 跳登录页
- 验签从每请求 1 次 DB 查询 → 0 次（这一条本身即可作为简历/面试点）
- JWT_SECRET 为空或等于默认值且非 dev 环境 → 启动直接失败（fail-fast，防止生产裸奔默认密钥）

## 决策 3：WebSocket 握手 token 鉴权 + 服务端身份反查

> 依据：2026-10-26 在生产服务器实测复现攻击——无凭证、无预约、伪造 userId=9999 并自称 `role=admin`，服务器返回 101 放行，并以任意伪造身份（张三/user_id=1）发言落库成功。旧协议的 `userId/avatar/role` 全部是客户端"自述"，不构成任何认证。

- URL 变更：`/imserver/{roomId}?token=<JWT>`（userId/avatar/role 全部从 URL 移除）
- `onOpen`：用与拦截器同一套验签逻辑解 token → userId/role → 查库取昵称头像 → 存入房间会话 Dict
  - 原"管理员跳过预约校验"逻辑保留，但 role 来源改为 token 声明（不可伪造）
- `onMessage`：忽略客户端传入的 username/userId，一律从 `roomUsers.get(roomId).get(sessionId)` 反查
- 前端 `websocket.js`：构造 URL 改为 `baseURL + /imserver/${roomId}?token=${localStorage 对应 token}`
- 为什么 token 放 URL 参数：浏览器 WebSocket API 不支持自定义请求头（与 REST 的关键差异），query 传 token 是业界通行做法；缓解项：token 短有效期（当前 4h，Spec 2 讨论缩短）
- 兼容性：协议破坏性变更，前后端同次部署（现有 bundle 流程天然满足）

## 决策 4：验签逻辑收敛

`JwtInterceptor` 与 `WebSocketServer.onOpen` 共用一个静态工具方法 `verifyToken(token) → {userId, role}`，避免两处各写一套（原代码就是两套逻辑各判各的）。

## 明确不做的（防止范围蔓延）

- 不做 token 吊销/黑名单、不做双 token 体系
- 不做登录失败次数锁定（进 Spec 2 随 Redis 一起）
- 不动 CORS/限流/验证码
