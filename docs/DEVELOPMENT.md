# DEVELOPMENT.md —— 开发交接与运维文档

> **本文档的读者**：未来接手本项目的开发者或 AI 助手。它假设你没有任何 prior 对话上下文，包含从 2026-10 起的全部工程决策、变更记录、踩坑档案与运维手册。项目代码本身、`docs/architecture.md`（架构图）、`openspec/`（规格与变更档案）、git log 是另外三块拼图，五者合起来是完整事实源。
>
> 最后更新：2026-10-26（Spec 3 test-ci-hardening 完成后）

---

## 1. 项目一页纸

- **是什么**：图书馆阅览室管理系统——座位预约 + 楼层聊天室，核心特色「物理身份-虚拟权限」联动（有有效座位预约才能进对应楼层聊天室发言，过期自动踢出）。个人全栈项目（本科毕设起点，2026-10 起工程化加固）。
- **线上**：http://124.223.80.145 （Nginx:80 静态 + `/api` 反代）。演示账号：管理员 `admin/123456`，用户 `20230001/123456`。
- **仓库**：https://github.com/q1ngn1ng-web/library-reading-room（分支 `main`）
- **技术栈**：Java 17 / Spring Boot 2.7 / MyBatis(tk.mybatis) / MySQL 8 / Redis 7 / 原生 WebSocket / Vue2 + ElementUI / Nginx / systemd
- **规模**：后端 ~55 Java 文件（含测试），前端 34 文件，7 张表，9 个 Controller
- **求职定位**：简历第三项目，证明 **Java 后端工程能力**（与作者的 shopkeeper-RAG、Trainer-Agent 两个 AI 项目互补，目标岗位 Agent 开发）

## 2. 三地代码同步模型（重要！）

代码存在于三个地方，**本地 Windows 是编辑源，服务器是构建+运行源，GitHub 是存档源**：

```
F:\Vue\learn_project\chat-room-project   ← 编辑（无本地 JDK/Node，不跑代码）
        │ git bundle → scp
        ▼
ubuntu@124.223.80.145:/opt/reading-room  ← 构建（mvn / npm）、运行、验收
        │ git push（SSH，密钥已绑定 GitHub 账号 q1ngn1ng-web）
        ▼
github.com/q1ngn1ng-web/library-reading-room
```

**为什么这么绕**：本机 Windows 的 git 凭据属于另一个账号（qingzheng-web），不能直接 push；服务器直连 GitHub 的 HTTPS 443 不通（大陆云网络），**SSH 22 通**且服务器密钥已绑定 q1ngn1ng-web → 一切推送走服务器 SSH remote。

### Redeploy 完整流程（改完代码后照抄）

```bash
# ① 本地（Git Bash, 项目根）
git add -A && git commit -m "feat(xxx): 描述"
git bundle create /tmp/rr.bundle main
scp -q /tmp/rr.bundle ubuntu@124.223.80.145:/tmp/

# ② 服务器
ssh ubuntu@124.223.80.145
cd /opt/reading-room && git pull --ff-only /tmp/rr.bundle main && rm /tmp/rr.bundle
# 后端有改动：
cd springboot && mvn -q test && mvn -q package -DskipTests && sudo systemctl restart reading-room
# 前端有改动：
cd ../vue && npm run build        # dist 即刻生效（Nginx 直接指向它）
# ③ 推 GitHub（在服务器上）
git push origin main
```

**验收纪律**：每次部署后必须按对应 openspec change 里的验收清单逐项验证并记录（历史验收全在 `openspec/changes/archive/*/tasks.md` 末尾）。curl 验证模板：

```bash
# 登录拿 token（管理员）
curl -s http://localhost/api/admin/login -X POST -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"123456"}'
# 手工签发用户 token（测试用，密钥在 /etc/reading-room.env 的 JWT_SECRET）
# 参照 scripts/bench_reserve.py 里的 jwt_token() 函数
```

## 3. 生产环境全貌（服务器 ubuntu@124.223.80.145）

