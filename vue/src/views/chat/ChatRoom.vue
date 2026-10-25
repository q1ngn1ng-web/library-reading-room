<template>
  <div class="chat-room">
    <el-button class="back-button" icon="el-icon-back" size="small" @click="goBack">
      返回{{ isAdmin ? '管理后台' : '聊天室列表' }}
    </el-button>

    <el-alert
      v-if="!canAccessChat && !isAdmin"
      title="您需要有预约或已签到的座位才能使用聊天室"
      type="warning"
      description="请前往座位预约页面预约座位，或完成已有预约的签到。"
      show-icon
      :closable="false"
      style="margin-bottom: 15px;"
    >
      <el-button size="small" type="primary" @click="goToSeatReservation">
        前往预约
      </el-button>
    </el-alert>

    <el-alert
      v-if="currentRoom && !currentRoom.isChat"
      title="当前聊天室已禁言"
      type="warning"
      description="管理员已开启禁言模式，暂时无法发送消息。"
      show-icon
      :closable="false"
      style="margin-bottom: 15px;"
    >
    </el-alert>

    <el-alert
      v-if="connectionError"
      title="WebSocket连接失败"
      type="error"
      description="无法建立WebSocket连接，请尝试刷新页面或检查网络连接。"
      show-icon
      :closable="false"
      style="margin-bottom: 15px;"
    >
      <el-button size="small" type="primary" @click="reconnectWebSocket">
        重新连接
      </el-button>
    </el-alert>

    <el-card class="room-info-card">
      <div slot="header" class="room-header">
        <h3 v-if="currentRoom">{{ currentRoom.floor }}楼 | {{ currentRoom.category }}</h3>
        <el-tag :type="currentRoom && currentRoom.isAvailable ? 'success' : 'danger'" size="small">
          {{ currentRoom && currentRoom.isAvailable ? '开放' : '关闭' }}
        </el-tag>
      </div>
      <div class="user-count">
        <i class="el-icon-user"></i> 在线人数: {{ onlineUsers.length }}/{{ currentRoom ? currentRoom.maxUser : 0 }}
      </div>
    </el-card>

    <el-container class="chat-container">
      <el-aside width="200px" class="user-list-aside">
        <div class="user-list-header">在线用户</div>
        <div class="user-list">
          <div v-for="user in onlineUsers" :key="user.username" class="user-item">
            <el-avatar :size="32" :src="user.avatar || defaultAvatar"></el-avatar>
            <span class="user-name">{{ formatUserName(user) }}</span>
          </div>
          <div v-if="onlineUsers.length === 0" class="no-users">
            暂无在线用户
          </div>
        </div>
      </el-aside>

      <el-main class="chat-main">
        <div class="message-list" ref="messageList">
          <div v-for="(msg, index) in messages" :key="index" class="message-item" :class="{'my-message': isMyMessage(msg)}">
            <div class="message-sender">
              <el-avatar :size="36" :src="msg.avatar || defaultAvatar"></el-avatar>
              <div class="message-info">
                <div class="sender-name">{{ msg.isAnonymous ? '匿名用户' : (msg.displayName || msg.nickname || msg.name) }}</div>
                <div class="message-time">{{ formatTime(msg.time) }}</div>
              </div>
            </div>
            <div class="message-content">{{ msg.content }}</div>
            <div v-if="!isMyMessage(msg)" class="message-actions">
              <el-button type="text" size="mini" icon="el-icon-warning-outline"
                         @click="openReport(msg)">举报</el-button>
            </div>
          </div>
          <div v-if="messages.length === 0" class="no-messages">
            暂无消息，发送第一条消息开始聊天吧！
          </div>
        </div>

        <div class="message-input-container">
          <div class="message-tools">
            <el-checkbox v-model="isAnonymous">匿名发送</el-checkbox>
          </div>
          <div class="input-area">
            <el-input
              v-model="messageContent"
              type="textarea"
              :rows="3"
              placeholder="请输入消息"
              :disabled="!currentRoom || !currentRoom.isAvailable || !currentRoom.isChat"
              @keyup.ctrl.enter.native="sendMessage"
            ></el-input>
            <el-button
              type="primary"
              :disabled="!messageContent.trim() || !currentRoom || !currentRoom.isAvailable || !currentRoom.isChat"
              @click="sendMessage"
            >
              发送
            </el-button>
          </div>
          <div class="send-tip">提示: 按Ctrl+Enter快速发送</div>
        </div>

        <!-- 违规举报弹窗 -->
        <el-dialog title="举报违规行为" :visible.sync="reportDialog" width="420px">
          <el-form label-width="80px">
            <el-form-item label="被举报人">
              <span>{{ reportTarget.name || '匿名用户' }}（{{ reportTarget.content ? reportTarget.content.slice(0, 20) : '' }}…）</span>
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="reportForm.type" style="width: 100%">
                <el-option label="喧哗交谈" value="TALK" />
                <el-option label="违规饮食" value="EAT" />
                <el-option label="占座不用" value="SEAT_OCCUPY" />
                <el-option label="其他" value="OTHER" />
              </el-select>
            </el-form-item>
            <el-form-item label="补充描述">
              <el-input v-model="reportForm.content" type="textarea" :rows="2"
                        placeholder="选填：补充具体情况（如位置、持续时间）"></el-input>
            </el-form-item>
            <el-form-item label="匿名举报">
              <el-switch v-model="reportForm.isAnonymous"></el-switch>
            </el-form-item>
          </el-form>
          <div slot="footer">
            <el-button @click="reportDialog = false">取消</el-button>
            <el-button type="primary" :loading="reportSubmitting" @click="submitReport">提交举报</el-button>
          </div>
        </el-dialog>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { mapActions, mapGetters } from 'vuex'
