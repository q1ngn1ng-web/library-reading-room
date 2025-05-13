package com.example.dao;

import com.example.entity.Params;
import com.example.entity.Reservation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;

import java.util.Date;
import java.util.List;

@Repository
public interface ReservationDao extends Mapper<Reservation> {

    @Select("select * from reservation where user_id = #{userId} and status in (1, 5) limit 1")
    Reservation findCurrentByUserId(@Param("userId") Integer userId);

    @Select("select * from reservation where user_id = #{userId} order by create_time desc")
    List<Reservation> findHistoryByUserId(@Param("userId") Integer userId);

    @Select("select * from reservation where user_id = #{userId} and status = #{status} order by create_time desc")
    List<Reservation> findHistoryByUserIdAndStatus(@Param("userId") Integer userId, @Param("status") Integer status);

    @Select("select * from reservation where status = 1 and timeout_at < #{now}")
    List<Reservation> findTimeoutReservations(@Param("now") Date now);

    @Update("update reservation set status = #{status} where id = #{id}")
    void updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    @Update("update reservation set status = #{status}, real_start_time = #{realStartTime} where id = #{id}")
    void checkIn(@Param("id") Integer id, @Param("status") Integer status, @Param("realStartTime") Date realStartTime);

    @Update("update reservation set status = #{status}, real_end_time = #{realEndTime} where id = #{id}")
    void checkout(@Param("id") Integer id, @Param("status") Integer status, @Param("realEndTime") Date realEndTime);

    // Complex query defined in XML mapper
    List<Reservation> findBySearch(@Param("params") Params params);
}