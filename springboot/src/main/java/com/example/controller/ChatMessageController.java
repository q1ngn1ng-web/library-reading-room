package com.example.controller;

import com.example.common.Result;
import com.example.entity.ChatMessage;
import com.example.service.ChatMessageService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
*  描述：群组聊天相关接口
*/
@RestController
@RequestMapping(value = "/chat")
public class ChatMessageController {

    @Resource
    private ChatMessageService chatMessageService;

    /**
     * 描述：新增
     */
    @PostMapping("/{id}")
    public Result add(@RequestBody ChatMessage chat) {

        chatMessageService.add(chat);
        return Result.success(chat);
    }

    /**
     * 描述：根据ID删除
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        chatMessageService.delete(id);
        return Result.success();
    }

    /**
     * 描述：更新
     */
    @PutMapping("/{id}")
    public Result update(@RequestBody ChatMessage chat) {

        chatMessageService.update(chat);
        return Result.success();
    }

    /**
     * 描述：根据ID查询
     */
    @GetMapping("/{id}")
    public Result detail(@PathVariable Integer id) {
        ChatMessage chat = chatMessageService.findById(id);
        return Result.success(chat);
    }

    /**
     * 描述：查询所有
     */
    @GetMapping
    public Result all() {
        return Result.success(chatMessageService.findAll());
    }

    /**
     * 描述：分页查询
     */
    @PostMapping("/page")
    public Result page(@RequestBody ChatMessage search,
                       @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "5") Integer pageSize) {
        return Result.success(chatMessageService.findPage(search, pageNum, pageSize));
    }

    @GetMapping("/history/{roomId}")
    public Result getHistoryMessages(
            @PathVariable Integer roomId,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @RequestParam(required = false, defaultValue = "0") Integer offset) {
        return Result.success(chatMessageService.getRoomMessages(roomId, offset, limit));
    }

}
