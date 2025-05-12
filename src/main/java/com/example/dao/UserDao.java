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
}