import WebSocketService from '@/utils/websocket'
import { submitFeedback } from '@/api/feedback'

export default {
  name: 'ChatRoom',
  data() {
    return {
      roomId: null,
      wsService: null,
      messageContent: '',
      isAnonymous: false,
      reportDialog: false,
      reportSubmitting: false,
      reportTarget: {},
      reportForm: { type: 'TALK', content: '', isAnonymous: false },
      defaultAvatar: require('@/assets/default-avatar.png'), // 请确保路径正确或使用在线链接
      user: null,
      admin:null,
      isConnected: false,
      showDebugInfo: true,
      connectionError: false, // 添加连接错误状态
      reconnectTimer: null, // 用于管理重连定时器
    }
  },
  computed: {
    ...mapGetters('chat', [
      'getCurrentRoom',
      'getMessages',
      'getOnlineUsers'
    ]),
    ...mapGetters('seat', ['canAccessChat', 'getActiveReservation']), // 添加活跃预约getter
    currentRoom() {
      return this.getCurrentRoom
    },
    messages() {
      return this.getMessages
    },
    onlineUsers() {
      return this.getOnlineUsers
    },
    isAdmin() {
      return !!localStorage.getItem('admin-token')
    },
    // 添加一个计算属性来判断是否是管理员视图
    isAdminView() {
      return this.$route.path.startsWith('/admin/')
    }
  },
  methods: {
    openReport(msg) {
      this.reportTarget = { userId: msg.userId, name: msg.displayName || msg.nickname || msg.name, content: msg.content, messageId: msg.id }
      this.reportForm = { type: 'TALK', content: '', isAnonymous: false }
      this.reportDialog = true
    },
    submitReport() {
      this.reportSubmitting = true
      submitFeedback({
        roomId: this.roomId ? Number(this.roomId) : null,
        targetUserId: this.reportTarget.userId,
        targetMessageId: this.reportTarget.messageId || null,
        type: this.reportForm.type,
        content: this.reportForm.content,
        isAnonymous: this.reportForm.isAnonymous
      }).then(res => {
        if (res.code === '0') {
          this.$message.success(res.data || '举报已提交')
          this.reportDialog = false
        } else {
          this.$message.error(res.msg)
        }
      }).finally(() => { this.reportSubmitting = false })
    },
    ...mapActions('chat', [
      'fetchChatRoom',
      'receiveMessage',
      'updateOnlineUsers',
      'clearCurrentRoomData',
      'fetchChatHistory'
    ]),

    goBack() {
      // 返回上一页
      this.$router.go(-1);
    },

    handleWebSocketError(errorMsg) {
      this.connectionError = true;
      this.$message.error(errorMsg || 'WebSocket连接失败，请刷新页面重试');
    },
    sendMessage() {
      if (this.messageContent.trim() && this.wsService && this.currentRoom &&
          this.currentRoom.isAvailable && this.currentRoom.isChat) {
        const success = this.wsService.sendMessage(this.messageContent, this.isAnonymous)
        if (success) {
          this.messageContent = ''
        } else {
          this.$message.error('消息发送失败，请检查网络连接')
        }
      }
    },
    handleMessage(message) {
      this.receiveMessage(message)
      this.$nextTick(() => {
        this.scrollToBottom()
      })
    },
    handleUserList(users) {
      // 处理用户列表，确保每个用户都有正确的显示名称
      const processedUsers = users.map(user => {
        // 如果用户没有displayName，添加一个
        if (!user.displayName) {
          // 如果用户名已经包含座位信息(例如 "1层2号 用户名")，则直接使用
          if (user.username && user.username.match(/\d+层\d+号/)) {
            user.displayName = user.username;
          } else {
            // 否则使用昵称或用户名
            user.displayName = user.nickname || user.username;
          }
        }
        return user;
      });

      console.log('处理后的用户列表:', processedUsers);
      this.updateOnlineUsers(processedUsers)
    },
    scrollToBottom() {
      const messageList = this.$refs.messageList
      if (messageList) {
        messageList.scrollTop = messageList.scrollHeight
      }
    },
    isMyMessage(message) {
      return message.userId === this.user?.id
    },
    formatTime(timestamp) {
      if (!timestamp) return ''
      const date = new Date(timestamp)
      const year = date.getFullYear()
      const month = (date.getMonth() + 1).toString().padStart(2, '0')
      const day = date.getDate().toString().padStart(2, '0')
      const hours = date.getHours().toString().padStart(2, '0')
      const minutes = date.getMinutes().toString().padStart(2, '0')
      const seconds = date.getSeconds().toString().padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    },
    reconnectWebSocket() {
      if (this.wsService) {
        this.wsService.disconnect();
        this.wsService = null;
      }
      this.connectionError = false;
      this.initWebSocket();
    },
    initWebSocket() {
      if (this.wsService) {
        this.wsService.disconnect();
      }

      // 根据当前路由判断是以管理员还是用户身份进入聊天室
      const isAdminRoute = this.$route.path.startsWith('/admin');
      const userInfo = isAdminRoute
        ? JSON.parse(localStorage.getItem('admin') || '{}')
        : JSON.parse(localStorage.getItem('user') || '{}');

      if (!userInfo || !userInfo.username) {
        this.$message.error('用户信息不完整，请重新登录');
        this.$router.push(isAdminRoute ? '/admin' : '/login');
        return;
      }

      this.user = userInfo;

      // 确保我们得到用户ID
      const userId = this.user.id;
      if (!userId) {
        console.warn('用户ID不存在，可能会导致数据库中user_id为0');
      }

      this.connectionError = false;
      this.isConnected = false;

      // 获取当前预约的座位信息
      const activeReservation = this.getActiveReservation;
      let displayName = this.user.nickname || this.user.username;

      // 如果是管理员，添加管理员标识
      if (this.isAdmin) {
        displayName = `[管理员] ${displayName}`;
      }
      // 如果有活跃预约且不是管理员，添加座位前缀
      else if (activeReservation && activeReservation.floorNumber && activeReservation.seatNumber) {
        displayName = `${activeReservation.floorNumber}层${activeReservation.seatNumber}号 ${displayName}`;
      }

      console.log('创建WebSocket连接，用户信息:', {
        用户ID: userId,
        昵称: this.user.nickname,
        用户名: this.user.username,
        显示名称: displayName,
        是否管理员: this.isAdmin,
        预约信息: !this.isAdmin && activeReservation ? {
          楼层: activeReservation.floorNumber,
          座位号: activeReservation.seatNumber
        } : '无需预约(管理员)'
      });

      this.wsService = new WebSocketService(
        this.roomId,
        userId,
        displayName,
        this.user.avatar || '',
        this.handleMessage,
        this.handleUserList
      );

      // 设置错误处理回调
      this.wsService.onError = (errorMsg) => {
        this.connectionError = true;
        this.isConnected = false;
        this.$message.error(errorMsg || 'WebSocket连接失败，请刷新页面重试');
      };

      // 设置连接成功回调
      this.wsService.onOpen = () => {
        this.connectionError = false;
        this.isConnected = true;
        // console.log('WebSocket连接已建立');
      };

      // 执行连接
      this.wsService.connect();
    },
    async loadRoomData() {
      try {
        await this.fetchChatRoom(this.roomId)
        if (!this.currentRoom) {
          this.$message.error('该聊天室不存在或已关闭，请重新选择一个聊天室进入')
          this.$router.push('/chat-rooms')
          return false
        }

        // 加载历史消息
        await this.fetchChatHistory(this.roomId)

        return true
      } catch (error) {
        this.$message.error('加载聊天室信息失败')
        return false
      }
    },

    // 检查用户是否可以进入聊天室
    checkAccess() {
      if (this.isAdmin) {
        return true;  // 管理员直接返回true，允许访问
      }

      if (!this.canAccessChat) {
        this.$message.warning('您需要有预约或已签到的座位才能使用聊天室')
        this.$router.push('/chat')
        return false
      }

      return true
    },
    // 处理用户显示名称
    formatUserName(user) {
      // 如果用户已经有格式化的显示名称，则直接使用
      if (user.displayName) {
        return user.displayName;
      }

      // 如果用户名已经包含座位信息(例如 "1层2号 用户名")，则直接使用
      if (user.username && user.username.match(/\d+层\d+号/)) {
        return user.username;
      }

      // 否则使用昵称或用户名
      return user.nickname || user.username;
    },
    // 添加跳转到座位预约页面的方法
    goToSeatReservation() {
      this.$router.push('/seat-reservation');
    },
  },
  watch: {
    // 监听currentRoom数据变化
    currentRoom: {
      handler(newRoom) {
        if (newRoom) {
          console.log('聊天室数据已更新:');
          // this.logRoomState();
        }
      },
      deep: true
    },
    // 监听在线用户列表变化
    onlineUsers: {
      handler(newUsers) {
        console.log('在线用户列表已更新:', newUsers);
      },
      deep: true
    }
  },
  async created() {
    this.roomId = parseInt(this.$route.params.id)
    if (!this.roomId) {
      // 根据视图类型决定重定向路径
      const redirectPath = this.isAdminView ? '/admin/chat-rooms' : '/chat-rooms';
      this.$router.push(redirectPath)
      return
    }

    // 检查是否有活跃预约（仅对用户视图进行检查）
    if (!this.isAdminView && !this.checkAccess()) {
      // 如果没有预约，仍然加载聊天室信息，但不连接WebSocket
      await this.loadRoomData()
      return
    }

    // 清除之前的聊天室数据
    this.clearCurrentRoomData()

    try {
      // 加载聊天室数据
      const success = await this.loadRoomData()
      if (success) {
        this.initWebSocket()
      }
    } catch (error) {
      console.error('连接聊天室失败:', error)
      if (error.message === 'Network Error') {
        this.$message.error('无法连接到服务器，请确认后端服务已启动')
        // 根据视图类型决定重定向路径
        const redirectPath = this.isAdminView ? '/admin/chat-rooms' : '/chat-rooms';
        this.$router.push(redirectPath)
      }
    }
  },
  beforeDestroy() {
    // 确保在组件销毁前清理所有定时器和连接
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    if (this.wsService) {
      console.log('组件销毁，断开WebSocket连接');
      this.wsService.disconnect();
      this.wsService = null;
    }
    this.clearCurrentRoomData();
  }
}
</script>

