package com.example.common;

import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.exception.CustomException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 拦截器：统一密钥本地验签（不查库），角色来自 token 声明，
 * 不再信任客户端 X-Role 头；/admin/** 路径强制要求 admin 角色。
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("token");
        if (StrUtil.isBlank(token)) {
            token = request.getParameter("token");
        }
        if (StrUtil.isBlank(token)) {
            throw new CustomException("无token，请重新登录");
        }

        Integer userId;
        String role;
        try {
            DecodedJWT jwt = JwtTokenUtils.verifyToken(token);
            userId = JwtTokenUtils.getTokenUserId(jwt);
            role = JwtTokenUtils.getTokenRole(jwt);
        } catch (JWTVerificationException e) {
            throw new CustomException("token验证失败，请重新登录");
        } catch (Exception e) {
            log.error("token解析失败", e);
            throw new CustomException("token解析失败，请重新登录");
        }

        // 管理端路径必须持有 admin 角色 token，防止普通用户 token 调用管理接口
        String uri = request.getRequestURI();
        if (uri.contains("/admin") && !"admin".equals(role)) {
            log.warn("普通用户 token 访问管理接口被拒绝: uri={}, userId={}", uri, userId);
            throw new CustomException("无权访问管理接口");
        }

        // 验签通过，注入请求上下文供 Controller 使用
        request.setAttribute("userId", userId);
        request.setAttribute("userRole", role);
        return true;
    }
}
