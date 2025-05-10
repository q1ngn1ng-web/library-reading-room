package com.example.dao;

import com.example.entity.ChatRoom;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

@Repository
public interface ChatRoomDao extends Mapper<ChatRoom> {
    List<ChatRoom> findBySearch(@Param("search") ChatRoom search);
    int updateCurrentUser(@Param("roomId") Integer roomId, @Param("increment") int increment);
}