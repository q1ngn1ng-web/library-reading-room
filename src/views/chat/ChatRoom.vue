<template>
  <div class="chat-room">
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
        <el-tag :type="currentRoom && currentRoom.status ? 'success' : 'danger'" size="small">
          {{ currentRoom && currentRoom.status ? '开放' : '关闭' }}
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
          <div v-for="user in onlineUsers" :key="user.name" class="user-item">
            <el-avatar :size="32" :src="user.avatar || defaultAvatar"></el-avatar>
            <span class="user-name">{{ user.name }}</span>
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
                <div class="sender-name">{{ msg.isAnonymous ? '匿名用户' : msg.name }}</div>
                <div class="message-time">{{ formatTime(msg.time) }}</div>
              </div>
            </div>
            <div class="message-content">{{ msg.content }}</div>
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
              :disabled="!currentRoom || !currentRoom.status"
              @keyup.ctrl.enter.native="sendMessage"
            ></el-input>
            <el-button
              type="primary"
              :disabled="!messageContent.trim() || !currentRoom || !currentRoom.status"
              @click="sendMessage"
            >
              发送
            </el-button>
          </div>
          <div class="send-tip">提示: 按Ctrl+Enter快速发送</div>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { mapActions, mapGetters } from 'vuex'
import WebSocketService from '@/utils/websocket'

export default {
  name: 'ChatRoom',
  data() {
    return {
      roomId: null,
      wsService: null,
      messageContent: '',
      isAnonymous: false,
      defaultAvatar: require('@/assets/default-avatar.png'), // 请确保路径正确或使用在线链接
      user: null,
      isConnected: false,
      showDebugInfo: true,
      connectionError: false, // 添加连接错误状态
      reconnectTimer: null // 用于管理重连定时器
    }
  },
  computed: {
    ...mapGetters('chat', [
      'getCurrentRoom',
      'getMessages',
      'getOnlineUsers'
    ]),
    currentRoom() {
      return this.getCurrentRoom
    },
    messages() {
      return this.getMessages
    },
    onlineUsers() {
      return this.getOnlineUsers
    }
  },
  methods: {
    ...mapActions('chat', [
      'fetchChatRoom',
      'receiveMessage',
      'updateOnlineUsers',
      'clearCurrentRoomData'
    ]),
    handleWebSocketError(errorMsg) {
      this.connectionError = true;
      this.$message.error(errorMsg || 'WebSocket连接失败，请刷新页面重试');
    },
    sendMessage() {
      if (this.messageContent.trim() && this.wsService && this.currentRoom && this.currentRoom.status) {
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
      this.updateOnlineUsers(users)
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
      const hours = date.getHours().toString().padStart(2, '0')
      const minutes = date.getMinutes().toString().padStart(2, '0')
      return `${hours}:${minutes}`
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

      this.user = JSON.parse(localStorage.getItem('user') || '{}');
      if (!this.user || !this.user.name) {
        this.$message.error('用户信息不完整，请重新登录');
        this.$router.push('/login');
        return;
      }

      // 确保我们得到用户ID
      const userId = this.user.id;
      console.log('User info from localStorage:', this.user);
      if (!userId) {
        console.warn('用户ID不存在，可能会导致数据库中user_id为0');
      }

      this.connectionError = false;
      this.isConnected = false;

      this.wsService = new WebSocketService(
        this.roomId,
        userId,
        this.user.name,
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
        console.log('WebSocket连接已建立');
      };

      // 执行连接
      this.wsService.connect();
    },
    async loadRoomData() {
      try {
        await this.fetchChatRoom(this.roomId)
        if (!this.currentRoom) {
          this.$message.error('聊天室不存在或已关闭')
          this.$router.push('/chat-rooms')
          return false
        }
        return true
      } catch (error) {
        console.error('加载聊天室信息失败:', error)
        this.$message.error('加载聊天室信息失败')
        return false
      }
    }
  },
  async created() {
    this.roomId = parseInt(this.$route.params.id)
    if (!this.roomId) {
      this.$router.push('/chat-rooms')
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
        this.$router.push('/chat-rooms')
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
}

.message-item {
  margin-bottom: 20px;
}

.message-sender {
  display: flex;
  align-items: center;
  margin-bottom: 5px;
}

.message-info {
  margin-left: 10px;
}

.sender-name {
  font-weight: bold;
  font-size: 14px;
}

.message-time {
  font-size: 12px;
  color: #909399;
}

.message-content {
  padding: 10px 15px;
  background-color: #f2f6fc;
  border-radius: 4px;
  word-break: break-word;
  max-width: 80%;
  margin-left: 46px;
  position: relative;
}

.message-content:before {
  content: '';
  position: absolute;
  top: -6px;
  left: 15px;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-bottom: 6px solid #f2f6fc;
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
  margin-left: 0;
  margin-right: 46px;
  background-color: #ecf5ff;
  float: right;
}

.my-message .message-content:before {
  left: auto;
  right: 15px;
  border-bottom-color: #ecf5ff;
}

.no-messages {
  text-align: center;
  padding: 30px 0;
  color: #909399;
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