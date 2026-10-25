# 阅览室管理系统

基于 **Spring Boot + Vue** 前后端分离的图书馆阅览室管理系统：**座位预约 + 楼层聊天室**。

核心特色是「**物理身份 - 虚拟权限**」联动机制：将座位预约状态与聊天室访问权限动态绑定——读者只有持有有效预约（或已签到占用）的座位，才能进入对应楼层的聊天室发言；预约过期或退座后，连接会被服务端主动断开。以此实现图书馆物理空间资源与线上交流行为的双重约束。

## 功能特性

**用户端**
- 注册 / 登录（JWT + 验证码），个人资料与头像管理
- 多楼层座位分布查看、座位预约、签到、退座
- 预约超时自动释放（30 分钟未签到视为超时）
- 楼层聊天室：多房间消息隔离、匿名发言、心跳保活、断线重连
- 公告查看、个人预约历史

**管理端**
- 数据看板（在线用户数、活跃聊天室、预约统计）
- 用户管理（增删查、重置密码）、座位管理（单个/整层禁用）
- 聊天室管理（开放/关闭、禁言、违规消息删除）
- 公告发布与管理

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 2.7、MyBatis（XML + 通用 Mapper）、PageHelper |
| 实时通信 | 原生 WebSocket（`@ServerEndpoint` 多房间会话管理） |
| 认证 | JWT（java-jwt）+ 拦截器双角色校验、easy-captcha 验证码 |
| 其他 | Guava RateLimiter 接口限流、`@Scheduled` 定时任务、全局异常处理 |
| 前端 | Vue 2.6、Vue Router、Vuex、Element UI、Axios |
| 数据库 | MySQL 8.0 |

## 项目结构

```
├── library_room.sql          # 数据库初始化脚本（含演示数据）
├── springboot/               # 后端
│   └── src/main/java/com/example
│       ├── controller/       # 接口层
│       ├── service/          # 业务层（含定时任务）
│       ├── dao/              # 数据访问层（MyBatis）
│       ├── entity/           # 实体与枚举
│       └── common/           # JWT、WebSocket、限流、CORS 等基础组件
└── vue/                      # 前端（Vue2 + Element UI）
    └── src
        ├── views/            # 用户端 / 管理端页面
        ├── store/            # Vuex（聊天、座位模块）
        └── utils/            # axios 封装、WebSocket 封装（心跳/重连）
```

## 快速开始

环境要求：JDK 17+、Node 14+、MySQL 8.0+

```bash
# 1. 初始化数据库
mysql -uroot -p -e "CREATE DATABASE library_room DEFAULT CHARACTER SET utf8mb4"
mysql -uroot -p library_room < library_room.sql

# 2. 启动后端（敏感配置通过环境变量注入；JWT_SECRET 未配置会拒绝启动）
cd springboot
DB_USERNAME=root DB_PASSWORD=你的密码 JWT_SECRET=本地开发随意一串字符 mvn spring-boot:run

# 3. 启动前端
cd ../vue
npm install
npm run serve        # 默认 http://localhost:8080
```

| 环境变量 | 说明 | 默认值 |
| --- | --- | --- |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | 数据库地址/端口/库名 | `localhost` / `3306` / `library_room` |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号/密码 | `root` / 空 |
| `JWT_SECRET` | JWT 签名密钥（**必填，未配置拒绝启动**） | 无 |

**演示账号**（密码均为 `123456`）：管理员 `admin`，普通用户 `20230001`

## 已知问题与 Roadmap

本项目正在持续重构加固，当前已知的技术债（也是接下来的迭代计划）：

- [x] **安全加固**（2026-10 `security-hardening` 变更完成）：密码 BCrypt 哈希存储（启动时自动迁移存量明文）；JWT 改独立服务端密钥（`JWT_SECRET` 环境变量，未配置拒绝启动）；WebSocket 握手改 token 鉴权，身份/角色/昵称/头像全部服务端反查，消除 URL 参数伪造
- [ ] **并发修复**：座位预约接口存在 check-then-act 竞态（并发下同一座位可被重复预约），计划引入 Redis 分布式锁 + 数据库乐观锁
- [ ] **Redis 引入**：验证码、登录态缓存、座位状态缓存、聊天室在线人数
- [ ] **RabbitMQ**：预约/签到/退座事件异步化，替代同步广播
- [ ] **测试与压测**：预约全流程单元测试 + JMeter 并发压测报告
- [ ] **部署工程化**：Dockerfile + docker-compose（MySQL/Redis/后端/Nginx）、GitHub Actions CI
- [ ] **AI 助手**：聊天室接入 LLM，支持自然语言查座/约座（Spring AI + Function Calling）

## 历史说明

仓库内含 2025 年 2 月至 5 月的完整开发历史（`springboot/` 与 `vue/` 两条开发线合并而来）。
