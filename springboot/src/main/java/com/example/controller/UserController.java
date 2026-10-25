package com.example.controller;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.CaptureConfig;
import com.example.common.JwtTokenUtils;
import com.example.common.Result;
import com.example.entity.User;
import com.example.entity.Params;
import com.example.service.UserService;
import com.github.pagehelper.PageInfo;
import com.wf.captcha.utils.CaptchaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Resource
    private UserService userService;

    @PostMapping("/login")
    public Result login(@RequestBody User user, @RequestParam String key, HttpServletRequest request) {
        String value = CaptureConfig.CAPTURE_MAP.get(key);
        if (ObjectUtil.isEmpty(user.getVerCode())) {
            return Result.error("请输入验证码");
        }
        if (!user.getVerCode().toLowerCase().equals(value)) {
            // 清除session中的验证码
            CaptchaUtil.clear(request);
            return Result.error("验证码不正确");
        }
        User loginUser = userService.login(user);
        return Result.success(loginUser);
    }

    @PostMapping("/password")
    public Result updatePassword(@RequestBody User user) {

        // 验证原密码是否为空
        if (ObjectUtil.isNull(user.getPassword()) || user.getPassword().trim().isEmpty()) {
            return Result.error("原密码不能为空");
        }

        // 验证新密码是否为空
        if (ObjectUtil.isNull(user.getNewPassword()) || user.getNewPassword().trim().isEmpty()) {
            return Result.error("新密码不能为空");
        }

        User currentUser = JwtTokenUtils.getCurrentUser();

        if (ObjectUtil.isNull(currentUser)) {
            return Result.error("用户获取不到，token验证失效，请重新登陆");
        }

        // 原密码服务端复核（BCrypt matches，不再依赖客户端比对）
        if (!userService.checkOldPassword(currentUser.getId(), user.getPassword())) {
            return Result.error("原密码输入错误");
        }
        User upd = new User();
        upd.setId(currentUser.getId());
        upd.setPassword(user.getNewPassword());
        userService.update(upd); // update 内统一做 BCrypt 编码
        return Result.success();
    }

    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        System.out.println("前端发送过来的User注册数据:" + user);
        userService.add(user);
        return Result.success();
    }

    @PostMapping
    public Result save(@RequestBody User user) {
        if (user.getId() == null) {
            userService.add(user);
        } else {
            userService.update(user);
        }
        return Result.success();
    }

    @GetMapping
    public Result findAll() {
        List<User> list = userService.findAll();
        // 响应不回传密码哈希
        list.forEach(u -> u.setPassword(null));
        return Result.success(list);
    }

    @GetMapping("/search")
    public Result findBySearch(Params params) {
        PageInfo<User> info = userService.findBySearch(params);
        info.getList().forEach(u -> u.setPassword(null));
        return Result.success(info);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        userService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/password")
    public Result resetPassword(@PathVariable Integer id) {
        String newPassword = userService.resetPassword(id);
        return Result.success(newPassword);
    }
}
