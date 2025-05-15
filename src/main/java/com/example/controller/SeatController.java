package com.example.controller;

import com.example.common.Result;
import com.example.entity.Seat;
import com.example.service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@RestController
@RequestMapping("/seats")
public class SeatController {

    @Autowired
    private SeatService seatService;
    private static final Logger log = LoggerFactory.getLogger(SeatController.class);

    /**
     * Get all seats by room/floor
     *
     * @param roomId Room/Floor ID
     * @return List of seats
     */
    @GetMapping("/status")
    public Result getSeatsByRoom(@RequestParam("roomId") Integer roomId) {
        System.out.println("/seats/status,getSeatsByRoom:");
        log.info("获取座位状态请求，roomId: {}", roomId);
        List<Seat> seats = seatService.getSeatsByRoom(roomId);
        log.info("找到 {} 个座位", seats.size());
        return Result.success(seats);
    }

    /**
     * Get seat detail
     *
     * @param roomId     Room/Floor ID
     * @param seatNumber Seat number
     * @return Seat detail
     */
    @GetMapping("/detail")
    public Seat getSeatDetail(@RequestParam("roomId") Integer roomId,
            @RequestParam("seatNumber") Integer seatNumber) {
        return seatService.getSeatDetail(roomId, seatNumber);
    }

    /**
     * Get all rooms/floors
     *
     * @return List of room IDs
     */
    @GetMapping("/floors")
    public List<Integer> getAllRooms() {
        return seatService.getAllRooms();
    }

    /**
     * Add a new seat (admin only)
     *
     * @param seat Seat information
     * @return Operation result
     */
    @PostMapping
    public Result addSeat(@RequestBody Seat seat) {
        seatService.addSeat(seat);
        return Result.success();
    }

    /**
     * Update seat information (admin only)
     *
     * @param id   Seat ID
     * @param seat Updated seat information
     * @return Operation result
     */
    @PutMapping("/{id}")
    public Result updateSeat(@PathVariable("id") Integer id, @RequestBody Seat seat) {
        seat.setId(id);
        seatService.updateSeat(seat);
        return Result.success();
    }

    /**
     * Delete a seat (admin only)
     *
     * @param id Seat ID
     * @return Operation result
     */
    @DeleteMapping("/{id}")
    public Result deleteSeat(@PathVariable("id") Integer id) {
        seatService.deleteSeat(id);
        return Result.success();
    }

    /**
     * Update seat status (admin only)
     *
     * @param seatId Seat ID
     * @param status New status
     * @return Operation result
     */
    @PutMapping("/status")
    public Result updateSeatStatus(@RequestParam("seatId") Integer seatId,
            @RequestParam("status") Integer status) {
        seatService.updateSeatStatus(seatId, status);
        return Result.success();
    }
}