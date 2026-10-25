package com.example.service;

import com.example.common.JwtTokenUtils;
import com.example.common.ServerConfig;
import com.example.dao.UserDao;
import com.example.entity.User;
import com.example.exception.CustomException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 登录密码校验三分支：BCrypt 哈希命中 / 存量明文自动升级 / 密码错误。
 * JwtTokenUtils 为静态密钥设计，测试前通过反射注入测试密钥。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDao userDao;

    @Mock
    private ServerConfig serverConfig;

    @Spy
    private org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

    @InjectMocks
    private UserService userService;

    @BeforeAll
    static void initJwtSecret() throws Exception {
        Field secret = JwtTokenUtils.class.getDeclaredField("SECRET");
        secret.setAccessible(true);
        secret.set(null, "unit-test-secret");
        Field hours = JwtTokenUtils.class.getDeclaredField("EXPIRE_HOURS");
        hours.setAccessible(true);
        hours.set(null, 4);
    }

    private User dbUser(String storedPassword) {
        User u = new User();
        u.setId(1);
        u.setUsername("20230001");
        u.setPassword(storedPassword);
        u.setNickname("张三");
        return u;
    }

    private User loginRequest(String password) {
        User u = new User();
        u.setUsername("20230001");
        u.setPassword(password);
        return u;
    }

    @Test
    void login_bcryptHash_matchesWithoutUpgrade() {
        String hash = passwordEncoder.encode("123456");
        when(userDao.findByName("20230001")).thenReturn(dbUser(hash));

        User result = userService.login(loginRequest("123456"));

        assertNotNull(result.getToken());
        assertNull(result.getPassword(), "响应不得回传密码");
        verify(userDao, never()).updateByPrimaryKeySelective(any()); // 无需升级
    }

    @Test
    void login_legacyPlaintext_upgradesToHash() {
        when(userDao.findByName("20230001")).thenReturn(dbUser("123456")); // 存量明文

        User result = userService.login(loginRequest("123456"));

        assertNotNull(result.getToken());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDao).updateByPrimaryKeySelective(captor.capture());
        assertTrue(captor.getValue().getPassword().startsWith("$2a$"), "应回写 BCrypt 哈希");
        assertTrue(passwordEncoder.matches("123456", captor.getValue().getPassword()));
    }

    @Test
    void login_wrongPassword_throws() {
        when(userDao.findByName("20230001")).thenReturn(dbUser(passwordEncoder.encode("123456")));
        CustomException e = assertThrows(CustomException.class,
                () -> userService.login(loginRequest("wrong")));
        assertEquals("用户名或密码输入错误", e.getMsg());
    }

    @Test
    void login_userNotFound_throws() {
        when(userDao.findByName("ghost")).thenReturn(null);
        assertThrows(CustomException.class, () -> userService.login(loginRequest("123456")));
    }

    @Test
    void checkOldPassword_hashAndPlaintextBothSupported() {
        when(userDao.selectByPrimaryKey(1)).thenReturn(dbUser(passwordEncoder.encode("123456")));
        assertTrue(userService.checkOldPassword(1, "123456"));
        assertFalse(userService.checkOldPassword(1, "bad"));

        when(userDao.selectByPrimaryKey(1)).thenReturn(dbUser("123456")); // 明文兜底分支
        assertTrue(userService.checkOldPassword(1, "123456"));
    }
}
