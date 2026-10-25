package com.example.common;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.entity.Admin;
import com.example.entity.ChatMessage;
import com.example.entity.ChatRoom;
import com.example.entity.User;
import com.example.entity.Reservation;
import com.example.entity.ReservationStatus;
import com.example.service.AdminService;
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
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 服务。
 * 握手协议：/imserver/{roomId}?token=<JWT>——身份、角色、昵称、头像全部由服务端
 * 验签 token 后从数据库反查，不信任任何客户端传入的身份字段。
 */
@ServerEndpoint(value = "/imserver/{roomId}")
@Component
public class WebSocketServer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);

    /**
     * 记录当前在线连接数（保留全局会话管理，用于系统消息广播）
     */
    public static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    public static final Map<String, Object> userMap = new ConcurrentHashMap<>();

    // 房间会话管理
    private static final Map<Integer, Map<String, Session>> roomSessions = new ConcurrentHashMap<>();
    private static final Map<Integer, Map<String, Object>> roomUsers = new ConcurrentHashMap<>();

    @Resource
    private ChatRoomService chatRoomService;

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private AdminService adminService;

    @Resource
    private ReservationDao reservationDao;

    @Resource
    private UserDao userDao;

    static ChatMessageService staticChatMessageService;
    static ChatRoomService staticChatRoomService;
    static AdminService staticAdminService;
    static ReservationDao staticReservationDao;
    static UserDao staticUserDao;

    @OnOpen
    public void onOpen(Session session, @PathParam("roomId") Integer roomId) {
        log.info("WebSocket连接请求: roomId={}", roomId);

        try {
            // 1. token 鉴权：身份以 token 为准，URL 不再携带任何身份参数
            String token = firstParam(session, "token");
            if (StrUtil.isBlank(token)) {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "缺少token，请重新登录"));
                return;
            }
            Integer userId;
            String role;
            try {
                DecodedJWT jwt = JwtTokenUtils.verifyToken(token);
                userId = JwtTokenUtils.getTokenUserId(jwt);
                role = JwtTokenUtils.getTokenRole(jwt);
            } catch (Exception e) {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "token无效，请重新登录"));
                return;
            }

            // 2. 检查房间是否存在、开放且未满
            ChatRoom room = staticChatRoomService.findById(roomId);
            if (room == null || !room.getIsChat() || !room.getIsAvailable()) {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT,
                        room == null ? "聊天室不存在" : "聊天室已禁言"));
                return;
            }
            if (room.getCurrentUser() >= room.getMaxUser()) {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "聊天室已满"));
                return;
            }

            // 3. 身份信息（昵称/头像）从数据库反查；管理员豁免预约校验
            String displayName;
            String avatar;
            if ("admin".equals(role)) {
                Admin admin = staticAdminService.findById(userId);
                if (admin == null) {
                    session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "管理员不存在"));
                    return;
                }
                displayName = "[管理员]" + admin.getNickname();
                avatar = admin.getAvatar();
                log.info("管理员 {} 访问聊天室 {}", userId, roomId);
            } else {
                User user = staticUserDao.selectByPrimaryKey(userId);
                if (user == null) {
                    session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "用户不存在"));
                    return;
                }
                displayName = user.getNickname();
                avatar = user.getAvatar();

                Reservation reservation = staticReservationDao.findCurrentByUserId(userId);
                if (reservation == null || !isValidReservation(reservation)) {
                    log.info("用户 {} 无有效预约，拒绝聊天室访问", userId);
                    session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "无有效预约，无法进入聊天室"));
                    return;
                }
            }

            // 4. 注册房间会话（防重复加入）
            roomSessions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());
            roomUsers.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());

            String userIdStr = userId.toString();
            boolean userAlreadyInRoom = roomUsers.get(roomId).values().stream()
                    .anyMatch(u -> userIdStr.equals(((Dict) u).getStr("userId")));
            if (userAlreadyInRoom) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "你已经在此聊天室中"));
                return;
            }

            roomSessions.get(roomId).put(session.getId(), session);
            roomUsers.get(roomId).put(session.getId(), Dict.create()
                    .set("userId", userIdStr)
                    .set("avatar", avatar)
                    .set("name", displayName)
                    .set("role", role));

            // 5. 更新房间人数并广播用户列表
            staticChatRoomService.updateCurrentUser(roomId, 1);
            sendRoomMessage(roomId, JSONUtil.toJsonStr(Dict.create()
                    .set("type", "userList")
                    .set("users", roomUsers.get(roomId).values())));

            log.info("[onOpen] 用户加入房间成功: roomId={}, userId={}, role={}, 当前房间人数={}",
                    roomId, userId, role, roomSessions.get(roomId).size());
        } catch (Exception e) {
            log.error("处理WebSocket连接时发生错误", e);
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "服务器内部错误"));
            } catch (IOException ex) {
                log.error("关闭WebSocket连接失败", ex);
            }
        }
    }

    private static String firstParam(Session session, String name) {
        Map<String, List<String>> params = session.getRequestParameterMap();
        List<String> values = params == null ? null : params.get(name);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }

    /**
     * 判断预约是否有效
     */
    private static boolean isValidReservation(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.SUCCESS.getCode() &&
                reservation.getStatus() != ReservationStatus.CHECKED_IN.getCode()) {
            return false;
        }
        // 如果未签到，检查是否已超时
        if (reservation.getStatus() == ReservationStatus.SUCCESS.getCode()) {
            return new Date().before(reservation.getTimeoutAt());
        }
        return true;
    }

    @OnClose
    public void onClose(Session session, @PathParam("roomId") Integer roomId) {
        if (roomSessions.containsKey(roomId)) {
            roomSessions.get(roomId).remove(session.getId());
            roomUsers.get(roomId).remove(session.getId());

            staticChatRoomService.updateCurrentUser(roomId, -1);

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
        try {
            JSONObject jsonObject = JSONUtil.parseObj(message);
            String type = jsonObject.getStr("type");

            if ("message".equals(type)) {
                String content = jsonObject.getStr("content");
                Boolean isAnonymous = jsonObject.getBool("isAnonymous", false);

                // 身份一律取服务端会话（握手时已由 token 反查），忽略客户端传入的 username/userId
                Dict userDict = (Dict) roomUsers.get(roomId).get(fromSession.getId());
                if (userDict == null) {
                    log.warn("找不到用户信息，无法发送消息: roomId={}, sessionId={}", roomId, fromSession.getId());
                    return;
                }

                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setRoomId(roomId);
                chatMessage.setContent(content);
                chatMessage.setTime(DateUtil.now());
                chatMessage.setType("message");
                chatMessage.setIsAnonymous(isAnonymous);
                chatMessage.setName(userDict.getStr("name"));
                chatMessage.setAvatar((String) userDict.get("avatar"));
                chatMessage.setUserId(Integer.parseInt(userDict.getStr("userId")));

                chatMessage = staticChatMessageService.add(chatMessage);

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

                sendRoomMessage(roomId, JSONUtil.toJsonStr(responseMessage));

            } else if ("heartbeat".equals(type)) {
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
    @Scheduled(fixedRate = 60000)
    public void disconnectExpiredUsers() {
        Date now = new Date();
        int totalDisconnected = 0;

        for (Integer roomId : roomSessions.keySet()) {
            Map<String, Session> sessions = roomSessions.get(roomId);
            Map<String, Object> users = roomUsers.get(roomId);

            List<Session> sessionsToClose = new ArrayList<>();

            for (String sessionId : sessions.keySet()) {
                Dict user = (Dict) users.get(sessionId);
                // 管理员豁免预约校验
                if (user != null && "admin".equals(user.getStr("role"))) {
                    continue;
                }
                if (user != null && user.getStr("userId") != null) {
                    try {
                        Integer userId = Integer.parseInt(user.getStr("userId"));
                        Reservation reservation = staticReservationDao.findCurrentByUserId(userId);

                        if (reservation == null || !isValidReservation(reservation)) {
                            log.info("用户 {} 的预约已过期或无效，准备断开连接", userId);
                            sessionsToClose.add(sessions.get(sessionId));
                        }
                    } catch (Exception e) {
                        log.error("检查用户 {} 预约状态时出错", user.getStr("userId"), e);
                    }
                }
            }

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
        }
    }

    /**
     * 向指定用户单播消息（遍历其所在的所有房间会话；不在线则忽略——尽力而为）
     */
    public static void sendToUser(Integer userId, String type, Dict payload) {
        String json = JSONUtil.toJsonStr(payload.set("type", type));
        for (Integer roomId : roomUsers.keySet()) {
            Map<String, Object> users = roomUsers.get(roomId);
            for (Map.Entry<String, Object> entry : users.entrySet()) {
                if (String.valueOf(userId).equals(((Dict) entry.getValue()).getStr("userId"))) {
                    Session s = roomSessions.get(roomId).get(entry.getKey());
                    if (s != null && s.isOpen()) {
                        try {
                            s.getBasicRemote().sendText(json);
                        } catch (IOException e) {
                            log.error("向用户 {} 推送消息失败", userId, e);
                        }
                    }
                }
            }
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
        staticAdminService = adminService;
        staticReservationDao = reservationDao;
        staticUserDao = userDao;
        log.info("WebSocketServer初始化完成，静态服务对象已注入");
    }
}
