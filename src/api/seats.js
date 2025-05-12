import request from '@/utils/request'

// 获取指定楼层的所有座位
export function getSeatStatus(roomId) {
  return request({
    url: '/seats/status',
    method: 'get',
    role: 'user',
    params: { roomId }
  })
}

// 获取座位详情
export function getSeatDetail(seatId) {
  return request({
    url: '/seats/detail',
    method: 'get',
    role: 'user',
    params: { seatId }
  })
}

// 获取所有楼层信息
export function getFloors() {
  return request({
    url: '/seats/floors',
    method: 'get',
    role: 'user'
  })
}

// 创建预约
export function reserveSeat(data) {
  // 确保data包含userId、seatId等必要字段
  console.log('API 发送预约数据:', data)
  return request({
    url: '/reservations/reserve',
    method: 'post',
    role: 'user', // 显式指定角色
    headers: {
      'Content-Type': 'application/json'
    },
    data: data
  })
}

// 取消预约
export function cancelReservation(reservationId) {
  return request({
    url: '/reservations/cancel',
    method: 'post',
    role: 'user',
    params: { reservationId }
  })
}

// 签到
export function checkIn(reservationId) {
  return request({
    url: '/reservations/checkIn',
    method: 'post',
    role: 'user',
    params: { reservationId }
  })
}

// 退座
export function checkOut(reservationId) {
  return request({
    url: '/reservations/checkout',
    method: 'post',
    role: 'user',
    params: { reservationId }
  })
}

// 获取当前预约
export function getCurrentReservation(userId) {
  return request({
    url: '/reservations/current',
    method: 'get',
    role: 'user',
    params: { userId }
  })
}

// 获取预约历史
export function getReservationHistory(params) {
  return request({
    url: '/reservations/history',
    method: 'get',
    role: 'user',
    params
  })
}

// 检查超时
export function checkTimeout(reservationId) {
  return request({
    url: '/reservations/checkTimeout',
    method: 'post',
    role: 'user',
    params: { reservationId }
  })
}