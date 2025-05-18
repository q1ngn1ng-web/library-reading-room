package com.example.service.impl;

import com.example.common.Result;
import com.example.dao.ReservationDao;
import com.example.dao.SeatDao;
import com.example.entity.Params;
import com.example.entity.Reservation;
import com.example.entity.ReservationStatus;
import com.example.entity.Seat;
import com.example.entity.SeatStatus;
import com.example.service.ReservationService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReservationServiceImpl implements ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationServiceImpl.class);

    @Autowired
    private ReservationDao reservationDao;

    @Autowired
    private SeatDao seatDao;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    // 添加一个辅助方法来广播座位和预约状态变更
    private void broadcastStatusChange(Integer userId, Reservation reservation, Seat seat) {
        if (messagingTemplate != null && userId != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "reservationUpdate");
            payload.put("reservation", reservation);
            payload.put("seat", seat);

            // 发送给具体用户
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/reservations",
                    payload);

            // 同时广播座位状态变化给所有用户
            Map<String, Object> seatUpdate = new HashMap<>();
            seatUpdate.put("type", "seatUpdate");
            seatUpdate.put("seat", seat);
            messagingTemplate.convertAndSend("/topic/seats", seatUpdate);
        }
    }

    /**
     * 验证预约是否有效，并根据需要自动更新过期预约的状态
     *
     * @param reservation 要验证的预约
     * @param autoUpdate  是否自动更新过期的预约状态（释放座位）
     * @return 验证结果，null表示预约有效，否则返回包含状态信息的Result
     */
    private Result validateReservation(Reservation reservation, boolean autoUpdate) {
        if (reservation == null) {
            Result result = Result.success();
            result.setMsg("no_reservation");
            result.setData(null);
            return result;
        }

        Date now = new Date();

        // 检查是否已超过预约结束时间
        if (reservation.getEndTime() != null && now.after(reservation.getTimeoutAt())) {
            log.info("预约ID {} 已超过预约结束时间 {} > {}", reservation.getId(), now, reservation.getTimeoutAt());
            Result result = Result.success();
            result.setMsg("reservation_ended");
            result.setData(null);
            return result;
        }

        // 检查是否已完成（有退座时间）
        if (reservation.getRealEndTime() != null) {
            log.info("预约ID {} 已有退座记录，退座时间: {}", reservation.getId(), reservation.getRealEndTime());
            Result result = Result.success();
            result.setMsg("reservation_checked_out");
            result.setData(null);
            return result;
        }

        // 检查未签到预约是否已超时
        if (reservation.getStatus() == ReservationStatus.SUCCESS.getCode() &&
                reservation.getTimeoutAt() != null && now.after(reservation.getTimeoutAt())) {
            log.info("未签到预约ID {} 已超时 {} > {}", reservation.getId(), now, reservation.getTimeoutAt());

            if (autoUpdate) {
                // 自动更新预约状态为超时
                reservationDao.updateStatus(reservation.getId(), ReservationStatus.TIMEOUT.getCode());

                // 释放座位
                Seat seat = seatDao.selectByPrimaryKey(reservation.getSeatId());
                if (seat != null) {
                    seat.setStatus(SeatStatus.AVAILABLE.getCode());
                    seatDao.updateByPrimaryKey(seat);

                    // 通知前端
                    broadcastStatusChange(reservation.getUserId(), reservation, seat);
                }
            }

            Result result = Result.success();
            result.setMsg("reservation_timeout");
            result.setData(null);
            return result;
        }

        // 预约有效
        return null;
    }

    @Override
    @Transactional
    public Result reserve(Reservation reservation) {
        // Check if the user already has an active reservation
        Reservation currentReservation = reservationDao.findCurrentByUserId(reservation.getUserId());

        // 验证当前预约是否有效
        Result validationResult = validateReservation(currentReservation, true);
        if (validationResult == null) {
            return Result.error("最多只能预约一个座位.");
        }

        // Check if the seat exists and is available
        Seat seat = seatDao.selectByPrimaryKey(reservation.getSeatId());
        if (seat == null) {
            return Result.error("The seat does not exist.");
        }

        if (seat.getStatus() != SeatStatus.AVAILABLE.getCode()) {
            return Result.error("该座位已被预约.");
        }

        // Set reservation details
        reservation.setStatus(ReservationStatus.SUCCESS.getCode());
        Date now = new Date();

        // Set timeout for check-in (30 minutes from now)
        Date timeoutAt = new Date(now.getTime() + 30 * 60 * 1000);
        reservation.setTimeoutAt(timeoutAt);

        // Insert the reservation
        reservationDao.insert(reservation);

        // Update seat status to RESERVED
        seat.setStatus(SeatStatus.RESERVED.getCode());
        seatDao.updateByPrimaryKey(seat);

        // 广播状态变更
        broadcastStatusChange(reservation.getUserId(), reservation, seat);

        // Create a result with success message and set the reservation ID as data
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

        // Check if the reservation can be cancelled
        if (reservation.getStatus() != ReservationStatus.SUCCESS.getCode()) {
            return Result.error("只有当前活跃预约才能被取消.");
        }

        // Update reservation status
        reservationDao.updateStatus(reservationId, ReservationStatus.CANCELLED.getCode());

        // Fetch updated reservation
        reservation = reservationDao.selectByPrimaryKey(reservationId);

        // Free up the seat，取消预约之后释放座位
        Seat seat = seatDao.selectByPrimaryKey(reservation.getSeatId());
        if (seat != null) {
            seat.setStatus(SeatStatus.AVAILABLE.getCode());
            seatDao.updateByPrimaryKey(seat);

            // 广播状态变更
            broadcastStatusChange(reservation.getUserId(), reservation, seat);
        }

        return Result.success("Reservation cancelled successfully.");
    }

    @Override
    @Transactional
    public Result checkIn(Integer reservationId) {
        Reservation reservation = reservationDao.selectByPrimaryKey(reservationId);
        if (reservation == null) {
            return Result.error("Reservation not found.");
        }

        // 验证预约的有效性
        Result validationResult = validateReservation(reservation, false);
        if (validationResult != null) {
            return Result.error("预约失效无法签到：" + validationResult.getMsg());
        }

        // Check if the reservation is active
        if (reservation.getStatus() != ReservationStatus.SUCCESS.getCode()) {
            return Result.error("Only active reservations can be checked in.");
        }

        // Update reservation status and set real start time
        Date now = new Date();
        reservationDao.checkIn(reservationId, ReservationStatus.CHECKED_IN.getCode(), now);

        // Fetch updated reservation
        reservation = reservationDao.selectByPrimaryKey(reservationId);

        // Update seat status to IN_USE
        Seat seat = seatDao.selectByPrimaryKey(reservation.getSeatId());
        if (seat != null) {
            seat.setStatus(SeatStatus.IN_USE.getCode());
            seatDao.updateByPrimaryKey(seat);

            // 广播状态变更
            broadcastStatusChange(reservation.getUserId(), reservation, seat);
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

        // Check if the reservation is in use
        if (reservation.getStatus() != ReservationStatus.CHECKED_IN.getCode() ||
                reservation.getRealStartTime() == null) {
            return Result.error("Only checked-in reservations can be checked out.");
        }

        // Update reservation status and set real end time
        Date now = new Date();
        reservationDao.checkout(reservationId, ReservationStatus.COMPLETED.getCode(), now);

        // Fetch updated reservation
        reservation = reservationDao.selectByPrimaryKey(reservationId);

        // Free up the seat
        Seat seat = seatDao.selectByPrimaryKey(reservation.getSeatId());
        if (seat != null) {
            seat.setStatus(SeatStatus.AVAILABLE.getCode());
            seatDao.updateByPrimaryKey(seat);

            // 广播状态变更
            broadcastStatusChange(reservation.getUserId(), reservation, seat);
        }

        return Result.success("Check-out successful.");
    }

    @Override
    public Result getCurrentReservation(Integer userId) {
        log.info("查询当前预约 - 用户ID: {}", userId);
        Reservation reservation = reservationDao.findCurrentByUserId(userId);
        log.info("数据库查询结果: {}", reservation);

        // 验证预约的有效性，并自动更新过期预约
        Result validationResult = validateReservation(reservation, true);
        if (validationResult != null) {
            return validationResult;
        }

        // 预约有效，返回预约信息
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

    @Override
    @Scheduled(fixedRate = 60000) // Run every minute
    public void checkTimeoutReservations() {
        Date now = new Date();
        List<Reservation> timeoutReservations = reservationDao.findTimeoutReservations(now);

        for (Reservation reservation : timeoutReservations) {
            // Update reservation status
            reservationDao.updateStatus(reservation.getId(), ReservationStatus.TIMEOUT.getCode());

            // Free up the seat
            Seat seat = seatDao.selectByPrimaryKey(reservation.getSeatId());
            if (seat != null) {
                seat.setStatus(SeatStatus.AVAILABLE.getCode());
                seatDao.updateByPrimaryKey(seat);

                // 广播座位状态变更
                broadcastStatusChange(reservation.getUserId(), reservation, seat);

                // 通知用户预约已过期
                notifyReservationExpired(reservation.getUserId(), reservation.getId(),
                        seat.getNumber() != null ? seat.getNumber().toString() : "未知座位");
            }
        }
    }

    /**
     * 通知用户预约已过期
     */
    private void notifyReservationExpired(Integer userId, Integer reservationId, String seatNumber) {
        if (messagingTemplate != null && userId != null) {
            try {
                // 构建通知消息
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "reservationExpired");
                payload.put("reservationId", reservationId);
                payload.put("seatNumber", seatNumber);
                payload.put("message", "您的座位预约已过期，系统已自动取消");
                payload.put("time", new Date());

                // 发送给特定用户
                messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/notifications",
                        payload);

                // 同时也发送一个系统消息到用户连接的任何聊天室
                Map<String, Object> systemMessage = new HashMap<>();
                systemMessage.put("type", "system");
                systemMessage.put("content", "您的座位预约已过期，请重新预约");
                systemMessage.put("time", new Date());

                messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/messages",
                        systemMessage);

                log.info("已向用户 {} 发送预约过期通知，预约ID: {}", userId, reservationId);
            } catch (Exception e) {
                log.error("发送预约过期通知失败", e);
            }
        }
    }

    @Override
    @Transactional
    public Result checkTimeout(Integer reservationId) {
        Reservation reservation = reservationDao.selectByPrimaryKey(reservationId);

        // 使用验证方法检查预约的有效性
        Result validationResult = validateReservation(reservation, true);
        if (validationResult != null && "reservation_timeout".equals(validationResult.getMsg())) {
            return Result.success("The reservation has timed out.");
        }

        return Result.success("The reservation has not timed out.");
    }

    @Override
    public PageInfo<Reservation> findBySearch(Params params, Integer page, Integer size) {
        PageHelper.startPage(page, size);
        // Assuming the mapper XML provides the findBySearch implementation
        List<Reservation> reservations = reservationDao.findBySearch(params);
        return new PageInfo<>(reservations);
    }
}