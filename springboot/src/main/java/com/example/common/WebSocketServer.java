package com.example.common;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.URLUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.entity.ChatMessage;
import com.example.entity.ChatRoom;
import com.example.entity.User;
import com.example.entity.Reservation;
import com.example.entity.ReservationStatus;
import com.example.service.ChatMessageService;
import com.example.service.ChatRoomService;
import com.example.dao.ReservationDao;
import com.example.dao.UserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import cn.hutool.http.HttpUtil;
import cn.hutool.core.util.CharsetUtil;

/**
 * websocket服务
 */
@ServerEndpoint(value = "/imserver/{roomId}/{userId}/{avatar}")
@Component
public class WebSocketServer implements InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);
    // 不知道有没有添加管理员的
    /**
     * 记录当前在线连接数
     */
    // 保留原有的全局会话管理，用于系统消息广播
    public static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    public static final Map<String, Object> userMap = new ConcurrentHashMap<>();

    // 新增房间会话管理
    private static final Map<Integer, Map<String, Session>> roomSessions = new ConcurrentHashMap<>();
    private static final Map<Integer, Map<String, Object>> roomUsers = new ConcurrentHashMap<>();

    @Resource
    private ChatRoomService chatRoomService;

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private ReservationDao reservationDao;

    @Resource
    private UserDao userDao;

    static ChatMessageService staticChatMessageService;
    static ChatRoomService staticChatRoomService;
    static ReservationDao staticReservationDao;
    static UserDao staticUserDao;

    @OnOpen
    public void onOpen(Session session, @PathParam("roomId") Integer roomId,
            @PathParam("userId") String userId,
            @PathParam("avatar") String avatar) {
        log.info("WebSocket连接请求: roomId={}, userId={}, avatar={}", roomId, userId, avatar);

        try {
            // 1. 检查房间是否存在且可用
            ChatRoom room = staticChatRoomService.findById(roomId);
            if (room == null || !room.getIsChat() || !room.getIsAvailable()) {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT,
                        room == null ? "聊天室不存在" : "聊天室已禁言"));
                return;
            }

            // 2. 检查房间人数是否已满
            if (room.getCurrentUser() >= room.getMaxUser()) {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "聊天室已满"));
                return;
            }

            // 解码用户ID和头像
            String decodedUserId = URLUtil.decode(Base64.decodeStr(userId));
            String decodedAvatar = URLUtil.decode(avatar);

            try {
                // 从WebSocket URL参数中获取角色信息
                Map<String, String> pathParameters = session.getPathParameters();
                String queryString = session.getQueryString();
                Map<String, String> queryParams = HttpUtil.decodeParamMap(queryString, CharsetUtil.CHARSET_UTF_8);

                // 检查是否是管理员
                boolean isAdmin = "admin".equalsIgnoreCase(queryParams.get("role"));
                log.info("用户角色检查 - userId: {}, isAdmin: {}", decodedUserId, isAdmin);

                // 如果不是管理员，则验证预约
                if (!isAdmin) {
                    Integer userIdInt = Integer.parseInt(decodedUserId);
                    // 获取用户当前预约
                    Reservation reservation = staticReservationDao.findCurrentByUserId(userIdInt);

                    // 检查预约是否有效
                    if (reservation == null || !isValidReservation(reservation)) {
                        log.info("用户 {} 无有效预约，拒绝聊天室访问", decodedUserId);
                        session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "无有效预约，无法进入聊天室"));
                        return;
                    }
                    log.info("用户 {} 预约验证通过，允许访问聊天室 {}", decodedUserId, roomId);
                } else {
                    log.info("管理员 {} 访问聊天室 {}", decodedUserId, roomId);
                }
            } catch (Exception e) {
                log.error("验证用户失败", e);
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "验证用户时发生错误"));
                return;
            }

            // 3. 初始化房间会话
            roomSessions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());
            roomUsers.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());

            // 检查用户是否已经在房间中(通过用户ID判断)
            boolean userAlreadyInRoom = roomUsers.get(roomId).values().stream()
                    .anyMatch(user -> decodedUserId.equals(((Dict) user).getStr("userId")));

            if (userAlreadyInRoom) {
                log.info("用户ID {} 已经在房间 {} 中，拒绝重复加入", decodedUserId, roomId);
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "你已经在此聊天室中"));
                return;
            }

            // 4. 添加用户到房间
            roomSessions.get(roomId).put(session.getId(), session);
            Dict user = Dict.create()
                    .set("userId", decodedUserId)
                    .set("avatar", decodedAvatar);
            roomUsers.get(roomId).put(session.getId(), user);

            // 5. 更新房间人数
            staticChatRoomService.updateCurrentUser(roomId, 1);

            // 6. 发送房间用户列表更新
            sendRoomMessage(roomId, JSONUtil.toJsonStr(Dict.create()
                    .set("type", "userList")
                    .set("users", roomUsers.get(roomId).values())));

            log.info("[onOpen] 用户加入房间成功: roomId={}, session={}, 当前房间人数={}",
                    roomId, session.getId(), roomSessions.get(roomId).size());
        } catch (Exception e) {
            log.error("处理WebSocket连接时发生错误", e);
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "服务器内部错误"));
            } catch (IOException ex) {
                log.error("关闭WebSocket连接失败", ex);
            }
        }
    }

    /**
     * 判断预约是否有效
     */
    private static boolean isValidReservation(Reservation reservation) {
        // 检查预约是否有效：状态是否正确
        if (reservation.getStatus() != ReservationStatus.SUCCESS.getCode() &&
                reservation.getStatus() != ReservationStatus.CHECKED_IN.getCode()) {
            return false;
        }

        // 如果未签到，检查是否已超时
        if (reservation.getStatus() == ReservationStatus.SUCCESS.getCode()) {
            Date now = new Date();
            if (now.after(reservation.getTimeoutAt())) {
                return false; // 预约已超时
            }
        }

        return true;
    }

    @OnClose
    public void onClose(Session session, @PathParam("roomId") Integer roomId) {
        // 1. 从房间移除用户
        if (roomSessions.containsKey(roomId)) {
            roomSessions.get(roomId).remove(session.getId());
            roomUsers.get(roomId).remove(session.getId());

            // 2. 更新房间人数
            staticChatRoomService.updateCurrentUser(roomId, -1);

            // 3. 发送房间用户列表更新
            sendRoomMessage(roomId, JSONUtil.toJsonStr(Dict.create()
                    .set("type", "userList")
                    .set("users", roomUsers.get(roomId).values())));
        }

        log.info("[onClose] 用户离开房间 roomId={}, session={}, 当前房间人数={}",
                roomId, session.getId(),
                roomSessions.containsKey(roomId) ? roomSessions.get(roomId).size() : 0);
    }

    @OnMessage
    public void onMessage(String message, Session fromSession, @PathParam("roomId") Integer roomId) {
        log.info("服务端收到消息: roomId={}, message={}", roomId, message);

        try {
            // 解析消息类型
            JSONObject jsonObject = JSONUtil.parseObj(message);
            String type = jsonObject.getStr("type");

            if ("message".equals(type)) {
                // 处理聊天消息
                String content = jsonObject.getStr("content");
                Boolean isAnonymous = jsonObject.getBool("isAnonymous", false);

                // 从消息中获取用户名
                String username = jsonObject.getStr("username");
                if (username == null || username.isEmpty()) {
                    log.warn("消息中缺少username字段，消息将无法发送");
                    return;
                }

                // 获取当前用户信息（从Session中）
                Dict userDict = (Dict) roomUsers.get(roomId).get(fromSession.getId());
                if (userDict == null) {
                    log.warn("找不到用户信息，无法发送消息: roomId={}, sessionId={}", roomId, fromSession.getId());
                    return;
                }

                // 创建聊天消息
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setRoomId(roomId);
                chatMessage.setContent(content);
                chatMessage.setTime(DateUtil.now());
                chatMessage.setType("message");
                chatMessage.setIsAnonymous(isAnonymous);

                // 使用从消息中获取的用户名
                chatMessage.setName(username);
                chatMessage.setAvatar((String) userDict.get("avatar"));

                // 设置用户ID - 优先从消息中获取
                Integer userId = null;
                if (jsonObject.containsKey("userId")) {
                    userId = jsonObject.getInt("userId");
                    if (userId != null && userId > 0) {
                        chatMessage.setUserId(userId);
                        log.info("从消息中获取用户ID: {}", userId);
                    } else {
                        log.warn("消息中的userId无效: {}, 使用默认值", userId);
                        chatMessage.setUserId(0); // 使用默认值0
                    }
                } else {
                    log.warn("消息中未包含userId, 使用默认值");
                    chatMessage.setUserId(0); // 使用默认值0
                }

                log.info("保存聊天消息: roomId={}, content={}, from={}, userId={}",
                        roomId, content, chatMessage.getName(), chatMessage.getUserId());

                // 存储数据到数据库
                chatMessage = staticChatMessageService.add(chatMessage);

                // 发送消息到房间，按照前端预期的格式
                Dict responseMessage = Dict.create()
                        .set("type", "message")
                        .set("id", chatMessage.getId())
                        .set("roomId", chatMessage.getRoomId())
                        .set("userId", chatMessage.getUserId())
                        .set("name", chatMessage.getName())
                        .set("avatar", chatMessage.getAvatar())
                        .set("content", chatMessage.getContent())
                        .set("time", chatMessage.getTime())
                        .set("isAnonymous", chatMessage.getIsAnonymous());

                String jsonResponse = JSONUtil.toJsonStr(responseMessage);
                log.info("广播消息到房间: roomId={}, message={}", roomId, jsonResponse);
                sendRoomMessage(roomId, jsonResponse);

            } else if ("heartbeat".equals(type)) {
                // 处理心跳包
                log.debug("收到心跳包，房间: {}, 会话: {}", roomId, fromSession.getId());
            } else {
                log.warn("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理消息失败: {}", e.getMessage(), e);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("[onError] 发生错误", error);
    }

    // 发送消息到指定房间
    private void sendRoomMessage(Integer roomId, String message) {
        Map<String, Session> roomSession = roomSessions.get(roomId);
        if (roomSession != null) {
            roomSession.values().forEach(session -> {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    log.error("发送消息到房间{}失败", roomId, e);
                }
            });
        }
    }

    /**
     * 定期检查并断开过期预约用户的连接
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void disconnectExpiredUsers() {
        log.debug("开始检查过期预约用户...");
        Date now = new Date();
        int totalDisconnected = 0;

        // 遍历所有房间
        for (Integer roomId : roomSessions.keySet()) {
            Map<String, Session> sessions = roomSessions.get(roomId);
            Map<String, Object> users = roomUsers.get(roomId);

            // 创建待断开连接的列表（避免并发修改异常）
            List<Session> sessionsToClose = new ArrayList<>();

            for (String sessionId : sessions.keySet()) {
                Dict user = (Dict) users.get(sessionId);
                if (user != null && user.getStr("userId") != null) {
                    try {
                        Integer userId = Integer.parseInt(user.getStr("userId"));
                        Reservation reservation = staticReservationDao.findCurrentByUserId(userId);

                        // 检查预约是否无效或已过期
                        if (reservation == null || !isValidReservation(reservation)) {
                            log.info("用户 {} 的预约已过期或无效，准备断开连接", userId);
                            sessionsToClose.add(sessions.get(sessionId));
                        }
                    } catch (Exception e) {
                        log.error("检查用户 {} 预约状态时出错", user.getStr("userId"), e);
                    }
                }
            }

            // 关闭所有无效会话
            for (Session session : sessionsToClose) {
                try {
                    session.close(new CloseReason(
                            CloseReason.CloseCodes.VIOLATED_POLICY, "预约已过期，请重新预约"));
                    totalDisconnected++;
                } catch (IOException e) {
                    log.error("关闭过期会话失败", e);
                }
            }
        }

        if (totalDisconnected > 0) {
            log.info("已断开 {} 个过期预约用户的连接", totalDisconnected);
        } else {
            log.debug("没有发现过期用户连接");
        }
    }

    // 静态方法，供其他类调用
    public static void sendRoomStatusUpdate(Integer roomId, Boolean status, String message) {
        Map<String, Session> roomSession = roomSessions.get(roomId);
        if (roomSession != null && !roomSession.isEmpty()) {
            String statusMessage = JSONUtil.toJsonStr(Dict.create()
                    .set("type", "roomStatus")
                    .set("status", status)
                    .set("message", message));

            roomSession.values().forEach(session -> {
                try {
                    session.getBasicRemote().sendText(statusMessage);
                } catch (IOException e) {
                    log.error("发送状态更新到房间{}失败", roomId, e);
                }
            });
        }
    }

    // 获取房间会话，供其他类调用
    public static Map<String, Session> getRoomSessions(Integer roomId) {
        return roomSessions.get(roomId);
    }

    @Override
    public void afterPropertiesSet() {
        staticChatMessageService = chatMessageService;
        staticChatRoomService = chatRoomService;
        staticReservationDao = reservationDao;
        staticUserDao = userDao;
        log.info("WebSocketServer初始化完成，静态服务对象已注入");
    }
}
