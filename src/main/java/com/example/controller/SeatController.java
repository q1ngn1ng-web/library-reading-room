package com.example.controller;

import com.example.common.Result;
import com.example.entity.Seat;
import com.example.service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seats")
public class SeatController {

    @Autowired
    private SeatService seatService;

    /**
     * Get all seats by room/floor
     *
     * @param roomId Room/Floor ID
     * @return List of seats
     */
    @GetMapping("/status")
    public List<Seat> getSeatsByRoom(@RequestParam("roomId") Integer roomId) {
        return seatService.getSeatsByRoom(roomId);
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
    @GetMapping("/rooms")
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