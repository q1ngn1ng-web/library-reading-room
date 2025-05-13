<template>
  <div class="chat-room-list">
    <div v-if="serverOffline" class="server-offline-alert">
      <el-alert
        title="服务器连接失败"
        type="error"
        description="无法连接到后端服务器，请确认后端服务已启动并且在正确的端口上运行。"
        show-icon
        :closable="false"
      >
        <el-button size="small" type="primary" @click="loadChatRooms" style="margin-top: 10px;">
          重试连接
        </el-button>
      </el-alert>
    </div>

    <!-- 添加预约提示信息 -->
    <div v-if="!canAccessChat && !isAdmin" class="reservation-alert">
      <el-alert
        title="您需要有预约或已签到的座位才能使用聊天室"
        type="warning"
        description="请前往座位预约页面预约一个座位，或完成已有预约的签到。"
        show-icon
        :closable="false"
      >
        <el-button size="small" type="primary" @click="goToSeatReservation" style="margin-top: 10px;">
          前往预约座位
        </el-button>
      </el-alert>
    </div>

    <div class="room-container" v-loading="loading">
      <div v-if="!serverOffline && chatRooms.length === 0" class="empty-state">
        <i class="el-icon-chat-dot-round" style="font-size: 48px; color: #909399;"></i>
        <p>暂无聊天室</p>
        <el-button v-if="isAdmin" type="primary" @click="showCreateRoomDialog">创建聊天室</el-button>
      </div>

      <el-row :gutter="20" v-else>
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="room in chatRooms" :key="room.id" class="room-col">
  <el-card class="room-card" :body-style="{ padding: '0px' }">
    <div class="room-header">
      <h3 class="room-title">{{ room.floor }}楼 | {{ room.category }}</h3>
      <div class="room-status">

        <el-tag :type="isRoomAvailable(room) ? 'success' : 'danger'" size="mini">
          {{ isRoomAvailable(room) ? '开放' : '关闭' }}
        </el-tag>
      </div>
    </div>
    <div class="room-info">
      <div class="room-users">
        <i class="el-icon-user"></i> {{ room.currentUser }}/{{ room.maxUser }}
      </div>
      <el-button
        type="primary"
        size="small"
        :disabled="!isRoomAvailable(room) || room.currentUser >= room.maxUser || serverOffline || (!canAccessChat && !isAdmin)"
        @click="enterRoom(room.id)"
      >
        进入聊天室
      </el-button>
      <div style="font-size: 10px; color: #999; margin-top: 5px;" v-if="!isRoomAvailable(room) || room.currentUser >= room.maxUser || serverOffline || (!canAccessChat && !isAdmin)">
        <span v-if="!isRoomAvailable(room)">聊天室已关闭</span>
        <span v-else-if="room.currentUser >= room.maxUser">聊天室已满</span>
        <span v-else-if="serverOffline">服务器离线</span>
        <span v-else-if="!canAccessChat && !isAdmin">需要有预约或已签到的座位</span>
      </div>
    </div>
  </el-card>
</el-col>
      </el-row>
    </div>

    <!-- 可拖动的聊天室创建按钮 (仅管理员可见) -->
    <div
      v-if="isAdmin && !serverOffline"
      class="floating-button"
      :style="{ left: floatingButtonPosition.x + 'px', top: floatingButtonPosition.y + 'px' }"
      @mousedown="startDrag"
    >
      <el-button type="primary" circle @click="showCreateRoomDialog">
        <i class="el-icon-plus"></i>
      </el-button>
    </div>

    <!-- 创建聊天室对话框 -->
    <el-dialog title="创建聊天室" :visible.sync="dialogVisible" width="30%">
      <el-form :model="roomForm" label-width="80px">
        <el-form-item label="楼层号">
          <el-input-number v-model="roomForm.floor" :min="1" controls-position="right"></el-input-number>
        </el-form-item>
        <el-form-item label="学科类型">
          <el-input v-model="roomForm.category" placeholder="请输入学科类型"></el-input>
        </el-form-item>
        <el-form-item label="最大人数">
          <el-input-number v-model="roomForm.maxUser" :min="5" :max="100" controls-position="right"></el-input-number>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="roomForm.isAvailable" active-text="开放" inactive-text="关闭"></el-switch>
        </el-form-item>
        <el-form-item label="允许发言">
          <el-switch v-model="roomForm.is_chat" active-text="允许" inactive-text="禁言"></el-switch>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="createRoom">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { mapActions, mapGetters } from 'vuex'

