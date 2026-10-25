package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.JwtTokenUtils;
import com.example.common.ServerConfig;
import com.example.dao.UserDao;
import com.example.entity.User;
import com.example.entity.Params;
import com.example.exception.CustomException;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class UserService {

    private static final String BCRYPT_PREFIX = "$2a$";

    @Autowired
    private ServerConfig serverConfig;

    @Resource
    private UserDao userDao;

    @Resource
    private BCryptPasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return userDao.selectAll();
    }

    public PageInfo<User> findBySearch(Params params) {
        // 开启分页查询
        int pageNum = params.getPageNum() == null ? 1 : params.getPageNum();
        int pageSize = params.getPageSize() == null ? 10 : params.getPageSize();
        PageHelper.startPage(pageNum, pageSize);
        // 接下来的查询会自动按照当前开启的分页设置来查询
        List<User> list = userDao.findBySearch(params);
        return PageInfo.of(list);
    }

    public void add(User admin) {
        // 1. 用户名一定要有，否则不让新增（后面需要用户名登录）
        if (admin.getUsername() == null || "".equals(admin.getUsername())) {
            System.out.println(admin);
            throw new CustomException("UserService用户名不能为空");
        }
        // 2. 进行重复性判断，同一名字的管理员不允许重复新增：只要根据用户名去数据库查询一下就可以了
        User user = userDao.findByName(admin.getUsername());
        if (user != null) {
            // 说明已经有了，这里我们就要提示前台不允许新增了
            throw new CustomException("该用户名已存在，请更换用户名");
        }
        // 初始化一个密码
        if (admin.getPassword() == null) {
            admin.setPassword("123456");
        }
        // 新密码一律 BCrypt 哈希入库
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        if (ObjectUtil.isEmpty(admin.getAvatar())) {
            admin.setAvatar(serverConfig.getFileBaseUrl() + "qy-default.png");
        }
        userDao.insertSelective(admin);
    }

    public void update(User user) {
        // 密码字段非空时统一做 BCrypt 编码（覆盖改密、重置等所有写入口）
        if (user.getPassword() != null && !"".equals(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userDao.updateByPrimaryKeySelective(user);
    }

    public void delete(Integer id) {
        userDao.deleteByPrimaryKey(id);
    }

    public User login(User user) {
        // 1. 进行一些非空判断
        if (user.getUsername() == null || "".equals(user.getUsername())) {
            throw new CustomException("用户名不能为空");
        }
        if (user.getPassword() == null || "".equals(user.getPassword())) {
            throw new CustomException("密码不能为空");
        }
        // 2. 按用户名查询，密码在本地校验（不再把明文密码拼进 SQL）
        User loginUser = userDao.findByName(user.getUsername());
        if (loginUser == null || !passwordMatches(user.getPassword(), loginUser.getPassword())) {
            throw new CustomException("用户名或密码输入错误");
        }
        // 3. 存量明文密码校验通过后就地升级为 BCrypt 哈希
        if (!loginUser.getPassword().startsWith(BCRYPT_PREFIX)) {
            User upd = new User();
            upd.setId(loginUser.getId());
            upd.setPassword(passwordEncoder.encode(user.getPassword()));
            userDao.updateByPrimaryKeySelective(upd);
        }
        // 4. 生成token
        String token = JwtTokenUtils.genToken(loginUser.getId(), "user");
        loginUser.setToken(token);
        loginUser.setPassword(null); // 响应不回传密码
        return loginUser;
    }

    /**
     * 校验原密码（明文/哈希兼容），用于改密前的身份复核
     */
    public boolean checkOldPassword(Integer id, String rawPassword) {
        User user = userDao.selectByPrimaryKey(id);
        return user != null && passwordMatches(rawPassword, user.getPassword());
    }

    private boolean passwordMatches(String rawPassword, String stored) {
        if (stored == null) {
            return false;
        }
        if (stored.startsWith(BCRYPT_PREFIX)) {
            return passwordEncoder.matches(rawPassword, stored);
        }
        // 兼容未迁移的存量明文（正常情况下启动迁移器已全部哈希化）
        return rawPassword.equals(stored);
    }

    public User findById(Integer id) {
        return userDao.selectByPrimaryKey(id);
    }

    /**
     * 重置用户密码
     *
     * @param id 用户ID
     * @return 新密码
     */
    public String resetPassword(Integer id) {
        // 生成新的随机密码
        String newPassword = "123456"; // 这里可以改成更复杂的随机密码生成逻辑

        // 创建用户对象并设置新密码（哈希入库，明文仅本次返回给管理员）
        User user = new User();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(newPassword));

        // 更新数据库中的密码
        userDao.updateByPrimaryKeySelective(user);

        return newPassword;
    }
}
