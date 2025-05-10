package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.JwtTokenUtils;
import com.example.dao.ChatDao;
import com.example.entity.Chat;
import com.example.entity.User;
import com.example.exception.CustomException;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ChatService {

    @Resource
    private ChatDao chatRoomDao;

    public Chat add(Chat chat) {
        chatRoomDao.insertSelective(chat);
        return chat;
    }

    public void delete(Integer id) {
        chatRoomDao.deleteByPrimaryKey(id);
    }

    public void update(Chat chat) {
        chatRoomDao.updateByPrimaryKeySelective(chat);
    }

    public Chat findById(Integer id) {
        return chatRoomDao.selectByPrimaryKey(id);
    }

    public List<Chat> findAll() {
        return chatRoomDao.findBySearch(null);
    }

    public PageInfo<Chat> findPage(Chat search, Integer pageNum, Integer pageSize) {
        User user = JwtTokenUtils.getCurrentUser();
        if (ObjectUtil.isNull(user)) {
            throw new CustomException("token验证失效");
        }
        PageHelper.startPage(pageNum, pageSize);
        List<Chat> all = findByCondition(user, search);

        return PageInfo.of(all);
    }

    public List<Chat> findByCondition(User user, Chat search) {
		return chatRoomDao.findBySearch(search);
    }
}