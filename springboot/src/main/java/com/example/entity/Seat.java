package com.example.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "seat")
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "room_id")
    private Integer roomId;

    @Column(name = "number")
    private Integer number;

    @Column(name = "status")
    private Integer status; // 0:可预约, 1:已预约, 2:使用中, 3:禁用
}