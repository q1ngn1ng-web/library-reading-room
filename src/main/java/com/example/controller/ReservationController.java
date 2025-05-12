package com.example.controller;

import com.example.common.Result;
import com.example.entity.Params;
import com.example.entity.Reservation;
import com.example.service.ReservationService;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;
    private static final Logger log = LoggerFactory.getLogger(ReservationController.class);

    /**
     * Create a new reservation
     *
     * @param reservation Reservation details
     * @return Result with reservation ID
     */
    @PostMapping("/reserve")
    public Result reserve(@RequestBody Reservation reservation, HttpServletRequest request) {
        // Extract user ID from request attributes (set by JWT interceptor)
//        log.info("预约请求开始处理");
//        log.info("请求头信息:");

        // 打印所有请求头
//        Enumeration<String> headerNames = request.getHeaderNames();
//        while (headerNames.hasMoreElements()) {
//            String headerName = headerNames.nextElement();
//            log.info("Header {}: {}", headerName, request.getHeader(headerName));
//        }

        // 打印所有请求属性
//        log.info("请求属性信息:");
//        Enumeration<String> attributeNames = request.getAttributeNames();
//        while (attributeNames.hasMoreElements()) {
//            String attributeName = attributeNames.nextElement();
//            log.info("Attribute {}: {}", attributeName, request.getAttribute(attributeName));
//        }

        Integer userId = (Integer) request.getAttribute("userId");
//        log.info("从请求中获取的userId: {}", userId);

        if (userId == null) {
            log.error("userId为空，JWT拦截器可能没有正确设置或该请求路径没有经过拦截器");
            return Result.error("用户未登录或会话已过期");
        }

        reservation.setUserId(userId);
//        log.info("设置userId后的reservation: {}", reservation);

        return reservationService.reserve(reservation);
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
        return reservationService.getCurrentReservation(userId);
    }

    /**
     * Get reservation history
     *
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
     *
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
}