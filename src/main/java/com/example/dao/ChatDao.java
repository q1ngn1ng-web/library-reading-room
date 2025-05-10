package com.example.dao;

import com.example.entity.Chat;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

@Repository
public interface ChatDao extends Mapper<Chat> {

	List<Chat> findBySearch(@Param("search") Chat search);

}
