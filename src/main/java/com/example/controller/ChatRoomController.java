package com.example.controller;

import com.example.common.Result;
import com.example.entity.ChatRoom;
import com.example.service.ChatRoomService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/chatRoom")
public class ChatRoomController {
    @Resource
    private ChatRoomService chatRoomService;

    @PostMapping
    public Result createRoom(@RequestBody ChatRoom room) {
        return Result.success(chatRoomService.createRoom(room));
    }


    /**
     * 更新聊天室状态（启用/禁言）
     */
    @PutMapping("/{id}/status")
    public Result updateStatus(@PathVariable Integer id, @RequestParam Boolean status) {
        chatRoomService.updateRoomStatus(id, status);
        return Result.success();
    }

    @GetMapping("/list")
    public Result getAllRooms() {
        return Result.success(chatRoomService.findAll());
    }

    @GetMapping("/{id}")
    public Result getRoomDetail(@PathVariable Integer id) {
        return Result.success(chatRoomService.findById(id));
    }

    /**
     * 删除聊天室
     */
    @DeleteMapping("/{id}")
    public Result deleteRoom(@PathVariable Integer id) {
        chatRoomService.deleteRoom(id);
        return Result.success();
    }
}