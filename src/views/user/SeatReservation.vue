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
        您当前有一个预约:
        {{
          (activeReservation.floorNumber ||
          activeReservation.seat?.roomId ||
          '未知')
        }}楼
        {{
          (activeReservation.seatNumber ||
          activeReservation.seat?.number ||
          '未知')
        }}号座位
        <div class="reservation-date-info">{{ reservationDateInfo }}</div>
        <div class="reservation-time-info">{{ reservationTimeInfo }}</div>
        <el-button size="mini" type="danger" @click="cancelCurrentReservation" style="margin-left: 10px;">
          取消预约
        </el-button>
        <el-button
          size="mini"
          type="primary"
          @click="checkInCurrentReservation"
          v-if="showCheckInButton"
          style="margin-left: 10px;"
        >
          签到
        </el-button>
        <el-button
          size="mini"
          type="warning"
          @click="checkOutCurrentReservation"
          v-if="showCheckOutButton"
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
            :picker-options="startTimeOptions"
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
        date: '',  // 初始化为空，将在created钩子中设置
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
    },

    // 格式化预约时间信息
    reservationTimeInfo() {
      if (!this.activeReservation) return '';

      // 检查是否是嵌套结构
      let reservation = this.activeReservation;
      if (this.activeReservation.reservation && typeof this.activeReservation.reservation === 'object') {
        reservation = {
          ...this.activeReservation.reservation,
          floorNumber: this.activeReservation.seat?.roomId,
          seatNumber: this.activeReservation.seat?.number
        };
      }

      // 获取开始结束时间
      const startTimeStr = reservation.startTime || reservation.start_time;
      const endTimeStr = reservation.endTime || reservation.end_time;
      const realStartTimeStr = reservation.realStartTime || reservation.real_start_time;

      let startTime = startTimeStr ? new Date(startTimeStr) : null;
      let endTime = endTimeStr ? new Date(endTimeStr) : null;
      let realStartTime = realStartTimeStr ? new Date(realStartTimeStr) : null;

      // 如果已签到，显示实际开始时间
      if ((reservation.status === 'checked_in' || reservation.status === 2) && realStartTime) {
        const formattedDate = formatDateTime(realStartTime, 'yyyy-MM-dd');
        const formattedStartTime = formatDateTime(realStartTime, 'HH:mm');
        const formattedEndTime = endTime ? formatDateTime(endTime, 'HH:mm') : '未知';
        // 显示日期和时间在不同的行
        return `签到时间: ${formattedStartTime} - ${formattedEndTime}`;
      } else {
        // 否则显示预约时间
        const formattedStartTime = startTime ? formatDateTime(startTime, 'HH:mm') : '未知';
        const formattedEndTime = endTime ? formatDateTime(endTime, 'HH:mm') : '未知';
        return `预约时间: ${formattedStartTime} - ${formattedEndTime}`;
      }
    },

    // 获取预约日期信息
    reservationDateInfo() {
      if (!this.activeReservation) return '';

      // 检查是否是嵌套结构
      let reservation = this.activeReservation;
      if (this.activeReservation.reservation && typeof this.activeReservation.reservation === 'object') {
        reservation = this.activeReservation.reservation;
      }

      // 获取日期
      const startTimeStr = reservation.startTime || reservation.start_time;
      const realStartTimeStr = reservation.realStartTime || reservation.real_start_time;

      // 如果已签到，优先显示签到日期
      if ((reservation.status === 'checked_in' || reservation.status === 2) && realStartTimeStr) {
        const realStartTime = new Date(realStartTimeStr);
        return `日期: ${formatDateTime(realStartTime, 'yyyy-MM-dd')}`;
      } else if (startTimeStr) {
        const startTime = new Date(startTimeStr);
        return `日期: ${formatDateTime(startTime, 'yyyy-MM-dd')}`;
      }

      return '日期: 未知';
    },

    // 开始时间选择器配置
    startTimeOptions() {
      // 获取当前时间
      const now = new Date();
      const currentHour = now.getHours();
      const currentMinute = now.getMinutes();

      // 选择当前时间后最近的30分钟时间点作为最早可选时间
      let minHour = currentHour;
      let minMinute = currentMinute >= 30 ? 30 : 0;

      // 如果当前时间已经超过本小时的30分钟点，进入下一小时
      if (currentMinute > 30) {
        minHour += 1;
        minMinute = 0;
      }

      // 格式化为"HH:mm"格式，确保小于10的数字前面有0
      const minTime = `${minHour < 10 ? '0' + minHour : minHour}:${minMinute === 0 ? '00' : minMinute}`;

      // 判断是否为今天预约
      const isToday = this.reservationForm.date === this.getCurrentDateString();

      // 如果是今天预约，使用当前时间后最近的30分钟点作为最早可选时间
      // 如果是明天预约，允许从8点开始选择
      return {
        start: isToday ? minTime : '08:00',
        step: '00:30',
        end: '22:00'
      };
    },

    // 是否显示签到按钮
    showCheckInButton() {
      if (!this.activeReservation) return false;

      // 嵌套结构
      if (this.activeReservation.reservation) {
        const status = this.activeReservation.reservation.status;
        return status === 'reserved' || status === 1;
      }

      // 扁平结构
      return this.activeReservation.status === 'reserved' || this.activeReservation.status === 1;
    },

    // 是否显示退座按钮
    showCheckOutButton() {
      if (!this.activeReservation) return false;

      // 嵌套结构
      if (this.activeReservation.reservation) {
        const status = this.activeReservation.reservation.status;
        return status === 'checked_in' || status === 2;
      }

      // 扁平结构
      return this.activeReservation.status === 'checked_in' || this.activeReservation.status === 2;
    }
  },
  // 初始化座位数据
  async created() {
    this.loading = true
    try {
      // 设置默认预约日期为今天
      this.reservationForm.date = this.getCurrentDateString();

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
        date: this.getCurrentDateString(),
        startTime: '',
        endTime: '',
        remark: ''
      }

      // 显示预约对话框
      this.showReservationDialog = true

      // 监听日期变化，当日期改变时重置开始时间
      this.$watch('reservationForm.date', (newDate) => {
        // 重置开始和结束时间
        this.reservationForm.startTime = '';
        this.reservationForm.endTime = '';
      });
    },

    // 提交预约
    async submitReservation() {
      if (!this.reservationForm.startTime || !this.reservationForm.endTime) {
        this.$message.warning('请选择预约时间段');
        return;
      }

      // 验证所选时间是否在当前时间之后
      const isValid = this.validateReservationTime();
      if (!isValid) {
        this.$message.warning('预约时间必须在当前时间之后');
        return;
      }

      if (!this.currentUser || !this.currentUser.id) {
        this.$message.error('用户信息不完整，请重新登录');
        return;
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

    // 验证预约时间是否在当前时间之后
    validateReservationTime() {
      const now = new Date();
      const reservationDate = new Date(this.reservationForm.date);
      const [hours, minutes] = this.reservationForm.startTime.split(':').map(Number);

      reservationDate.setHours(hours, minutes, 0, 0);

      // 预约时间必须至少在当前时间5分钟之后
      const minReservationTime = new Date(now.getTime() + 5 * 60 * 1000);

      return reservationDate > minReservationTime;
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
      if (!reservation || typeof reservation !== 'object' || Array.isArray(reservation)) {
        return false;
      }

      // 检查是否是嵌套结构
      if (reservation.reservation && typeof reservation.reservation === 'object' && reservation.seat && typeof reservation.seat === 'object') {
        const reservationData = reservation.reservation;
        const seatData = reservation.seat;

        return reservationData.id &&
          seatData.roomId &&
          seatData.number;
      }

      // 扁平结构检查
      return reservation &&
        typeof reservation === 'object' &&
        !Array.isArray(reservation) &&
        (reservation.floorNumber || reservation.roomId) &&
        (reservation.seatNumber || reservation.number) &&
        (reservation.reservationId || reservation.id);
    },

    // 导出工具函数供模板使用
    getStatusText,
    getStatusTagType,
    getSeatClass,
    formatDateTime,
    isSeatAvailable,

    // 获取当前日期字符串，格式为yyyy-MM-dd
    getCurrentDateString() {
      const date = new Date();
      const year = date.getFullYear();
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const day = date.getDate().toString().padStart(2, '0');
      return `${year}-${month}-${day}`;
    }
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

.reservation-date-info {
  font-size: 14px;
  margin: 5px 0;
  color: #606266;
  font-weight: bold;
}

.reservation-time-info {
  font-size: 14px;
  margin: 5px 0;
  color: #606266;
}
</style>