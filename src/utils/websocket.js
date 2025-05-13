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
    this.isUsingNewFormat = false; // 添加新格式使用标志
  }

  connect() {
    if (this.isManualClosed) {
      console.log('WebSocket已手动关闭，不再尝试重连');
      return;
    }

    this.isConnected = false;
    const baseURL = process.env.VUE_APP_WS_URL || "ws://localhost:9090";

    // 添加调试输出，查看原始用户名
    console.log('原始用户名:', this.username);
    console.log('原始用户ID:', this.userId);
    console.log('原始头像:', this.avatar);

    // 警告：这个URL格式改动需要后端支持
    console.warn('【重要】WebSocket连接URL格式已修改为查询参数形式，请确保后端支持此格式!');
    console.warn('如果连接失败，可能需要恢复为原来的路径参数格式: /imserver/{roomId}/{username}/{avatar}');

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

    // 不再对用户名进行额外的URL编码，直接使用经过encodeURIComponent处理的值
    // 防止中文和特殊字符在传输过程中出现问题
    const encodedUsername = encodeURIComponent(this.username);
    const encodedAvatar = encodeURIComponent(avatarParam || '');

    // 调试输出处理后的值
    console.log('编码后的用户名:', encodedUsername);
    console.log('编码后的头像URL:', encodedAvatar);

    // 构建两种格式的WebSocket URL
    // 1. 查询参数格式（新格式，解决编码问题）
    const wsUrlQuery = `${baseURL}/imserver/${this.roomId}?username=${encodedUsername}&avatar=${encodedAvatar}&userId=${this.userId}`;
    // 2. 路径参数格式（旧格式，兼容现有后端）
    const wsUrlPath = `${baseURL}/imserver/${this.roomId}/${encodedUsername}/${encodedAvatar}`;

    // 根据环境变量或之前的连接尝试来决定使用哪种URL格式
    const useLegacyWs = process.env.VUE_APP_USE_LEGACY_WS === 'true' || !this.isUsingNewFormat;
    const wsUrl = useLegacyWs ? wsUrlPath : wsUrlQuery;

    // 记录当前是否正在使用新格式
    this.isUsingNewFormat = !useLegacyWs;

    console.log(`使用${this.isUsingNewFormat ? '新' : '旧'}版WebSocket URL格式：`, wsUrl);

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
          // 如果是使用新格式失败的，尝试切换到旧格式
          if (this.isUsingNewFormat && this.reconnectAttempts === 0) {
            console.log('新格式WebSocket连接失败，尝试使用旧格式连接');
            this.isUsingNewFormat = false;
            setTimeout(() => {
              if (this.ws) {
                this.ws.close();
              }
              this.connect();
            }, 1000);
            return;
          }

          // 正常重连逻辑
          this.reconnect();
        } else {
          console.log('WebSocket正常关闭，不再重连');
        }
      };

      this.ws.onerror = (error) => {
        console.error('WebSocket error:', error);
        this.isConnected = false;

        // 如果是使用新格式失败的，等待onclose处理切换到旧格式
        if (this.isUsingNewFormat) {
          console.log('新格式WebSocket连接出错，等待切换到旧格式');
          return;
        }

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