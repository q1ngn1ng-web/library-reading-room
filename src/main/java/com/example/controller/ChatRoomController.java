package com.example.controller;

import com.example.common.Result;
import com.example.entity.ChatRoom;
import com.example.service.ChatRoomService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
*  描述：群组聊天相关接口
*/
@RestController
@RequestMapping(value = "/ChatRoom")
public class ChatRoomController {

    @Resource
    private ChatRoomService chatRoomService;

    /**
     * 描述：新增
     */
    @PostMapping
    public Result add(@RequestBody ChatRoom chatRoom) {

        chatRoomService.add(chatRoom);
        return Result.success(chatRoom);
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
    public Result update(@RequestBody ChatRoom chatRoom) {

        chatRoomService.update(chatRoom);
        return Result.success();
    }

    /**
     * 描述：根据ID查询
     */
    @GetMapping("/{id}")
    public Result detail(@PathVariable Integer id) {
        ChatRoom chatRoom = chatRoomService.findById(id);
        return Result.success(chatRoom);
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
    public Result page(@RequestBody ChatRoom search,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "5") Integer pageSize) {
        return Result.success(chatRoomService.findPage(search, pageNum, pageSize));
    }




}
