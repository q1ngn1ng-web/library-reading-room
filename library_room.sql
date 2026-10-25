-- 阅览室管理系统 数据库初始化脚本（MySQL 8.0+）
-- 演示数据：所有账号密码均为 123456；服务首次启动会自动将库内密码全部迁移为 BCrypt 哈希

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '管理员编号',
  `username` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '管理员账号',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '管理员密码',
  `nickname` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '管理员昵称',
  `role` int(10) NOT NULL DEFAULT 1 COMMENT '角色(1普通/2超级)',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'admin.png' COMMENT '头像',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理员表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of admin
-- ----------------------------
INSERT INTO `admin` VALUES (1, 'admin', '123456', '系统管理员', 2, 'admin1.png');
INSERT INTO `admin` VALUES (2, 'manager', '123456', '楼层管理员', 1, 'admin2.png');
INSERT INTO `admin` VALUES (3, 'admin1', '123456', '管理员1', 1, 'admin.png');
INSERT INTO `admin` VALUES (4, 'admin2', '123456', '管理员2', 2, 'admin.png');

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `room_id` int(10) NOT NULL COMMENT '聊天室ID',
  `user_id` int(10) NOT NULL COMMENT '用户ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '聊天名称(楼层+座位)',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息时间',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'text' COMMENT '消息类型',
  `is_anonymous` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否匿名(1匿名)',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '头像',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_room`(`room_id` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_time`(`time` ASC) USING BTREE,
  CONSTRAINT `fk_message_room` FOREIGN KEY (`room_id`) REFERENCES `chat_room` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_message_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 32 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_message
-- ----------------------------
INSERT INTO `chat_message` VALUES (1, 1, 1, '1楼-1号', '大家好，今天有人一起学习Java吗？', '2025-05-12 15:02:06', 'text', 0, 'avatar1.png');
INSERT INTO `chat_message` VALUES (3, 2, 3, '2楼-3号', '文学社的同学有在的吗？', '2025-05-12 15:02:06', 'text', 0, 'avatar3.png');
INSERT INTO `chat_message` VALUES (4, 1, 5, '�[�z', '是的', '2025-05-13 17:13:10', 'message', 0, 'qy-default.png');
INSERT INTO `chat_message` VALUES (5, 1, 5, '�[�z', '哈哈', '2025-05-13 17:13:49', 'message', 0, 'qy-default.png');
INSERT INTO `chat_message` VALUES (6, 1, 5, '�[�z', 'hh', '2025-05-13 17:18:51', 'message', 0, 'qy-default.png');
INSERT INTO `chat_message` VALUES (7, 1, 5, '�[�z', '你号\n', '2025-05-13 17:20:27', 'message', 1, 'qy-default.png');
INSERT INTO `chat_message` VALUES (8, 1, 5, '1层2号 user', '你们好', '2025-05-15 22:44:58', 'message', 0, 'qy-default.png');
INSERT INTO `chat_message` VALUES (9, 1, 5, '1层2号 q1ng', '你们好', '2025-05-15 22:49:34', 'message', 0, 'qy-default.png');
INSERT INTO `chat_message` VALUES (10, 1, 5, '1层2号 q1ng', '你们好啊', '2025-05-15 22:51:33', 'message', 0, 'qy-default.png');
INSERT INTO `chat_message` VALUES (11, 1, 5, '1层2号 q1ng', '现在是5月15号', '2025-05-15 22:52:06', 'message', 0, 'qy-default.png');
INSERT INTO `chat_message` VALUES (12, 1, 5, '1层2号 q1ng', '我旁边有人占座，请管理员来解决', '2025-05-15 22:52:22', 'message', 0, 'qy-default.png');
INSERT INTO `chat_message` VALUES (13, 1, 5, '1层2号 q1ng', '我现在匿名了', '2025-05-15 22:52:29', 'message', 1, 'qy-default.png');
INSERT INTO `chat_message` VALUES (14, 1, 5, '1层1号 111', '是是的', '2025-05-18 11:11:47', 'message', 0, '1747531633454-chromee - 副本.exe');
INSERT INTO `chat_message` VALUES (15, 1, 5, '1层1号 111', '都对对', '2025-05-18 11:11:49', 'message', 0, '1747531633454-chromee - 副本.exe');
INSERT INTO `chat_message` VALUES (16, 1, 5, '1层1号 111', '都对对', '2025-05-18 11:11:51', 'message', 0, '1747531633454-chromee - 副本.exe');
INSERT INTO `chat_message` VALUES (17, 1, 5, '1层1号 111', '都对对', '2025-05-18 11:11:52', 'message', 0, '1747531633454-chromee - 副本.exe');
INSERT INTO `chat_message` VALUES (18, 1, 5, '1层1号 111', '都对对', '2025-05-18 11:11:54', 'message', 0, '1747531633454-chromee - 副本.exe');
INSERT INTO `chat_message` VALUES (19, 1, 5, '1层5号 111', '你们好', '2025-05-22 20:58:32', 'message', 0, '1747531633454-chromee - 副本.exe');
INSERT INTO `chat_message` VALUES (20, 1, 5, '1层5号 111', '我是111', '2025-05-22 20:58:35', 'message', 0, '1747531633454-chromee - 副本.exe');
INSERT INTO `chat_message` VALUES (21, 2, 5, '1层5号 111', '不在', '2025-05-22 21:00:38', 'message', 0, '1747531633454-chromee - 副本.exe');
INSERT INTO `chat_message` VALUES (22, 2, 5, '1层5号 111', '你想要说什么', '2025-05-22 21:00:43', 'message', 0, '1747531633454-chromee - 副本.exe');
INSERT INTO `chat_message` VALUES (23, 2, 5, '[管理员] 111', 'hello', '2025-05-23 16:22:09', 'message', 0, '1747531633454-chromee - 副本.exe');
INSERT INTO `chat_message` VALUES (24, 2, 5, '[管理员] 111', '管理员在吗？\n', '2025-05-23 16:33:37', 'message', 0, '1747531633454-chromee - 副本.exe');
INSERT INTO `chat_message` VALUES (25, 2, 5, '[管理员] 111', '你们好', '2025-05-23 16:42:11', 'message', 0, '1747531633454-chromee - 副本.exe');
INSERT INTO `chat_message` VALUES (26, 2, 5, '1层1号 111', '大家好，现在是演示用例', '2025-05-23 16:46:27', 'message', 0, '1747531633454-chromee - 副本.exe');
INSERT INTO `chat_message` VALUES (27, 1, 4, '[管理员] 管理员2', '我是管理员', '2025-05-23 20:21:14', 'message', 0, 'admin.png');
INSERT INTO `chat_message` VALUES (28, 1, 4, '[管理员] 管理员2', '是的', '2025-05-23 20:21:32', 'message', 0, 'admin.png');
INSERT INTO `chat_message` VALUES (29, 1, 4, '[管理员] 管理员2', '我是管理员2号', '2025-05-23 20:23:21', 'message', 0, 'admin.png');
INSERT INTO `chat_message` VALUES (30, 1, 4, '[管理员] 管理员2', '大家好', '2025-05-23 20:23:26', 'message', 0, 'admin.png');
INSERT INTO `chat_message` VALUES (31, 2, 4, '[管理员] 管理员2', '大家晚上好，我是管理员', '2025-05-23 20:25:57', 'message', 0, 'admin.png');
INSERT INTO `chat_message` VALUES (32, 2, 4, '[管理员] 管理员2', '今天是5月23号', '2025-05-23 20:26:20', 'message', 0, 'admin.png');

