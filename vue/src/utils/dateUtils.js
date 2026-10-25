/**
 * 日期和时间工具函数
 */

/**
 * 将日期和时间合并为完整的日期时间字符串
 * @param {string} date - 日期字符串，格式：YYYY-MM-DD
 * @param {string} time - 时间字符串，格式：HH:mm
 * @returns {string} - 合并后的日期时间字符串，格式：YYYY-MM-DD HH:mm:ss
 */
export function combineDateTime(date, time) {
  if (!date || !time) return null;
  return `${date} ${time}:00`;
}

/**
 * 检查时间段是否有效
 * @param {string} startTime - 开始时间，格式：HH:mm
 * @param {string} endTime - 结束时间，格式：HH:mm
 * @returns {boolean} - 时间段是否有效
 */
export function validateTimeRange(startTime, endTime) {
  if (!startTime || !endTime) return false;

  const start = new Date(`2000-01-01 ${startTime}`);
  const end = new Date(`2000-01-01 ${endTime}`);

  // 结束时间必须晚于开始时间
  return end > start;
}

/**
 * 格式化日期时间
 * @param {string|Date} dateTime - 日期时间字符串或日期对象
 * @param {boolean} includeSeconds - 是否包含秒
 * @returns {string} - 格式化后的日期时间字符串
 */
export function formatDateTime(dateTime, includeSeconds = false) {
  if (!dateTime) return '';

  const date = typeof dateTime === 'string' ? new Date(dateTime) : dateTime;

  if (isNaN(date.getTime())) return '无效日期';

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  if (includeSeconds) {
    const seconds = String(date.getSeconds()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  }

  return `${year}-${month}-${day} ${hours}:${minutes}`;
}

/**
 * 获取当前日期的字符串，格式：YYYY-MM-DD
 * @returns {string} - 当前日期字符串
 */
export function getCurrentDateString() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');

  return `${year}-${month}-${day}`;
}

/**
 * 检查时间是否已过期
 * @param {string|Date} dateTime - 日期时间字符串或日期对象
 * @returns {boolean} - 是否已过期
 */
export function isTimeExpired(dateTime) {
  if (!dateTime) return true;

  const date = typeof dateTime === 'string' ? new Date(dateTime) : dateTime;
  const now = new Date();

  return date < now;
}