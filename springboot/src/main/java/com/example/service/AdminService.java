package com.example.service;

import com.example.common.JwtTokenUtils;
import com.example.common.ServerConfig;
import com.example.dao.AdminDao;
import com.example.dao.UserDao;
import com.example.dao.ChatRoomDao;
import com.example.dao.ReservationDao;
import com.example.entity.Admin;
import com.example.exception.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private static final String BCRYPT_PREFIX = "$2a$";

    @Autowired
    private ServerConfig serverConfig;

    @Resource
    private AdminDao adminDao;

    @Resource
    private UserDao userDao;

    @Resource
    private ChatRoomDao chatRoomDao;

    @Resource
    private ReservationDao reservationDao;

    @Resource
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * 管理员登录
     */
    public Admin login(Admin admin) {
        // 1. 进行一些非空判断
        if (admin.getUsername() == null || "".equals(admin.getUsername())) {
            throw new CustomException("用户名不能为空");
        }
        if (admin.getPassword() == null || "".equals(admin.getPassword())) {
            throw new CustomException("密码不能为空");
        }
        // 2. 按用户名查询，密码本地校验
        Admin loginAdmin = adminDao.findByName(admin.getUsername());
        if (loginAdmin == null || !passwordMatches(admin.getPassword(), loginAdmin.getPassword())) {
            throw new CustomException("用户名或密码输入错误");
        }
        // 3. 存量明文密码校验通过后就地升级为 BCrypt 哈希
        if (!loginAdmin.getPassword().startsWith(BCRYPT_PREFIX)) {
            Admin upd = new Admin();
            upd.setId(loginAdmin.getId());
            upd.setPassword(passwordEncoder.encode(admin.getPassword()));
            adminDao.updateByPrimaryKeySelective(upd);
        }
        // 4. 生成token
        String token = JwtTokenUtils.genToken(loginAdmin.getId(), "admin");
        loginAdmin.setToken(token);
        loginAdmin.setPassword(null); // 响应不回传密码
        return loginAdmin;
    }

    public boolean checkOldPassword(Integer id, String rawPassword) {
        Admin admin = adminDao.selectByPrimaryKey(id);
        return admin != null && passwordMatches(rawPassword, admin.getPassword());
    }

    private boolean passwordMatches(String rawPassword, String stored) {
        if (stored == null) {
            return false;
        }
        if (stored.startsWith(BCRYPT_PREFIX)) {
            return passwordEncoder.matches(rawPassword, stored);
        }
        return rawPassword.equals(stored);
    }

    /**
     * 查询所有管理员
     */
    public List<Admin> findAll() {
        return adminDao.selectAll();
    }

    /**
     * 根据ID查询管理员
     */
    public Admin findById(Integer id) {
        return adminDao.selectByPrimaryKey(id);
    }

    /**
     * 更新管理员信息
     */
    public void update(Admin admin) {
        // 先查询是否存在该管理员
        Admin existAdmin = adminDao.selectByPrimaryKey(admin.getId());
        if (existAdmin == null) {
            throw new CustomException("管理员不存在");
        }
        // 密码字段非空时统一做 BCrypt 编码
        if (admin.getPassword() != null && !"".equals(admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        }
        // 执行更新
        adminDao.updateByPrimaryKeySelective(admin);
    }

    /**
     * 删除管理员
     */
    public void delete(Integer id) {
        // 先查询是否存在该管理员
        Admin existAdmin = adminDao.selectByPrimaryKey(id);
        if (existAdmin == null) {
            throw new CustomException("管理员不存在");
        }
        // 执行删除
        adminDao.deleteByPrimaryKey(id);
    }

    /**
     * 获取仪表盘数据
     */
    public Map<String, Object> getDashboardData() {
        Map<String, Object> dashboardData = new HashMap<>();

        // 获取用户总数
        int userCount = userDao.findAll().size();
        dashboardData.put("userCount", userCount);

        // 获取在线用户数
        int onlineUsers = userDao.countCurrentOnlineUsers();
        dashboardData.put("onlineUsers", onlineUsers);

        // 获取活跃聊天室数量
        int activeRooms = chatRoomDao.countActiveRooms();
        dashboardData.put("activeRooms", activeRooms);

        // 获取总预约数
        int totalReservations = reservationDao.countAll();
        dashboardData.put("totalReservations", totalReservations);

        // 获取当前活跃预约数
        int activeReservations = reservationDao.countActiveReservations();
        dashboardData.put("activeReservations", activeReservations);

        // 统计数据
        Map<String, Object> statistics = new HashMap<>();

        // 每日统计 - 最近7天
        List<Map<String, Object>> dailyStats = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(formatter);

            Map<String, Object> dayStat = new HashMap<>();
            dayStat.put("date", dateStr);
            dayStat.put("reservations", reservationDao.countByDate(dateStr));
            // 不再使用按日期统计活跃用户
            // dayStat.put("activeUsers", userDao.countActiveUsersByDate(dateStr));

            dailyStats.add(dayStat);
        }
        statistics.put("daily", dailyStats);

        // 每周统计 - 最近4周
        List<Map<String, Object>> weeklyStats = new ArrayList<>();
        // 实际项目中应添加周统计逻辑
        statistics.put("weekly", weeklyStats);

        // 每月统计 - 最近6个月
        List<Map<String, Object>> monthlyStats = new ArrayList<>();
        // 实际项目中应添加月统计逻辑
        statistics.put("monthly", monthlyStats);

        dashboardData.put("statistics", statistics);

        return dashboardData;
    }

    /**
     * 上传头像
     */
    public String uploadAvatar(MultipartFile file) {
        try {
            // 获取文件名
            String fileName = file.getOriginalFilename();
            // 获取文件后缀
            String suffix = fileName.substring(fileName.lastIndexOf("."));
            // 生成新文件名
            String newFileName = System.currentTimeMillis() + suffix;
            // 文件保存路径
            String filePath = serverConfig.getUploadPath() + newFileName;
            // 保存文件
            file.transferTo(new java.io.File(filePath));
            // 返回文件访问路径
            return serverConfig.getUrl() + "/files/" + newFileName;
        } catch (Exception e) {
            throw new CustomException("上传头像失败: " + e.getMessage());
        }
    }
}
