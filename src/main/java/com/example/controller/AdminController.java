package com.example.controller;

import com.example.common.JwtTokenUtils;
import com.example.common.Result;
import com.example.entity.Admin;
import com.example.service.AdminService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private AdminService adminService;

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public Result login(@RequestBody Admin admin) {
        Admin loginAdmin = adminService.login(admin);
        return Result.success(loginAdmin);
    }

    /**
     * 修改密码
     */
    @PostMapping("/password")
    public Result updatePassword(@RequestBody Admin admin) {
        Admin currentAdmin = JwtTokenUtils.getCurrentAdmin();
        if (currentAdmin == null) {
            return Result.error("token验证失效，请重新登陆");
        }
        if (!admin.getPassword().equals(currentAdmin.getPassword())) {
            return Result.error("原密码输入错误");
        }
        admin.setPassword(admin.getNewPassword());
        adminService.update(admin);
        return Result.success();
    }

    /**
     * 获取所有管理员
     */
    @GetMapping
    public Result findAll() {
        List<Admin> list = adminService.findAll();
        return Result.success(list);
    }

    /**
     * 根据ID查询管理员
     */
    @GetMapping("/{id}")
    public Result findById(@PathVariable Integer id) {
        Admin admin = adminService.findById(id);
        return Result.success(admin);
    }

    /**
     * 更新管理员信息
     */
    @PutMapping
    public Result update(@RequestBody Admin admin) {
        adminService.update(admin);
        return Result.success();
    }

    /**
     * 删除管理员
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        adminService.delete(id);
        return Result.success();
    }
}
