/**
 * WebSocket服务类 - 处理聊天室WebSocket连接
 */
export default class WebSocketService {
  constructor(roomId, userId, displayName, avatar, onMessage, onUserList) {
    this.roomId = roomId;
    this.userId = userId;
    this.username = displayName; // 使用displayName作为username传递给后端
    this.displayName = displayName; // 存储显示名称
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
      // console.log('WebSocket已手动关闭，不再尝试重连');
      return;
    }

    this.isConnected = false;
    // 优先使用环境变量；生产环境未配置时按当前页面地址同源推导（经 Nginx /api 反代）
    const baseURL = process.env.VUE_APP_WS_URL
      || (process.env.NODE_ENV === 'production'
        ? `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/api`
        : 'ws://localhost:9090');

    // console.log('用户信息:', {
    //   roomId: this.roomId,
    //   userId: this.userId,
    //   username: this.username,
    //   displayName: this.displayName,
    //   avatar: this.avatar
    // });

    // 按当前路由选择角色 token（身份由后端验签 token 后反查，前端只负责携带）
    const isAdmin = window.location.pathname.startsWith('/admin');
    const token = localStorage.getItem(isAdmin ? 'admin-token' : 'user-token');
    if (!token) {
      console.warn('未找到登录token，无法建立WebSocket连接');
      if (typeof this.onError === 'function') {
        this.onError('未登录或登录已过期');
      }
      return;
    }

    const wsUrl = `${baseURL}/imserver/${this.roomId}?token=${encodeURIComponent(token)}`;
    // console.log('WebSocket连接URL:', wsUrl);

    try {
      this.ws = new WebSocket(wsUrl);

      this.ws.onopen = (event) => {
        // console.log('WebSocket connected');
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
            // Process user list to add displayName property
            const processedUsers = data.users.map(user => {
              // 确保每个用户都有displayName属性
              if (!user.displayName) {
                // 如果用户名格式是 "楼层号座位号 用户名" 的格式，保留原样
                if (user.username && user.username.match(/\d+层\d+号/)) {
                  user.displayName = user.username;
                } else {
                  // 否则使用用户昵称或用户名
                  user.displayName = user.nickname || user.username;
                }
              }
              return user;
            });

            //console.log('接收到用户列表:', processedUsers);
            this.onUserList(processedUsers);
          } else if (data.type === 'message') {
            // Process message to ensure it has proper displayName
            if (data.isAnonymous !== true) {
              if (!data.displayName) {
                data.displayName = data.nickname || data.username || data.name;
              }
            }
            this.onMessage(data);
          }
        } catch (error) {
          //console.error('WebSocket message parse error:', error);
        }
      };

      this.ws.onclose = (event) => {
        //console.log('WebSocket disconnected', event);
        this.stopHeartbeat();
        this.isConnected = false;

        // 检查是否是正常关闭或手动关闭
        if (!this.isManualClosed && (event.code !== 1000 || event.wasClean !== true)) {
          this.reconnect();
        } else {
          //console.log('WebSocket正常关闭，不再重连');
        }
      };

      this.ws.onerror = (error) => {
        //console.error('WebSocket error:', error);
        this.isConnected = false;

        // 调用错误回调
        if (typeof this.onError === 'function') {
          this.onError('WebSocket连接失败，请检查网络连接');
        }
      };
    } catch (e) {
      //console.error('WebSocket创建失败:', e);
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
      //console.log('WebSocket已手动关闭，不再尝试重连');
      return;
    }

    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      //console.log(`尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
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
        userId: this.userId,
        username: this.username,  // 保留用户名
        displayName: this.displayName // 添加显示名称
      };
      console.log('发送消息:', message);
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