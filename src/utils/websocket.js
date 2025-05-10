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
    this.onOpen = null; // 添加连接成功回调
    this.ws = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectTimeout = 3000;
    this.heartbeatInterval = null;
    this.isManualClosed = false; // 添加标志，区分手动关闭和异常关闭
    this.isConnected = false; // 添加连接状态
  }

  connect() {
    if (this.isManualClosed) {
      console.log('WebSocket已手动关闭，不再尝试重连');
      return;
    }

    this.isConnected = false;
    const baseURL = process.env.VUE_APP_WS_URL || "ws://localhost:9090";

    // 处理avatar参数
    let avatarParam = this.avatar || '';
    if (avatarParam && typeof avatarParam === 'string' && avatarParam.includes('http')) {
      try {
        const url = new URL(avatarParam);
        const pathSegments = url.pathname.split('/');
        avatarParam = pathSegments[pathSegments.length - 1];
      } catch (e) {
        console.error('Avatar URL解析失败:', e);
        avatarParam = 'default-avatar';
      }
    }

    // 确保只进行一次URL编码
    const encodedUsername = this.username.includes('%') ? this.username : encodeURIComponent(this.username);

    // 先解码确保没有重复编码
    try {
      if (avatarParam.includes('%')) {
        avatarParam = decodeURIComponent(avatarParam);
      }
    } catch (e) {
      console.error('头像URL解码失败:', e);
    }
    const encodedAvatar = encodeURIComponent(avatarParam);

    const wsUrl = `${baseURL}/imserver/${this.roomId}/${encodedUsername}/${encodedAvatar}`;
    console.log('连接WebSocket:', wsUrl);

    try {
      this.ws = new WebSocket(wsUrl);

      this.ws.onopen = (event) => {
        console.log('WebSocket connected');
        this.reconnectAttempts = 0;
        this.isConnected = true;
        this.startHeartbeat();

        // 调用连接成功回调
        if (typeof this.onOpen === 'function') {
          this.onOpen();
        }
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

      this.ws.onclose = (event) => {
        console.log('WebSocket disconnected', event);
        this.stopHeartbeat();
        this.isConnected = false;

        // 检查是否是正常关闭或手动关闭
        if (!this.isManualClosed && (event.code !== 1000 || event.wasClean !== true)) {
          this.reconnect();
        } else {
          console.log('WebSocket正常关闭，不再重连');
        }
      };

      this.ws.onerror = (error) => {
        console.error('WebSocket error:', error);
        this.isConnected = false;

        // 调用错误回调
        if (typeof this.onError === 'function') {
          this.onError('WebSocket连接失败，请检查网络连接');
        }
      };
    } catch (e) {
      console.error('WebSocket创建失败:', e);
      this.isConnected = false;

      // 调用错误回调
      if (typeof this.onError === 'function') {
        this.onError('WebSocket创建失败: ' + e.message);
      }

      this.reconnect();
    }
  }

  reconnect() {
    if (this.isManualClosed) {
      console.log('WebSocket已手动关闭，不再尝试重连');
      return;
    }

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
    this.isManualClosed = true;
    this.stopHeartbeat();
    if (this.ws) {
      try {
        // 使用正常关闭代码
        this.ws.close(1000, "Normal closure");
      } catch (e) {
        console.error('关闭WebSocket时发生错误:', e);
      }
      this.ws = null;
    }
  }
}

export default WebSocketService;