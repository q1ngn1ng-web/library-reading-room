package com.example.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name")
    private String name;
    @Column(name = "password")
    private String password;
    @Column(name = "sex")
    private String sex;
    @Column(name = "age")
    private Integer age;
    @Column(name = "phone")
    private String phone;
    @Column(name = "role")
    private String role;
    @Column(name = "avatar")
    private String avatar;
    @Transient
    private String token;
    @Transient
    private String verCode;
    @Transient
    private String newPassword;

}
