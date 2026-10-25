package com.example.common;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.entity.Admin;
import com.example.entity.User;
import com.example.service.AdminService;
import com.example.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * JWT 工具：统一使用服务端独立密钥签发与验签（密钥经环境变量 JWT_SECRET 注入），
 * payload 携带 userId（audience）与 role（claim：user/admin）。
 * REST 拦截器与 WebSocket 握手共用 verifyToken，身份以 token 为准。
 */
@Component
public class JwtTokenUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenUtils.class);

    private static UserService staticUserService;
    private static AdminService staticAdminService;
    private static String SECRET;
    private static int EXPIRE_HOURS;

    @Resource
    private UserService userService;

    @Resource
    private AdminService adminService;

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expire-hours:4}")
    private int expireHours;

    @PostConstruct
    public void init() {
        if (StrUtil.isBlank(secret)) {
            // fail-fast：密钥未配置直接拒绝启动，避免生产环境裸奔默认密钥
            throw new IllegalStateException("JWT_SECRET 未配置：请通过环境变量注入强随机密钥");
        }
        SECRET = secret;
        EXPIRE_HOURS = expireHours;
        staticUserService = userService;
        staticAdminService = adminService;
    }

    /**
     * 生成 token：audience 存 userId，claim 存角色
     */
    public static String genToken(Integer userId, String role) {
        return JWT.create()
                .withAudience(userId.toString())
                .withClaim("role", role)
                .withExpiresAt(DateUtil.offsetHour(new Date(), EXPIRE_HOURS))
                .sign(Algorithm.HMAC256(SECRET));
    }

    /**
     * 验签并解析 token。验签失败抛出 JWTVerificationException。
     */
    public static DecodedJWT verifyToken(String token) {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
        return verifier.verify(token);
    }

    public static Integer getTokenUserId(DecodedJWT jwt) {
        return Integer.valueOf(jwt.getAudience().get(0));
    }

    public static String getTokenRole(DecodedJWT jwt) {
        String role = jwt.getClaim("role").asString();
        return StrUtil.isBlank(role) ? "user" : role;
    }

    /**
     * 获取当前登录用户（userId/role 由 JwtInterceptor 验签后注入请求上下文）
     */
    public static User getCurrentUser() {
        HttpServletRequest request = currentRequest();
        if (request == null || !"user".equals(request.getAttribute("userRole"))) {
            return null;
        }
        Integer userId = (Integer) request.getAttribute("userId");
        try {
            return staticUserService.findById(userId);
        } catch (Exception e) {
            log.error("获取当前登录用户失败, userId={}", userId, e);
            return null;
        }
    }

    public static Admin getCurrentAdmin() {
        HttpServletRequest request = currentRequest();
        if (request == null || !"admin".equals(request.getAttribute("userRole"))) {
            return null;
        }
        Integer userId = (Integer) request.getAttribute("userId");
        try {
            return staticAdminService.findById(userId);
        } catch (Exception e) {
            log.error("获取当前登录管理员失败, adminId={}", userId, e);
            return null;
        }
    }

    private static HttpServletRequest currentRequest() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        } catch (Exception e) {
            return null;
        }
    }
}
