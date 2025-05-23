package com.example.dao;

import com.example.entity.Seat;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

@Repository
public interface SeatDao extends Mapper<Seat> {

    @Select("select * from seat where room_id = #{roomId}")
    List<Seat> findByRoomId(@Param("roomId") Integer roomId);

    @Select("select * from seat where room_id = #{roomId} and number = #{number} limit 1")
    Seat findByRoomIdAndNumber(@Param("roomId") Integer roomId, @Param("number") Integer number);

    @Select("select distinct room_id from seat order by room_id")
    List<Integer> findAllRooms();

    @Update("update seat set status = #{status} where id = #{id}")
    void updateSeatStatus(@Param("id") Integer id, @Param("status") Integer status);

    @Update("update seat set status = #{status} where room_id = #{roomId}")
    void batchUpdateSeatStatus(@Param("roomId") Integer roomId, @Param("status") Integer status);
}