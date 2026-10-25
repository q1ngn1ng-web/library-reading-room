package com.example.service.impl;

import com.example.common.Result;
import com.example.common.WebSocketServer;
import com.example.dao.ReservationDao;
import com.example.dao.SeatDao;
import com.example.entity.Params;
import com.example.entity.Reservation;
import com.example.entity.ReservationStatus;
import com.example.entity.Seat;
import com.example.entity.SeatStatus;
import com.example.service.ReservationService;
import cn.hutool.core.lang.Dict;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ReservationServiceImpl implements ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationServiceImpl.class);

    @Autowired
    private ReservationDao reservationDao;

    @Autowired
    private SeatDao seatDao;

    /**
     * 验证预约是否有效，并根据需要自动结算过期预约。
     * 终态保证：任何预约最终必达 COMPLETED/TIMEOUT/CANCELLED/ADMIN_RELEASED，
     * 用户离线未退座由定时任务兜底（checkTimeoutReservations）。
     *
     * @param reservation 要验证的预约
     * @param autoSettle  是否自动结算过期预约（释放座位）
     * @return null 表示预约有效，否则返回包含状态信息的 Result
     */
    private Result validateReservation(Reservation reservation, boolean autoSettle) {
        if (reservation == null) {
            Result result = Result.success();
            result.setMsg("no_reservation");
            result.setData(null);
            return result;
        }

        Date now = new Date();

        // 检查是否已超过预约结束时段（比较 endTime；早期版本误比 timeoutAt 导致
        // 签到满30分钟后"当前预约消失但座位永久占用"，已修复）
        if (reservation.getEndTime() != null && now.after(reservation.getEndTime())) {
            log.info("预约ID {} 已超过预约结束时段 {} > {}", reservation.getId(), now, reservation.getEndTime());
            if (autoSettle) {
                settleReservation(reservation, ReservationStatus.COMPLETED, "预约时段已结束，系统自动退座");
            }
            Result result = Result.success();
            result.setMsg("reservation_ended");
            result.setData(null);
            return result;
        }

        // 检查是否已完成（有退座时间）
        if (reservation.getRealEndTime() != null) {
            Result result = Result.success();
            result.setMsg("reservation_checked_out");
            result.setData(null);
            return result;
        }

        // 检查未签到预约是否已超时
        if (reservation.getStatus() == ReservationStatus.SUCCESS.getCode() &&
                reservation.getTimeoutAt() != null && now.after(reservation.getTimeoutAt())) {
            log.info("未签到预约ID {} 已超时 {} > {}", reservation.getId(), now, reservation.getTimeoutAt());

            if (autoSettle) {
                settleReservation(reservation, ReservationStatus.TIMEOUT, "超时未签到，系统自动取消");
            }

            Result result = Result.success();
            result.setMsg("reservation_timeout");
            result.setData(null);
            return result;
        }

        // 预约有效
        return null;
    }

    /**
     * 结算预约：更新状态 + 条件释放座位（座位已被管理员改动时跳过覆盖）+ 尽力通知用户
     */
    private void settleReservation(Reservation reservation, ReservationStatus target, String reason) {
        Date now = new Date();
        if (target == ReservationStatus.COMPLETED) {
            reservationDao.checkout(reservation.getId(), target.getCode(), now);
        } else {
            reservationDao.updateStatus(reservation.getId(), target.getCode());
        }

        // 条件释放：仅当座位仍处于该预约对应状态时才释放，避免覆盖管理员禁用
        Integer expectedSeatStatus = reservation.getStatus() == ReservationStatus.CHECKED_IN.getCode()
                ? SeatStatus.IN_USE.getCode() : SeatStatus.RESERVED.getCode();
        int released = seatDao.tryUpdateStatus(reservation.getSeatId(), expectedSeatStatus, SeatStatus.AVAILABLE.getCode());
        if (released == 0) {
            log.info("预约ID {} 结算时座位 {} 状态已变化（可能被管理员禁用），跳过释放", reservation.getId(), reservation.getSeatId());
        }

        // 尽力通知在线用户（WS 单播）
        WebSocketServer.sendToUser(reservation.getUserId(), "reservationSettled",
                Dict.create()
                        .set("reservationId", reservation.getId())
                        .set("status", target.getCode())
                        .set("message", reason));
    }

    @Override
    @Transactional
    public Result reserve(Reservation reservation) {
        // Check if the user already has an active reservation
        Reservation currentReservation = reservationDao.findCurrentByUserId(reservation.getUserId());
        Result validationResult = validateReservation(currentReservation, true);
        if (validationResult == null) {
            return Result.error("最多只能预约一个座位.");
        }

        // Check if the seat exists
        Seat seat = seatDao.selectByPrimaryKey(reservation.getSeatId());
        if (seat == null) {
            return Result.error("The seat does not exist.");
        }

        // 并发抢占：条件更新（WHERE status=AVAILABLE），两个并发请求只有一个能成功
        int grabbed = seatDao.tryUpdateStatus(seat.getId(), SeatStatus.AVAILABLE.getCode(), SeatStatus.RESERVED.getCode());
        if (grabbed == 0) {
            return Result.error("手慢了，该座位刚被别人约走");
        }

        // 插入预约记录（失败则整个事务回滚，座位状态一并回退）
        reservation.setStatus(ReservationStatus.SUCCESS.getCode());
        Date now = new Date();
        reservation.setTimeoutAt(new Date(now.getTime() + 30 * 60 * 1000));
        reservationDao.insert(reservation);

        Result result = Result.success();
        result.setMsg("Reservation created successfully.");
        result.setData(reservation.getId());
        return result;
    }

    @Override
    @Transactional
    public Result cancel(Integer reservationId) {
        Reservation reservation = reservationDao.selectByPrimaryKey(reservationId);
        if (reservation == null) {
            return Result.error("预约记录未找到.");
        }

        if (reservation.getStatus() != ReservationStatus.SUCCESS.getCode()) {
            return Result.error("只有当前活跃预约才能被取消.");
        }

        reservationDao.updateStatus(reservationId, ReservationStatus.CANCELLED.getCode());

        // 条件释放（仅当座位仍为 RESERVED）
        seatDao.tryUpdateStatus(reservation.getSeatId(), SeatStatus.RESERVED.getCode(), SeatStatus.AVAILABLE.getCode());

        return Result.success("Reservation cancelled successfully.");
    }

    @Override
    @Transactional
    public Result checkIn(Integer reservationId) {
        Reservation reservation = reservationDao.selectByPrimaryKey(reservationId);
        if (reservation == null) {
            return Result.error("Reservation not found.");
        }

        Result validationResult = validateReservation(reservation, false);
        if (validationResult != null) {
            return Result.error("预约失效无法签到：" + validationResult.getMsg());
        }

        if (reservation.getStatus() != ReservationStatus.SUCCESS.getCode()) {
            return Result.error("Only active reservations can be checked in.");
        }

        Date now = new Date();
        reservationDao.checkIn(reservationId, ReservationStatus.CHECKED_IN.getCode(), now);

        // 条件更新：RESERVED → IN_USE
        int updated = seatDao.tryUpdateStatus(reservation.getSeatId(), SeatStatus.RESERVED.getCode(), SeatStatus.IN_USE.getCode());
        if (updated == 0) {
            log.warn("签到成功但座位 {} 状态非 RESERVED（可能被管理员禁用），预约保持有效", reservation.getSeatId());
        }

        return Result.success("Check-in successful.");
    }

    @Override
    @Transactional
    public Result checkout(Integer reservationId) {
        Reservation reservation = reservationDao.selectByPrimaryKey(reservationId);
        if (reservation == null) {
            return Result.error("Reservation not found.");
        }

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN.getCode() ||
                reservation.getRealStartTime() == null) {
            return Result.error("Only checked-in reservations can be checked out.");
        }

        Date now = new Date();
        reservationDao.checkout(reservationId, ReservationStatus.COMPLETED.getCode(), now);

        // 条件释放：仅当座位仍为 IN_USE
        seatDao.tryUpdateStatus(reservation.getSeatId(), SeatStatus.IN_USE.getCode(), SeatStatus.AVAILABLE.getCode());

        return Result.success("Check-out successful.");
    }

    @Override
    public Result getCurrentReservation(Integer userId) {
        Reservation reservation = reservationDao.findCurrentByUserId(userId);

        Result validationResult = validateReservation(reservation, true);
        if (validationResult != null) {
            return validationResult;
        }

        return Result.success(reservation);
    }

    @Override
    public PageInfo<Reservation> getReservationHistory(Integer userId, Integer page, Integer size, Integer status) {
        PageHelper.startPage(page, size);
        List<Reservation> reservations;

        if (status != null) {
            reservations = reservationDao.findHistoryByUserIdAndStatus(userId, status);
        } else {
            reservations = reservationDao.findHistoryByUserId(userId);
        }

        return new PageInfo<>(reservations);
    }

    /**
     * 兜底回收（每 60 秒）：保证所有预约最终到达终态。
     * ① 未签到超时 → TIMEOUT + 释放座位
     * ② 已签到但预约时段结束（用户离线未退座）→ 自动退座 COMPLETED + 释放座位
     */
    @Override
    @Scheduled(fixedRate = 60000)
    public void checkTimeoutReservations() {
        Date now = new Date();

        List<Reservation> timeoutReservations = reservationDao.findTimeoutReservations(now);
        for (Reservation reservation : timeoutReservations) {
            settleReservation(reservation, ReservationStatus.TIMEOUT, "超时未签到，系统自动取消");
        }

        List<Reservation> expiredCheckedIn = reservationDao.findExpiredCheckedIn(now);
        for (Reservation reservation : expiredCheckedIn) {
            settleReservation(reservation, ReservationStatus.COMPLETED, "预约时段已结束，系统自动退座");
        }

        int total = timeoutReservations.size() + expiredCheckedIn.size();
        if (total > 0) {
            log.info("定时结算完成：未签到超时 {} 条，到期自动退座 {} 条",
                    timeoutReservations.size(), expiredCheckedIn.size());
        }
    }

    @Override
    @Transactional
    public Result checkTimeout(Integer reservationId) {
        Reservation reservation = reservationDao.selectByPrimaryKey(reservationId);

        Result validationResult = validateReservation(reservation, true);
        if (validationResult != null && "reservation_timeout".equals(validationResult.getMsg())) {
            return Result.success("The reservation has timed out.");
        }

        return Result.success("The reservation has not timed out.");
    }

    @Override
    public PageInfo<Reservation> findBySearch(Params params, Integer page, Integer size) {
        PageHelper.startPage(page, size);
        List<Reservation> reservations = reservationDao.findBySearch(params);
        return new PageInfo<>(reservations);
    }
}
