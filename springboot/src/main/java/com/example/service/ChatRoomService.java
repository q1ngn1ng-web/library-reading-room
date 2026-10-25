package com.example.service;

import com.example.dao.ChatRoomDao;
import com.example.entity.ChatRoom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class ChatRoomService {
    private static final Logger log = LoggerFactory.getLogger(ChatRoomService.class);

    @Resource
    private ChatRoomDao chatRoomDao;

    /**
     * 初始化时修复所有负数的用户计数
     */
    @PostConstruct
    public void init() {
        int fixed = fixNegativeUserCounts();
        if (fixed > 0) {
            log.info("系统启动时修复了 {} 个聊天室的负数用户计数", fixed);
        }
    }

    /**
     * 修复负数的用户计数
     *
     * @return 修复的记录数
     */
    @Transactional
    public int fixNegativeUserCounts() {
        return chatRoomDao.fixNegativeUserCounts();
    }

    public ChatRoom createRoom(ChatRoom room) {
        room.setIsAvailable(true);
        room.setIsChat(true);
        room.setCurrentUser(0);
        chatRoomDao.insert(room);
        return room;
    }

    @Transactional
    public void updateRoomStatus(Integer roomId, Boolean status) {
        ChatRoom room = new ChatRoom();
        room.setId(roomId);
        room.setIsAvailable(status);
        chatRoomDao.updateByPrimaryKeySelective(room);
    }

    @Transactional
    public void updateRoomChatStatus(Integer roomId, Boolean chatStatus) {
        ChatRoom room = new ChatRoom();
        room.setId(roomId);
        room.setIsChat(chatStatus);
        chatRoomDao.updateByPrimaryKeySelective(room);
    }

    @Transactional
    public void updateCurrentUser(Integer roomId, int increment) {
        chatRoomDao.updateCurrentUser(roomId, increment);
    }

    /**
     * 增加聊天室当前用户数
     *
     * @param roomId 聊天室ID
     */
    public void incrementUserCount(Integer roomId) {
        updateCurrentUser(roomId, 1);
    }

    /**
     * 减少聊天室当前用户数
     *
     * @param roomId 聊天室ID
     */
    public void decrementUserCount(Integer roomId) {
        updateCurrentUser(roomId, -1);
    }

    public List<ChatRoom> findAll() {
        return chatRoomDao.selectAll();
    }

    public ChatRoom findById(Integer id) {
        return chatRoomDao.selectByPrimaryKey(id);
    }

    @Transactional
    public void deleteRoom(Integer id) {
        chatRoomDao.deleteByPrimaryKey(id);
    }
}