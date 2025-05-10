package com.example.service;

import com.example.dao.ChatRoomDao;
import com.example.entity.ChatRoom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ChatRoomService {
    @Resource
    private ChatRoomDao chatRoomDao;

    public ChatRoom createRoom(ChatRoom room) {
        room.setStatus(true);
        room.setCurrentUser(0);
        chatRoomDao.insert(room);
        return room;
    }

    @Transactional
    public void updateRoomStatus(Integer roomId, Boolean status) {
        ChatRoom room = new ChatRoom();
        room.setId(roomId);
        room.setStatus(status);
        chatRoomDao.updateByPrimaryKeySelective(room);
    }

    @Transactional
    public void updateCurrentUser(Integer roomId, int increment) {
        chatRoomDao.updateCurrentUser(roomId, increment);
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