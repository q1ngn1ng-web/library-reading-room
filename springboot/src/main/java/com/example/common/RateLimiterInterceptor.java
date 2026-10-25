package com.example.common;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.revinate.guava.util.concurrent.RateLimiter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class RateLimiterInterceptor implements HandlerInterceptor {

    private static final Log log = LogFactory.get();

    /**
     * 令牌桶里放50个令牌，外界调用本系统所有接口，都得先在桶里获取令牌
     * 拿到令牌后，才可以正常调用接口，为了限流
     */
    private static final RateLimiter limiter = RateLimiter.create(50);



    /**
     * 令牌桶全局限流 50 QPS。快速失败：拿不到令牌立即返回 429，
     * 不排队（阻塞式 acquire 会把过载传导给所有请求方）。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!limiter.tryAcquire()) {
            response.setStatus(429);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":\"429\",\"msg\":\"请求过于频繁，请稍后再试\",\"data\":null}");
            return false;
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
