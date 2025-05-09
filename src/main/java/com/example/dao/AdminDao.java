package com.example.dao;

import com.example.entity.Admin;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;

@Repository
public interface AdminDao extends Mapper<Admin> {

    @Select("select * from admin where name = #{name} limit 1")
    Admin findByName(@Param("name") String name);

    @Select("select * from admin where name = #{name} and password = #{password} limit 1")
    Admin findByNameAndPassword(@Param("name") String name, @Param("password") String password);
}