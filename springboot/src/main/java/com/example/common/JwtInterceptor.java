package com.example.common;

import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.example.entity.User;
import com.example.entity.Admin;
import com.example.exception.CustomException;
import com.example.service.UserService;
import com.example.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * jwt拦截器
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtInterceptor.class);

    @Resource
    private UserService userService;

    @Resource
    private AdminService adminService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从请求头获取角色信息，默认为user
        String role = request.getHeader("X-Role");
        if (StrUtil.isBlank(role)) {
            // 如果没有指定角色，尝试从请求路径判断
            String requestURI = request.getRequestURI();
            role = requestURI.contains("/admin/") || requestURI.startsWith("/admin") ? "admin" : "user";
        }

        // 根据角色确定要使用的token类型
        String tokenKey = "user-token";
        if ("admin".equalsIgnoreCase(role)) {
            tokenKey = "admin-token";
        }

        log.info("请求路径: {}, 角色: {}, 使用token类型: {}", request.getRequestURI(), role, tokenKey);

        // 从http请求的header中获取相应的token
        String token = request.getHeader(tokenKey);

        if (StrUtil.isBlank(token)) {
            // 尝试从通用token头中获取
            token = request.getHeader("token");

            // 如果还是没有，尝试从URL参数中获取
            if (StrUtil.isBlank(token)) {
                token = request.getParameter(tokenKey);
                if (StrUtil.isBlank(token)) {
                    token = request.getParameter("token");
                }
            }
        }

        // 2. 开始执行认证
        if (StrUtil.isBlank(token)) {
            throw new CustomException("无token，请重新登录");
        }

        // 获取 token 中的userId/adminId
        String userId;

        try {
            userId = JWT.decode(token).getAudience().get(0);
        } catch (Exception e) {
            String errMsg = "token解析失败，请重新登录";
            log.error(errMsg + ", token=" + token, e);
            throw new CustomException(errMsg);
        }

        // 根据角色选择不同的验证逻辑
        if ("admin".equalsIgnoreCase(role)) {
            // 管理员验证逻辑
            return verifyAdminToken(request, token, userId);
        } else {
            // 普通用户验证逻辑
            return verifyUserToken(request, token, userId);
        }
    }

    /**
     * 验证普通用户Token
     */
    private boolean verifyUserToken(HttpServletRequest request, String token, String userId) {
        User user;
        try {
            // 根据token中的userid查询数据库
            user = userService.findById(Integer.parseInt(userId));

            if (user == null) {
                throw new CustomException("用户不存在，请重新登录");
            }

            // 用户密码加签验证 token
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(user.getPassword())).build();
            jwtVerifier.verify(token); // 验证token

            // 验证成功后，将用户ID设置到request中，供后续的Controller使用
            request.setAttribute("userId", user.getId());
            request.setAttribute("userRole", "user");
            log.info("用户token验证成功，设置用户ID到请求中: {}, 角色: {}", user.getId(), "user");

            return true;
        } catch (JWTVerificationException e) {
            throw new CustomException("token验证失败，请重新登录");
        } catch (Exception e) {
            String errMsg = "用户token验证失败，请重新登录";
            log.error(errMsg, e);
            throw new CustomException(errMsg);
        }
    }

    /**
     * 验证管理员Token
     */
    private boolean verifyAdminToken(HttpServletRequest request, String token, String adminId) {
        Admin admin;
        try {
            // 根据token中的adminId查询数据库
            admin = adminService.findById(Integer.parseInt(adminId));

            if (admin == null) {
                throw new CustomException("管理员不存在，请重新登录");
            }

            // 管理员密码加签验证 token
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(admin.getPassword())).build();
            jwtVerifier.verify(token); // 验证token

            // 验证成功后，将管理员ID设置到request中，供后续的Controller使用
            request.setAttribute("userId", admin.getId());
            request.setAttribute("userRole", "admin");
            log.info("管理员token验证成功，设置管理员ID到请求中: {}, 角色: {}", admin.getId(), "admin");

            return true;
        } catch (JWTVerificationException e) {
            throw new CustomException("token验证失败，请重新登录");
        } catch (Exception e) {
            String errMsg = "管理员token验证失败，请重新登录";
            log.error(errMsg, e);
            throw new CustomException(errMsg);
        }
    }
}