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

    /**
     * 条件更新座位状态（乐观并发）：仅当前置状态匹配时才更新，返回影响行数。
     * 抢座/释放必须使用本方法，防止并发超卖与误覆盖管理员禁用态。
     */
    @Update("update seat set status = #{to} where id = #{id} and status = #{from}")
    int tryUpdateStatus(@Param("id") Integer id, @Param("from") Integer from, @Param("to") Integer to);

    @Update("update seat set status = #{status} where room_id = #{roomId}")
    void batchUpdateSeatStatus(@Param("roomId") Integer roomId, @Param("status") Integer status);
}