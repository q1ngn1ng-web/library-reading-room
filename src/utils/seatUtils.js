/**
 * 座位状态常量
 */
export const SEAT_STATUS = {
  AVAILABLE: 0,    // 可预约
  RESERVED: 1,     // 已预约
  OCCUPIED: 2,     // 使用中/已签到
  TIMEOUT: 3,      // 已超时
  CANCELED: 4,     // 已取消
  COMPLETED: 5     // 已完成
};

/**
 * 座位状态对应的显示文本
 */
export const STATUS_TEXT_MAP = {
  [SEAT_STATUS.AVAILABLE]: '可预约',
  [SEAT_STATUS.RESERVED]: '已预约',
  [SEAT_STATUS.OCCUPIED]: '使用中',
  [SEAT_STATUS.TIMEOUT]: '已超时',
  [SEAT_STATUS.CANCELED]: '已取消',
  [SEAT_STATUS.COMPLETED]: '已完成'
};

/**
 * 座位状态对应的Element UI标签类型
 */
export const STATUS_TAG_TYPE_MAP = {
  [SEAT_STATUS.AVAILABLE]: 'info',
  [SEAT_STATUS.RESERVED]: 'primary',
  [SEAT_STATUS.OCCUPIED]: 'success',
  [SEAT_STATUS.TIMEOUT]: 'warning',
  [SEAT_STATUS.CANCELED]: 'danger',
  [SEAT_STATUS.COMPLETED]: 'info'
};

/**
 * 座位状态对应的CSS类名
 */
export const SEAT_CLASS_MAP = {
  [SEAT_STATUS.AVAILABLE]: 'seat-available',
  [SEAT_STATUS.RESERVED]: 'seat-reserved',
  [SEAT_STATUS.OCCUPIED]: 'seat-occupied',
  [SEAT_STATUS.TIMEOUT]: 'seat-timeout',
  [SEAT_STATUS.CANCELED]: 'seat-canceled',
  [SEAT_STATUS.COMPLETED]: 'seat-completed'
};

/**
 * 获取座位状态对应的显示文本
 * @param {number} status - 座位状态
 * @returns {string} 状态显示文本
 */
export function getStatusText(status) {
  return STATUS_TEXT_MAP[status] || '未知状态';
}

/**
 * 获取座位状态对应的标签类型
 * @param {number} status - 座位状态
 * @returns {string} 标签类型
 */
export function getStatusTagType(status) {
  return STATUS_TAG_TYPE_MAP[status] || 'info';
}

/**
 * 获取座位对应的CSS类名
 * @param {Object} seat - 座位对象
 * @returns {Object} CSS类名对象
 */
export function getSeatClass(seat) {
  if (!seat) return {};
  return {
    [SEAT_CLASS_MAP[seat.status]]: true
  };
}

/**
 * 检查座位是否可预约
 * @param {Object} seat - 座位对象
 * @returns {boolean} 是否可预约
 */
export function isSeatAvailable(seat) {
  return seat && seat.status === SEAT_STATUS.AVAILABLE;
}

/**
 * 检查座位是否已被当前用户预约
 * @param {Object} seat - 座位对象
 * @param {number} userId - 用户ID
 * @returns {boolean} 是否已被当前用户预约
 */
export function isSeatReservedByUser(seat, userId) {
  return seat &&
         seat.status === SEAT_STATUS.RESERVED &&
         seat.user_id === userId;
}

/**
 * 检查座位是否已被当前用户占用
 * @param {Object} seat - 座位对象
 * @param {number} userId - 用户ID
 * @returns {boolean} 是否已被当前用户占用
 */
export function isSeatOccupiedByUser(seat, userId) {
  return seat &&
         seat.status === SEAT_STATUS.OCCUPIED &&
         seat.user_id === userId;
}