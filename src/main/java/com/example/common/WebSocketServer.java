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
import com.example.service.ChatMessageService;
import com.example.service.ChatRoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * websocket服务
 */
@ServerEndpoint(value = "/imserver/{roomId}/{username}/{avatar}")
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

    static ChatMessageService staticChatMessageService;
    static ChatRoomService staticChatRoomService;

    @OnOpen
    public void onOpen(Session session, @PathParam("roomId") Integer roomId,
            @PathParam("username") String username,
            @PathParam("avatar") String avatar) {
        log.info("WebSocket连接请求: roomId={}, username={}, avatar={}", roomId, username, avatar);

        try {
            // 1. 检查房间是否存在且可用
            ChatRoom room = staticChatRoomService.findById(roomId);
            if (room == null || !room.getIsChat()||!room.getIsAvailable()) {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT,
                        room == null ? "聊天室不存在" : "聊天室已禁言"));
                return;
            }

            // 2. 检查房间人数是否已满
            if (room.getCurrentUser() >= room.getMaxUser()) {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "聊天室已满"));
                return;
            }

            // 3. 初始化房间会话
            roomSessions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());
            roomUsers.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());

            // 解码用户名和头像
            String decodedUsername = URLUtil.decode(Base64.decodeStr(username));
            String decodedAvatar = URLUtil.decode(avatar);

            // 检查用户是否已经在房间中(通过用户名判断)
            boolean userAlreadyInRoom = roomUsers.get(roomId).values().stream()
                    .anyMatch(user -> decodedUsername.equals(((Dict) user).getStr("name")));

            if (userAlreadyInRoom) {
                log.info("用户 {} 已经在房间 {} 中，拒绝重复加入", decodedUsername, roomId);
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "你已经在此聊天室中"));
                return;
            }

            // 4. 添加用户到房间
            roomSessions.get(roomId).put(session.getId(), session);
            Dict user = Dict.create()
                    .set("name", decodedUsername)
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

                // 从用户Session中获取名称和头像
                chatMessage.setName((String) userDict.get("name"));
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
        log.info("WebSocketServer初始化完成，静态服务对象已注入");
    }
}
/**
 * 连接建立成功调用的方法
 */
// @OnOpen
// public void onOpen(Session session, @PathParam("username") String username,
// @PathParam("avatar") String avatar) {
// sessionMap.put(session.getId(), session);
// Dict user = Dict.create().set("name",
// URLUtil.decode(Base64.decodeStr(username))).set("avatar",
// URLUtil.decode(Base64.decodeStr(avatar)));
// userMap.put(session.getId(), user);
// log.info("userMap: {}", userMap);
// log.info("[onOpen] 有新用户加入，session={}, 当前在线人数为：{}", session.getId(),
// sessionMap.size());
// sendAllMessage(JSONUtil.toJsonStr(Dict.create().set("users",
// userMap.values()))); // 后台发送消息给所有的客户端
// }
// /**
// * 连接关闭调用的方法
// */
// @OnClose
// public void onClose(Session session) {
// sessionMap.remove(session.getId());
// userMap.remove(session.getId());
// log.info("[onClose] 有一连接关闭，session={}, 当前在线人数为：{}", session.getId(),
// sessionMap.size());
// sendAllMessage(JSONUtil.toJsonStr(Dict.create().set("users",
// userMap.values()))); // 后台发送消息给所有的客户端
// }

// /**
// * 收到客户端消息后调用的方法
// * 后台收到客户端发送过来的消息
// * onMessage 是一个消息的中转站
// * 接受 浏览器端 socket.send 发送过来的 json数据
// * @param message 客户端发送过来的消息
// */
// @OnMessage
// public void onMessage(String message, Session fromSession) {
// log.info("服务端收到消息:{}", message);
// ChatMessage chatMessage = JSONUtil.toBean(message, ChatMessage.class);
// chatMessage.setTime(DateUtil.now());
// // 存储数据到数据库
// staticChatMessageService.add(chatMessage);
// String jsonStr = JSONUtil.toJsonStr(chatMessage); // 处理后的消息体
// this.sendAllMessage(jsonStr);
// log.info("[onMessage] 发送消息：{}", jsonStr);
// }

// @OnError
// public void onError(Session session, Throwable error) {
// log.error("[onError] 发生错误", error);
// }

// /**
// * 服务端发送消息给除了自己的其他客户端
// */
// private void sendMessage(Session fromSession, String message) {
// sessionMap.values().forEach(session -> {
// if (fromSession != session) {
// log.info("服务端给客户端[{}]发送消息{}", session.getId(), message);
// try {
// session.getBasicRemote().sendText(message);
// } catch (IOException e) {
// log.error("服务端发送消息给客户端异常", e);
// }
// }
// });
// }

// /**
// * 服务端发送消息给所有客户端
// */
// private void sendAllMessage(String message) {
// try {
// for (Session session : sessionMap.values()) {
// log.info("服务端给客户端[{}]发送消息{}", session.getId(), message);
// session.getBasicRemote().sendText(message);
// }
// } catch (Exception e) {
// log.error("服务端发送消息给客户端失败", e);
// }
// }

// @Override
// public void afterPropertiesSet() {
// staticChatMessageService = chatMessageService;
// }