export default {
  name: 'ChatRoomList',
  data() {
    return {
      dialogVisible: false,
      roomForm: {
        floor: 1,
        category: '',
        maxUser: 50,
        is_available: true,
        is_chat: true
      },
      floatingButtonPosition: {
        x: window.innerWidth - 100,
        y: window.innerHeight - 100
      },
      isDragging: false,
      dragOffset: { x: 0, y: 0 },
      loading: false,
      serverOffline: false,
    }
  },
  computed: {
    ...mapGetters('chat', ['getChatRooms']),
    ...mapGetters('seat', ['canAccessChat']),
    chatRooms() {
      return this.getChatRooms
    },
    isAdmin() {
      return !!localStorage.getItem('admin-token')
    }
  },
  methods: {
    ...mapActions('chat', ['fetchChatRooms', 'createChatRoom']),

    // 跳转到座位预约页面
    goToSeatReservation() {
      this.$router.push('/seat-reservation')
    },

    async loadChatRooms() {
      this.loading = true
      this.serverOffline = false

      try {
        await this.fetchChatRooms()
        // 如果成功获取数据，服务器肯定在线
        this.serverOffline = false
      } catch (error) {
        console.error('加载聊天室失败:', error)
        // 检查是否是网络错误
        if (error.message === 'Network Error') {
          this.serverOffline = true
        }
      } finally {
        this.loading = false
      }
    },

    enterRoom(roomId) {
      if (this.serverOffline) {
        this.$message.error('服务器离线，无法进入聊天室')
        return
      }

      // 非管理员需要预约或已签到才能进入聊天室
      if (!this.isAdmin && !this.canAccessChat) {
        this.$message.warning('您需要有预约或已签到的座位才能进入聊天室')
        this.$confirm('您需要有预约或已签到的座位才能进入聊天室，是否前往预约?', '提示', {
          confirmButtonText: '去预约',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.goToSeatReservation()
        }).catch(() => {})
        return
      }

      this.$router.push(`/chat-room/${roomId}`)
    },

    showCreateRoomDialog() {
      if (!this.isAdmin) {
        this.$message.warning('只有管理员才能创建聊天室')
        return
      }
      this.dialogVisible = true
    },

    async createRoom() {
      try {
        const res = await this.createChatRoom(this.roomForm)
        if (res.code === '0') {
          this.$message.success('创建聊天室成功')
          this.dialogVisible = false
          this.loadChatRooms()
          // 重置表单
          this.roomForm = {
            floor: 1,
            category: '',
            maxUser: 50,
            is_available: true,
            is_chat: true
          }
        } else {
          this.$message.error(res.msg || '创建聊天室失败')
        }
      } catch (error) {
        this.$message.error('创建聊天室失败')
      }
    },

    // 可拖动按钮相关方法
    startDrag(event) {
      if (this.isAdmin) {
        this.isDragging = true
        const button = event.target.closest('.floating-button')
        const rect = button.getBoundingClientRect()
        this.dragOffset.x = event.clientX - rect.left
        this.dragOffset.y = event.clientY - rect.top
        document.addEventListener('mousemove', this.onDrag)
        document.addEventListener('mouseup', this.stopDrag)
      }
    },

    onDrag(event) {
      if (this.isDragging) {
        this.floatingButtonPosition.x = event.clientX - this.dragOffset.x
        this.floatingButtonPosition.y = event.clientY - this.dragOffset.y
      }
    },

    stopDrag() {
      this.isDragging = false
      document.removeEventListener('mousemove', this.onDrag)
      document.removeEventListener('mouseup', this.stopDrag)
    },

    // 判断聊天室是否开放
    isRoomAvailable(room) {
      // 首先检查对应的camelCase字段
      const availabilityValue = room.isAvailable !== undefined ? room.isAvailable : room.is_available;



      // 处理不同类型的值，确保正确解释
      if (typeof availabilityValue === 'string') {
        return availabilityValue === 'true' || availabilityValue === '1';
      } else if (typeof availabilityValue === 'number') {
        return availabilityValue === 1;
      } else {
        return !!availabilityValue;
      }
    }
  },
  created() {
    this.loadChatRooms()
  },
  beforeDestroy() {
    document.removeEventListener('mousemove', this.onDrag)
    document.removeEventListener('mouseup', this.stopDrag)
  }
}
</script>

<style scoped>
.chat-room-list {
  padding: 20px;
  position: relative;
  min-height: calc(100vh - 120px);
}

.server-offline-alert {
  margin-bottom: 20px;
}

.room-container {
  max-width: 1200px;
  margin: 0 auto;
}

.empty-state {
  text-align: center;
  padding: 50px 0;
  color: #909399;
}

.empty-state p {
  margin: 10px 0 20px;
  font-size: 16px;
}

.room-col {
  margin-bottom: 20px;
}

.room-card {
  height: 150px;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
}

.room-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.room-header {
  padding: 15px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.room-title {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.room-info {
  padding: 15px;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.room-users {
  font-size: 14px;
  color: #606266;
  margin-bottom: 15px;
}

.floating-button {
  position: fixed;
  z-index: 999;
  cursor: move;
}
</style>