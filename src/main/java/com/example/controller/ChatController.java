package com.example.controller;

import com.example.common.Result;
import com.example.entity.Chat;
import com.example.service.ChatService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
*  描述：群组聊天相关接口
*/
@RestController
@RequestMapping(value = "/chat")
public class ChatController {

    @Resource
    private ChatService chatRoomService;

    /**
     * 描述：新增
     */
    @PostMapping
    public Result add(@RequestBody Chat chat) {

        chatRoomService.add(chat);
        return Result.success(chat);
    }

    /**
     * 描述：根据ID删除
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        chatRoomService.delete(id);
        return Result.success();
    }

    /**
     * 描述：更新
     */
    @PutMapping
    public Result update(@RequestBody Chat chat) {

        chatRoomService.update(chat);
        return Result.success();
    }

    /**
     * 描述：根据ID查询
     */
    @GetMapping("/{id}")
    public Result detail(@PathVariable Integer id) {
        Chat chat = chatRoomService.findById(id);
        return Result.success(chat);
    }

    /**
     * 描述：查询所有
     */
    @GetMapping
    public Result all() {
        return Result.success(chatRoomService.findAll());
    }

    /**
     * 描述：分页查询
     */
    @PostMapping("/page")
    public Result page(@RequestBody Chat search,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "5") Integer pageSize) {
        return Result.success(chatRoomService.findPage(search, pageNum, pageSize));
    }




}
