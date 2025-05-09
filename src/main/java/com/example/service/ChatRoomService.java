package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.JwtTokenUtils;
import com.example.dao.ChatRoomDao;
import com.example.entity.ChatRoom;
import com.example.entity.User;
import com.example.exception.CustomException;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ChatRoomService {

    @Resource
    private ChatRoomDao chatRoomDao;

    public ChatRoom add(ChatRoom chatRoom) {
        chatRoomDao.insertSelective(chatRoom);
        return chatRoom;
    }

    public void delete(Integer id) {
        chatRoomDao.deleteByPrimaryKey(id);
    }

    public void update(ChatRoom chatRoom) {
        chatRoomDao.updateByPrimaryKeySelective(chatRoom);
    }

    public ChatRoom findById(Integer id) {
        return chatRoomDao.selectByPrimaryKey(id);
    }

    public List<ChatRoom> findAll() {
        return chatRoomDao.findBySearch(null);
    }

    public PageInfo<ChatRoom> findPage(ChatRoom search, Integer pageNum, Integer pageSize) {
        User user = JwtTokenUtils.getCurrentUser();
        if (ObjectUtil.isNull(user)) {
            throw new CustomException("token验证失效");
        }
        PageHelper.startPage(pageNum, pageSize);
        List<ChatRoom> all = findByCondition(user, search);

        return PageInfo.of(all);
    }

    public List<ChatRoom> findByCondition(User user, ChatRoom search) {
		return chatRoomDao.findBySearch(search);
    }
}