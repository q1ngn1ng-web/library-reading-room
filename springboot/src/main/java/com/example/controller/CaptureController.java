package com.example.controller;

import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import com.wf.captcha.utils.CaptchaUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

@CrossOrigin
@RestController
@RequestMapping
public class CaptureController {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @RequestMapping("/captcha")
    public void captcha(@RequestParam String key, HttpServletRequest request, HttpServletResponse response) throws Exception {
        SpecCaptcha captcha = new SpecCaptcha(135, 33, 4);
        captcha.setCharType(Captcha.TYPE_NUM_AND_UPPER);
        // 验证码存 Redis：5 分钟 TTL、登录校验后即删（替代原内存 Map 的无过期/泄漏问题）
        stringRedisTemplate.opsForValue().set("captcha:" + key, captcha.text().toLowerCase(), 5, TimeUnit.MINUTES);
        CaptchaUtil.out(captcha, request, response);
    }
}
