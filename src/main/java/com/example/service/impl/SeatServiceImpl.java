package com.example.service.impl;

import com.example.dao.SeatDao;
import com.example.entity.Seat;
import com.example.entity.SeatStatus;
import com.example.service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeatServiceImpl implements SeatService {

    @Autowired
    private SeatDao seatDao;

    @Override
    public List<Seat> getSeatsByRoom(Integer roomId) {
        return seatDao.findByRoomId(roomId);
    }

    @Override
    public Seat getSeatDetail(Integer roomId, Integer seatNumber) {
        return seatDao.findByRoomIdAndNumber(roomId, seatNumber);
    }

    @Override
    public List<Integer> getAllRooms() {
        return seatDao.findAllRooms();
    }

    @Override
    public void updateSeatStatus(Integer seatId, Integer status) {
        seatDao.updateSeatStatus(seatId, status);
    }

    @Override
    public void addSeat(Seat seat) {
        // Set default status to AVAILABLE if not specified
        if (seat.getStatus() == null) {
            seat.setStatus(SeatStatus.AVAILABLE.getCode());
        }
        seatDao.insert(seat);
    }

    @Override
    public void updateSeat(Seat seat) {
        seatDao.updateByPrimaryKey(seat);
    }

    @Override
    public void deleteSeat(Integer id) {
        seatDao.deleteByPrimaryKey(id);
    }

    @Override
    public void batchUpdateSeatStatus(Integer roomId, Integer status) {
        seatDao.batchUpdateSeatStatus(roomId, status);
    }
}