package com.example.controller;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import com.example.common.JwtTokenUtils;
import com.example.common.Result;
import com.example.common.WebSocketServer;
import com.example.dao.FeedbackDao;
import com.example.entity.Feedback;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 违规举报：读者提交（防滥用 + 管理员实时通知），管理员处理闭环。
 */
@RestController
public class FeedbackController {

    private static final Logger log = LoggerFactory.getLogger(FeedbackController.class);
    private static final Set<String> VALID_TYPES = Set.of("TALK", "EAT", "SEAT_OCCUPY", "OTHER");

    @Resource
    private FeedbackDao feedbackDao;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 读者提交举报 */
    @PostMapping("/feedback")
    public Result submit(@RequestBody Feedback feedback, HttpServletRequest request) {
        Integer reporterId = (Integer) request.getAttribute("userId");
        if (reporterId == null) {
            return Result.error("未登录或会话已过期");
        }
        if (feedback.getType() == null || !VALID_TYPES.contains(feedback.getType())) {
            return Result.error("举报类型不合法");
        }
        if (feedback.getTargetUserId() == null && StrUtil.isBlank(feedback.getContent())) {
            return Result.error("请选择举报对象或填写描述");
        }

        // 防滥用：同一用户 60 秒内仅可提交一次（Redis SETNX）
        String limitKey = "feedback:limit:" + reporterId;
        try {
            Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(limitKey, "1", 60, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(first)) {
                return Result.error("操作过于频繁，请 1 分钟后再试");
            }
        } catch (Exception e) {
            log.warn("举报限流检查失败（Redis 不可用），放行", e);
        }

        feedback.setReporterId(reporterId);
        feedback.setStatus(0);
        feedback.setCreatedAt(new Date());
        if (feedback.getIsAnonymous() == null) {
            feedback.setIsAnonymous(false);
        }
        feedbackDao.insert(feedback);

        // 实时通知所有在线管理员
        String typeDesc = switch (feedback.getType()) {
            case "TALK" -> "喧哗";
            case "EAT" -> "违规饮食";
            case "SEAT_OCCUPY" -> "占座";
            default -> "其他";
        };
        WebSocketServer.sendToAdmins("newFeedback", Dict.create()
                .set("feedbackId", feedback.getId())
                .set("type", typeDesc)
                .set("roomId", feedback.getRoomId())
                .set("message", "收到新的违规举报（" + typeDesc + "），请前往举报管理处理"));

        log.info("用户 {} 提交举报 {}：type={}, target={}", reporterId, feedback.getId(),
                feedback.getType(), feedback.getTargetUserId());
        return Result.success("举报已提交，管理员会尽快处理");
    }

    /** 读者查看自己的举报与处理结果 */
    @GetMapping("/feedback/my")
    public Result my(HttpServletRequest request) {
        Integer reporterId = (Integer) request.getAttribute("userId");
        if (reporterId == null) {
            return Result.error("未登录或会话已过期");
        }
        List<Feedback> list = feedbackDao.findByReporterId(reporterId);
        list.forEach(f -> f.setReporterId(null)); // 不回传自己的 id 语义冗余字段
        return Result.success(list);
    }

    /** 管理端：分页列表（可按状态筛选）+ 待处理数 */
    @GetMapping("/admin/feedback/list")
    public Result adminList(@RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "10") Integer size,
                            @RequestParam(required = false) Integer status) {
        PageHelper.startPage(page, size);
        List<Feedback> list = feedbackDao.findList(status);
        PageInfo<Feedback> info = new PageInfo<>(list);
        // 匿名保护：管理端展示层不暴露举报人身份（库中保留可溯源）
        info.getList().forEach(f -> {
            if (Boolean.TRUE.equals(f.getIsAnonymous())) {
                f.setReporterName("匿名");
                f.setReporterId(null);
            }
        });
        return Result.success(Dict.create()
                .set("page", info)
                .set("pending", feedbackDao.countPending()));
    }

    /** 管理端：处理（已处理/驳回 + 备注） */
    @PutMapping("/admin/feedback/handle")
    public Result handle(@RequestBody Feedback body, HttpServletRequest request) {
        if (body.getId() == null || body.getStatus() == null
                || (body.getStatus() != 1 && body.getStatus() != 2)) {
            return Result.error("参数不合法（status 仅支持 1 已处理 / 2 驳回）");
        }
        Feedback exist = feedbackDao.selectByPrimaryKey(body.getId());
        if (exist == null) {
            return Result.error("举报记录不存在");
        }
        if (exist.getStatus() != 0) {
            return Result.error("该举报已处理过");
        }
        Feedback upd = new Feedback();
        upd.setId(body.getId());
        upd.setStatus(body.getStatus());
        upd.setHandleNote(body.getHandleNote());
        upd.setHandledBy((Integer) request.getAttribute("userId"));
        upd.setHandledAt(new Date());
        feedbackDao.updateByPrimaryKeySelective(upd);
        return Result.success("处理完成");
    }
}
