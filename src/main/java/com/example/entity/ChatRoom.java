package com.example.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "chat_room")
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "floor")
    private Integer floor;

    @Column(name = "category")
    private String category;

    @Column(name = "is_available")
    private Boolean isAvailable;

    @Column(name = "is_chat")
    private Boolean isChat;

    @Column(name = "maxUser")
    private Integer maxUser;

    @Column(name = "currentUser")
    private Integer currentUser;
}
