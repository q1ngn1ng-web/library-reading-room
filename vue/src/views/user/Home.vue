<template>
  <div>
    <div style="color: #5050bb; font-weight: bold; font-size: 20px">
      欢迎您 {{user.name}}，这里是图书馆阅览室系统 ！
    </div>
    <div style="display: flex; margin-top: 20px">
      <div style="flex: 1">
        <div style="margin-bottom: 15px; font-weight: bold; font-size: 18px; color: #344675">系统公告</div>
        <el-collapse v-model="activeName" accordion>
          <el-collapse-item v-for="item in data" :title="item.name" :name="item.id">
            <template slot="title">
              <i class="header-icon el-icon-info"> {{item.name}} </i>
            </template>
            <div style="padding: 0 20px">{{item.content}}</div>
          </el-collapse-item>
        </el-collapse>
      </div>
      <div style="flex: 1; margin-left: 20px;">
        <el-card class="reservation-card">
          <div slot="header">
            <span>🕒 我的当前预约</span>
            <el-button
              size="mini"
              @click="$router.push('/seat-reservation')"
              style="float: right; padding: 3px 0"
            >
              {{ activeReservation ? '修改预约' : '去预约' }}
            </el-button>
          </div>

          <div v-loading="loading">
            <!-- 无预约时显示提示 -->
            <div v-if="!activeReservation || !formatReservation(activeReservation)" class="empty-tip">
              <i class="el-icon-info"></i> 当前没有进行中的预约
            </div>

            <!-- 有预约时显示竖式卡片 -->
            <div v-else class="reservation-vertical-card">
              <div v-if="activeReservation" class="reservation-info">
                <div class="reservation-item">
                  <div class="item-label">楼层/座位</div>
                  <div class="item-value">{{ formatReservation(activeReservation).floorNumber }}楼{{ formatReservation(activeReservation).seatNumber }}号</div>
                </div>

                <div class="reservation-item">
                  <div class="item-label">日期</div>
                  <div class="item-value">{{ formatReservation(activeReservation).dateOnly }}</div>
                </div>

                <div class="reservation-item">
                  <div class="item-label">时间段</div>
                  <div class="item-value">{{ formatReservation(activeReservation).timeRange }}</div>
                </div>

                <div class="reservation-item">
                  <div class="item-label">预约超时时间</div>
                  <div class="item-value">{{ formatTimeoutAt(activeReservation) }}</div>
                </div>

                <div class="reservation-item">
                  <div class="item-label">状态</div>
                  <div class="item-value">
                    <el-tag :type="getStatusTagType(formatReservation(activeReservation).status)" size="small">
                      {{ getReservationStatusText(formatReservation(activeReservation).status) }}
                    </el-tag>
                  </div>
                </div>

                <div class="reservation-item">
                  <div class="item-label">操作</div>
                  <div class="item-value">
                    <!-- 已预约状态显示签到和取消按钮 -->
                    <template v-if="isReservedStatus(formatReservation(activeReservation).status)">
                      <el-button
                        size="mini"
                        type="primary"
                        @click="handleCheckIn()"
                      >
                        签到
                      </el-button>
                      <el-button
                        size="mini"
                        type="danger"
                        @click="handleCancelReservation()"
                      >
                        取消预约
                      </el-button>
                    </template>

                    <!-- 已签到状态显示退座按钮 -->
                    <template v-if="isCheckedInStatus(formatReservation(activeReservation).status)">
                      <el-button
                        size="mini"
                        type="warning"
                        @click="handleCheckOut()"
                      >
                        退座
                      </el-button>
                    </template>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script>
import request from "@/utils/request";
import { mapState, mapActions } from 'vuex';
import { formatDateTime } from '@/utils/dateUtils';
import { RESERVATION_STATUS, getReservationStatusText, getReservationStatusTagType } from '@/utils/reservationUtils';

