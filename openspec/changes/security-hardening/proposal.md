# Proposal: security-hardening（安全三件套）

## 为什么

当前存在三个 P0 级安全问题（详见 README 已知问题、docs/architecture.md）：

1. **密码明文存储与比对**：`findByNameAndPassword` 明文查库，数据库泄露 = 全部账号沦陷
2. **JWT 以用户密码为签名密钥**：`HMAC256(密码)` 签发 token；密码即密钥，泄露可伪造任意用户 token；且每次验签都要查一次数据库
3. **WebSocket 身份伪造**：连接 URL 中的 `userId`/`role` 参数可被客户端任意伪造，`?role=admin` 可绕过"有预约才能进聊天室"的核心机制；消息中的 username/userId 也信任客户端

## 改什么

| 模块 | 变更 |
| --- | --- |
| auth-password | 密码存储改 BCrypt 哈希；存量明文在首次登录时自动升级（无感迁移，不需要数据脚本） |
| auth-token | JWT 改用独立服务端密钥（环境变量注入）；payload 携带 userId + role；拦截器验签不再查库 |
| ws-auth | WS 握手改 token 鉴权；消息中的身份字段一律由服务端会话反查，不信任客户端 |

## 非目标（本变更不做）

- 不引入 Spring Security 全家桶（只用 spring-security-crypto 的 BCrypt 工具，保持本科生可理解的拦截器体系）
- 不做 token 黑名单 / refresh token（记录为后续项）
- 不改验证码、限流、参数校验（进 Spec 2/3）
- 不动前端整体架构，只改 websocket.js 与登录相关调用

## 影响

- 前后端必须同步部署（WS 协议变更，不兼容旧前端）——由现有 bundle 部署流程保证
- 丢失"改密码自动使 token 失效"的副作用特性（原特性源于密码做密钥），以"改密后前端强制登出"补偿
- 部署需在 /etc/reading-room.env 追加 JWT_SECRET

## 验收（部署后在 http://124.223.80.145 验证）

1. 旧明文账号首次登录成功，且库中密码字段变为 `$2a$` 哈希；再次登录正常
2. 篡改 token 任意一位 → 401
3. 不带 token 连 WS → 拒绝；伪造 `role=admin` 的旧式 URL → 拒绝
4. 正常用户有预约 → 能进聊天室发言；预约超时 → 60s 内被踢出（回归验证联动机制）