| 组件 | 位置/方式 | 要点 |
| --- | --- | --- |
| 后端 | systemd 服务 `reading-room` | `/etc/systemd/system/reading-room.service`；User=ubuntu；WorkingDirectory=/opt/reading-room/springboot（**上传目录 file/ 依赖它**）；EnvironmentFile=/etc/reading-room.env；Restart=on-failure；监听 127.0.0.1:9090（不暴露公网） |
| Nginx | :80 | `/etc/nginx/sites-available/reading-room`：静态 `vue/dist` + `location /api/` 反代（**含 WebSocket Upgrade 的 map**，map 必须在 server 块外）+ gzip |
| MySQL 8 | 系统服务 | 库 `library_room`；专用账号 `readingroom`（仅此库权限）；密码在 env 文件；root 走 sudo mysql（auth_socket） |
| Redis 7 | 系统服务 | **已设 requirepass**（密码在 env 文件）+ 默认仅监听 127.0.0.1；用途：验证码 TTL、用户/管理员信息缓存 |
| 密钥 | `/etc/reading-room.env`（600） | DB_USERNAME / DB_PASSWORD / DB_HOST / DB_PORT / DB_NAME / SERVER_ADDRESS=127.0.0.1 / JWT_SECRET / REDIS_PASSWORD |

**环境变量速查（application.yml 全部走 `${ENV:默认值}`）**：`DB_USERNAME`(root)、`DB_PASSWORD`(空)、`DB_HOST/DB_PORT/DB_NAME`、`SERVER_ADDRESS`(0.0.0.0，生产必须 127.0.0.1)、`JWT_SECRET`(**必填，空则启动失败——fail-fast**)、`REDIS_HOST/PORT/PASSWORD`。

### 常用运维命令

```bash
systemctl status reading-room          # 服务状态
sudo journalctl -u reading-room -f     # 跟日志（Ctrl+C 退出）
sudo journalctl -u reading-room --since "10 minutes ago" -o cat | grep -v "^\tat "   # 近10分钟日志去堆栈
sudo systemctl restart reading-room    # 重启
sudo mysql library_room -e "SQL"       # 查库
redis-cli -a $(sudo grep ^REDIS_PASSWORD= /etc/reading-room.env | cut -d= -f2) --no-auth-warning keys '*'   # 查 Redis
sudo ss -tlnp | grep -E '9090|6379|3306'   # 端口
curl -s -o /dev/null -w "%{http_code}" http://124.223.80.145/    # 外网探活（从本地跑）
```

## 4. 核心模块实现要点（配合 docs/architecture.md 的图看）

### 4.1 认证（common/JwtTokenUtils + JwtInterceptor）
- JWT 用**独立服务端密钥**（JWT_SECRET）HMAC256，payload：audience=userId、claim `role`=user/admin，4h 有效
- 拦截器**纯本地验签零 DB 查询**；`/admin/**` 路径强制 role=admin（拒绝 user token）
- 拦截路径注册在 `WebConfig`——**必须与 Controller @RequestMapping 大小写完全一致**（历史坑：`/chatroom` vs `/chatRoom` 导致裸奔，见踩坑档案 #4）
- 当前用户信息（getCurrentUser/getCurrentAdmin）走 **Redis 缓存**（key `user:{id}`/`admin:{id}`，TTL 30min，JSON）；**写路径主动失效**（UserService/AdminService 的 update/delete/resetPassword 调 JwtTokenUtils.evictCache）；Redis 挂了自动降级直查 DB
- 密码 **BCrypt**（spring-security-crypto，只用编码器不引入框架）；存量明文由 `PasswordMigrationRunner`（启动时）与登录分支（兜底）双路径自动哈希化；改密必须服务端 checkOldPassword 复核

### 4.2 预约状态机（service/impl/ReservationServiceImpl）
- 双状态线：`reservation.status`（1 SUCCESS→5 CHECKED_IN→6 COMPLETED / 3 TIMEOUT / 4 CANCELLED / **7 ADMIN_RELEASED**）与 `seat.status`（0 AVAILABLE/1 RESERVED/2 IN_USE/3 MAINTENANCE）同步迁移，全程 @Transactional
- **终态保证**：任何预约必达终态。兜底 = `@Scheduled(60s)` 双扫描：未签到超时（status=1 & timeout_at<now → TIMEOUT）/ 已签到到期（status=5 & end_time<now → 自动 checkout COMPLETED）；另有读接口惰性结算（validateReservation autoSettle）
- **并发防超卖**：所有 seat.status 变更走 `SeatDao.tryUpdateStatus(id, from, to)` 条件更新判影响行数（乐观并发）。reserve 先抢占后插预约，插入失败事务回滚；结算释放遇到座位已被管理员禁用时跳过覆盖
- **整层禁用联动**（SeatServiceImpl.batchUpdateSeatStatus）：目标为 MAINTENANCE 时先作废该层活跃预约（→7）并 `WebSocketServer.sendToUser` 通知，再批量禁座

