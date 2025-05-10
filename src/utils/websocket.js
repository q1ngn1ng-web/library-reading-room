/**
 * WebSocket服务类 - 处理聊天室WebSocket连接
 */
class WebSocketService {
  constructor(roomId, userId, username, avatar, onMessage, onUserList) {
    this.roomId = roomId;
    this.userId = userId;
    this.username = username;
    this.avatar = avatar;
    this.onMessage = onMessage;
    this.onUserList = onUserList;
    this.onError = null;
    this.ws = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectTimeout = 3000;
    this.heartbeatInterval = null;
  }

  connect() {
    const baseURL = process.env.VUE_APP_WS_URL || "ws://localhost:9090";

    // 处理avatar参数 - 如果是完整URL，只取文件名部分
    let avatarParam = this.avatar;
    if (avatarParam && avatarParam.includes('http')) {
      // 提取文件名部分
      try {
        const url = new URL(avatarParam);
        const pathSegments = url.pathname.split('/');
        avatarParam = pathSegments[pathSegments.length - 1];
      } catch (e) {
        console.error('Avatar URL解析失败:', e);
        avatarParam = 'default-avatar';
      }
    }

    // 使用encodeURIComponent确保所有参数正确编码
    const encodedUsername = encodeURIComponent(this.username);
    const encodedAvatar = encodeURIComponent(avatarParam);

    const wsUrl = `${baseURL}/imserver/${this.roomId}/${encodedUsername}/${encodedAvatar}`;
    console.log('连接WebSocket:', wsUrl);

    this.ws = new WebSocket(wsUrl);

    this.ws.onopen = () => {
      console.log('WebSocket connected');
      this.reconnectAttempts = 0;
      this.startHeartbeat();
    };

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.type === 'userList') {
          this.onUserList(data.users);
        } else if (data.type === 'message') {
          this.onMessage(data);
        }
      } catch (error) {
        console.error('WebSocket message parse error:', error);
      }
    };

    this.ws.onclose = () => {
      console.log('WebSocket disconnected');
      this.stopHeartbeat();
      this.reconnect();
    };

    this.ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      // 如果是第一次连接就出错，直接通知用户
      if (this.reconnectAttempts === 0 && typeof this.onError === 'function') {
        this.onError('WebSocket连接失败，请检查网络连接');
      }
    };
  }

  reconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
      setTimeout(() => this.connect(), this.reconnectTimeout);
    } else {
      console.error('WebSocket重连失败，已达到最大重试次数');
      // 通知用户连接失败
      if (typeof this.onError === 'function') {
        this.onError('连接服务器失败，请刷新页面重试');
      }
    }
  }

  startHeartbeat() {
    this.heartbeatInterval = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({ type: 'heartbeat' }));
      }
    }, 30000); // 每30秒发送一次心跳
  }

  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  sendMessage(content, isAnonymous = false) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      const message = {
        type: 'message',
        content: content,
        isAnonymous: isAnonymous,
        userId: this.userId
      };
      console.log('Sending message with userId:', this.userId);
      this.ws.send(JSON.stringify(message));
      return true;
    }
    return false;
  }

  disconnect() {
    this.stopHeartbeat();
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }
}

export default WebSocketService;