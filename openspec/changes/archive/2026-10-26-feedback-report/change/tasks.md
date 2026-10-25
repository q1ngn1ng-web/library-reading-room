# Tasks: feedback-report

- [x] T1 feedback 表 DDL（追加至 library_room.sql + 线上建表）
- [x] T2 Feedback 实体 + FeedbackDao（分页联表查询带昵称 @Transient）
- [x] T3 FeedbackController：POST /feedback（token 取举报人 + Redis 60s 防滥用 + WS 通知管理员）、GET /feedback/my、GET /admin/feedback/list、PUT /admin/feedback/handle
- [x] T4 WebSocketServer.sendToAdmins()
- [x] T5 WebConfig 拦截 /feedback/**
- [x] T6 前端 api/feedback.js + ChatRoom.vue 消息举报弹窗
- [x] T7 管理端 FeedbackManagement.vue + 路由 + Dashboard 入口
- [x] T8 部署验收 4 项 + 归档 + 面试文档同步（03/90 补"举报已实现"）

## 验收记录（2026-10-26，6/6）

1. ✅ 用户提交举报成功落库（含楼层/被举报人/关联消息/匿名）
2. ✅ 60 秒内二次提交被 Redis SETNX 拒绝
3. ✅ 管理端分页列表可见，匿名者脱敏为"匿名"（reporterId 置空，库中可溯源）
4. ✅ 标记已处理（留备注+处理人+时间），重复处理被拒
5. ✅ 用户"我的举报"可见处理状态
6. ✅ 提交即 WS 实时通知在线管理员（日志确认执行）