### 4.3 WebSocket（common/WebSocketServer）
- 端点 `/imserver/{roomId}?token=<JWT>`（**URL 不含任何身份参数**）；onOpen 验签→DB 反查昵称/头像→房间校验（存在/开放/未满/未禁言）→非 admin 需有效预约→注册会话
- 双层 `ConcurrentHashMap<roomId, Map<sessionId, Dict>>`；消息身份一律从会话反查（客户端 username/userId 被忽略）
- 每分钟踢人任务：遍历在线用户校验预约，**admin 角色豁免**（历史坑：曾误踢管理员）
- 注意：`@ServerEndpoint` 每连接实例化 → Spring Bean 只能静态注入（afterPropertiesSet 模式），这是保留 static hack 的原因（已知技术债，多实例化时需重构）

### 4.4 Redis（spring-boot-starter-data-redis，Lettuce）
- 验证码：`captcha:{key}` SETEX 300，登录成功即 DEL（一次性），Redis 不可用时登录明确报错
- 用户缓存：见 4.1
- **注入坑**：`@Resource` 按字段名注入，字段名必须叫 `stringRedisTemplate`（叫 redisTemplate 会命中 RedisTemplate 类型的同名 Bean → 启动崩溃，踩坑档案 #5）

### 4.5 限流（RateLimiterInterceptor）
Guava 令牌桶全局 50 QPS，`tryAcquire()` **快速失败返回 429 JSON**（原为阻塞 acquire，已修）。

### 4.6 测试（springboot/src/test）
- `ReservationServiceImplTest`（13 用例）：抢占成败/重复预约/签到退座/validateReservation 三分支/座位被改兜底
- `UserServiceTest`（5 用例）：BCrypt 命中/明文自动升级/密码错误/用户不存在/旧密码校验
- 跑法：服务器上 `cd springboot && mvn test`；CI（.github/workflows/ci.yml）push 即跑后端测试 + 前端构建
- **Mockito 两个坑**（踩坑档案 #6/#7）：thenReturn 参数里不得调用 spy 方法；`Result.success(String)` 的字符串在 **data** 字段不在 msg

## 5. openspec SDD 流程（本项目的开发纪律）

```
openspec/
├── config.yaml                 # schema 与项目上下文（含"本科生难度、避免过度设计"约束）
├── specs/<capability>/spec.md  # 已生效规格（需求 + 当/则场景）
└── changes/
    ├── <进行中的变更>/          # proposal.md(为什么/改什么/非目标/验收) + design.md(决策) + tasks.md(≤2h任务+验收勾选) + specs/(增量)
    └── archive/<日期>-<名>/     # 已完成归档（tasks.md 末尾附验收记录）
```

**流程**：写 proposal/design → （重要决策点问用户）→ 实现 → 服务器部署 → **按 proposal 验收清单逐项验证并记录到 tasks.md** → 归档 + 规格合入 specs/ → commit push。
已完成归档：`2026-10-26-security-hardening`（验收 10/10）、`2026-10-26-reservation-integrity-redis`（5/5）、`test-ci-hardening`。

## 6. 变更档案摘要（详见 openspec/changes/archive/）

| 变更 | 一句话 | 验收 |
| --- | --- | --- |
| security-hardening | BCrypt 密码（启动迁移器全量哈希化）、JWT 独立密钥+角色声明+fail-fast、WS token 握手鉴权+服务端身份反查；顺带修复：改密原密码校验形同虚设、拦截路径缺口(/chatRoom /chat /notice 裸奔)、定时踢人误踢管理员 | 10/10（含 PoC 复验：伪造身份攻击被 404/Close 拒绝） |
| reservation-integrity-redis | endTime 误比 timeoutAt 终态 bug、双扫描定时结算、条件更新防超卖、整层禁用联动、Redis 引入（验证码+缓存）；顺带修复：select * 无列别名致 seatId=null（**原超时释放从未真正生效**，清理了 55 个悬空座位/4 条悬空预约） | 5/5（含 10 并发抢座 1 成功） |
| test-ci-hardening | 18 个单元测试、参数校验+友好报错、限流快速失败、GitHub Actions CI、压测脚本+报告 | 测试全绿；压测数据见 docs/benchmark-2026-10-26.md |

