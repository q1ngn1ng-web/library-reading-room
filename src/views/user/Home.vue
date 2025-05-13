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
              @click="$router.push('/user/seats')"
              style="float: right; padding: 3px 0"
            >
              {{ activeReservation ? '修改预约' : '去预约' }}
            </el-button>
          </div>

          <el-table
            :data="activeReservation ? [formatReservation(activeReservation)] : []"
            style="width: 100%"
            v-loading="loading"
          >
            <template v-slot:empty>
              <div class="empty-tip">
                <i class="el-icon-info"></i> 当前没有进行中的预约
              </div>
            </template>
            <el-table-column label="楼层/座位" width="120">
              <template slot-scope="{row}">
                {{ row.floorNumber }}楼{{ row.seatNumber }}号
              </template>
            </el-table-column>

            <el-table-column label="日期" width="120">
              <template slot-scope="{row}">
                {{ row.dateOnly }}
              </template>
            </el-table-column>

            <el-table-column label="时间段">
              <template slot-scope="{row}">
                {{ row.timeRange }}
              </template>
            </el-table-column>

            <el-table-column label="状态" width="120">
              <template slot-scope="{row}">
                <el-tag :type="getStatusTagType(row.status)" size="small">
                  {{ getReservationStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="180">
              <template slot-scope="{row}">
                <div>
                  <!-- 调试信息 -->
                  <div style="font-size: 11px; color: #999; margin-bottom: 5px;">
                    状态值: {{row.status}} ({{typeof row.status}})
                  </div>

                  <!-- 已预约状态显示签到和取消按钮 -->
                  <template v-if="isReservedStatus(row.status)">
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
                  <template v-if="isCheckedInStatus(row.status)">
                    <el-button
                      size="mini"
                      type="warning"
                      @click="handleCheckOut()"
                    >
                      退座
                    </el-button>
                  </template>
                </div>
              </template>
            </el-table-column>
          </el-table>
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

      console.log('状态转换结果:', {
        originalStatus: reservation.status,
        convertedStatus: status
      });

      // 提取日期和时间范围
      let startTime = startTimeStr ? new Date(startTimeStr) : null;
      let endTime = endTimeStr ? new Date(endTimeStr) : null;
      let realStartTime = realStartTimeStr ? new Date(realStartTimeStr) : null;

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

      const result = {
        id: reservationId,
        floorNumber,
        seatNumber,
        dateOnly: formattedDate,       // 仅日期
        timeRange: formattedTimeRange, // 仅时间
        startTime: startTime,
        endTime: endTime,
        status,
        // 为调试目的添加原始状态
        _originalStatus: reservation.status
      };

      return result;
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
</style>
