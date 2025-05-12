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
      v-if="activeReservation"
      :title="`您当前有一个预约: ${activeReservation.floorNumber}楼 ${activeReservation.seatNumber}号座位`"
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
          type="success"
          @click="checkInCurrentReservation"
          v-if="activeReservation.status === 'reserved'"
          style="margin-left: 10px;"
        >
          签到
        </el-button>
        <el-button
          size="mini"
          type="warning"
          @click="checkOutCurrentReservation"
          v-if="activeReservation.status === 'checked_in'"
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
      <div v-if="selectedSeat">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="座位号">
            {{ selectedSeat.number }} ({{ selectedSeat.room_id }}楼)
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(selectedSeat.status)">
              {{ getStatusText(selectedSeat.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="预约人" v-if="selectedSeat.user_id">
            {{ selectedSeat.user_id }}
          </el-descriptions-item>
        </el-descriptions>
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
import { getStatusText, getStatusTagType, getSeatClass, isSeatAvailable } from '@/utils/seatUtils'
import { combineDateTime, validateTimeRange, formatDateTime, getCurrentDateString } from '@/utils/dateUtils'

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
      return JSON.parse(localStorage.getItem('user') || '{}')
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

    // 座位点击处理
    async handleSeatClick(seat) {
      if (!seat) return

      this.selectedSeat = seat

      // 如果座位可用，显示预约对话框
      if (isSeatAvailable(seat)) {
        this.reservationForm.seatId = seat.id
        this.reservationForm.userId = this.currentUser.id || ''
        this.reservationForm.roomId = seat.room_id
        this.reservationForm.number = seat.number
        this.showReservationDialog = true
      } else {
        // 否则显示座位详情
        this.showSeatDetailDialog = true
      }
    },

    // 验证预约表单
    validateForm() {
      if (!this.reservationForm.date) {
        this.$message.warning('请选择预约日期')
        return false
      }

      if (!this.reservationForm.startTime || !this.reservationForm.endTime) {
        this.$message.warning('请选择预约时间段')
        return false
      }

      if (!validateTimeRange(this.reservationForm.startTime, this.reservationForm.endTime)) {
        this.$message.warning('结束时间必须晚于开始时间')
        return false
      }

      return true
    },

    // 提交预约
    async submitReservation() {
      if (!this.validateForm()) return

      this.submitting = true

      try {
        // 合并日期时间
        const startTime = combineDateTime(this.reservationForm.date, this.reservationForm.startTime)
        const endTime = combineDateTime(this.reservationForm.date, this.reservationForm.endTime)

        const reservationData = {
          user_id: this.currentUser.id,
          seat_id: this.reservationForm.seatId,
          start_time: startTime,
          end_time: endTime,
          remark: this.reservationForm.remark
        }

        const result = await this.reserveSeat(reservationData)

        if (result.success) {
          this.$message.success('预约成功')
          this.showReservationDialog = false

          // 重新获取座位数据
          await this.fetchFloorSeats(this.currentFloor)
        } else {
          this.$message.error(result.message || '预约失败')
        }
      } catch (error) {
        console.error('提交预约失败:', error)
        this.$message.error('预约提交失败，请重试')
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

    // 导出工具函数供模板使用
    getStatusText,
    getStatusTagType,
    getSeatClass,
    formatDateTime
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
}

.seat {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.seat:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
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
</style>