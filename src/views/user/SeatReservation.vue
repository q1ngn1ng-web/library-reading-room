<template>
  <div class="seat-reservation-container">
    <!-- 楼层切换控制 -->
    <div class="floor-controls">
      <el-button
        v-for="floorNumber in totalFloors"
        :key="floorNumber"
        @click="changeFloor(floorNumber)"
        :type="currentFloor === floorNumber ? 'primary' : 'info'"
      >
        {{ floorNumber }}层
      </el-button>
    </div>

    <!-- 当前活跃预约提示 -->
    <el-alert
      v-if="isValidReservation(activeReservation)"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 20px;"
    >
      <template slot="title">
        您当前有一个预约: {{activeReservation.floorNumber}}楼 {{activeReservation.seatNumber}}号座位
        <el-button size="mini" type="danger" @click="cancelCurrentReservation" style="margin-left: 10px;">
          取消预约
        </el-button>
        <el-button
          size="mini"
          type="primary"
          @click="checkInCurrentReservation"
          v-if="activeReservation.status === 'reserved' || activeReservation.status === 1"
          style="margin-left: 10px;"
        >
          签到
        </el-button>
        <el-button
          size="mini"
          type="warning"
          @click="checkOutCurrentReservation"
          v-if="activeReservation.status === 'checked_in' || activeReservation.status === 2"
          style="margin-left: 10px;"
        >
          退座
        </el-button>
      </template>
    </el-alert>

    <div class="seat-map-header">
      <h2>{{ currentFloor }}层 座位预约</h2>
      <div class="seat-legend">
        <div class="legend-item">
          <div class="seat-icon seat-available"></div>
          <span>可预约</span>
        </div>
        <div class="legend-item">
          <div class="seat-icon seat-reserved"></div>
          <span>已预约</span>
        </div>
        <div class="legend-item">
          <div class="seat-icon seat-occupied"></div>
          <span>使用中</span>
        </div>
        <div class="legend-item">
          <div class="seat-icon seat-maintenance"></div>
          <span>维护中</span>
        </div>
      </div>
    </div>

    <!-- 座位地图 -->
    <div v-loading="loading" class="seat-map">
      <div
        v-for="seat in currentFloorSeats"
        :key="seat.id"
        class="seat"
        :class="getSeatClass(seat)"
        @click="handleSeatClick(seat)"
      >
        <div class="seat-number">{{ seat.seatNumber }}</div>
      </div>
    </div>

    <!-- 座位详情对话框 -->
    <el-dialog
      title="座位详情"
      :visible.sync="showSeatDetailDialog"
      width="500px"
    >
      <div v-if="selectedSeat" class="seat-detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="座位号">
            {{ selectedSeat.seatNumber || selectedSeat.number }} ({{ selectedSeat.roomId || selectedSeat.room_id }}楼)
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(selectedSeat.status)">
              {{ getStatusText(selectedSeat.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="位置" v-if="selectedSeat.position">
            {{ selectedSeat.position }}
          </el-descriptions-item>
          <el-descriptions-item label="预约人" v-if="selectedSeat.userId || selectedSeat.user_id">
            {{ selectedSeat.userName || selectedSeat.user_name || selectedSeat.userId || selectedSeat.user_id }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="action-buttons" v-if="isSeatAvailable(selectedSeat)">
          <el-button type="primary" @click="openReservationDialog(selectedSeat)">
            预约此座位
          </el-button>
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="showSeatDetailDialog = false">关闭</el-button>
      </span>
    </el-dialog>

    <!-- 预约对话框 -->
    <el-dialog
      :title="`预约${currentFloor}层${selectedSeat?.seatNumber || ''}号座位`"
      :visible.sync="showReservationDialog"
      width="500px"
    >
      <el-form :model="reservationForm" label-width="100px" :rules="reservationRules" ref="reservationForm">
        <el-form-item label="座位号">
          <el-input v-model="reservationForm.seatNumber" disabled></el-input>
        </el-form-item>
        <el-form-item label="预约日期" prop="date">
          <el-date-picker
            v-model="reservationForm.date"
            type="date"
            placeholder="选择日期"
            value-format="yyyy-MM-dd"
            :picker-options="dateOptions"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="时间段" required>
          <el-time-select
            v-model="reservationForm.startTime"
            :picker-options="{
              start: '08:00',
              step: '00:30',
              end: '22:00'
            }"
            placeholder="开始时间"
            style="width: 45%;"
          ></el-time-select>
          <span style="margin: 0 10px;">至</span>
          <el-time-select
            v-model="reservationForm.endTime"
            :picker-options="{
              start: '08:30',
              step: '00:30',
              end: '22:30',
              minTime: reservationForm.startTime
            }"
            placeholder="结束时间"
            style="width: 45%;"
          ></el-time-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            type="textarea"
            :rows="2"
            placeholder="请输入备注信息"
            v-model="reservationForm.remark"
          ></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="showReservationDialog = false">取 消</el-button>
        <el-button
          type="primary"
          @click="submitReservation"
          :loading="submitting"
        >确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { mapState, mapGetters, mapActions } from 'vuex'
import { getStatusText, getStatusTagType, getSeatClass, isSeatAvailable, SEAT_STATUS } from '@/utils/seatUtils'
import { combineDateTime, validateTimeRange, formatDateTime, getCurrentDateString } from '@/utils/dateUtils'
import { RESERVATION_STATUS, getReservationStatusText, getReservationStatusTagType } from '@/utils/reservationUtils'

export default {
  name: 'SeatReservation',
  data() {
    return {
      loading: false,
      showReservationDialog: false,
      showSeatDetailDialog: false,
      submitting: false,
      selectedSeat: null,
      reservationForm: {
        userId: '',
        seatId: '',
        roomId: '',
        number: '',
        date: getCurrentDateString(),
        startTime: '',
        endTime: '',
        remark: ''
      },
      reservationRules: {
        date: [
          { required: true, message: '请选择预约日期', trigger: 'change' }
        ]
      },
      dateOptions: {
        disabledDate(time) {
          const today = new Date();
          today.setHours(0, 0, 0, 0);
          const tomorrow = new Date(today);
          tomorrow.setDate(today.getDate() + 1);
          return time.getTime() < today.getTime() || time.getTime() > tomorrow.getTime();
        }
      }
    }
  },
  computed: {
    ...mapState('seat', ['currentFloor', 'totalFloors', 'activeReservation']),
    ...mapGetters('seat', ['currentFloorSeats']),

    // 获取当前用户信息
    currentUser() {
      const userInfo = JSON.parse(localStorage.getItem('user') || '{}')
      console.log('当前用户信息:', userInfo)
      return userInfo
    }
  },
  // 初始化座位数据
  async created() {
    this.loading = true
    try {
      // 初始化楼层
      await this.initializeFloor(this.currentFloor || 1)
      // 获取座位信息
      await this.fetchFloorSeats(this.currentFloor)
      // 启动超时检查
      this.startTimeoutChecker()
    } catch (error) {
      console.error('初始化座位数据失败:', error)
      this.$message.error('加载座位数据失败，请刷新页面重试')
    } finally {
      this.loading = false
    }
  },
  beforeDestroy() {
    // 停止超时检查
    this.stopTimeoutChecker()
  },
  methods: {
    ...mapActions('seat', [
      'initializeFloor',
      'changeFloor',
      'fetchFloorSeats',
      'reserveSeat',
      'cancelReservation',
      'checkIn',
      'checkOut',
      'startTimeoutChecker',
      'stopTimeoutChecker'
    ]),

    // 处理座位点击
    handleSeatClick(seat) {
      console.log('点击座位:', seat)
      this.selectedSeat = seat

      // 显示座位详情对话框
      this.showSeatDetailDialog = true
    },

    // 打开预约对话框
    openReservationDialog(seat) {
      if (!seat || !isSeatAvailable(seat)) {
        this.$message.warning('该座位不可预约')
        return
      }

      // 关闭座位详情对话框
      this.showSeatDetailDialog = false

      // 设置预约表单
      this.reservationForm = {
        userId: this.currentUser.id,
        seatId: seat.id,
        roomId: seat.roomId || seat.room_id,
        seatNumber: seat.seatNumber || seat.number,
        date: getCurrentDateString(),
        startTime: '',
        endTime: '',
        remark: ''
      }

      // 显示预约对话框
      this.showReservationDialog = true
    },

    // 提交预约
    async submitReservation() {
      if (!this.reservationForm.startTime || !this.reservationForm.endTime) {
        this.$message.warning('请选择预约时间段')
        return
      }

      if (!this.currentUser || !this.currentUser.id) {
        this.$message.error('用户信息不完整，请重新登录')
        return
      }

      this.submitting = true
      try {
        // 构建预约数据 - 使用驼峰命名法匹配后端实体类字段
        const startTime = combineDateTime(this.reservationForm.date, this.reservationForm.startTime);
        const endTime = combineDateTime(this.reservationForm.date, this.reservationForm.endTime);

        // 计算最晚签到时间（预约开始时间后30分钟）
        const startDateTime = new Date(startTime);
        const timeoutAt = new Date(startDateTime.getTime() + 30 * 60 * 1000);

        const reservationData = {
          userId: this.currentUser.id, // 确保用户ID正确传递
          seatId: this.reservationForm.seatId,
          startTime: startTime,
          endTime: endTime,
          timeout_at: timeoutAt.toISOString(), // 添加最晚签到时间
          status: 0,  // 预约状态，可能需要根据后端定义设置
          // 以下字段可能不是必需的，但保留以便后端处理
          floorNumber: this.currentFloor,
          seatNumber: this.reservationForm.seatNumber,
          remark: this.reservationForm.remark
        }

        console.log('发送预约数据:', reservationData)

        // 提交预约
        const result = await this.reserveSeat(reservationData)
        if (result.success) {
          this.$message.success('预约成功')
          this.showReservationDialog = false
          // 刷新座位数据
          this.fetchFloorSeats(this.currentFloor)
        } else {
          this.$message.error(result.message || '预约失败')
        }
      } catch (error) {
        console.error('提交预约失败:', error)
        this.$message.error('预约失败，请重试')
      } finally {
        this.submitting = false
      }
    },

    // 取消当前预约
    async cancelCurrentReservation() {
      try {
        this.$confirm('确定要取消当前预约吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(async () => {
          const result = await this.cancelReservation()
          if (result.success) {
            this.$message.success('预约已取消')
            // 刷新座位数据
            this.fetchFloorSeats(this.currentFloor)
          } else {
            this.$message.error(result.message || '取消预约失败')
          }
        }).catch(() => {})
      } catch (error) {
        console.error('取消预约失败:', error)
        this.$message.error('取消预约失败，请重试')
      }
    },

    // 签到当前预约
    async checkInCurrentReservation() {
      try {
        const result = await this.checkIn()
        if (result.success) {
          this.$message.success('签到成功')
          // 刷新座位数据
          this.fetchFloorSeats(this.currentFloor)
        } else {
          this.$message.error(result.message || '签到失败')
        }
      } catch (error) {
        console.error('签到失败:', error)
        this.$message.error('签到失败，请重试')
      }
    },

    // 退座
    async checkOutCurrentReservation() {
      try {
        this.$confirm('确定要退座吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(async () => {
          const result = await this.checkOut()
          if (result.success) {
            this.$message.success('已退座')
            // 刷新座位数据
            this.fetchFloorSeats(this.currentFloor)
          } else {
            this.$message.error(result.message || '退座失败')
          }
        }).catch(() => {})
      } catch (error) {
        console.error('退座失败:', error)
        this.$message.error('退座失败，请重试')
      }
    },

    // 检查预约是否有效
    isValidReservation(reservation) {
      return reservation &&
        typeof reservation === 'object' &&
        !Array.isArray(reservation) &&
        reservation.floorNumber &&
        reservation.seatNumber &&
        (reservation.reservationId || reservation.id);
    },

    // 导出工具函数供模板使用
    getStatusText,
    getStatusTagType,
    getSeatClass,
    formatDateTime,
    isSeatAvailable
  }
}
</script>

<style scoped>
.seat-reservation-container {
  padding: 20px;
}

.floor-controls {
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.seat-map-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.seat-legend {
  display: flex;
  gap: 15px;
}

.legend-item {
  display: flex;
  align-items: center;
  margin-right: 10px;
}

.seat-icon {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  margin-right: 5px;
}

.seat-map {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 15px;
  margin-top: 20px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.seat {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  font-weight: bold;
}

.seat:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.seat:before {
  content: "";
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 16px;
  height: 8px;
  background-color: inherit;
  border-radius: 4px 4px 0 0;
}

.seat-number {
  font-weight: bold;
  font-size: 16px;
}

.seat-available {
  background-color: #67C23A;
  color: white;
}

.seat-reserved {
  background-color: #E6A23C;
  color: white;
}

.seat-occupied {
  background-color: #409EFF;
  color: white;
}

.seat-maintenance {
  background-color: #F56C6C;
  color: white;
}

.seat-detail {
  padding: 10px;
}

.action-buttons {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>