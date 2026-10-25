package com.example.service.impl;

import com.example.common.WebSocketServer;
import com.example.dao.ReservationDao;
import com.example.dao.SeatDao;
import com.example.entity.Reservation;
import com.example.entity.ReservationStatus;
import com.example.entity.Seat;
import com.example.entity.SeatStatus;
import com.example.service.SeatService;
import cn.hutool.core.lang.Dict;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SeatServiceImpl implements SeatService {

    private static final Logger log = LoggerFactory.getLogger(SeatServiceImpl.class);

    @Autowired
    private SeatDao seatDao;

    @Autowired
    private ReservationDao reservationDao;

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

    /**
     * 整层批量更新座位状态。
     * 禁用整层（MAINTENANCE）时联动作废该层全部活跃预约（终态保证），
     * 并尽力通知在线用户；恢复可用时无逆向动作。
     */
    @Override
    @Transactional
    public void batchUpdateSeatStatus(Integer roomId, Integer status) {
        if (status != null && status == SeatStatus.MAINTENANCE.getCode()) {
            List<Reservation> actives = reservationDao.findActiveByRoomId(roomId);
            for (Reservation r : actives) {
                reservationDao.updateStatus(r.getId(), ReservationStatus.ADMIN_RELEASED.getCode());
                WebSocketServer.sendToUser(r.getUserId(), "reservationCancelled",
                        Dict.create()
                                .set("reservationId", r.getId())
                                .set("message", "所在楼层被管理员禁用，预约已作废"));
            }
            if (!actives.isEmpty()) {
                log.info("整层禁用联动：房间 {} 作废活跃预约 {} 条", roomId, actives.size());
            }
        }
        seatDao.batchUpdateSeatStatus(roomId, status);
    }
}
