package com.example.config;

import com.example.dao.AdminDao;
import com.example.dao.UserDao;
import com.example.entity.Admin;
import com.example.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 启动迁移器：把库中所有非 BCrypt（非 $2a$ 前缀）的存量明文密码就地哈希化。
 * 保证服务启动后数据库里的密码字段一定全部为哈希，不依赖任何用户登录触发。
 */
@Component
public class PasswordMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordMigrationRunner.class);
    private static final String BCRYPT_PREFIX = "$2a$";

    @Resource
    private UserDao userDao;

    @Resource
    private AdminDao adminDao;

    @Resource
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        int migrated = 0;

        List<User> users = userDao.selectAll();
        for (User u : users) {
            if (needsHash(u.getPassword())) {
                User upd = new User();
                upd.setId(u.getId());
                upd.setPassword(passwordEncoder.encode(u.getPassword()));
                userDao.updateByPrimaryKeySelective(upd);
                migrated++;
            }
        }

        List<Admin> admins = adminDao.selectAll();
        for (Admin a : admins) {
            if (needsHash(a.getPassword())) {
                Admin upd = new Admin();
                upd.setId(a.getId());
                upd.setPassword(passwordEncoder.encode(a.getPassword()));
                adminDao.updateByPrimaryKeySelective(upd);
                migrated++;
            }
        }

        if (migrated > 0) {
            log.info("密码哈希迁移完成：共 {} 条明文密码已升级为 BCrypt", migrated);
        } else {
            log.info("密码哈希迁移检查完成：无明文密码需要处理");
        }
    }

    private boolean needsHash(String stored) {
        return stored != null && !stored.startsWith(BCRYPT_PREFIX);
    }
}
