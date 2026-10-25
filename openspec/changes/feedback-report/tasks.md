# Tasks: feedback-report

- [ ] T1 feedback 表 DDL（追加至 library_room.sql + 线上建表）
- [ ] T2 Feedback 实体 + FeedbackDao（分页联表查询带昵称 @Transient）
- [ ] T3 FeedbackController：POST /feedback（token 取举报人 + Redis 60s 防滥用 + WS 通知管理员）、GET /feedback/my、GET /admin/feedback/list、PUT /admin/feedback/handle
- [ ] T4 WebSocketServer.sendToAdmins()
- [ ] T5 WebConfig 拦截 /feedback/**
- [ ] T6 前端 api/feedback.js + ChatRoom.vue 消息举报弹窗
- [ ] T7 管理端 FeedbackManagement.vue + 路由 + Dashboard 入口
- [ ] T8 部署验收 4 项 + 归档 + 面试文档同步（03/90 补"举报已实现"）