export default {
  name: 'HomeView',

  data() {
    return {
      activeName: '1',
      data: [],
      user: localStorage.getItem("user") ? JSON.parse(localStorage.getItem("user")) : {},
      admin: localStorage.getItem("admin") ? JSON.parse(localStorage.getItem("admin")) : {},
      baseURL: process.env.VUE_APP_BASE_API,
      loading: false,
      refreshTimer: null
    };
  },
  computed: {
    ...mapState('seat', ['activeReservation'])
  },
  mounted() {
    this.findNotice();
    this.fetchCurrentReservation();

    // 设置定时刷新 - 每30秒刷新一次预约状态
    this.refreshTimer = setInterval(() => {
      this.refreshData();
    }, 30000);
  },
  beforeDestroy() {
    // 组件销毁前清除定时器
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
    }
  },
  methods: {
    ...mapActions('seat', [
      'fetchCurrentReservation',
      'cancelReservation',
      'checkIn',
      'checkOut'
    ]),
    findNotice() {
      request.get("/notice").then(res => {
        if (res.code === '0') {
          this.data = res.data;
          this.activeName = res.data[0]?.id;
        } else {
          this.$message.error(res.msg)
        }
      })
    },
    // 格式化预约信息用于表格显示
    formatReservation(reservation) {
      // 检查是否为有效的预约对象
      if (!reservation || typeof reservation !== 'object' || Array.isArray(reservation)) {

        return {
          status: 0, // 设置为可预约状态
          _originalStatus: null
        };
      }

      // 检查是否存在嵌套结构（reservation和seat对象）
      let reservationData = reservation;
      let seatData = {};

      if (reservation.reservation && typeof reservation.reservation === 'object') {
        reservationData = reservation.reservation;
        seatData = reservation.seat || {};

        // 构建扁平化的结构
        const flatReservation = {
          // 预约信息
          id: reservationData.id,
          reservationId: reservationData.id,
          userId: reservationData.userId,
          seatId: reservationData.seatId,
          status: reservationData.status,
          startTime: reservationData.startTime,
          endTime: reservationData.endTime,
          realStartTime: reservationData.realStartTime,
          realEndTime: reservationData.realEndTime,
          timeout_at: reservationData.timeoutAt,

          // 座位信息
          floorNumber: seatData.roomId,
          seatNumber: seatData.number,

          // 保留原始数据
          _originalData: reservation
        };

        return this.formatFlatReservation(flatReservation);
      }

      // 如果已经是扁平结构，直接处理
      return this.formatFlatReservation(reservation);
    },
    // 处理扁平化的预约数据结构
    formatFlatReservation(reservation) {
      // 检查是否包含必要字段
      if (!reservation.reservationId && !reservation.id) {
        return {
          status: 0, // 设置为可预约状态
          _originalStatus: reservation.status
        };
      }

      // 确保处理所有可能的字段名称
      const reservationId = reservation.reservationId || reservation.id;
      const floorNumber = reservation.floorNumber || reservation.floor_number || reservation.roomId || reservation.room_id;
      const seatNumber = reservation.seatNumber || reservation.seat_number || reservation.number;
      const startTimeStr = reservation.startTime || reservation.start_time;
      const endTimeStr = reservation.endTime || reservation.end_time;
      const realStartTimeStr = reservation.realStartTime || reservation.real_start_time;
      const timeoutAtStr = reservation.timeoutAt || reservation.timeout_at;

      // 必要字段检查
      if (!floorNumber || !seatNumber) {
        return {
          id: reservationId,
          status: 0, // 设置为可预约状态
          _originalStatus: reservation.status
        };
      }

      // 处理不同格式的状态 - 统一转为数字类型
      let status;
      if (typeof reservation.status === 'number') {
        // 数字类型，直接使用
        status = reservation.status;
      } else if (typeof reservation.status === 'string' && !isNaN(parseInt(reservation.status))) {
        // 数字字符串，转为数字
        status = parseInt(reservation.status);
      } else if (typeof reservation.status === 'string') {
        // 字符串类型，转换为对应的数字
        switch(reservation.status.toLowerCase()) {
          case 'reserved': status = 1; break;
          case 'checked_in':
          case 'occupied': status = 5; break; // 已占用状态转为签到完成状态
          case 'timeout': status = 3; break;
          case 'cancelled':
          case 'canceled': status = 4; break;
          case 'completed': status = 5; break;
          default: status = 0;
        }
      } else {
        // 未定义或其他情况，默认为0 (可预约)
        status = 0;
      }

      // 提取日期和时间范围
      let startTime = startTimeStr ? new Date(startTimeStr) : null;
      let endTime = endTimeStr ? new Date(endTimeStr) : null;
      let realStartTime = realStartTimeStr ? new Date(realStartTimeStr) : null;
      let timeoutAt = timeoutAtStr ? new Date(timeoutAtStr) : null;

      // 如果有实际开始时间，优先使用
      if (realStartTime && (status === 5 || status === RESERVATION_STATUS.COMPLETED)) {
        startTime = realStartTime;
      }

      // 格式化日期和时间 - 确保只显示日期或只显示时间
      const formattedDate = startTime ? formatDateTime(startTime, 'yyyy-MM-dd') : '未知日期';

      // 仅提取时间部分
      const formattedStartTime = startTime ? formatDateTime(startTime, 'HH:mm') : '未知';
      const formattedEndTime = endTime ? formatDateTime(endTime, 'HH:mm') : '未知';
      const formattedTimeRange = `${formattedStartTime} - ${formattedEndTime}`;
      const formattedTimeoutAt = timeoutAt ? formatDateTime(timeoutAt, 'yyyy-MM-dd HH:mm:ss') : '未知';

      const result = {
        id: reservationId,
        floorNumber,
        seatNumber,
        dateOnly: formattedDate,       // 仅日期
        timeRange: formattedTimeRange, // 仅时间
        timeoutAt: formattedTimeoutAt, // 超时时间
        startTime: startTime,
        endTime: endTime,
        status,
        // 为调试目的添加原始状态
        _originalStatus: reservation.status
      };

      return result;
    },
    // 格式化预约超时时间
    formatTimeoutAt(reservation) {
      if (!reservation) return '未知';

      // 嵌套结构
      if (reservation.reservation) {
        const timeoutAt = reservation.reservation.timeoutAt || reservation.reservation.timeout_at;
        if (timeoutAt) {
          return formatDateTime(new Date(timeoutAt), 'yyyy-MM-dd HH:mm:ss');
        }
      }

      // 扁平结构
      const timeoutAt = reservation.timeoutAt || reservation.timeout_at;
      if (timeoutAt) {
        return formatDateTime(new Date(timeoutAt), 'yyyy-MM-dd HH:mm:ss');
      }

      return '未知';
    },
    // 刷新数据
    refreshData() {
      this.loading = true;

      // 刷新预约信息
      this.fetchCurrentReservation().finally(() => {
        this.loading = false;
      });
    },
    // 签到
    async handleCheckIn() {
      try {
        this.loading = true;
        const result = await this.checkIn();
        this.loading = false;

        if (result.success) {
          this.$message.success('签到成功');

          // 强制刷新预约信息
          await this.fetchCurrentReservation();

          // 如果页面状态没有正确更新，可以强制刷新数据
          setTimeout(() => {
            this.fetchCurrentReservation();
          }, 1000);
        } else {
          this.$message.error(result.message || '签到失败');
        }
      } catch (error) {
        this.loading = false;
        console.error('签到失败:', error);
        this.$message.error('签到失败，请重试');
      }
    },
    // 取消预约
    async handleCancelReservation() {
      try {
        this.$confirm('确定要取消当前预约吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(async () => {
          this.loading = true;
          const result = await this.cancelReservation();
          this.loading = false;

          if (result.success) {
            this.$message.success('预约已取消');
            this.fetchCurrentReservation();
          } else {
            this.$message.error(result.message || '取消预约失败');
          }
        }).catch(() => {});
      } catch (error) {
        this.loading = false;
        console.error('取消预约失败:', error);
        this.$message.error('取消预约失败，请重试');
      }
    },
    // 退座
    async handleCheckOut() {
      try {
        this.$confirm('确定要退座吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(async () => {
          this.loading = true;
          const result = await this.checkOut();
          this.loading = false;

          if (result.success) {
            this.$message.success('已退座');
            this.fetchCurrentReservation();
          } else {
            this.$message.error(result.message || '退座失败');
          }
        }).catch(() => {});
      } catch (error) {
        this.loading = false;
        console.error('退座失败:', error);
        this.$message.error('退座失败，请重试');
      }
    },
    // 获取预约状态的显示文本
    getReservationStatusText(status) {
      return getReservationStatusText(status);
    },
    // 获取状态标签类型
    getStatusTagType(status) {
      return getReservationStatusTagType(status);
    },
    // 检查是否是"已预约"状态
    isReservedStatus(status) {
      // 检查是否等于1（预约成功）
      if (typeof status === 'number') return status === RESERVATION_STATUS.RESERVED;
      if (typeof status === 'string') {
        if (status === 'reserved') return true;
        if (!isNaN(parseInt(status))) return parseInt(status) === RESERVATION_STATUS.RESERVED;
      }
      return false;
    },

    // 检查是否是"已签到/使用中"状态
    isCheckedInStatus(status) {
      // 检查是否等于5（签到完成）
      if (typeof status === 'number') return status === RESERVATION_STATUS.COMPLETED;
      if (typeof status === 'string') {
        if (['completed', 'checked_in', 'occupied'].includes(status)) return true;
        if (!isNaN(parseInt(status))) return parseInt(status) === RESERVATION_STATUS.COMPLETED;
      }
      return false;
    },
  }
}
</script>

<style scoped>
.el-collapse-item__header {
  color: #344675 !important;
}

.reservation-card {
  margin-bottom: 20px;
}

.empty-tip {
  color: #909399;
  text-align: center;
  padding: 20px 0;
  font-size: 14px;
}

.empty-tip i {
  margin-right: 5px;
  font-size: 16px;
}

.reservation-vertical-card {
  border: 1px solid #EBEEF5;
  border-radius: 8px;
  background-color: #FAFAFA;
  padding: 15px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.reservation-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reservation-item {
  border-bottom: 1px solid #EBEEF5;
  padding-bottom: 10px;
}

.item-label {
  color: #606266;
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 5px;
}

.item-value {
  color: #303133;
  font-size: 15px;
}

.reservation-item:last-child {
  border-bottom: none;
}
</style>
