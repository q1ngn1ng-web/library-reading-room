package com.example.common;

import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.example.entity.User;
import com.example.exception.CustomException;
import com.example.service.UserService;
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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从请求头获取角色信息，默认为user
        String role = request.getHeader("X-Role");
        if (StrUtil.isBlank(role)) {
            role = "user"; // 如果没有指定，默认为用户角色
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

        // 获取 token 中的userId
        String userId;
        User user;
        try {
            userId = JWT.decode(token).getAudience().get(0);
            // 根据token中的userid查询数据库
            user = userService.findById(Integer.parseInt(userId));
        } catch (Exception e) {
            String errMsg = "token验证失败，请重新登录";
            log.error(errMsg + ", token=" + token, e);
            throw new CustomException(errMsg);
        }
        if (user == null) {
            throw new CustomException("用户不存在，请重新登录");
        }

        // 验证角色与token是否匹配
        boolean isAdmin = user.getRole() != null && "admin".equalsIgnoreCase(user.getRole());

        if ("admin".equalsIgnoreCase(role) && !isAdmin) {
            throw new CustomException("无权限执行管理员操作");
        }

        try {
            // 用户密码加签验证 token
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(user.getPassword())).build();
            jwtVerifier.verify(token); // 验证token

            // 验证成功后，将用户ID设置到request中，供后续的Controller使用
            request.setAttribute("userId", user.getId());
            request.setAttribute("userRole", isAdmin ? "admin" : "user");
            log.info("设置用户ID到请求中: {}, 角色: {}", user.getId(), isAdmin ? "admin" : "user");
        } catch (JWTVerificationException e) {
            throw new CustomException("token验证失败，请重新登录");
        }
        log.info("token验证成功，允许放行");
        return true;
    }
}