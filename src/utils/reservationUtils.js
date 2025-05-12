/**
 * 预约状态常量
 */
export const RESERVATION_STATUS = {
  SUCCESS: 1,   // 预约成功
  FAILED: 2,    // 预约失败
  TIMEOUT: 3,   // 预约超时
  CANCELLED: 4  // 预约取消
};

/**
 * 预约状态对应的显示文本
 */
export const RESERVATION_STATUS_TEXT = {
  [RESERVATION_STATUS.SUCCESS]: '预约成功',
  [RESERVATION_STATUS.FAILED]: '预约失败',
  [RESERVATION_STATUS.TIMEOUT]: '预约超时',
  [RESERVATION_STATUS.CANCELLED]: '已取消'
};

/**
 * 预约状态对应的标签类型
 */
export const RESERVATION_STATUS_TAG_TYPE = {
  [RESERVATION_STATUS.SUCCESS]: 'success',
  [RESERVATION_STATUS.FAILED]: 'danger',
  [RESERVATION_STATUS.TIMEOUT]: 'warning',
  [RESERVATION_STATUS.CANCELLED]: 'info'
};

/**
 * 获取预约状态显示文本
 * @param {number} status - 预约状态
 * @returns {string} 状态显示文本
 */
export function getReservationStatusText(status) {
  return RESERVATION_STATUS_TEXT[status] || '未知状态';
}

/**
 * 获取预约状态标签类型
 * @param {number} status - 预约状态
 * @returns {string} 标签类型
 */
export function getReservationStatusTagType(status) {
  return RESERVATION_STATUS_TAG_TYPE[status] || 'info';
}

/**
 * 检查预约是否有效（成功预约且未超时）
 * @param {Object} reservation - 预约对象
 * @returns {boolean} 是否有效
 */
export function isReservationValid(reservation) {
  return reservation && reservation.status === RESERVATION_STATUS.SUCCESS;
}

/**
 * 检查预约是否已取消
 * @param {Object} reservation - 预约对象
 * @returns {boolean} 是否已取消
 */
export function isReservationCancelled(reservation) {
  return reservation && reservation.status === RESERVATION_STATUS.CANCELLED;
}

/**
 * 检查预约是否已超时
 * @param {Object} reservation - 预约对象
 * @returns {boolean} 是否已超时
 */
export function isReservationTimedOut(reservation) {
  return reservation && reservation.status === RESERVATION_STATUS.TIMEOUT;
}