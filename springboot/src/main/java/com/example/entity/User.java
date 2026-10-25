package com.example.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username")
    private String username;
    @Column(name = "password")
    private String password;
    @Column(name = "nickname")
    private String nickname;
    @Column(name = "sex")
    private String sex;
    @Column(name = "age")
    private Integer age;
    @Column(name = "phone")
    private String phone;
    @Column(name = "chat")
    private Boolean chat;
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
