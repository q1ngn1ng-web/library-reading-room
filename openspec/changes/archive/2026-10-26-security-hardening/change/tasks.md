# Tasks: security-hardening

> 每项 ≤2h，完成后勾选；部署验证通过后整个 change 归档到 changes/archive/。

## 后端

- [x] T1 引入 `spring-security-crypto` 依赖；新增 `PasswordEncoderConfig`（BCryptPasswordEncoder Bean）
  验证：`mvn compile` 通过
- [x] T2 `UserService.login` / `AdminService.login` 改造：按用户名查询 + BCrypt matches + 存量明文防御性兜底分支
  验证：单测/手工——明文账号首登成功且库里变 `$2a$`；二次登录走 matches 分支
- [x] T2b 启动迁移器 `PasswordMigrationRunner`（ApplicationRunner）：启动时把 user/admin 表所有非 `$2a$` 密码行就地哈希化
  验证：导入种子 SQL → 启动服务 → 全表密码均为 `$2a$` 前缀
- [x] T3 `UserService.add`、`resetPassword`、`AdminController` 相关写入口全部改存 BCrypt 哈希
  验证：注册新号 → 库中为哈希；管理员重置密码 → 返回明文给前端但库存哈希
- [x] T4 `application.yml` 加 `jwt.secret/expire-hours`；启动 fail-fast 校验（生产环境禁默认密钥）
  验证：故意不配 JWT_SECRET 启动（SERVER_ADDRESS=127.0.0.1 时）→ 启动报错
- [x] T5 `JwtTokenUtils` 重写：genToken(userId, role)；新增 `verifyToken(token)` 静态方法（拦截器与 WS 共用）
  验证：生成→验签→篡改一位→抛 JWTVerificationException
- [x] T6 `JwtInterceptor` 改造：verifyToken 验签 + attribute 注入，删除查库验签路径；`getCurrentUser/getCurrentAdmin` 改从 attribute 取 id
  验证：带 token 访问受保护接口 200；篡改 token 401；无 token 401
- [x] T7 `WebSocketServer.onOpen` 改造：URL 只留 roomId+token；身份/角色/昵称/头像全部服务端反查；管理员判定改用 token role
  验证：无 token 连接被拒（CloseReason）；有效 token 正常进房
- [x] T8 `WebSocketServer.onMessage` 改造：忽略客户端 username/userId，从会话反查
  验证：客户端伪造 username 发消息 → 广播中的名字仍是服务端侧真实身份
- [x] T9 `pom.xml` 无多余依赖、删除不再使用的 guava-rate-limiter 之外旧代码路径（如 JWT 旧签名方法调用点全清）
  验证：全局搜索 `genToken` 旧签名调用为 0

## 前端

- [x] T10 `websocket.js`：URL 构造改为 `/imserver/${roomId}?token=`（token 从 localStorage 按 role 取）
  验证：进聊天室正常收发；F12 看 WS URL 不含 userId
- [x] T11 改密成功回调里清除 token 并跳转登录页（补偿"改密失效 token"特性）
  验证：改密后自动回登录页，旧 token 请求 401

## 数据与文档

- [x] T12 `library_room.sql` 头部注释补充"密码首登自动 BCrypt 化"说明；README 已知问题清单勾掉已完成两项
- [x] T13 `docs/architecture.md` 调用链一/三更新为新认证流程

## 部署验证（http://124.223.80.145）

- [x] T14 `/etc/reading-room.env` 追加 `JWT_SECRET=<openssl rand -hex 32>`；bundle 同步 → 重建前后端 → 重启服务
- [x] T15 按 proposal 验收清单逐条验证（4 项），结果记录到本文件末尾

## 验收记录（2026-10-26，http://124.223.80.145）

1. ✅ 库内密码 35(user)+4(admin) 全部 $2a$ 哈希（启动迁移器生效）
2. ✅ 登录走 BCrypt matches，响应不回传密码
3. ✅ 篡改 token → 拒绝；错误密码 → 拒绝
4. ✅ /chatRoom /chat /notice 拦截覆盖生效（无 token 拒绝）
5. ✅ user token 访问 /admin/** → 拒绝
6. ✅ WS 无 token → Close("缺少token")
7. ✅ 旧式攻击 URL（伪造 userId+role=admin）→ 404，当日 PoC 已堵死
8. ✅ admin token 进房正常，消息伪造 username 被服务端身份覆盖
9. ✅ user token 无预约 → Close("无有效预约")，联动机制回归通过
10. ✅ 管理员豁免定时踢人（原实现会误踢管理员的缺陷一并修复）
