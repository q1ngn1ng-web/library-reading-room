package com.example.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "admin")
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "role")
    private Integer role;

    @Column(name = "avatar")
    private String avatar;

    @Transient
    private String token;

    @Transient
    private String verCode;

    @Transient
    private String newPassword;
}
