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

    @Select("select id, user_id as userId, seat_id as seatId, start_time as startTime, " +
            "end_time as endTime, timeout_at as timeoutAt, real_start_time as realStartTime, " +
            "real_end_time as realEndTime, status " +
            "from reservation where user_id = #{userId} and status in (1, 5) " +
            "order by id desc limit 1")
    Reservation findCurrentByUserId(@Param("userId") Integer userId);

    @Select("select * from reservation where user_id = #{userId} order by start_time desc")
    List<Reservation> findHistoryByUserId(@Param("userId") Integer userId);

    @Select("select * from reservation where user_id = #{userId} and status = #{status} order by start_time desc")
    List<Reservation> findHistoryByUserIdAndStatus(@Param("userId") Integer userId, @Param("status") Integer status);

    // 以下自定义查询必须显式别名列名（项目未开启驼峰映射，select * 会导致实体字段为 null）

    @Select("select id, user_id as userId, seat_id as seatId, start_time as startTime, " +
            "end_time as endTime, timeout_at as timeoutAt, real_start_time as realStartTime, " +
            "real_end_time as realEndTime, status " +
            "from reservation where status = 1 and timeout_at < #{now}")
    List<Reservation> findTimeoutReservations(@Param("now") Date now);

    /**
     * 已签到但预约时段已结束的预约（用于定时自动结算：用户离线未退座的兜底）
     */
    @Select("select id, user_id as userId, seat_id as seatId, start_time as startTime, " +
            "end_time as endTime, timeout_at as timeoutAt, real_start_time as realStartTime, " +
            "real_end_time as realEndTime, status " +
            "from reservation where status = 5 and end_time < #{now}")
    List<Reservation> findExpiredCheckedIn(@Param("now") Date now);

    /**
     * 某房间（楼层）的全部活跃预约（整层禁用时作废用）
     */
    @Select("select r.id, r.user_id as userId, r.seat_id as seatId, r.start_time as startTime, " +
            "r.end_time as endTime, r.timeout_at as timeoutAt, r.real_start_time as realStartTime, " +
            "r.real_end_time as realEndTime, r.status " +
            "from reservation r join seat s on r.seat_id = s.id " +
            "where s.room_id = #{roomId} and r.status in (1, 5)")
    List<Reservation> findActiveByRoomId(@Param("roomId") Integer roomId);

    @Update("update reservation set status = #{status} where id = #{id}")
    void updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    @Update("update reservation set status = #{status}, real_start_time = #{realStartTime} where id = #{id}")
    void checkIn(@Param("id") Integer id, @Param("status") Integer status, @Param("realStartTime") Date realStartTime);

    @Update("update reservation set status = #{status}, real_end_time = #{realEndTime} where id = #{id}")
    void checkout(@Param("id") Integer id, @Param("status") Integer status, @Param("realEndTime") Date realEndTime);

    // Complex query defined in XML mapper
    List<Reservation> findBySearch(@Param("params") Params params);

    /**
     * 统计所有预约数量
     *
     * @return 预约总数
     */
    @Select("select count(*) from reservation")
    int countAll();

    /**
     * 统计当前活跃预约数量（已预约和使用中的）
     *
     * @return 活跃预约数量
     */
    @Select("select count(*) from reservation where status in (1, 5)")
    int countActiveReservations();

    /**
     * 统计指定日期的预约数量
     *
     * @param date 日期字符串，格式：yyyy-MM-dd
     * @return 当日预约数量
     */
    @Select("select count(*) from reservation where DATE(start_time) = #{date}")
    int countByDate(@Param("date") String date);
}