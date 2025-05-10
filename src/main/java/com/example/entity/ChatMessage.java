package com.example.entity;

import lombok.Data;

import javax.persistence.*;

/**
 * 群组聊天
 */
@Data
@Table(name = "chat_message")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	/** 房间id */
	@Column(name = "room_id")
	private Integer roomId;
	/** 用户id */
	@Column(name = "user_id")
    private Integer userId;
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
	/** 是否匿名 */
	@Column(name = "is_anonymous")
    private Boolean isAnonymous;

}
