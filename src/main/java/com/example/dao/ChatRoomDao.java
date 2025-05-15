package com.example.dao;

import com.example.entity.ChatRoom;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

@Repository
public interface ChatRoomDao extends Mapper<ChatRoom> {
    List<ChatRoom> findBySearch(@Param("search") ChatRoom search);

    int updateCurrentUser(@Param("roomId") Integer roomId, @Param("increment") int increment);

    /**
     * 统计活跃聊天室数量
     *
     * @return 活跃聊天室数量
     */
    @Select("select count(*) from chat_room where is_available = 1")
    int countActiveRooms();
}