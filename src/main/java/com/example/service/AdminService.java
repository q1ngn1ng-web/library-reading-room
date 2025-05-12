package com.example.service;

import com.example.common.JwtTokenUtils;
import com.example.common.ServerConfig;
import com.example.dao.AdminDao;
import com.example.entity.Admin;
import com.example.exception.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class AdminService {

    @Autowired
    private ServerConfig serverConfig;

    @Resource
    private AdminDao adminDao;

    /**
     * 管理员登录
     */
    public Admin login(Admin admin) {
        // 1. 进行一些非空判断
        if (admin.getUsername() == null || "".equals(admin.getUsername())) {
            throw new CustomException("用户名不能为空");
        }
        if (admin.getPassword() == null || "".equals(admin.getPassword())) {
            throw new CustomException("密码不能为空");
        }
        // 2. 从数据库里面根据这个用户名和密码去查询对应的管理员信息
        Admin adminUser = adminDao.findByNameAndPassword(admin.getUsername(), admin.getPassword());
        if (adminUser == null) {
            throw new CustomException("用户名或密码输入错误");
        }
        // 生成token
        String token = JwtTokenUtils.genToken(adminUser.getId().toString(), adminUser.getPassword());
        adminUser.setToken(token);
        return adminUser;
    }

    /**
     * 查询所有管理员
     */
    public List<Admin> findAll() {
        return adminDao.selectAll();
    }

    /**
     * 根据ID查询管理员
     */
    public Admin findById(Integer id) {
        return adminDao.selectByPrimaryKey(id);
    }

    /**
     * 更新管理员信息
     */
    public void update(Admin admin) {
        // 先查询是否存在该管理员
        Admin existAdmin = adminDao.selectByPrimaryKey(admin.getId());
        if (existAdmin == null) {
            throw new CustomException("管理员不存在");
        }
        // 执行更新
        adminDao.updateByPrimaryKeySelective(admin);
    }

    /**
     * 删除管理员
     */
    public void delete(Integer id) {
        // 先查询是否存在该管理员
        Admin existAdmin = adminDao.selectByPrimaryKey(id);
        if (existAdmin == null) {
            throw new CustomException("管理员不存在");
        }
        // 执行删除
        adminDao.deleteByPrimaryKey(id);
    }
}
