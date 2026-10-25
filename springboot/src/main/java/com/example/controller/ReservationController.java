package com.example.controller;

import com.example.common.Result;
import com.example.dao.SeatDao;
import com.example.entity.Params;
import com.example.entity.Reservation;
import com.example.service.ReservationService;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Enumeration;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private SeatDao seatDao;

    private static final Logger log = LoggerFactory.getLogger(ReservationController.class);

    /**
     * Create a new reservation
     *
     * @param reservation Reservation details
     * @return Result with reservation ID
     */
    @PostMapping("/reserve")
    public Result reserve(@RequestBody @Valid Reservation reservation, HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("userId");
        // log.info("从请求中获取的userId: {}", userId);

        if (userId == null) {
            log.error("userId为空，JWT拦截器可能没有正确设置或该请求路径没有经过拦截器");
            return Result.error("用户未登录或会话已过期");
        }

        reservation.setUserId(userId);
        // log.info("设置userId后的reservation: {}", reservation);

        return reservationService.reserve(reservation);// 这个是一个result
    }

    /**
     * Cancel a reservation
     *
     * @param reservationId Reservation ID
     * @return Operation result
     */
    @PostMapping("/cancel")
    public Result cancel(@RequestParam("reservationId") Integer reservationId, HttpServletRequest request) {
        // For security, we could also check if the reservation belongs to the current
        // user
        return reservationService.cancel(reservationId);
    }

    /**
     * Check in for a reservation
     *
     * @param reservationId Reservation ID
     * @return Operation result
     */
    @PostMapping("/checkIn")
    public Result checkIn(@RequestParam("reservationId") Integer reservationId, HttpServletRequest request) {
        return reservationService.checkIn(reservationId);
    }

    /**
     * Check out from a reservation
     *
     * @param reservationId Reservation ID
     * @return Operation result
     */
    @PostMapping("/checkout")
    public Result checkout(@RequestParam("reservationId") Integer reservationId, HttpServletRequest request) {
        return reservationService.checkout(reservationId);
    }

    /**
     * Get current active reservation
     *
     * @return Current reservation
     */
    @GetMapping("/current")
    public Result getCurrentReservation(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        log.info("获取当前预约信息 - 用户ID: {}", userId);

        Result result = reservationService.getCurrentReservation(userId);
        log.info("查询结果: {}", result.getData());

        // 如果没有有效预约（数据为null），直接返回结果
        if (result.getData() == null) {
            log.info("用户 {} 无有效预约, 消息: {}", userId, result.getMsg());
            return result;
        }

        // 如果有预约记录，处理预约和座位信息
        if (result.getData() instanceof Reservation) {
            Reservation reservation = (Reservation) result.getData();
            log.info("预约详情: id={}, userId={}, seatId={}, status={}",
                    reservation.getId(), reservation.getUserId(),
                    reservation.getSeatId(), reservation.getStatus());

            if (reservation != null && reservation.getSeatId() != null) {
                // 获取对应的座位信息
                try {
                    Object seat = seatDao.selectByPrimaryKey(reservation.getSeatId());
                    if (seat == null) {
                        // 座位不存在的情况
                        return Result.error("无法找到对应的座位信息，座位ID: " + reservation.getSeatId());
                    }
                    return Result.success()
                            .setDataAndReturn(java.util.Map.of(
                                    "reservation", reservation,
                                    "seat", seat));
                } catch (Exception e) {
                    log.error("获取座位信息失败", e);
                    return Result.error("获取座位信息失败: " + e.getMessage());
                }
            }
        }

        return result;
    }

    /**
     * Get reservation history
     * @param page   Page number
     * @param size   Page size
     * @param status Optional status filter
     * @return Paged reservation history
     */
    @GetMapping("/history")
    public PageInfo<Reservation> getHistory(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "status", required = false) Integer status,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return reservationService.getReservationHistory(userId, page, size, status);
    }

    /**
     * Check if a reservation has timed out
     *
     * @param reservationId Reservation ID
     * @return Timeout status
     */
    @PostMapping("/checkTimeout")
    public Result checkTimeout(@RequestParam("reservationId") Integer reservationId) {
        return reservationService.checkTimeout(reservationId);
    }

    /**
     * Search reservations (admin only)
     * @param params Search parameters
     * @param page   Page number
     * @param size   Page size
     * @return Paged search results
     */
    @PostMapping("/search")
    public PageInfo<Reservation> search(
            @RequestBody Params params,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return reservationService.findBySearch(params, page, size);
    }

    /**
     * Get all seat statuses
     * This endpoint is public and doesn't require authentication
     *
     * @return List of all seats with their current status
     */
    @GetMapping("/status/seats")
    public Result getAllSeatStatuses() {
        return Result.success(seatDao.selectAll());
    }
}