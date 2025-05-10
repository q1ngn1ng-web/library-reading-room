package com.example.dao;

import com.example.entity.ChatMessage;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

@Repository
public interface ChatMessageDao extends Mapper<ChatMessage> {

    List<ChatMessage> findBySearch(@Param("search") ChatMessage search);

    List<ChatMessage> findByRoomId(@Param("roomId") Integer roomId,
            @Param("page") Integer page,
            @Param("size") Integer size);
}
