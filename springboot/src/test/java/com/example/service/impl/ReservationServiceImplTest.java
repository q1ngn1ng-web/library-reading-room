package com.example.service.impl;

import com.example.common.Result;
import com.example.dao.ReservationDao;
import com.example.dao.SeatDao;
import com.example.entity.Reservation;
import com.example.entity.Seat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 预约状态机单元测试：覆盖抢占成败、重复预约、签到/退座、
 * 以及 validateReservation 的三分支（到期结算/超时结算/已退座）。
 * WebSocket 单播在无连接时为空操作，无需打桩。
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private ReservationDao reservationDao;

    @Mock
    private SeatDao seatDao;

    @InjectMocks
    private ReservationServiceImpl service;

    private static final Date FUTURE = new Date(System.currentTimeMillis() + 3600_000);
    private static final Date PAST = new Date(System.currentTimeMillis() - 3600_000);

    private Reservation activeReservation(int status) {
        Reservation r = new Reservation();
        r.setId(100);
        r.setUserId(1);
        r.setSeatId(7);
        r.setStatus(status);
        r.setStartTime(PAST);
        r.setEndTime(FUTURE);          // 时段内
        r.setTimeoutAt(FUTURE);        // 未超签到时限
        return r;
    }

    // ---------- reserve ----------

    @Test
    void reserve_success_insertsAndSetsTimeout() {
        when(reservationDao.findCurrentByUserId(1)).thenReturn(null);
        Seat seat = new Seat();
        seat.setId(7);
        seat.setStatus(0);
        when(seatDao.selectByPrimaryKey(7)).thenReturn(seat);
        when(seatDao.tryUpdateStatus(7, 0, 1)).thenReturn(1);

        Reservation req = new Reservation();
        req.setUserId(1);
        req.setSeatId(7);
        Result result = service.reserve(req);

        assertEquals("Reservation created successfully.", result.getMsg());
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationDao).insert(captor.capture());
        Reservation saved = captor.getValue();
        assertEquals(1, saved.getStatus());
        assertNotNull(saved.getTimeoutAt(), "应设置 30 分钟签到时限");
        assertTrue(saved.getTimeoutAt().after(new Date()), "签到时限应在未来");
    }

    @Test
    void reserve_seatTaken_returnsFriendlyError() {
        when(reservationDao.findCurrentByUserId(1)).thenReturn(null);
        Seat seat = new Seat();
        seat.setId(7);
        seat.setStatus(0);
        when(seatDao.selectByPrimaryKey(7)).thenReturn(seat);
        when(seatDao.tryUpdateStatus(7, 0, 1)).thenReturn(0); // 抢占失败

        Reservation req = new Reservation();
        req.setUserId(1);
        req.setSeatId(7);
        Result result = service.reserve(req);

        assertTrue(result.getMsg().contains("手慢了"));
        verify(reservationDao, never()).insert(any());
    }

    @Test
    void reserve_hasActiveReservation_rejected() {
        when(reservationDao.findCurrentByUserId(1)).thenReturn(activeReservation(5)); // 已签到且有效
        Reservation req = new Reservation();
        req.setUserId(1);
        req.setSeatId(7);
        Result result = service.reserve(req);
        assertTrue(result.getMsg().contains("最多只能预约一个座位"));
        verify(seatDao, never()).tryUpdateStatus(anyInt(), anyInt(), anyInt());
    }

    @Test
    void reserve_seatMissing_error() {
        when(reservationDao.findCurrentByUserId(1)).thenReturn(null);
        when(seatDao.selectByPrimaryKey(99)).thenReturn(null);
        Reservation req = new Reservation();
        req.setUserId(1);
        req.setSeatId(99);
        Result result = service.reserve(req);
        assertTrue(result.getMsg().contains("does not exist"));
    }

    // ---------- checkIn / checkout ----------

    @Test
    void checkIn_success() {
        Reservation r = activeReservation(1);
        when(reservationDao.selectByPrimaryKey(100)).thenReturn(r);
        when(seatDao.tryUpdateStatus(7, 1, 2)).thenReturn(1);

        Result result = service.checkIn(100);

        assertEquals("Check-in successful.", result.getMsg());
        verify(reservationDao).checkIn(eq(100), eq(5), any(Date.class));
    }

    @Test
    void checkIn_wrongStatus_rejected() {
        when(reservationDao.selectByPrimaryKey(100)).thenReturn(activeReservation(5));
        Result result = service.checkIn(100);
        assertTrue(result.getMsg().contains("Only active"));
        verify(reservationDao, never()).checkIn(anyInt(), anyInt(), any());
    }

    @Test
    void checkout_success_releasesSeat() {
        Reservation r = activeReservation(5);
        r.setRealStartTime(PAST);
        when(reservationDao.selectByPrimaryKey(100)).thenReturn(r);
        when(seatDao.tryUpdateStatus(7, 2, 0)).thenReturn(1);

        Result result = service.checkout(100);

        assertEquals("Check-out successful.", result.getMsg());
        verify(reservationDao).checkout(eq(100), eq(6), any(Date.class));
        verify(seatDao).tryUpdateStatus(7, 2, 0);
    }

    @Test
    void checkout_notCheckedIn_rejected() {
        when(reservationDao.selectByPrimaryKey(100)).thenReturn(activeReservation(1));
        Result result = service.checkout(100);
        assertTrue(result.getMsg().contains("Only checked-in"));
    }

    // ---------- validateReservation 三分支（经 getCurrentReservation 触发） ----------

    @Test
    void current_ended_autoCompletesAndReleasesSeat() {
        Reservation r = activeReservation(5);
        r.setEndTime(PAST); // 时段已结束（用户离线未退座场景）
        when(reservationDao.findCurrentByUserId(1)).thenReturn(r);
        when(seatDao.tryUpdateStatus(7, 2, 0)).thenReturn(1);

        Result result = service.getCurrentReservation(1);

        assertEquals("reservation_ended", result.getMsg());
        verify(reservationDao).checkout(eq(100), eq(6), any(Date.class));
        verify(seatDao).tryUpdateStatus(7, 2, 0);
    }

    @Test
    void current_ended_seatChangedByAdmin_stillSettlesWithoutException() {
        Reservation r = activeReservation(5);
        r.setEndTime(PAST);
        when(reservationDao.findCurrentByUserId(1)).thenReturn(r);
        when(seatDao.tryUpdateStatus(7, 2, 0)).thenReturn(0); // 座位已被管理员禁用

        Result result = service.getCurrentReservation(1);

        assertEquals("reservation_ended", result.getMsg());
        verify(reservationDao).checkout(eq(100), eq(6), any(Date.class)); // 预约仍结算
    }

    @Test
    void current_timeout_marksTimeoutAndReleases() {
        Reservation r = activeReservation(1);
        r.setTimeoutAt(PAST); // 未签到超时（endTime 仍在未来）
        when(reservationDao.findCurrentByUserId(1)).thenReturn(r);
        when(seatDao.tryUpdateStatus(7, 1, 0)).thenReturn(1);

        Result result = service.getCurrentReservation(1);

        assertEquals("reservation_timeout", result.getMsg());
        verify(reservationDao).updateStatus(100, 3);
        verify(seatDao).tryUpdateStatus(7, 1, 0);
    }

    @Test
    void current_alreadyCheckedOut() {
        Reservation r = activeReservation(6);
        r.setRealEndTime(PAST);
        when(reservationDao.findCurrentByUserId(1)).thenReturn(r);
        Result result = service.getCurrentReservation(1);
        assertEquals("reservation_checked_out", result.getMsg());
        verifyNoInteractions(seatDao);
    }

    @Test
    void current_valid_returnsReservation() {
        when(reservationDao.findCurrentByUserId(1)).thenReturn(activeReservation(5));
        Result result = service.getCurrentReservation(1);
        assertEquals("0", result.getCode());
        assertEquals(100, result.getData() != null ? ((Reservation) result.getData()).getId() : null);
    }
}
