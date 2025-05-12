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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ReservationServiceImpl implements ReservationService {

    @Autowired
    private ReservationDao reservationDao;

    @Autowired
    private SeatDao seatDao;

    @Override
    @Transactional
    public Result reserve(Reservation reservation) {
        // Check if the user already has an active reservation
        Reservation currentReservation = reservationDao.findCurrentByUserId(reservation.getUserId());
        if (currentReservation != null) {
            return Result.error("You already have an active reservation.");
        }

        // Check if the seat exists and is available
        Seat seat = seatDao.selectByPrimaryKey(reservation.getSeatId());
        if (seat == null) {
            return Result.error("The seat does not exist.");
        }

        if (seat.getStatus() != SeatStatus.AVAILABLE.getCode()) {
            return Result.error("The seat is not available.");
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
            return Result.error("Reservation not found.");
        }

        // Check if the reservation can be cancelled
        if (reservation.getStatus() != ReservationStatus.SUCCESS.getCode()) {
            return Result.error("Only active reservations can be cancelled.");
        }

        // Update reservation status
        reservationDao.updateStatus(reservationId, ReservationStatus.CANCELLED.getCode());

        // Free up the seat
        Seat seat = seatDao.selectByPrimaryKey(reservation.getSeatId());
        if (seat != null) {
            seat.setStatus(SeatStatus.AVAILABLE.getCode());
            seatDao.updateByPrimaryKey(seat);
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

        // Check if the reservation is active
        if (reservation.getStatus() != ReservationStatus.SUCCESS.getCode()) {
            return Result.error("Only active reservations can be checked in.");
        }

        // Check if the reservation has timed out
        Date now = new Date();
        if (now.after(reservation.getTimeoutAt())) {
            return Result.error("The reservation has timed out.");
        }

        // Update reservation status and set real start time
        reservation.setRealStartTime(now);
        reservationDao.checkIn(reservationId, ReservationStatus.SUCCESS.getCode(), now);

        // Update seat status to IN_USE
        Seat seat = seatDao.selectByPrimaryKey(reservation.getSeatId());
        if (seat != null) {
            seat.setStatus(SeatStatus.IN_USE.getCode());
            seatDao.updateByPrimaryKey(seat);
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
        if (reservation.getStatus() != ReservationStatus.SUCCESS.getCode() ||
                reservation.getRealStartTime() == null) {
            return Result.error("Only checked-in reservations can be checked out.");
        }

        // Update reservation status and set real end time
        Date now = new Date();
        reservationDao.checkout(reservationId, ReservationStatus.SUCCESS.getCode(), now);

        // Free up the seat
        Seat seat = seatDao.selectByPrimaryKey(reservation.getSeatId());
        if (seat != null) {
            seat.setStatus(SeatStatus.AVAILABLE.getCode());
            seatDao.updateByPrimaryKey(seat);
        }

        return Result.success("Check-out successful.");
    }

    @Override
    public Result getCurrentReservation(Integer userId) {
        Reservation reservation = reservationDao.findCurrentByUserId(userId);
        if (reservation == null) {
            return Result.success("No active reservation found.");
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
            }
        }
    }

    @Override
    @Transactional
    public Result checkTimeout(Integer reservationId) {
        Reservation reservation = reservationDao.selectByPrimaryKey(reservationId);
        if (reservation == null) {
            return Result.error("Reservation not found.");
        }

        // Check if the reservation is active
        if (reservation.getStatus() != ReservationStatus.SUCCESS.getCode()) {
            return Result.error("Only active reservations can time out.");
        }

        // Check if the reservation has timed out
        Date now = new Date();
        if (now.after(reservation.getTimeoutAt())) {
            // Update reservation status
            reservationDao.updateStatus(reservationId, ReservationStatus.TIMEOUT.getCode());

            // Free up the seat
            Seat seat = seatDao.selectByPrimaryKey(reservation.getSeatId());
            if (seat != null) {
                seat.setStatus(SeatStatus.AVAILABLE.getCode());
                seatDao.updateByPrimaryKey(seat);
            }

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