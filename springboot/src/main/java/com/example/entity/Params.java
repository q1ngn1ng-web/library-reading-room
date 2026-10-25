package com.example.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Params {

    private String name;
    private String username;
    private String phone;
    private String author;
    private Integer userId;
    private Integer pageNum;
    private Integer pageSize;
    private String role;

    // Additional fields for seat reservation
    private Integer floorNumber;
    private Integer seatNumber;
    private Integer status;
    private Date startDate;
    private Date endDate;
    private Integer reservationId;

}
