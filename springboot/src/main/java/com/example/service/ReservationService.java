package com.example.service;

import com.example.common.Result;
import com.example.entity.Params;
import com.example.entity.Reservation;
import com.github.pagehelper.PageInfo;

public interface ReservationService {

    /**
     * Create a new reservation
     *
     * @param reservation Reservation object
     * @return Reservation result with ID
     */
    Result reserve(Reservation reservation);

    /**
     * Cancel a reservation
     *
     * @param reservationId Reservation ID
     * @return Result of operation
     */
    Result cancel(Integer reservationId);

    /**
     * Check in for a reservation
     *
     * @param reservationId Reservation ID
     * @return Result of operation
     */
    Result checkIn(Integer reservationId);

    /**
     * Check out from a reservation
     *
     * @param reservationId Reservation ID
     * @return Result of operation
     */
    Result checkout(Integer reservationId);

    /**
     * Get current reservation for a user
     *
     * @param userId User ID
     * @return Current active reservation or null
     */
    Result getCurrentReservation(Integer userId);

    /**
     * Get reservation history for a user
     *
     * @param userId User ID
     * @param page   Page number
     * @param size   Page size
     * @param status Filter by status (optional)
     * @return Paged reservation history
     */
    PageInfo<Reservation> getReservationHistory(Integer userId, Integer page, Integer size, Integer status);

    /**
     * Check for timeout reservations and update their status
     */
    void checkTimeoutReservations();

    /**
     * Check if a specific reservation has timed out
     *
     * @param reservationId Reservation ID
     * @return Result of operation
     */
    Result checkTimeout(Integer reservationId);

    /**
     * Find reservations by search criteria
     *
     * @param params Search parameters
     * @param page   Page number
     * @param size   Page size
     * @return Paged reservation results
     */
    PageInfo<Reservation> findBySearch(Params params, Integer page, Integer size);
}