<style scoped>
.chat-room {
  padding: 20px;
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.back-button {
  margin-bottom: 15px;
}

.room-info-card {
  margin-bottom: 20px;
}

.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.room-header h3 {
  margin: 0;
}

.user-count {
  font-size: 14px;
  color: #606266;
}

.chat-container {
  flex: 1;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
  height: calc(100% - 80px);
}

.user-list-aside {
  border-right: 1px solid #ebeef5;
  background-color: #f9fafc;
  overflow-y: auto;
}

.user-list-header {
  padding: 15px;
  font-weight: bold;
  border-bottom: 1px solid #ebeef5;
  background-color: #f2f6fc;
}

.user-list {
  padding: 10px;
}

.user-item {
  display: flex;
  align-items: center;
  padding: 8px;
  border-radius: 4px;
}

.user-item:hover {
  background-color: #f0f2f5;
}

.user-name {
  margin-left: 10px;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.no-users {
  padding: 20px 0;
  text-align: center;
  color: #909399;
}

.chat-main {
  padding: 0;
  display: flex;
  flex-direction: column;
}

.message-list {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.message-item {
  margin-bottom: 30px;
  clear: both;
  width: 100%;
}

.message-sender {
  display: flex;
  align-items: flex-start;
  margin-bottom: 8px;
}

.message-info {
  margin-left: 10px;
  flex: 1;
}

.sender-name {
  font-weight: bold;
  font-size: 14px;
  line-height: 1.2;
  margin-bottom: 4px;
}

.message-time {
  font-size: 12px;
  color: #909399;
}

.message-content {
  padding: 12px 16px;
  background-color: #f2f6fc;
  border-radius: 4px;
  word-break: break-word;
  max-width: 80%;
  margin-left: 46px;
  position: relative;
}

.my-message .message-sender {
  flex-direction: row-reverse;
}

.my-message .message-info {
  margin-left: 0;
  margin-right: 10px;
  text-align: right;
}

.my-message .message-content {
  float: right;
  margin-left: 0;
  margin-right: 46px;
  background-color: #ecf5ff;
}

.my-message .message-content:before {
  left: auto;
  right: 15px;
}

.no-messages {
  text-align: center;
  padding: 30px 0;
  color: #909399;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-input-container {
  border-top: 1px solid #ebeef5;
  padding: 15px;
}

.message-tools {
  margin-bottom: 10px;
}

.input-area {
  display: flex;
}

.input-area .el-textarea {
  flex: 1;
  margin-right: 10px;
}

.send-tip {
  margin-top: 5px;
  font-size: 12px;
  color: #909399;
  text-align: right;
}
</style>