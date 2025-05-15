<template>
  <div class="seat-management">
    <div class="page-header">
      <h2>座位管理</h2>
      <div>
        <el-select
          v-model="selectedFloor"
          placeholder="请选择楼层"
          @change="handleFloorChange">
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

    <!-- 搜索栏 -->
    <el-form :inline="true" :model="searchForm" class="search-form">
      <el-form-item label="座位编号">
        <el-input v-model="searchForm.seatNumber" placeholder="请输入座位编号" clearable></el-input>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
          <el-option label="可用" :value="0"></el-option>
          <el-option label="已预约" :value="1"></el-option>
          <el-option label="使用中" :value="2"></el-option>
          <el-option label="维修中" :value="3"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

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
        <span>维修中</span>
      </div>
    </div>

    <!-- 座位列表展示 -->
    <div class="seats-container">
      <el-table
        v-loading="loading"
        :data="filteredSeats"
        border
        style="width: 100%">
        <el-table-column
          prop="id"
          label="ID"
          width="80">
        </el-table-column>
        <el-table-column
          prop="seatNumber"
          label="座位编号"
          width="120">
        </el-table-column>
        <el-table-column
          label="状态"
          width="120">
          <template slot-scope="scope">
            <el-tag :type="getSeatTagType(scope.row.status)">
              {{ getSeatStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="roomId"
          label="所属楼层"
          width="120">
          <template slot-scope="scope">
            {{ getFloorName(scope.row.roomId) }}
          </template>
        </el-table-column>
        <el-table-column
          label="最近预约信息"
          width="200">
          <template slot-scope="scope">
            <span v-if="scope.row.lastReservation">
              {{ scope.row.lastReservation }}
            </span>
            <span v-else>暂无预约记录</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="updateTime"
          label="更新时间"
          width="180">
          <template slot-scope="scope">
            <span>{{ scope.row.updateTime || '无记录' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="操作">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="primary"
              @click="handleSeatClick(scope.row)">编辑</el-button>
            <el-button
              size="mini"
              type="success"
              @click="sendSeatNotification(scope.row)">发送通知</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页控件 -->
      <div class="pagination">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="totalSeats">
        </el-pagination>
      </div>
    </div>

    <!-- 座位详情对话框 -->
    <el-dialog
      title="座位详情"
      :visible.sync="seatDialogVisible"
      width="400px">
      <div v-if="selectedSeat" class="seat-detail">
        <p><strong>座位编号：</strong>{{ selectedSeat.seatNumber }}</p>
        <p><strong>当前状态：</strong>{{ getSeatStatusText(selectedSeat.status) }}</p>
        <p><strong>所属楼层：</strong>{{ getFloorName(selectedSeat.roomId) }}</p>

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

    <!-- 发送通知对话框 -->
    <el-dialog
      title="发送座位通知"
      :visible.sync="notifyDialogVisible"
      width="400px">
      <div v-if="selectedSeat" class="seat-detail">
        <p><strong>座位编号：</strong>{{ selectedSeat.seatNumber }}</p>
        <p><strong>当前状态：</strong>{{ getSeatStatusText(selectedSeat.status) }}</p>

        <el-form :model="notifyForm" label-width="80px">
          <el-form-item label="通知消息">
            <el-input
              type="textarea"
              :rows="3"
              v-model="notifyForm.message"
              placeholder="请输入要发送的通知内容"></el-input>
          </el-form-item>
        </el-form>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="notifyDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="sendNotification">发 送</el-button>
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

      // 搜索表单
      searchForm: {
        seatNumber: '',
        status: ''
      },

      // 座位数据
      allSeats: [], // 所有座位
      loading: false,

      // 分页相关
      currentPage: 1,
      pageSize: 10,
      totalSeats: 0,

      // 座位详情
      seatDialogVisible: false,
      selectedSeat: null,
      seatForm: {
        status: 0,
        message: ''
      },

      // 发送通知
      notifyDialogVisible: false,
      notifyForm: {
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
  computed: {
    // 过滤后的座位数据
    filteredSeats() {
      let result = [...this.allSeats]

      // 按座位编号筛选
      if (this.searchForm.seatNumber) {
        result = result.filter(seat =>
          String(seat.seatNumber || seat.number).includes(this.searchForm.seatNumber)
        )
      }

      // 按状态筛选
      if (this.searchForm.status !== '' && this.searchForm.status !== null) {
        result = result.filter(seat => seat.status === this.searchForm.status)
      }

      // 更新总数
      this.totalSeats = result.length

      // 分页
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return result.slice(start, end)
    }
  },
  created() {
    this.fetchFloors()
  },
  methods: {
    // 获取楼层列表
    fetchFloors() {
      this.loading = true
      request.get('/chatRoom/list')
        .then(res => {
          this.loading = false
          if (res.code === '0') {
            this.floors = res.data || []
            console.log('获取到楼层列表:', this.floors)

            if (this.floors.length > 0) {
              this.selectedFloor = this.floors[0].roomId
              this.fetchSeats()
            } else {
              this.$message.warning('未找到可用楼层')
            }
          } else {
            this.$message.error(res.msg || '获取楼层列表失败')
          }
        })
        .catch(err => {
          this.loading = false
          console.error('获取楼层列表失败', err)
          this.$message.error('获取楼层列表失败，请检查网络连接')
        })
    },

    // 楼层切换
    handleFloorChange(value) {
      console.log('切换楼层:', value)
      this.currentPage = 1 // 重置页码
      this.searchForm = { // 重置搜索条件
        seatNumber: '',
        status: ''
      }
      this.fetchSeats()
    },

    // 根据楼层ID获取楼层名称
    getFloorName(roomId) {
      const floor = this.floors.find(f => f.roomId === roomId)
      return floor ? floor.floor : `楼层 ${roomId}`
    },

    // 获取座位列表
    fetchSeats() {
      if (!this.selectedFloor) {
        this.$message.warning('请先选择楼层')
        return
      }

      this.loading = true
      console.log('正在获取座位列表，楼层ID:', this.selectedFloor)

      // 尝试直接从seats/list接口获取座位数据
      request.get(`/seats/list?roomId=${this.selectedFloor}`)
        .then(res => {
          this.loading = false
          console.log('座位API响应:', res)

          if (res.code === '0') {
            // 数据标准化处理
            this.allSeats = (res.data || []).map(seat => ({
              id: seat.id,
              roomId: seat.roomId || this.selectedFloor,
              seatNumber: seat.seatNumber || seat.number || `座位${seat.id}`,
              status: typeof seat.status === 'number' ? seat.status : 0,
              updateTime: seat.updateTime || null,
              lastReservation: seat.lastReservation || null
            }))

            console.log('获取到座位列表:', this.allSeats)
            this.totalSeats = this.allSeats.length

            if (this.allSeats.length === 0) {
              // 如果没有数据，尝试创建示例座位数据
              this.createDemoSeats()
            }
          } else {
            // 如果API调用失败，尝试创建示例座位数据
            console.error('获取座位列表失败, 尝试获取备用API:', res.msg)
            this.fetchSeatsAlternative()
          }
        })
        .catch(err => {
          this.loading = false
          console.error('获取座位列表失败，尝试备用API', err)
          this.fetchSeatsAlternative()
        })
    },

    // 备用方法：从status接口获取座位数据
    fetchSeatsAlternative() {
      this.loading = true

      // 尝试原来的API端点
      request.get(`/seats/status?roomId=${this.selectedFloor}`)
        .then(res => {
          this.loading = false
          console.log('备用座位API响应:', res)

          if (res.code === '0' && res.data && res.data.length > 0) {
            // 数据标准化处理
            this.allSeats = (res.data || []).map(seat => ({
              id: seat.id,
              roomId: seat.roomId || this.selectedFloor,
              seatNumber: seat.seatNumber || seat.number || `座位${seat.id}`,
              status: typeof seat.status === 'number' ? seat.status : 0,
              updateTime: seat.updateTime || null,
              lastReservation: seat.lastReservation || null
            }))

            console.log('备用API获取到座位列表:', this.allSeats)
            this.totalSeats = this.allSeats.length
          } else {
            console.error('备用API也未能获取座位数据，创建模拟数据')
            this.createDemoSeats()
          }
        })
        .catch(err => {
          this.loading = false
          console.error('备用API也失败，创建模拟数据', err)
          this.createDemoSeats()
        })
    },

    // 创建模拟座位数据用于展示
    createDemoSeats() {
      console.log('创建模拟座位数据')

      // 获取当前选中的楼层信息
      const currentFloor = this.floors.find(f => f.roomId === this.selectedFloor)
      const floorName = currentFloor ? currentFloor.floor : this.selectedFloor

      // 创建20个模拟座位
      const demoSeats = []
      for (let i = 1; i <= 20; i++) {
        demoSeats.push({
          id: `demo-${this.selectedFloor}-${i}`,
          roomId: this.selectedFloor,
          seatNumber: `${floorName}-${i.toString().padStart(2, '0')}`,
          status: Math.floor(Math.random() * 4), // 随机状态
          updateTime: new Date().toLocaleString(),
          lastReservation: null
        })
      }

      this.allSeats = demoSeats
      this.totalSeats = demoSeats.length

      this.$message.warning('未能从服务器获取座位数据，显示模拟数据')
    },

    // 搜索
    search() {
      this.currentPage = 1 // 重置页码
      // filteredSeats计算属性会自动处理过滤
    },

    // 重置搜索
    resetSearch() {
      this.searchForm = {
        seatNumber: '',
        status: ''
      }
      this.currentPage = 1
    },

    // 分页大小改变
    handleSizeChange(val) {
      this.pageSize = val
    },

    // 页码改变
    handleCurrentChange(val) {
      this.currentPage = val
    },

    // 获取状态标签类型
    getSeatTagType(status) {
      switch (Number(status)) {
        case 0: return 'success' // 可用
        case 1: return 'warning' // 已预约
        case 2: return 'danger'  // 使用中
        case 3: return 'info'    // 维修中
        default: return 'success'
      }
    },

    // 获取座位状态文本
    getSeatStatusText(status) {
      switch (Number(status)) {
        case 0: return '可用'
        case 1: return '已预约'
        case 2: return '使用中'
        case 3: return '维修中'
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

    // 打开发送通知对话框
    sendSeatNotification(seat) {
      this.selectedSeat = seat
      this.notifyForm = {
        message: ''
      }
      this.notifyDialogVisible = true
    },

    // 发送通知
    sendNotification() {
      if (!this.selectedSeat || !this.notifyForm.message) {
        this.$message.warning('请输入通知内容')
        return
      }

      // 检查是否为模拟数据
      if (String(this.selectedSeat.id).startsWith('demo-')) {
        this.$message.info(`模拟通知：已发送消息到座位 ${this.selectedSeat.seatNumber}: ${this.notifyForm.message}`)
        this.notifyDialogVisible = false
        return
      }

      this.notifySeatUser(this.selectedSeat.id, this.notifyForm.message)
      this.notifyDialogVisible = false
    },

    // 更新单个座位状态
    updateSeat() {
      if (!this.selectedSeat) return

      const payload = {
        seatId: this.selectedSeat.id,
        status: this.seatForm.status
      }

      console.log('更新座位状态:', payload)
      this.loading = true

      // 检查是否为模拟数据
      if (String(this.selectedSeat.id).startsWith('demo-')) {
        // 处理模拟数据的更新
        setTimeout(() => {
          this.loading = false

          // 更新本地数据
          const index = this.allSeats.findIndex(s => s.id === this.selectedSeat.id)
          if (index !== -1) {
            this.allSeats[index].status = this.seatForm.status
            this.allSeats[index].updateTime = new Date().toLocaleString()
          }

          this.$message.success('座位状态更新成功（模拟）')
          this.seatDialogVisible = false

          // 如果有通知消息，模拟发送通知
          if (this.seatForm.message) {
            this.$message.info(`模拟通知：已向座位 ${this.selectedSeat.seatNumber} 的用户发送消息: ${this.seatForm.message}`)
          }
        }, 1000)
        return
      }

      // 实际API调用
      request.put('/seats/status', payload)
        .then(res => {
          this.loading = false
          if (res.code === '0') {
            // 如果有通知消息，则发送通知
            if (this.seatForm.message) {
              this.notifySeatUser(this.selectedSeat.id, this.seatForm.message)
            }

            this.$message.success('座位状态更新成功')
            this.seatDialogVisible = false

            // 更新本地数据
            const index = this.allSeats.findIndex(s => s.id === this.selectedSeat.id)
            if (index !== -1) {
              this.allSeats[index].status = this.seatForm.status
              this.allSeats[index].updateTime = new Date().toLocaleString()
            }
          } else {
            this.$message.error(res.msg || '座位状态更新失败')
          }
        })
        .catch(err => {
          this.loading = false
          console.error('座位状态更新失败', err)
          this.$message.error('座位状态更新失败，请检查网络连接')
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
      if (!this.selectedFloor) {
        this.$message.warning('请先选择楼层')
        return
      }

      console.log('批量更新座位状态:', this.selectedFloor, this.batchForm.status)
      this.loading = true

      // 检查是否为模拟数据
      if (this.allSeats.length > 0 && String(this.allSeats[0].id).startsWith('demo-')) {
        // 处理模拟数据的批量更新
        setTimeout(() => {
          this.loading = false

          // 更新所有座位状态
          this.allSeats.forEach(seat => {
            seat.status = this.batchForm.status
            seat.updateTime = new Date().toLocaleString()
          })

          this.$message.success('批量更新座位状态成功（模拟）')
          this.batchDialogVisible = false

          // 如果有通知消息，模拟发送通知
          if (this.batchForm.message) {
            this.$message.info(`模拟通知：已向 ${this.getFloorName(this.selectedFloor)} 的所有座位用户发送消息: ${this.batchForm.message}`)
          }
        }, 1000)
        return
      }

      // 实际API调用
      request.put(`/seats/floor/${this.selectedFloor}/status?status=${this.batchForm.status}`)
        .then(res => {
          this.loading = false
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
        })
        .catch(err => {
          this.loading = false
          console.error('批量更新座位状态失败', err)
          this.$message.error('批量更新座位状态失败，请检查网络连接')
        })
    },

    // 发送座位通知
    notifySeatUser(seatId, message) {
      console.log('发送座位通知:', seatId, message)

      // 检查是否为模拟数据
      if (String(seatId).startsWith('demo-')) {
        this.$message.info(`模拟通知：已发送消息到座位 ${seatId}: ${message}`)
        return
      }

      request.post('/seats/notify', {
        seatId,
        message
      })
        .then(res => {
          if (res.code === '0') {
            this.$message.success('通知已成功发送')
          } else {
            this.$message.error(res.msg || '发送通知失败')
          }
        })
        .catch(err => {
          console.error('发送通知失败', err)
          this.$message.error('发送通知失败，请检查网络连接')
        })
    },

    // 发送楼层通知
    notifyFloorUsers(floorId, message) {
      console.log('发送楼层通知:', floorId, message)

      // 检查是否为模拟数据
      if (this.allSeats.length > 0 && String(this.allSeats[0].id).startsWith('demo-')) {
        this.$message.info(`模拟通知：已发送消息到楼层 ${this.getFloorName(floorId)} 的所有用户: ${message}`)
        return
      }

      request.post('/seats/notify/floor', {
        floorId,
        message
      })
        .then(res => {
          if (res.code === '0') {
            this.$message.success('通知已成功发送给所有用户')
          } else {
            this.$message.error(res.msg || '发送通知失败')
          }
        })
        .catch(err => {
          console.error('发送通知失败', err)
          this.$message.error('发送通知失败，请检查网络连接')
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

.search-form {
  margin-bottom: 20px;
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
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

.pagination {
  margin-top: 20px;
  text-align: right;
}

.seat-detail {
  margin-bottom: 20px;
}

.no-data {
  text-align: center;
  color: #909399;
  padding: 40px 0;
  font-size: 14px;
}
</style>