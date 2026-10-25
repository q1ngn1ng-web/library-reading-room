package com.example.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * 违规举报：读者对喧哗/饮食/占座等行为的反馈，管理员处理闭环
 */
@Data
@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "reporter_id")
    private Integer reporterId;

    @Column(name = "room_id")
    private Integer roomId;

    @Column(name = "target_user_id")
    private Integer targetUserId;

    @Column(name = "target_message_id")
    private Integer targetMessageId;

    /** TALK 喧哗 / EAT 饮食 / SEAT_OCCUPY 占座 / OTHER 其他 */
    @Column(name = "type")
    private String type;

    @Column(name = "content")
    private String content;

    @Column(name = "is_anonymous")
    private Boolean isAnonymous;

    /** 0 待处理 / 1 已处理 / 2 驳回 */
    @Column(name = "status")
    private Integer status;

    @Column(name = "handle_note")
    private String handleNote;

    @Column(name = "handled_by")
    private Integer handledBy;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "handled_at")
    private Date handledAt;

    /** 联表展示用（不入库） */
    @Transient
    private String reporterName;

    @Transient
    private String targetName;

    @Transient
    private String roomName;
}
