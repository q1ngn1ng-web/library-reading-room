<template>
  <div class="seat-management">
    <div class="page-header">
      <h2>座位管理</h2>
      <div>
        <el-select
          v-model="selectedFloor"
          placeholder="请选择楼层"
          @change="fetchSeats">
          <el-option
            v-for="floor in floors"
            :key="floor.roomId"
            :label="floor.floor"
            :value="floor.roomId">
          </el-option>
        </el-select>
        <el-button
          type="primary"
          :disabled="!selectedFloor"
          @click="batchUpdateSeats">
          批量更新座位状态
        </el-button>
      </div>
    </div>

    <!-- 座位状态颜色说明 -->
    <div class="legend-container">
      <div class="legend-item">
        <div class="legend-color available"></div>
        <span>可用</span>
      </div>
      <div class="legend-item">
        <div class="legend-color reserved"></div>
        <span>已预约</span>
      </div>
      <div class="legend-item">
        <div class="legend-color occupied"></div>
        <span>使用中</span>
      </div>
      <div class="legend-item">
        <div class="legend-color disabled"></div>
        <span>不可用</span>
      </div>
    </div>

    <!-- 座位展示区域 -->
    <div class="seats-container" v-loading="loading">
      <div v-if="!selectedFloor" class="no-floor-selected">
        请选择楼层
      </div>
      <div v-else-if="seats.length === 0 && !loading" class="no-seats">
        当前楼层暂无座位
      </div>
      <template v-else>
        <div class="seat-grid">
          <div
            v-for="seat in seats"
            :key="seat.id"
            class="seat"
            :class="getSeatClass(seat.status)"
            @click="handleSeatClick(seat)">
            {{ seat.seatNumber }}
          </div>
        </div>
      </template>
    </div>

    <!-- 座位详情对话框 -->
    <el-dialog
      title="座位详情"
      :visible.sync="seatDialogVisible"
      width="400px">
      <div v-if="selectedSeat" class="seat-detail">
        <p><strong>座位编号：</strong>{{ selectedSeat.seatNumber }}</p>
        <p><strong>当前状态：</strong>{{ getSeatStatusText(selectedSeat.status) }}</p>

        <el-form :model="seatForm" label-width="80px">
          <el-form-item label="修改状态">
            <el-select v-model="seatForm.status" placeholder="请选择状态">
              <el-option :value="0" label="可用"></el-option>
              <el-option :value="1" label="已预约"></el-option>
              <el-option :value="2" label="使用中"></el-option>
              <el-option :value="3" label="不可用"></el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="通知消息">
            <el-input
              type="textarea"
              :rows="3"
              v-model="seatForm.message"
              placeholder="可选填，修改状态时发送给用户的通知"></el-input>
          </el-form-item>
        </el-form>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="seatDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="updateSeat">确 定</el-button>
      </span>
    </el-dialog>

    <!-- 批量更新对话框 -->
    <el-dialog
      title="批量更新座位状态"
      :visible.sync="batchDialogVisible"
      width="400px">
      <el-form :model="batchForm" label-width="80px">
        <el-form-item label="修改状态">
          <el-select v-model="batchForm.status" placeholder="请选择状态">
            <el-option :value="0" label="可用"></el-option>
            <el-option :value="1" label="已预约"></el-option>
            <el-option :value="2" label="使用中"></el-option>
            <el-option :value="3" label="不可用"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="通知消息">
          <el-input
            type="textarea"
            :rows="3"
            v-model="batchForm.message"
            placeholder="可选填，修改状态时发送给用户的通知"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="batchDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitBatchUpdate">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'SeatManagement',
  data() {
    return {
      // 楼层相关
      floors: [],
      selectedFloor: null,

      // 座位数据
      seats: [],
      loading: false,

      // 座位详情
      seatDialogVisible: false,
      selectedSeat: null,
      seatForm: {
        status: 0,
        message: ''
      },

      // 批量更新
      batchDialogVisible: false,
      batchForm: {
        status: 0,
        message: ''
      }
    }
  },
  created() {
    this.fetchFloors()
  },
  methods: {
    // 获取楼层列表
    fetchFloors() {
      request.get('/chatRoom/list').then(res => {
        if (res.code === '0') {
          this.floors = res.data || []
          if (this.floors.length > 0) {
            this.selectedFloor = this.floors[0].roomId
            this.fetchSeats()
          }
        } else {
          this.$message.error(res.msg || '获取楼层列表失败')
        }
      }).catch(err => {
        console.error('获取楼层列表失败', err)
        this.$message.error('获取楼层列表失败')
      })
    },

    // 获取座位列表
    fetchSeats() {
      if (!this.selectedFloor) return

      this.loading = true
      request.get(`/seats/status?roomId=${this.selectedFloor}`).then(res => {
        if (res.code === '0') {
          this.seats = res.data || []
        } else {
          this.$message.error(res.msg || '获取座位列表失败')
        }
      }).catch(err => {
        console.error('获取座位列表失败', err)
        this.$message.error('获取座位列表失败')
      }).finally(() => {
        this.loading = false
      })
    },

    // 根据座位状态获取样式类名
    getSeatClass(status) {
      switch (status) {
        case 0: return 'available'
        case 1: return 'reserved'
        case 2: return 'occupied'
        case 3: return 'disabled'
        default: return 'available'
      }
    },

    // 获取座位状态文本
    getSeatStatusText(status) {
      switch (status) {
        case 0: return '可用'
        case 1: return '已预约'
        case 2: return '使用中'
        case 3: return '不可用'
        default: return '未知状态'
      }
    },

    // 点击座位
    handleSeatClick(seat) {
      this.selectedSeat = seat
      this.seatForm = {
        status: seat.status,
        message: ''
      }
      this.seatDialogVisible = true
    },

    // 更新单个座位状态
    updateSeat() {
      if (!this.selectedSeat) return

      const payload = {
        seatId: this.selectedSeat.id,
        status: this.seatForm.status
      }

      request.put('/seats/status', payload).then(res => {
        if (res.code === '0') {
          // 如果有通知消息，则发送通知
          if (this.seatForm.message) {
            this.notifySeatUser(this.selectedSeat.id, this.seatForm.message)
          }

          this.$message.success('座位状态更新成功')
          this.seatDialogVisible = false
          // 更新本地数据
          this.selectedSeat.status = this.seatForm.status
        } else {
          this.$message.error(res.msg || '座位状态更新失败')
        }
      }).catch(err => {
        console.error('座位状态更新失败', err)
        this.$message.error('座位状态更新失败')
      })
    },

    // 打开批量更新对话框
    batchUpdateSeats() {
      this.batchForm = {
        status: 0,
        message: ''
      }
      this.batchDialogVisible = true
    },

    // 批量更新座位状态
    submitBatchUpdate() {
      if (!this.selectedFloor) return

      request.put(`/seats/floor/${this.selectedFloor}/status?status=${this.batchForm.status}`).then(res => {
        if (res.code === '0') {
          // 如果有通知消息，则发送通知给所有座位用户
          if (this.batchForm.message) {
            this.notifyFloorUsers(this.selectedFloor, this.batchForm.message)
          }

          this.$message.success('批量更新座位状态成功')
          this.batchDialogVisible = false
          this.fetchSeats() // 重新获取座位数据
        } else {
          this.$message.error(res.msg || '批量更新座位状态失败')
        }
      }).catch(err => {
        console.error('批量更新座位状态失败', err)
        this.$message.error('批量更新座位状态失败')
      })
    },

    // 发送座位通知
    notifySeatUser(seatId, message) {
      request.post('/seats/notify', {
        seatId,
        message
      }).then(res => {
        if (res.code === '0') {
          this.$message.success('通知已发送')
        }
      }).catch(err => {
        console.error('发送通知失败', err)
      })
    },

    // 发送楼层通知
    notifyFloorUsers(floorId, message) {
      request.post('/seats/notify/floor', {
        floorId,
        message
      }).then(res => {
        if (res.code === '0') {
          this.$message.success('通知已发送给所有用户')
        }
      }).catch(err => {
        console.error('发送通知失败', err)
      })
    }
  }
}
</script>

<style scoped>
.seat-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.legend-container {
  display: flex;
  margin-bottom: 20px;
  background-color: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
}

.legend-item {
  display: flex;
  align-items: center;
  margin-right: 20px;
}

.legend-color {
  width: 20px;
  height: 20px;
  margin-right: 5px;
  border-radius: 4px;
}

.legend-color.available {
  background-color: #67C23A;
}

.legend-color.reserved {
  background-color: #E6A23C;
}

.legend-color.occupied {
  background-color: #F56C6C;
}

.legend-color.disabled {
  background-color: #909399;
}

.seats-container {
  min-height: 400px;
}

.no-floor-selected,
.no-seats {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
  color: #909399;
  font-size: 16px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.seat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 80px);
  grid-gap: 10px;
  padding: 20px;
  justify-content: center;
}

.seat {
  width: 80px;
  height: 80px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  color: white;
  transition: all 0.3s;
}

.seat:hover {
  transform: scale(1.05);
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.seat.available {
  background-color: #67C23A;
}

.seat.reserved {
  background-color: #E6A23C;
}

.seat.occupied {
  background-color: #F56C6C;
}

.seat.disabled {
  background-color: #909399;
}

.seat-detail {
  margin-bottom: 20px;
}
</style>