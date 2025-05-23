package com.example.service;

import com.example.entity.Seat;
import java.util.List;

public interface SeatService {

    /**
     * Get seats by room
     *
     * @param roomId Room/Floor ID
     * @return List of seats
     */
    List<Seat> getSeatsByRoom(Integer roomId);

    /**
     * Get seat detail by room and seat number
     *
     * @param roomId     Room/Floor ID
     * @param seatNumber Seat number
     * @return Seat detail
     */
    Seat getSeatDetail(Integer roomId, Integer seatNumber);

    /**
     * Get all available rooms/floors
     *
     * @return List of room IDs
     */
    List<Integer> getAllRooms();

    /**
     * Update seat status
     *
     * @param seatId Seat ID
     * @param status New status
     */
    void updateSeatStatus(Integer seatId, Integer status);

    /**
     * Add a new seat
     *
     * @param seat Seat object
     */
    void addSeat(Seat seat);

    /**
     * Update seat information
     *
     * @param seat Seat object
     */
    void updateSeat(Seat seat);

    /**
     * Delete a seat
     *
     * @param id Seat ID
     */
    void deleteSeat(Integer id);

    /**
     * Batch update seat status by floor
     *
     * @param roomId Room/Floor ID
     * @param status New status
     */
    void batchUpdateSeatStatus(Integer roomId, Integer status);
}