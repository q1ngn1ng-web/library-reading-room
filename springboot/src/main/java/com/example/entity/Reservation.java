package com.example.entity;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@Entity
@Table(name = "reservation")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @NotNull(message = "座位ID不能为空")
    @Column(name = "seat_id")
    private Integer seatId;

    @NotNull(message = "预约开始时间不能为空")
    @Column(name = "start_time")
    private Date startTime;

    @NotNull(message = "预约结束时间不能为空")
    @Column(name = "end_time")
    private Date endTime;

    @Column(name = "timeout_at")
    private Date timeoutAt;

    @Column(name = "real_start_time")
    private Date realStartTime;

    @Column(name = "real_end_time")
    private Date realEndTime;

    @Column(name = "status")
    private Integer status; // 1:成功, 2:失败, 3:超时, 4:取消
}