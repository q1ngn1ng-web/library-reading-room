package com.example.dao;

import com.example.entity.User;
import com.example.entity.Params;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

@Repository
public interface UserDao extends Mapper<User> {

    List<User> findBySearch(@Param("params") Params params);

    @Select("select * from user where username = #{username} limit 1")
    User findByName(@Param("username") String username);

    @Select("select * from user where username = #{username} and password = #{password} limit 1")
    User findByNameAndPassword(@Param("username") String username, @Param("password") String password);

    /**
     * 查询所有用户
     */
    @Select("select * from user")
    List<User> findAll();

    /**
     * 统计当前在线用户数 - 所有聊天室的currentUser总和
     *
     * @return 当前在线用户数
     */
    @Select("select COALESCE(SUM(currentUser), 0) from chat_room")
    int countCurrentOnlineUsers();

    /**
     * 统计指定日期活跃用户数
     *
     * @param date 日期字符串，格式：yyyy-MM-dd
     * @return 活跃用户数
     */
    @Select("select count(distinct user_id) from user_activity where DATE(activity_time) = #{date}")
    int countActiveUsersByDate(@Param("date") String date);
}
