package com.example.entity;

import lombok.Data;

import javax.persistence.*;

/**
 * 群组聊天
 */
@Data
@Table(name = "chatRoom")
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
	/** 内容 */
	@Column(name = "content")
	private String content;
	/** 用户名 */
	@Column(name = "name")
	private String name;
	/** 头像 */
	@Column(name = "avatar")
	private String avatar;
	/** 时间 */
	@Column(name = "time")
	private String time;
	/** 类型 */
	@Column(name = "type")
	private String type;

}
