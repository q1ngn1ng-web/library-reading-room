package com.example.controller;

import com.example.common.Result;
import com.example.entity.Params;
import com.example.entity.Reservation;
import com.example.service.ReservationService;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    /**
     * Create a new reservation
     * 
     * @param reservation Reservation details
     * @return Result with reservation ID
     */
    @PostMapping("/reserve")
    public Result reserve(@RequestBody Reservation reservation, HttpServletRequest request) {
        // Extract user ID from request attributes (set by JWT interceptor)
        Integer userId = (Integer) request.getAttribute("userId");
        reservation.setUserId(userId);
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