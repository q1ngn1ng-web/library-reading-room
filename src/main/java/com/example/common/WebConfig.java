package com.example.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private JwtInterceptor jwtInterceptor;

    // 加自定义拦截器JwtInterceptor，设置拦截规则
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .addPathPatterns("/user/**")
                .addPathPatterns("/admin/**")
                .addPathPatterns("/reservations/**")
                .addPathPatterns("/seats/**")
                .addPathPatterns("/chatroom/**")
                .excludePathPatterns("/files/**")
                .excludePathPatterns("/captcha")
                .excludePathPatterns("/type/upload")
                .excludePathPatterns("/admin/login")
                .excludePathPatterns("/user/login")
                .excludePathPatterns("/user/register");
    }
}