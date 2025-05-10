package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.JwtTokenUtils;
import com.example.dao.ChatMessageDao;
import com.example.entity.ChatMessage;
import com.example.entity.User;
import com.example.exception.CustomException;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ChatMessageService {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageService.class);

    @Resource
    private ChatMessageDao chatMessageDao;

    // 添加消息
    public ChatMessage add(ChatMessage chat) {
        try {
            // 保存消息到数据库
            chatMessageDao.insertSelective(chat);
            return chat;
        } catch (Exception e) {
            log.error("添加聊天消息失败: {}", e.getMessage(), e);
            return chat;
        }
    }

    // 删除消息
    public void delete(Integer id) {
        chatMessageDao.deleteByPrimaryKey(id);
    }

    // 更新消息
    public void update(ChatMessage chat) {
        chatMessageDao.updateByPrimaryKeySelective(chat);
    }

    // 根据id查询消息
    public ChatMessage findById(Integer id) {
        return chatMessageDao.selectByPrimaryKey(id);
    }

    public List<ChatMessage> findAll() {
        return chatMessageDao.findBySearch(null);
    }

    // 根据房间id查询消息
    public List<ChatMessage> getRoomMessages(Integer roomId, Integer page, Integer size) {
        return chatMessageDao.findByRoomId(roomId, page, size);
    }

    public PageInfo<ChatMessage> findPage(ChatMessage search, Integer pageNum, Integer pageSize) {
        try {
            User user = JwtTokenUtils.getCurrentUser();
            if (ObjectUtil.isNull(user)) {
                throw new CustomException("token验证失效");
            }
            PageHelper.startPage(pageNum, pageSize);
            List<ChatMessage> all = findByCondition(user, search);
            return PageInfo.of(all);
        } catch (Exception e) {
            log.error("分页查询聊天消息失败: {}", e.getMessage(), e);
            throw new CustomException("查询失败");
        }
    }

    public List<ChatMessage> findByCondition(User user, ChatMessage search) {
        return chatMessageDao.findBySearch(search);
    }
}