-- ----------------------------
-- Table structure for chat_room
-- ----------------------------
DROP TABLE IF EXISTS `chat_room`;
CREATE TABLE `chat_room`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '聊天室ID',
  `floor` int(10) NOT NULL COMMENT '楼层号',
  `category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '学科类型',
  `is_available` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可用(1可用)',
  `is_chat` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否禁言(1可发言)',
  `maxUser` int(10) NOT NULL COMMENT '最大用户数',
  `currentUser` int(10) NOT NULL DEFAULT 0 COMMENT '当前用户数',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_floor`(`floor` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天室表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_room
-- ----------------------------
INSERT INTO `chat_room` VALUES (1, 1, '计算机科学', 1, 1, 60, 0);
INSERT INTO `chat_room` VALUES (2, 2, '文学艺术', 1, 1, 60, 0);
INSERT INTO `chat_room` VALUES (3, 3, '工程技术', 1, 1, 60, 0);
INSERT INTO `chat_room` VALUES (4, 4, '数学', 1, 1, 60, 0);
INSERT INTO `chat_room` VALUES (5, 5, '物理', 1, 0, 60, 0);
INSERT INTO `chat_room` VALUES (6, 6, '化学', 1, 1, 60, 0);
INSERT INTO `chat_room` VALUES (7, 7, '生物', 1, 1, 60, 0);
INSERT INTO `chat_room` VALUES (8, 8, '历史', 1, 1, 60, 0);
INSERT INTO `chat_room` VALUES (9, 9, '地理', 1, 0, 60, 0);
INSERT INTO `chat_room` VALUES (10, 10, '经济', 1, 0, 60, 0);
INSERT INTO `chat_room` VALUES (11, 11, '其他', 1, 1, 0, 0);

-- ----------------------------
-- Table structure for notice
-- ----------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '公告标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '公告内容',
  `time` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '公告时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notice
-- ----------------------------
INSERT INTO `notice` VALUES (1, '系统维护通知', '本周六上午8:00-10:00系统维护，暂停服务', '2023-09-28 15:00:00');
INSERT INTO `notice` VALUES (3, '端午节放假通知', '图书馆端午节放假一天', '2025-05-15 16:22:56');
INSERT INTO `notice` VALUES (4, '今天', '今天是5月23号，请注意', '2025-05-23 16:55:27');
INSERT INTO `notice` VALUES (5, '今天', '今天是5月份的一天', '2025-05-23 19:58:44');

-- ----------------------------
-- Table structure for reservation
-- ----------------------------
DROP TABLE IF EXISTS `reservation`;
CREATE TABLE `reservation`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `user_id` int(10) NOT NULL COMMENT '用户ID',
  `seat_id` int(10) NOT NULL COMMENT '座位ID',
  `start_time` datetime NOT NULL COMMENT '预约开始时间',
  `end_time` datetime NOT NULL COMMENT '预约结束时间',
  `timeout_at` datetime NOT NULL COMMENT '最晚签到时间',
  `real_start_time` datetime NULL DEFAULT NULL COMMENT '实际开始时间',
  `real_end_time` datetime NULL DEFAULT NULL COMMENT '实际结束时间',
  `status` int(10) NOT NULL DEFAULT 1 COMMENT '状态(1成功/2失败/3超时/4取消)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_seat`(`seat_id` ASC) USING BTREE,
  CONSTRAINT `fk_reservation_seat` FOREIGN KEY (`seat_id`) REFERENCES `seat` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_reservation_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '预约记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of reservation
-- ----------------------------
INSERT INTO `reservation` VALUES (1, 1, 1, '2023-10-01 09:00:00', '2023-10-01 12:00:00', '2023-10-01 09:15:00', NULL, NULL, 3);
INSERT INTO `reservation` VALUES (3, 3, 23, '2023-10-02 10:00:00', '2023-10-02 16:00:00', '2023-10-02 10:15:00', NULL, NULL, 3);
INSERT INTO `reservation` VALUES (8, 5, 11, '2025-05-13 17:00:00', '2025-05-13 17:30:00', '2025-05-13 17:03:37', '2025-05-13 16:39:38', NULL, 4);
INSERT INTO `reservation` VALUES (9, 5, 1, '2025-05-13 18:00:00', '2025-05-13 21:00:00', '2025-05-13 17:57:11', '2025-05-13 17:28:36', '2025-05-13 17:37:51', 3);
INSERT INTO `reservation` VALUES (10, 5, 2, '2025-05-16 08:00:00', '2025-05-16 09:00:00', '2025-05-15 23:14:11', '2025-05-15 22:44:36', NULL, 5);
INSERT INTO `reservation` VALUES (11, 5, 1, '2025-05-18 09:00:00', '2025-05-18 10:00:00', '2025-05-18 09:18:45', NULL, NULL, 4);
INSERT INTO `reservation` VALUES (12, 5, 1, '2025-05-18 09:00:00', '2025-05-18 09:30:00', '2025-05-18 09:22:57', NULL, NULL, 3);
INSERT INTO `reservation` VALUES (13, 5, 3, '2025-05-18 10:00:00', '2025-05-18 10:30:00', '2025-05-18 10:07:44', NULL, NULL, 4);
INSERT INTO `reservation` VALUES (14, 5, 1, '2025-05-18 14:00:00', '2025-05-18 14:30:00', '2025-05-18 11:40:23', '2025-05-18 11:11:30', NULL, 5);
INSERT INTO `reservation` VALUES (15, 5, 3, '2025-05-18 20:00:00', '2025-05-18 21:00:00', '2025-05-18 15:13:52', NULL, NULL, 3);
INSERT INTO `reservation` VALUES (16, 5, 4, '2025-05-21 16:00:00', '2025-05-21 17:00:00', '2025-05-21 15:56:52', '2025-05-21 15:45:49', NULL, 5);
INSERT INTO `reservation` VALUES (17, 5, 5, '2025-05-22 21:00:00', '2025-05-22 22:00:00', '2025-05-22 21:24:53', '2025-05-22 21:02:40', '2025-05-22 21:04:13', 6);
INSERT INTO `reservation` VALUES (18, 5, 7, '2025-05-22 21:30:00', '2025-05-22 22:00:00', '2025-05-22 21:37:03', NULL, NULL, 4);
INSERT INTO `reservation` VALUES (19, 5, 7, '2025-05-22 21:30:00', '2025-05-22 22:00:00', '2025-05-22 21:38:25', NULL, NULL, 4);
INSERT INTO `reservation` VALUES (20, 5, 7, '2025-05-22 21:30:00', '2025-05-22 22:00:00', '2025-05-22 21:40:17', NULL, NULL, 4);
INSERT INTO `reservation` VALUES (21, 5, 7, '2025-05-22 21:30:00', '2025-05-22 22:00:00', '2025-05-22 21:42:04', '2025-05-22 21:12:07', '2025-05-22 21:12:10', 6);
INSERT INTO `reservation` VALUES (22, 5, 1, '2025-05-23 17:30:00', '2025-05-23 20:30:00', '2025-05-23 17:15:51', '2025-05-23 16:46:08', '2025-05-23 16:46:45', 6);

-- ----------------------------
-- Table structure for seat
-- ----------------------------
DROP TABLE IF EXISTS `seat`;
CREATE TABLE `seat`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '座位编号',
  `user_id` int(10) NULL DEFAULT NULL COMMENT '预约人ID',
  `room_id` int(10) NOT NULL COMMENT '所在楼层ID',
  `number` int(10) NOT NULL COMMENT '座位序号',
  `status` int(10) NOT NULL DEFAULT 0 COMMENT '状态(0可预约/1已预约/2使用中/3禁用)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_room`(`room_id` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_seat_room` FOREIGN KEY (`room_id`) REFERENCES `chat_room` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_seat_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 624 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '座位表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of seat
-- ----------------------------
INSERT INTO `seat` VALUES (1, NULL, 1, 1, 0);
INSERT INTO `seat` VALUES (2, NULL, 1, 2, 0);
INSERT INTO `seat` VALUES (3, NULL, 1, 3, 1);
INSERT INTO `seat` VALUES (4, NULL, 1, 4, 2);
INSERT INTO `seat` VALUES (5, NULL, 1, 5, 0);
INSERT INTO `seat` VALUES (6, NULL, 1, 6, 0);
INSERT INTO `seat` VALUES (7, NULL, 1, 7, 0);
INSERT INTO `seat` VALUES (8, NULL, 1, 8, 0);
INSERT INTO `seat` VALUES (9, NULL, 1, 9, 0);
INSERT INTO `seat` VALUES (10, NULL, 1, 10, 0);
INSERT INTO `seat` VALUES (11, NULL, 2, 1, 0);
INSERT INTO `seat` VALUES (12, NULL, 2, 2, 0);
INSERT INTO `seat` VALUES (13, NULL, 2, 3, 0);
INSERT INTO `seat` VALUES (14, NULL, 2, 4, 0);
INSERT INTO `seat` VALUES (15, NULL, 2, 5, 0);
INSERT INTO `seat` VALUES (16, NULL, 2, 6, 0);
INSERT INTO `seat` VALUES (17, NULL, 2, 7, 0);
INSERT INTO `seat` VALUES (18, NULL, 2, 8, 0);
INSERT INTO `seat` VALUES (19, NULL, 2, 9, 0);
INSERT INTO `seat` VALUES (20, NULL, 2, 10, 0);
INSERT INTO `seat` VALUES (21, NULL, 3, 1, 3);
INSERT INTO `seat` VALUES (22, NULL, 3, 2, 3);
INSERT INTO `seat` VALUES (23, NULL, 3, 3, 3);
INSERT INTO `seat` VALUES (24, NULL, 3, 4, 3);
INSERT INTO `seat` VALUES (25, NULL, 3, 5, 3);
INSERT INTO `seat` VALUES (26, NULL, 3, 6, 3);
INSERT INTO `seat` VALUES (27, NULL, 3, 7, 3);
INSERT INTO `seat` VALUES (28, NULL, 3, 8, 3);
INSERT INTO `seat` VALUES (29, NULL, 3, 9, 3);
INSERT INTO `seat` VALUES (30, NULL, 3, 10, 3);
INSERT INTO `seat` VALUES (31, NULL, 4, 1, 0);
INSERT INTO `seat` VALUES (32, NULL, 4, 2, 0);
INSERT INTO `seat` VALUES (33, NULL, 4, 3, 0);
INSERT INTO `seat` VALUES (34, NULL, 4, 4, 0);
INSERT INTO `seat` VALUES (35, NULL, 4, 5, 0);
INSERT INTO `seat` VALUES (36, NULL, 4, 6, 0);
INSERT INTO `seat` VALUES (37, NULL, 4, 7, 0);
INSERT INTO `seat` VALUES (38, NULL, 4, 8, 0);
INSERT INTO `seat` VALUES (39, NULL, 4, 9, 0);
INSERT INTO `seat` VALUES (40, NULL, 4, 10, 0);
INSERT INTO `seat` VALUES (41, NULL, 4, 11, 0);
INSERT INTO `seat` VALUES (42, NULL, 4, 12, 0);
INSERT INTO `seat` VALUES (43, NULL, 4, 13, 0);
INSERT INTO `seat` VALUES (44, NULL, 4, 14, 0);
INSERT INTO `seat` VALUES (45, NULL, 4, 15, 0);
INSERT INTO `seat` VALUES (46, NULL, 4, 16, 0);
INSERT INTO `seat` VALUES (47, NULL, 4, 17, 0);
INSERT INTO `seat` VALUES (48, NULL, 4, 18, 0);
INSERT INTO `seat` VALUES (49, NULL, 4, 19, 0);
INSERT INTO `seat` VALUES (50, NULL, 4, 20, 0);
INSERT INTO `seat` VALUES (51, NULL, 4, 21, 0);
INSERT INTO `seat` VALUES (52, NULL, 4, 22, 0);
INSERT INTO `seat` VALUES (53, NULL, 4, 23, 0);
INSERT INTO `seat` VALUES (54, NULL, 4, 24, 0);
INSERT INTO `seat` VALUES (55, NULL, 4, 25, 0);
INSERT INTO `seat` VALUES (56, NULL, 4, 26, 0);
INSERT INTO `seat` VALUES (57, NULL, 4, 27, 0);
INSERT INTO `seat` VALUES (58, NULL, 4, 28, 0);
INSERT INTO `seat` VALUES (59, NULL, 4, 29, 0);
INSERT INTO `seat` VALUES (60, NULL, 4, 30, 0);
INSERT INTO `seat` VALUES (61, NULL, 4, 31, 0);
INSERT INTO `seat` VALUES (62, NULL, 4, 32, 0);
INSERT INTO `seat` VALUES (63, NULL, 4, 33, 0);
INSERT INTO `seat` VALUES (64, NULL, 4, 34, 0);
INSERT INTO `seat` VALUES (65, NULL, 4, 35, 0);
INSERT INTO `seat` VALUES (66, NULL, 4, 36, 0);
INSERT INTO `seat` VALUES (67, NULL, 4, 37, 0);
INSERT INTO `seat` VALUES (68, NULL, 4, 38, 0);
INSERT INTO `seat` VALUES (69, NULL, 4, 39, 0);
INSERT INTO `seat` VALUES (70, NULL, 4, 40, 0);
INSERT INTO `seat` VALUES (71, NULL, 4, 41, 0);
INSERT INTO `seat` VALUES (72, NULL, 4, 42, 0);
INSERT INTO `seat` VALUES (73, NULL, 4, 43, 0);
INSERT INTO `seat` VALUES (74, NULL, 4, 44, 0);
INSERT INTO `seat` VALUES (75, NULL, 4, 45, 0);
INSERT INTO `seat` VALUES (76, NULL, 4, 46, 0);
INSERT INTO `seat` VALUES (77, NULL, 4, 47, 0);
INSERT INTO `seat` VALUES (78, NULL, 4, 48, 0);
INSERT INTO `seat` VALUES (79, NULL, 4, 49, 0);
INSERT INTO `seat` VALUES (80, NULL, 4, 50, 0);
INSERT INTO `seat` VALUES (81, NULL, 4, 51, 0);
INSERT INTO `seat` VALUES (82, NULL, 4, 52, 0);
INSERT INTO `seat` VALUES (83, NULL, 4, 53, 0);
INSERT INTO `seat` VALUES (84, NULL, 4, 54, 0);
INSERT INTO `seat` VALUES (85, NULL, 4, 55, 0);
INSERT INTO `seat` VALUES (86, NULL, 4, 56, 0);
INSERT INTO `seat` VALUES (87, NULL, 4, 57, 0);
INSERT INTO `seat` VALUES (88, NULL, 4, 58, 0);
INSERT INTO `seat` VALUES (89, NULL, 4, 59, 0);
INSERT INTO `seat` VALUES (90, NULL, 4, 60, 0);
INSERT INTO `seat` VALUES (94, NULL, 5, 1, 0);
INSERT INTO `seat` VALUES (95, NULL, 5, 2, 0);
INSERT INTO `seat` VALUES (96, NULL, 5, 3, 0);
INSERT INTO `seat` VALUES (97, NULL, 5, 4, 0);
INSERT INTO `seat` VALUES (98, NULL, 5, 5, 0);
INSERT INTO `seat` VALUES (99, NULL, 5, 6, 0);
INSERT INTO `seat` VALUES (100, NULL, 5, 7, 0);
INSERT INTO `seat` VALUES (101, NULL, 5, 8, 0);
INSERT INTO `seat` VALUES (102, NULL, 5, 9, 0);
INSERT INTO `seat` VALUES (103, NULL, 5, 10, 0);
INSERT INTO `seat` VALUES (104, NULL, 5, 11, 0);
INSERT INTO `seat` VALUES (105, NULL, 5, 12, 0);
INSERT INTO `seat` VALUES (106, NULL, 5, 13, 0);
INSERT INTO `seat` VALUES (107, NULL, 5, 14, 0);
INSERT INTO `seat` VALUES (108, NULL, 5, 15, 0);
INSERT INTO `seat` VALUES (109, NULL, 5, 16, 0);
INSERT INTO `seat` VALUES (110, NULL, 5, 17, 0);
INSERT INTO `seat` VALUES (111, NULL, 5, 18, 0);
INSERT INTO `seat` VALUES (112, NULL, 5, 19, 0);
INSERT INTO `seat` VALUES (113, NULL, 5, 20, 0);
INSERT INTO `seat` VALUES (114, NULL, 5, 21, 0);
INSERT INTO `seat` VALUES (115, NULL, 5, 22, 0);
INSERT INTO `seat` VALUES (116, NULL, 5, 23, 0);
INSERT INTO `seat` VALUES (117, NULL, 5, 24, 0);
INSERT INTO `seat` VALUES (118, NULL, 5, 25, 0);
INSERT INTO `seat` VALUES (119, NULL, 5, 26, 0);
INSERT INTO `seat` VALUES (120, NULL, 5, 27, 0);
INSERT INTO `seat` VALUES (121, NULL, 5, 28, 0);
INSERT INTO `seat` VALUES (122, NULL, 5, 29, 0);
INSERT INTO `seat` VALUES (123, NULL, 5, 30, 0);
INSERT INTO `seat` VALUES (124, NULL, 5, 31, 0);
INSERT INTO `seat` VALUES (125, NULL, 5, 32, 0);
INSERT INTO `seat` VALUES (126, NULL, 5, 33, 0);
INSERT INTO `seat` VALUES (127, NULL, 5, 34, 0);
INSERT INTO `seat` VALUES (128, NULL, 5, 35, 0);
INSERT INTO `seat` VALUES (129, NULL, 5, 36, 0);
INSERT INTO `seat` VALUES (130, NULL, 5, 37, 0);
INSERT INTO `seat` VALUES (131, NULL, 5, 38, 0);
INSERT INTO `seat` VALUES (132, NULL, 5, 39, 0);
INSERT INTO `seat` VALUES (133, NULL, 5, 40, 0);
INSERT INTO `seat` VALUES (134, NULL, 5, 41, 0);
INSERT INTO `seat` VALUES (135, NULL, 5, 42, 0);
INSERT INTO `seat` VALUES (136, NULL, 5, 43, 0);
INSERT INTO `seat` VALUES (137, NULL, 5, 44, 0);
INSERT INTO `seat` VALUES (138, NULL, 5, 45, 0);
INSERT INTO `seat` VALUES (139, NULL, 5, 46, 0);
INSERT INTO `seat` VALUES (140, NULL, 5, 47, 0);
INSERT INTO `seat` VALUES (141, NULL, 5, 48, 0);
INSERT INTO `seat` VALUES (142, NULL, 5, 49, 0);
INSERT INTO `seat` VALUES (143, NULL, 5, 50, 0);
INSERT INTO `seat` VALUES (144, NULL, 5, 51, 0);
INSERT INTO `seat` VALUES (145, NULL, 5, 52, 0);
INSERT INTO `seat` VALUES (146, NULL, 5, 53, 0);
INSERT INTO `seat` VALUES (147, NULL, 5, 54, 0);
INSERT INTO `seat` VALUES (148, NULL, 5, 55, 0);
INSERT INTO `seat` VALUES (149, NULL, 5, 56, 0);
INSERT INTO `seat` VALUES (150, NULL, 5, 57, 0);
INSERT INTO `seat` VALUES (151, NULL, 5, 58, 0);
INSERT INTO `seat` VALUES (152, NULL, 5, 59, 0);
INSERT INTO `seat` VALUES (153, NULL, 5, 60, 0);
INSERT INTO `seat` VALUES (157, NULL, 6, 1, 3);
INSERT INTO `seat` VALUES (158, NULL, 6, 2, 3);
INSERT INTO `seat` VALUES (159, NULL, 6, 3, 3);
INSERT INTO `seat` VALUES (160, NULL, 6, 4, 3);
INSERT INTO `seat` VALUES (161, NULL, 6, 5, 3);
INSERT INTO `seat` VALUES (162, NULL, 6, 6, 3);
INSERT INTO `seat` VALUES (163, NULL, 6, 7, 3);
INSERT INTO `seat` VALUES (164, NULL, 6, 8, 3);
INSERT INTO `seat` VALUES (165, NULL, 6, 9, 3);
INSERT INTO `seat` VALUES (166, NULL, 6, 10, 3);
INSERT INTO `seat` VALUES (167, NULL, 6, 11, 3);
INSERT INTO `seat` VALUES (168, NULL, 6, 12, 3);
INSERT INTO `seat` VALUES (169, NULL, 6, 13, 3);
INSERT INTO `seat` VALUES (170, NULL, 6, 14, 3);
INSERT INTO `seat` VALUES (171, NULL, 6, 15, 3);
INSERT INTO `seat` VALUES (172, NULL, 6, 16, 3);
INSERT INTO `seat` VALUES (173, NULL, 6, 17, 3);
INSERT INTO `seat` VALUES (174, NULL, 6, 18, 3);
INSERT INTO `seat` VALUES (175, NULL, 6, 19, 3);
INSERT INTO `seat` VALUES (176, NULL, 6, 20, 3);
INSERT INTO `seat` VALUES (177, NULL, 6, 21, 3);
INSERT INTO `seat` VALUES (178, NULL, 6, 22, 3);
INSERT INTO `seat` VALUES (179, NULL, 6, 23, 3);
INSERT INTO `seat` VALUES (180, NULL, 6, 24, 3);
INSERT INTO `seat` VALUES (181, NULL, 6, 25, 3);
INSERT INTO `seat` VALUES (182, NULL, 6, 26, 3);
INSERT INTO `seat` VALUES (183, NULL, 6, 27, 3);
INSERT INTO `seat` VALUES (184, NULL, 6, 28, 3);
INSERT INTO `seat` VALUES (185, NULL, 6, 29, 3);
INSERT INTO `seat` VALUES (186, NULL, 6, 30, 3);
INSERT INTO `seat` VALUES (187, NULL, 6, 31, 3);
INSERT INTO `seat` VALUES (188, NULL, 6, 32, 3);
INSERT INTO `seat` VALUES (189, NULL, 6, 33, 3);
INSERT INTO `seat` VALUES (190, NULL, 6, 34, 3);
INSERT INTO `seat` VALUES (191, NULL, 6, 35, 3);
INSERT INTO `seat` VALUES (192, NULL, 6, 36, 3);
INSERT INTO `seat` VALUES (193, NULL, 6, 37, 3);
INSERT INTO `seat` VALUES (194, NULL, 6, 38, 3);
INSERT INTO `seat` VALUES (195, NULL, 6, 39, 3);
INSERT INTO `seat` VALUES (196, NULL, 6, 40, 3);
INSERT INTO `seat` VALUES (197, NULL, 6, 41, 3);
INSERT INTO `seat` VALUES (198, NULL, 6, 42, 3);
INSERT INTO `seat` VALUES (199, NULL, 6, 43, 3);
INSERT INTO `seat` VALUES (200, NULL, 6, 44, 3);
INSERT INTO `seat` VALUES (201, NULL, 6, 45, 3);
INSERT INTO `seat` VALUES (202, NULL, 6, 46, 3);
INSERT INTO `seat` VALUES (203, NULL, 6, 47, 3);
INSERT INTO `seat` VALUES (204, NULL, 6, 48, 3);
INSERT INTO `seat` VALUES (205, NULL, 6, 49, 3);
INSERT INTO `seat` VALUES (206, NULL, 6, 50, 3);
INSERT INTO `seat` VALUES (207, NULL, 6, 51, 3);
INSERT INTO `seat` VALUES (208, NULL, 6, 52, 3);
INSERT INTO `seat` VALUES (209, NULL, 6, 53, 3);
INSERT INTO `seat` VALUES (210, NULL, 6, 54, 3);
INSERT INTO `seat` VALUES (211, NULL, 6, 55, 3);
INSERT INTO `seat` VALUES (212, NULL, 6, 56, 3);
INSERT INTO `seat` VALUES (213, NULL, 6, 57, 3);
INSERT INTO `seat` VALUES (214, NULL, 6, 58, 3);
INSERT INTO `seat` VALUES (215, NULL, 6, 59, 3);
INSERT INTO `seat` VALUES (216, NULL, 6, 60, 3);
INSERT INTO `seat` VALUES (220, NULL, 7, 1, 0);
INSERT INTO `seat` VALUES (221, NULL, 7, 2, 0);
INSERT INTO `seat` VALUES (222, NULL, 7, 3, 0);
INSERT INTO `seat` VALUES (223, NULL, 7, 4, 0);
INSERT INTO `seat` VALUES (224, NULL, 7, 5, 0);
INSERT INTO `seat` VALUES (225, NULL, 7, 6, 0);
INSERT INTO `seat` VALUES (226, NULL, 7, 7, 0);
INSERT INTO `seat` VALUES (227, NULL, 7, 8, 0);
INSERT INTO `seat` VALUES (228, NULL, 7, 9, 0);
INSERT INTO `seat` VALUES (229, NULL, 7, 10, 0);
INSERT INTO `seat` VALUES (230, NULL, 7, 11, 0);
INSERT INTO `seat` VALUES (231, NULL, 7, 12, 0);
INSERT INTO `seat` VALUES (232, NULL, 7, 13, 0);
INSERT INTO `seat` VALUES (233, NULL, 7, 14, 0);
INSERT INTO `seat` VALUES (234, NULL, 7, 15, 0);
INSERT INTO `seat` VALUES (235, NULL, 7, 16, 0);
INSERT INTO `seat` VALUES (236, NULL, 7, 17, 0);
INSERT INTO `seat` VALUES (237, NULL, 7, 18, 0);
INSERT INTO `seat` VALUES (238, NULL, 7, 19, 0);
INSERT INTO `seat` VALUES (239, NULL, 7, 20, 0);
INSERT INTO `seat` VALUES (240, NULL, 7, 21, 0);
INSERT INTO `seat` VALUES (241, NULL, 7, 22, 0);
INSERT INTO `seat` VALUES (242, NULL, 7, 23, 0);
INSERT INTO `seat` VALUES (243, NULL, 7, 24, 0);
INSERT INTO `seat` VALUES (244, NULL, 7, 25, 0);
INSERT INTO `seat` VALUES (245, NULL, 7, 26, 0);
INSERT INTO `seat` VALUES (246, NULL, 7, 27, 0);
INSERT INTO `seat` VALUES (247, NULL, 7, 28, 0);
INSERT INTO `seat` VALUES (248, NULL, 7, 29, 0);
INSERT INTO `seat` VALUES (249, NULL, 7, 30, 0);
INSERT INTO `seat` VALUES (250, NULL, 7, 31, 0);
INSERT INTO `seat` VALUES (251, NULL, 7, 32, 0);
INSERT INTO `seat` VALUES (252, NULL, 7, 33, 0);
INSERT INTO `seat` VALUES (253, NULL, 7, 34, 0);
INSERT INTO `seat` VALUES (254, NULL, 7, 35, 0);
INSERT INTO `seat` VALUES (255, NULL, 7, 36, 0);
INSERT INTO `seat` VALUES (256, NULL, 7, 37, 0);
INSERT INTO `seat` VALUES (257, NULL, 7, 38, 0);
INSERT INTO `seat` VALUES (258, NULL, 7, 39, 0);
INSERT INTO `seat` VALUES (259, NULL, 7, 40, 0);
INSERT INTO `seat` VALUES (260, NULL, 7, 41, 0);
INSERT INTO `seat` VALUES (261, NULL, 7, 42, 0);
INSERT INTO `seat` VALUES (262, NULL, 7, 43, 0);
INSERT INTO `seat` VALUES (263, NULL, 7, 44, 0);
INSERT INTO `seat` VALUES (264, NULL, 7, 45, 0);
INSERT INTO `seat` VALUES (265, NULL, 7, 46, 0);
INSERT INTO `seat` VALUES (266, NULL, 7, 47, 0);
INSERT INTO `seat` VALUES (267, NULL, 7, 48, 0);
INSERT INTO `seat` VALUES (268, NULL, 7, 49, 0);
INSERT INTO `seat` VALUES (269, NULL, 7, 50, 0);
INSERT INTO `seat` VALUES (270, NULL, 7, 51, 0);
INSERT INTO `seat` VALUES (271, NULL, 7, 52, 0);
INSERT INTO `seat` VALUES (272, NULL, 7, 53, 0);
INSERT INTO `seat` VALUES (273, NULL, 7, 54, 0);
INSERT INTO `seat` VALUES (274, NULL, 7, 55, 0);
INSERT INTO `seat` VALUES (275, NULL, 7, 56, 0);
INSERT INTO `seat` VALUES (276, NULL, 7, 57, 0);
INSERT INTO `seat` VALUES (277, NULL, 7, 58, 0);
INSERT INTO `seat` VALUES (278, NULL, 7, 59, 0);
INSERT INTO `seat` VALUES (279, NULL, 7, 60, 0);
INSERT INTO `seat` VALUES (283, NULL, 8, 1, 0);
INSERT INTO `seat` VALUES (284, NULL, 8, 2, 0);
INSERT INTO `seat` VALUES (285, NULL, 8, 3, 0);
INSERT INTO `seat` VALUES (286, NULL, 8, 4, 0);
INSERT INTO `seat` VALUES (287, NULL, 8, 5, 0);
INSERT INTO `seat` VALUES (288, NULL, 8, 6, 0);
INSERT INTO `seat` VALUES (289, NULL, 8, 7, 0);
INSERT INTO `seat` VALUES (290, NULL, 8, 8, 0);
INSERT INTO `seat` VALUES (291, NULL, 8, 9, 0);
INSERT INTO `seat` VALUES (292, NULL, 8, 10, 0);
INSERT INTO `seat` VALUES (293, NULL, 8, 11, 0);
INSERT INTO `seat` VALUES (294, NULL, 8, 12, 0);
INSERT INTO `seat` VALUES (295, NULL, 8, 13, 0);
INSERT INTO `seat` VALUES (296, NULL, 8, 14, 0);
INSERT INTO `seat` VALUES (297, NULL, 8, 15, 0);
INSERT INTO `seat` VALUES (298, NULL, 8, 16, 0);
INSERT INTO `seat` VALUES (299, NULL, 8, 17, 0);
INSERT INTO `seat` VALUES (300, NULL, 8, 18, 0);
INSERT INTO `seat` VALUES (301, NULL, 8, 19, 0);
INSERT INTO `seat` VALUES (302, NULL, 8, 20, 0);
INSERT INTO `seat` VALUES (303, NULL, 8, 21, 0);
INSERT INTO `seat` VALUES (304, NULL, 8, 22, 0);
INSERT INTO `seat` VALUES (305, NULL, 8, 23, 0);
INSERT INTO `seat` VALUES (306, NULL, 8, 24, 0);
INSERT INTO `seat` VALUES (307, NULL, 8, 25, 0);
INSERT INTO `seat` VALUES (308, NULL, 8, 26, 0);
INSERT INTO `seat` VALUES (309, NULL, 8, 27, 0);
INSERT INTO `seat` VALUES (310, NULL, 8, 28, 0);
INSERT INTO `seat` VALUES (311, NULL, 8, 29, 0);
INSERT INTO `seat` VALUES (312, NULL, 8, 30, 0);
INSERT INTO `seat` VALUES (313, NULL, 8, 31, 0);
INSERT INTO `seat` VALUES (314, NULL, 8, 32, 0);
INSERT INTO `seat` VALUES (315, NULL, 8, 33, 0);
INSERT INTO `seat` VALUES (316, NULL, 8, 34, 0);
INSERT INTO `seat` VALUES (317, NULL, 8, 35, 0);
INSERT INTO `seat` VALUES (318, NULL, 8, 36, 0);
INSERT INTO `seat` VALUES (319, NULL, 8, 37, 0);
INSERT INTO `seat` VALUES (320, NULL, 8, 38, 0);
INSERT INTO `seat` VALUES (321, NULL, 8, 39, 0);
INSERT INTO `seat` VALUES (322, NULL, 8, 40, 0);
INSERT INTO `seat` VALUES (323, NULL, 8, 41, 0);
INSERT INTO `seat` VALUES (324, NULL, 8, 42, 0);
INSERT INTO `seat` VALUES (325, NULL, 8, 43, 0);
INSERT INTO `seat` VALUES (326, NULL, 8, 44, 0);
INSERT INTO `seat` VALUES (327, NULL, 8, 45, 0);
INSERT INTO `seat` VALUES (328, NULL, 8, 46, 0);
INSERT INTO `seat` VALUES (329, NULL, 8, 47, 0);
INSERT INTO `seat` VALUES (330, NULL, 8, 48, 0);
INSERT INTO `seat` VALUES (331, NULL, 8, 49, 0);
INSERT INTO `seat` VALUES (332, NULL, 8, 50, 0);
INSERT INTO `seat` VALUES (333, NULL, 8, 51, 0);
INSERT INTO `seat` VALUES (334, NULL, 8, 52, 0);
INSERT INTO `seat` VALUES (335, NULL, 8, 53, 0);
INSERT INTO `seat` VALUES (336, NULL, 8, 54, 0);
INSERT INTO `seat` VALUES (337, NULL, 8, 55, 0);
INSERT INTO `seat` VALUES (338, NULL, 8, 56, 0);
INSERT INTO `seat` VALUES (339, NULL, 8, 57, 0);
INSERT INTO `seat` VALUES (340, NULL, 8, 58, 0);
INSERT INTO `seat` VALUES (341, NULL, 8, 59, 0);
INSERT INTO `seat` VALUES (342, NULL, 8, 60, 0);
INSERT INTO `seat` VALUES (346, NULL, 9, 1, 0);
INSERT INTO `seat` VALUES (347, NULL, 9, 2, 0);
INSERT INTO `seat` VALUES (348, NULL, 9, 3, 0);
INSERT INTO `seat` VALUES (349, NULL, 9, 4, 0);
INSERT INTO `seat` VALUES (350, NULL, 9, 5, 0);
INSERT INTO `seat` VALUES (351, NULL, 9, 6, 0);
INSERT INTO `seat` VALUES (352, NULL, 9, 7, 0);
INSERT INTO `seat` VALUES (353, NULL, 9, 8, 0);
INSERT INTO `seat` VALUES (354, NULL, 9, 9, 0);
INSERT INTO `seat` VALUES (355, NULL, 9, 10, 0);
INSERT INTO `seat` VALUES (356, NULL, 9, 11, 0);
INSERT INTO `seat` VALUES (357, NULL, 9, 12, 0);
INSERT INTO `seat` VALUES (358, NULL, 9, 13, 0);
INSERT INTO `seat` VALUES (359, NULL, 9, 14, 0);
INSERT INTO `seat` VALUES (360, NULL, 9, 15, 0);
INSERT INTO `seat` VALUES (361, NULL, 9, 16, 0);
INSERT INTO `seat` VALUES (362, NULL, 9, 17, 0);
INSERT INTO `seat` VALUES (363, NULL, 9, 18, 0);
INSERT INTO `seat` VALUES (364, NULL, 9, 19, 0);
INSERT INTO `seat` VALUES (365, NULL, 9, 20, 0);
INSERT INTO `seat` VALUES (366, NULL, 9, 21, 0);
INSERT INTO `seat` VALUES (367, NULL, 9, 22, 0);
INSERT INTO `seat` VALUES (368, NULL, 9, 23, 0);
INSERT INTO `seat` VALUES (369, NULL, 9, 24, 0);
INSERT INTO `seat` VALUES (370, NULL, 9, 25, 0);
INSERT INTO `seat` VALUES (371, NULL, 9, 26, 0);
INSERT INTO `seat` VALUES (372, NULL, 9, 27, 0);
INSERT INTO `seat` VALUES (373, NULL, 9, 28, 0);
INSERT INTO `seat` VALUES (374, NULL, 9, 29, 0);
INSERT INTO `seat` VALUES (375, NULL, 9, 30, 0);
INSERT INTO `seat` VALUES (376, NULL, 9, 31, 0);
INSERT INTO `seat` VALUES (377, NULL, 9, 32, 0);
INSERT INTO `seat` VALUES (378, NULL, 9, 33, 0);
INSERT INTO `seat` VALUES (379, NULL, 9, 34, 0);
INSERT INTO `seat` VALUES (380, NULL, 9, 35, 0);
INSERT INTO `seat` VALUES (381, NULL, 9, 36, 0);
INSERT INTO `seat` VALUES (382, NULL, 9, 37, 0);
INSERT INTO `seat` VALUES (383, NULL, 9, 38, 0);
INSERT INTO `seat` VALUES (384, NULL, 9, 39, 0);
INSERT INTO `seat` VALUES (385, NULL, 9, 40, 0);
INSERT INTO `seat` VALUES (386, NULL, 9, 41, 0);
INSERT INTO `seat` VALUES (387, NULL, 9, 42, 0);
INSERT INTO `seat` VALUES (388, NULL, 9, 43, 0);
INSERT INTO `seat` VALUES (389, NULL, 9, 44, 0);
INSERT INTO `seat` VALUES (390, NULL, 9, 45, 0);
INSERT INTO `seat` VALUES (391, NULL, 9, 46, 0);
INSERT INTO `seat` VALUES (392, NULL, 9, 47, 0);
INSERT INTO `seat` VALUES (393, NULL, 9, 48, 0);
INSERT INTO `seat` VALUES (394, NULL, 9, 49, 0);
INSERT INTO `seat` VALUES (395, NULL, 9, 50, 0);
INSERT INTO `seat` VALUES (396, NULL, 9, 51, 0);
INSERT INTO `seat` VALUES (397, NULL, 9, 52, 0);
INSERT INTO `seat` VALUES (398, NULL, 9, 53, 0);
INSERT INTO `seat` VALUES (399, NULL, 9, 54, 0);
INSERT INTO `seat` VALUES (400, NULL, 9, 55, 0);
INSERT INTO `seat` VALUES (401, NULL, 9, 56, 0);
INSERT INTO `seat` VALUES (402, NULL, 9, 57, 0);
INSERT INTO `seat` VALUES (403, NULL, 9, 58, 0);
INSERT INTO `seat` VALUES (404, NULL, 9, 59, 0);
INSERT INTO `seat` VALUES (405, NULL, 9, 60, 0);
INSERT INTO `seat` VALUES (409, NULL, 10, 1, 0);
INSERT INTO `seat` VALUES (410, NULL, 10, 2, 0);
INSERT INTO `seat` VALUES (411, NULL, 10, 3, 0);
INSERT INTO `seat` VALUES (412, NULL, 10, 4, 0);
INSERT INTO `seat` VALUES (413, NULL, 10, 5, 0);
INSERT INTO `seat` VALUES (414, NULL, 10, 6, 0);
INSERT INTO `seat` VALUES (415, NULL, 10, 7, 0);
INSERT INTO `seat` VALUES (416, NULL, 10, 8, 0);
INSERT INTO `seat` VALUES (417, NULL, 10, 9, 0);
INSERT INTO `seat` VALUES (418, NULL, 10, 10, 0);
INSERT INTO `seat` VALUES (419, NULL, 10, 11, 0);
INSERT INTO `seat` VALUES (420, NULL, 10, 12, 0);
INSERT INTO `seat` VALUES (421, NULL, 10, 13, 0);
INSERT INTO `seat` VALUES (422, NULL, 10, 14, 0);
INSERT INTO `seat` VALUES (423, NULL, 10, 15, 0);
INSERT INTO `seat` VALUES (424, NULL, 10, 16, 0);
INSERT INTO `seat` VALUES (425, NULL, 10, 17, 0);
INSERT INTO `seat` VALUES (426, NULL, 10, 18, 0);
INSERT INTO `seat` VALUES (427, NULL, 10, 19, 0);
INSERT INTO `seat` VALUES (428, NULL, 10, 20, 0);
INSERT INTO `seat` VALUES (429, NULL, 10, 21, 0);
INSERT INTO `seat` VALUES (430, NULL, 10, 22, 0);
INSERT INTO `seat` VALUES (431, NULL, 10, 23, 0);
INSERT INTO `seat` VALUES (432, NULL, 10, 24, 0);
INSERT INTO `seat` VALUES (433, NULL, 10, 25, 0);
INSERT INTO `seat` VALUES (434, NULL, 10, 26, 0);
INSERT INTO `seat` VALUES (435, NULL, 10, 27, 0);
INSERT INTO `seat` VALUES (436, NULL, 10, 28, 0);
INSERT INTO `seat` VALUES (437, NULL, 10, 29, 0);
INSERT INTO `seat` VALUES (438, NULL, 10, 30, 0);
INSERT INTO `seat` VALUES (439, NULL, 10, 31, 0);
INSERT INTO `seat` VALUES (440, NULL, 10, 32, 0);
INSERT INTO `seat` VALUES (441, NULL, 10, 33, 0);
INSERT INTO `seat` VALUES (442, NULL, 10, 34, 0);
INSERT INTO `seat` VALUES (443, NULL, 10, 35, 0);
INSERT INTO `seat` VALUES (444, NULL, 10, 36, 0);
INSERT INTO `seat` VALUES (445, NULL, 10, 37, 0);
INSERT INTO `seat` VALUES (446, NULL, 10, 38, 0);
INSERT INTO `seat` VALUES (447, NULL, 10, 39, 0);
INSERT INTO `seat` VALUES (448, NULL, 10, 40, 0);
INSERT INTO `seat` VALUES (449, NULL, 10, 41, 0);
INSERT INTO `seat` VALUES (450, NULL, 10, 42, 0);
INSERT INTO `seat` VALUES (451, NULL, 10, 43, 0);
INSERT INTO `seat` VALUES (452, NULL, 10, 44, 0);
INSERT INTO `seat` VALUES (453, NULL, 10, 45, 0);
INSERT INTO `seat` VALUES (454, NULL, 10, 46, 0);
INSERT INTO `seat` VALUES (455, NULL, 10, 47, 0);
INSERT INTO `seat` VALUES (456, NULL, 10, 48, 0);
INSERT INTO `seat` VALUES (457, NULL, 10, 49, 0);
INSERT INTO `seat` VALUES (458, NULL, 10, 50, 0);
INSERT INTO `seat` VALUES (459, NULL, 10, 51, 0);
INSERT INTO `seat` VALUES (460, NULL, 10, 52, 0);
INSERT INTO `seat` VALUES (461, NULL, 10, 53, 0);
INSERT INTO `seat` VALUES (462, NULL, 10, 54, 0);
INSERT INTO `seat` VALUES (463, NULL, 10, 55, 0);
INSERT INTO `seat` VALUES (464, NULL, 10, 56, 0);
INSERT INTO `seat` VALUES (465, NULL, 10, 57, 0);
INSERT INTO `seat` VALUES (466, NULL, 10, 58, 0);
INSERT INTO `seat` VALUES (467, NULL, 10, 59, 0);
INSERT INTO `seat` VALUES (468, NULL, 10, 60, 0);
INSERT INTO `seat` VALUES (472, NULL, 1, 11, 2);
INSERT INTO `seat` VALUES (473, NULL, 1, 12, 3);
INSERT INTO `seat` VALUES (474, NULL, 1, 13, 3);
INSERT INTO `seat` VALUES (475, NULL, 1, 14, 0);
INSERT INTO `seat` VALUES (476, NULL, 1, 15, 3);
INSERT INTO `seat` VALUES (477, NULL, 1, 16, 1);
INSERT INTO `seat` VALUES (478, NULL, 1, 17, 1);
INSERT INTO `seat` VALUES (479, NULL, 1, 18, 2);
INSERT INTO `seat` VALUES (480, NULL, 1, 19, 2);
INSERT INTO `seat` VALUES (481, NULL, 1, 20, 3);
INSERT INTO `seat` VALUES (482, NULL, 1, 21, 2);
INSERT INTO `seat` VALUES (483, NULL, 1, 22, 3);
INSERT INTO `seat` VALUES (484, NULL, 1, 23, 3);
INSERT INTO `seat` VALUES (485, NULL, 1, 24, 3);
INSERT INTO `seat` VALUES (486, NULL, 1, 25, 3);
INSERT INTO `seat` VALUES (487, NULL, 1, 26, 3);
INSERT INTO `seat` VALUES (488, NULL, 1, 27, 3);
INSERT INTO `seat` VALUES (489, NULL, 1, 28, 1);
INSERT INTO `seat` VALUES (490, NULL, 1, 29, 3);
INSERT INTO `seat` VALUES (491, NULL, 1, 30, 3);
INSERT INTO `seat` VALUES (492, NULL, 2, 11, 3);
INSERT INTO `seat` VALUES (493, NULL, 2, 12, 1);
INSERT INTO `seat` VALUES (494, NULL, 2, 13, 3);
INSERT INTO `seat` VALUES (495, NULL, 2, 14, 3);
INSERT INTO `seat` VALUES (496, NULL, 2, 15, 3);
INSERT INTO `seat` VALUES (497, NULL, 2, 16, 2);
INSERT INTO `seat` VALUES (498, NULL, 2, 17, 3);
INSERT INTO `seat` VALUES (499, NULL, 2, 18, 1);
INSERT INTO `seat` VALUES (500, NULL, 2, 19, 1);
INSERT INTO `seat` VALUES (501, NULL, 2, 20, 2);
INSERT INTO `seat` VALUES (502, NULL, 2, 21, 1);
INSERT INTO `seat` VALUES (503, NULL, 2, 22, 1);
INSERT INTO `seat` VALUES (504, NULL, 2, 23, 3);
INSERT INTO `seat` VALUES (505, NULL, 2, 24, 3);
INSERT INTO `seat` VALUES (506, NULL, 2, 25, 2);
INSERT INTO `seat` VALUES (507, NULL, 2, 26, 3);
INSERT INTO `seat` VALUES (508, NULL, 2, 27, 1);
INSERT INTO `seat` VALUES (509, NULL, 2, 28, 3);
INSERT INTO `seat` VALUES (510, NULL, 2, 29, 3);
INSERT INTO `seat` VALUES (511, NULL, 2, 30, 1);
INSERT INTO `seat` VALUES (512, NULL, 3, 11, 3);
INSERT INTO `seat` VALUES (513, NULL, 3, 12, 3);
INSERT INTO `seat` VALUES (514, NULL, 3, 13, 3);
INSERT INTO `seat` VALUES (515, NULL, 3, 14, 3);
INSERT INTO `seat` VALUES (516, NULL, 3, 15, 3);
INSERT INTO `seat` VALUES (517, NULL, 3, 16, 3);
INSERT INTO `seat` VALUES (518, NULL, 3, 17, 3);
INSERT INTO `seat` VALUES (519, NULL, 3, 18, 3);
INSERT INTO `seat` VALUES (520, NULL, 3, 19, 3);
INSERT INTO `seat` VALUES (521, NULL, 3, 20, 3);
INSERT INTO `seat` VALUES (522, NULL, 3, 21, 3);
INSERT INTO `seat` VALUES (523, NULL, 3, 22, 3);
INSERT INTO `seat` VALUES (524, NULL, 3, 23, 3);
INSERT INTO `seat` VALUES (525, NULL, 3, 24, 3);
INSERT INTO `seat` VALUES (526, NULL, 3, 25, 3);
INSERT INTO `seat` VALUES (527, NULL, 3, 26, 3);
INSERT INTO `seat` VALUES (528, NULL, 3, 27, 3);
INSERT INTO `seat` VALUES (529, NULL, 3, 28, 3);
INSERT INTO `seat` VALUES (530, NULL, 3, 29, 3);
INSERT INTO `seat` VALUES (531, NULL, 3, 30, 3);
INSERT INTO `seat` VALUES (535, NULL, 1, 31, 0);
INSERT INTO `seat` VALUES (536, NULL, 1, 32, 1);
INSERT INTO `seat` VALUES (537, NULL, 1, 33, 2);
INSERT INTO `seat` VALUES (538, NULL, 1, 34, 1);
INSERT INTO `seat` VALUES (539, NULL, 1, 35, 1);
INSERT INTO `seat` VALUES (540, NULL, 1, 36, 0);
INSERT INTO `seat` VALUES (541, NULL, 1, 37, 0);
INSERT INTO `seat` VALUES (542, NULL, 1, 38, 0);
INSERT INTO `seat` VALUES (543, NULL, 1, 39, 0);
INSERT INTO `seat` VALUES (544, NULL, 1, 40, 1);
INSERT INTO `seat` VALUES (545, NULL, 1, 41, 1);
INSERT INTO `seat` VALUES (546, NULL, 1, 42, 1);
INSERT INTO `seat` VALUES (547, NULL, 1, 43, 0);
INSERT INTO `seat` VALUES (548, NULL, 1, 44, 0);
INSERT INTO `seat` VALUES (549, NULL, 1, 45, 1);
INSERT INTO `seat` VALUES (550, NULL, 1, 46, 2);
INSERT INTO `seat` VALUES (551, NULL, 1, 47, 2);
INSERT INTO `seat` VALUES (552, NULL, 1, 48, 2);
INSERT INTO `seat` VALUES (553, NULL, 1, 49, 0);
INSERT INTO `seat` VALUES (554, NULL, 1, 50, 0);
INSERT INTO `seat` VALUES (555, NULL, 1, 51, 0);
INSERT INTO `seat` VALUES (556, NULL, 1, 52, 1);
INSERT INTO `seat` VALUES (557, NULL, 1, 53, 2);
INSERT INTO `seat` VALUES (558, NULL, 1, 54, 2);
INSERT INTO `seat` VALUES (559, NULL, 1, 55, 0);
INSERT INTO `seat` VALUES (560, NULL, 1, 56, 0);
INSERT INTO `seat` VALUES (561, NULL, 1, 57, 2);
INSERT INTO `seat` VALUES (562, NULL, 1, 58, 0);
INSERT INTO `seat` VALUES (563, NULL, 1, 59, 1);
INSERT INTO `seat` VALUES (564, NULL, 1, 60, 0);
INSERT INTO `seat` VALUES (565, NULL, 2, 31, 0);
INSERT INTO `seat` VALUES (566, NULL, 2, 32, 0);
INSERT INTO `seat` VALUES (567, NULL, 2, 33, 2);
INSERT INTO `seat` VALUES (568, NULL, 2, 34, 2);
INSERT INTO `seat` VALUES (569, NULL, 2, 35, 0);
INSERT INTO `seat` VALUES (570, NULL, 2, 36, 0);
INSERT INTO `seat` VALUES (571, NULL, 2, 37, 2);
INSERT INTO `seat` VALUES (572, NULL, 2, 38, 0);
INSERT INTO `seat` VALUES (573, NULL, 2, 39, 2);
INSERT INTO `seat` VALUES (574, NULL, 2, 40, 0);
INSERT INTO `seat` VALUES (575, NULL, 2, 41, 2);
INSERT INTO `seat` VALUES (576, NULL, 2, 42, 0);
INSERT INTO `seat` VALUES (577, NULL, 2, 43, 0);
INSERT INTO `seat` VALUES (578, NULL, 2, 44, 1);
INSERT INTO `seat` VALUES (579, NULL, 2, 45, 0);
INSERT INTO `seat` VALUES (580, NULL, 2, 46, 0);
INSERT INTO `seat` VALUES (581, NULL, 2, 47, 2);
INSERT INTO `seat` VALUES (582, NULL, 2, 48, 1);
INSERT INTO `seat` VALUES (583, NULL, 2, 49, 0);
INSERT INTO `seat` VALUES (584, NULL, 2, 50, 1);
INSERT INTO `seat` VALUES (585, NULL, 2, 51, 2);
INSERT INTO `seat` VALUES (586, NULL, 2, 52, 2);
INSERT INTO `seat` VALUES (587, NULL, 2, 53, 2);
INSERT INTO `seat` VALUES (588, NULL, 2, 54, 2);
INSERT INTO `seat` VALUES (589, NULL, 2, 55, 2);
INSERT INTO `seat` VALUES (590, NULL, 2, 56, 1);
INSERT INTO `seat` VALUES (591, NULL, 2, 57, 1);
INSERT INTO `seat` VALUES (592, NULL, 2, 58, 1);
INSERT INTO `seat` VALUES (593, NULL, 2, 59, 2);
INSERT INTO `seat` VALUES (594, NULL, 2, 60, 1);
INSERT INTO `seat` VALUES (595, NULL, 3, 31, 3);
INSERT INTO `seat` VALUES (596, NULL, 3, 32, 3);
INSERT INTO `seat` VALUES (597, NULL, 3, 33, 3);
INSERT INTO `seat` VALUES (598, NULL, 3, 34, 3);
INSERT INTO `seat` VALUES (599, NULL, 3, 35, 3);
INSERT INTO `seat` VALUES (600, NULL, 3, 36, 3);
INSERT INTO `seat` VALUES (601, NULL, 3, 37, 3);
INSERT INTO `seat` VALUES (602, NULL, 3, 38, 3);
INSERT INTO `seat` VALUES (603, NULL, 3, 39, 3);
INSERT INTO `seat` VALUES (604, NULL, 3, 40, 3);
INSERT INTO `seat` VALUES (605, NULL, 3, 41, 3);
INSERT INTO `seat` VALUES (606, NULL, 3, 42, 3);
INSERT INTO `seat` VALUES (607, NULL, 3, 43, 3);
INSERT INTO `seat` VALUES (608, NULL, 3, 44, 3);
INSERT INTO `seat` VALUES (609, NULL, 3, 45, 3);
INSERT INTO `seat` VALUES (610, NULL, 3, 46, 3);
INSERT INTO `seat` VALUES (611, NULL, 3, 47, 3);
INSERT INTO `seat` VALUES (612, NULL, 3, 48, 3);
INSERT INTO `seat` VALUES (613, NULL, 3, 49, 3);
INSERT INTO `seat` VALUES (614, NULL, 3, 50, 3);
INSERT INTO `seat` VALUES (615, NULL, 3, 51, 3);
INSERT INTO `seat` VALUES (616, NULL, 3, 52, 3);
INSERT INTO `seat` VALUES (617, NULL, 3, 53, 3);
INSERT INTO `seat` VALUES (618, NULL, 3, 54, 3);
INSERT INTO `seat` VALUES (619, NULL, 3, 55, 3);
INSERT INTO `seat` VALUES (620, NULL, 3, 56, 3);
INSERT INTO `seat` VALUES (621, NULL, 3, 57, 3);
INSERT INTO `seat` VALUES (622, NULL, 3, 58, 3);
INSERT INTO `seat` VALUES (623, NULL, 3, 59, 3);
INSERT INTO `seat` VALUES (624, NULL, 3, 60, 3);

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '用户编号，主键',
  `username` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户账号(学号)',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户密码',
  `nickname` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '用户零零漆' COMMENT '用户昵称',
  `sex` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '性别',
  `age` int(10) NULL DEFAULT NULL COMMENT '年龄',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '电话',
  `chat` tinyint(1) NOT NULL DEFAULT 1 COMMENT '发言权(1可发言)',
  `role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户角色(student/teacher)',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'default.png' COMMENT '头像',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 38 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, '20230001', '123456', '张三', '男', 20, '13800138001', 1, 'student', 'avatar1.png');
INSERT INTO `user` VALUES (3, 'T1001', '123456', '王教授', '男', 45, '13800138003', 1, 'teacher', 'avatar3.png');
INSERT INTO `user` VALUES (4, 'useraaa', '123456', '用户零零漆', NULL, NULL, NULL, 1, 'STUDENT', 'default.png');
INSERT INTO `user` VALUES (5, 'user', '123456', '七八九', '男', 20, '13800001111', 1, 'STUDENT', 'default.png');
INSERT INTO `user` VALUES (6, 'qqq', '123456', '零零柒', '男', 23, '', 1, 'STUDENT', 'default.png');
INSERT INTO `user` VALUES (8, '20250002', '123456', '知识捕手', '女', 22, '13922334455', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (9, '20250003', '123456', '图书馆常客', '男', 19, '15033445566', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (10, '20250004', '123456', '自习达人', '女', 21, '15144556677', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (11, '20250005', '123456', '早鸟学者', '男', 23, '15255667788', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (12, '20250006', '123456', '夜猫子', '女', 20, '15366778899', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (13, '20250007', '123456', '论文战士', '男', 24, '15477889900', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (14, '20250008', '123456', '书海航行者', '女', 18, '15588990011', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (15, '20250009', '123456', '安静读者', '男', 22, '15699001122', 0, 'student', 'default.png');
INSERT INTO `user` VALUES (16, '20250010', '123456', '知识挖掘机', '女', 21, '15700112233', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (17, '20250011', '123456', '学习永动机', '男', 20, '15811223344', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (18, '20250012', '123456', '资料收集者', '女', 19, '15922334455', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (19, '20250013', '123456', '古籍守护者', '男', 25, '16533445566', 0, 'student', 'default.png');
INSERT INTO `user` VALUES (20, '20250014', '123456', '现代书虫', '女', 22, '16644556677', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (21, '20250015', '123456', '数字阅读者', '男', 21, '16755667788', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (22, '20250016', '123456', '墨水爱好者', '女', 20, '16866778899', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (23, '20250017', '123456', '书架园丁', '男', 23, '16977889900', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (24, '20250018', '123456', '知识建筑师', '女', 19, '17088990011', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (25, '20250019', '123456', '智慧追寻者', '男', 24, '17199001122', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (26, '20250020', '123456', '文献探险家', '女', 20, '17200112233', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (27, '20250021', '123456', '数据捕手', '男', 21, '17311223344', 0, 'student', 'default.png');
INSERT INTO `user` VALUES (28, '20250022', '123456', '信息整理师', '女', 22, '17422334455', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (29, '20250023', '123456', '学术导航员', '男', 23, '17533445566', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (30, '20250024', '123456', '知识催化剂', '女', 19, '17644556677', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (31, '20250025', '123456', '思维导图师', '男', 20, '17755667788', 0, 'student', 'default.png');
INSERT INTO `user` VALUES (32, '20250026', '123456', '逻辑编织者', '女', 21, '17866778899', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (33, '20250027', '123456', '概念工程师', '男', 22, '17977889900', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (34, '20250028', '123456', '学术火花', '女', 23, '18088990011', 1, 'student', 'default.png');
INSERT INTO `user` VALUES (35, '20250029', '123456', '教育领航员', '男', 35, '18199001122', 1, 'teacher', 'default.png');
INSERT INTO `user` VALUES (36, '20250030', '123456', '学术导师', '女', 42, '18200112233', 1, 'teacher', 'default.png');
INSERT INTO `user` VALUES (37, 'www', '123456', '零零柒', '男', 20, '', 1, 'STUDENT', 'default.png');

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- Table structure for feedback（违规举报，2026-10 新增）
-- ----------------------------
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '举报ID',
  `reporter_id` int NOT NULL COMMENT '举报人',
  `room_id` int NULL DEFAULT NULL COMMENT '楼层/房间',
  `target_user_id` int NULL DEFAULT NULL COMMENT '被举报人',
  `target_message_id` int NULL DEFAULT NULL COMMENT '关联消息',
  `type` varchar(20) NOT NULL COMMENT '类型(TALK喧哗/EAT饮食/SEAT_OCCUPY占座/OTHER)',
  `content` varchar(255) NULL DEFAULT NULL COMMENT '补充描述',
  `is_anonymous` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否匿名',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态(0待处理/1已处理/2驳回)',
  `handle_note` varchar(255) NULL DEFAULT NULL COMMENT '处理备注',
  `handled_by` int NULL DEFAULT NULL COMMENT '处理人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `handled_at` datetime NULL DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_reporter`(`reporter_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '违规举报' ROW_FORMAT = Dynamic;