## 7. 踩坑档案（每条都是真实代价换来的，改动相关代码前先读）

1. **WebSocket URL 身份可伪造（安全）**：身份/角色放 URL 参数=零认证。实测无凭证伪造 userId+role=admin 即可进房冒充他人发言。→ 身份必须来自 token 验签 + 服务端反查。**教训：客户端传的一切皆可编辑。**
2. **JWT 用用户密码做签名密钥**：DB 泄露即可伪造任意 token，且每请求查库验签。→ 独立密钥 + fail-fast。
3. **validateReservation 误比 timeoutAt**：一行笔误导致"签到 30 分钟后界面预约消失、座位永久占用、可重复占座"。**教训：时间语义字段多的业务，字段命名要防串（timeoutAt vs endTime），边界场景要专门验收。**
4. **拦截路径大小写不一致**：注册 `/chatroom/**` 而控制器是 `/chatRoom` → 整组接口无保护。**教训：安全配置要与路由表做一致性核对（写个对照测试更好）。**
5. **@Resource 按字段名注入**：字段名 redisTemplate 命中 RedisTemplate Bean → BeanNotOfRequiredTypeException 启动崩溃。**教训：@Resource 按名、@Autowired 按类型；注入 Spring 自动配置的 Bean 时字段名要贴 Bean 名。**
6. **MyBatis select * 无驼峰映射**：项目未开 mapUnderscoreToCamelCase，`select *` 的实体字段静默为 null（无异常！）→ 定时释放座位从未生效数月。**教训：自定义 SQL 一律显式列别名（项目约定已写入 ReservationDao 注释）。**
7. **Mockito 嵌套打桩**：thenReturn(参数里调用 spy 方法) → UnfinishedStubbing。**教训：先算好值再打桩。**
8. **systemd 工作目录**：默认 / → 相对路径上传文件落到根目录。已设 WorkingDirectory。
9. **腾讯云安全组**：9090 外网不通 → 改同源 /api 反代反而获得更安全的架构。**教训：限制常常逼出更好的设计。**
10. **nginx WebSocket 反代**：需要 `map $http_upgrade $connection_upgrade`（http 上下文，即 site 文件 server 块之外）+ proxy_http_version 1.1。

## 8. 未完成事项与路线（按优先级）

1. **CI 自动部署**（需用户在 GitHub 仓库 Settings→Secrets 添加 `SSH_HOST`/`SSH_KEY`，workflow 已规划但未启用——这是唯一需要用户手动操作的事）
2. **举报释放机制**（用户业务设想）：他人举报占座 → WS 通知占座人 → 15 分钟内点"我还在"确认 → 超时自动释放+作废。proposal 草稿见 openspec/changes/report-release/proposal.md
3. **LLM 智能助手**：聊天室接入 LLM 查空座/代约/公告问答（Spring AI + Function Calling 调现有 REST）。proposal 草稿见 openspec/changes/llm-assistant/proposal.md
4. RabbitMQ 事件化、Docker Compose 化、登录失败次数锁定、token 缩短+refresh、WS 集群化（Redis Pub/Sub）——均已论证，时机按需
5. **简历**（用户铁律）：**以上业务全部完成后**才用 for-work 里的 resume skill 写简历，**只写本项目条目**，绝不动用户已有的 shopkeeper/Trainer 部分

## 9. 给接手 AI 的注意事项

- 用户不改代码时不轻易提问；破坏性操作先说理由；**验收必须实测留痕**
- 每轮结束把关键状态写入 ZCode 记忆（`C:\Users\青峥\.zcode\cli\memories\projects\chat-room-project-*\memory\`），用户可能换模型续作——**本文件 + 记忆目录就是交接面**
- 面试准备文档在 `interview-prep/`（**已 gitignore，绝不入库、不上 GitHub**），服务器副本在 `/home/ubuntu/for-work/02-面试-阅览室/`
- 用户求职全案在服务器 `/home/ubuntu/for-work/`（01-简历/02-面试/03-项目导学 + asu-skills），AGENTS.md 里有 skill 路由规则
- 本机 Windows 路径：项目 `F:\Vue\learn_project\chat-room-project`；git-backups（历史清洗备份）`F:\Vue\learn_project\git-backups\`
