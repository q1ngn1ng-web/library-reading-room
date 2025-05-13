/**
 * 预约状态常量
 */
export const RESERVATION_STATUS = {
  AVAILABLE: 0,  // 可预约/无预约
  RESERVED: 1,   // 预约成功/预约中
  CHECKED_IN: 2, // 已签到/使用中
  TIMEOUT: 3,    // 预约超时
  CANCELLED: 4,  // 预约取消
  COMPLETED: 5   // 已完成
};

/**
 * 预约状态对应的显示文本
 */
export const RESERVATION_STATUS_TEXT = {
  [RESERVATION_STATUS.AVAILABLE]: '可预约',
  [RESERVATION_STATUS.RESERVED]: '预约中',
  [RESERVATION_STATUS.CHECKED_IN]: '已签到',
  [RESERVATION_STATUS.TIMEOUT]: '已超时',
  [RESERVATION_STATUS.CANCELLED]: '已取消',
  [RESERVATION_STATUS.COMPLETED]: '已完成'
};

/**
 * 预约状态对应的标签类型
 */
export const RESERVATION_STATUS_TAG_TYPE = {
  [RESERVATION_STATUS.AVAILABLE]: 'info',
  [RESERVATION_STATUS.RESERVED]: 'primary',
  [RESERVATION_STATUS.CHECKED_IN]: 'success',
  [RESERVATION_STATUS.TIMEOUT]: 'warning',
  [RESERVATION_STATUS.CANCELLED]: 'danger',
  [RESERVATION_STATUS.COMPLETED]: 'info'
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
  return reservation && reservation.status === RESERVATION_STATUS.RESERVED;
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

/**
 * 获取预约状态对应的标签类型
 * @param {number} status - 预约状态
 * @returns {string} 标签类型
 */
export function getReservationTagType(status) {
  switch (status) {
    case RESERVATION_STATUS.AVAILABLE: return 'info';
    case RESERVATION_STATUS.RESERVED: return 'primary';
    case RESERVATION_STATUS.CHECKED_IN: return 'success';
    case RESERVATION_STATUS.TIMEOUT: return 'warning';
    case RESERVATION_STATUS.CANCELLED: return 'danger';
    case RESERVATION_STATUS.COMPLETED: return 'info';
    default: return 'info';
  }
}