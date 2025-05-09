package com.example.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Table(name = "admin")
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name")
    private String name;
    @Column(name = "password")
    private String password;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "role")
    private String role;


    @Transient
    private String token;

    @Transient
    private String verCode;

    @Transient
    private String newPassword;

}
