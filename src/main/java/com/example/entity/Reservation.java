package com.example.entity;

import lombok.Data;

import javax.persistence.*;
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

    @Column(name = "seat_id")
    private Integer seatId;

    @Column(name = "start_time")
    private Date startTime;

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