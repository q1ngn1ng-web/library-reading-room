import request from '@/utils/request'
import { getChatRoomList, getChatRoomDetail, createChatRoom, updateChatRoomStatus,
  updateChatRoomChatStatus, deleteChatRoom } from '@/api/chatRoom'

const state = {
  chatRooms: [],
  currentRoom: null,
  messages: [],
  onlineUsers: []
}

const mutations = {
  SET_CHAT_ROOMS(state, rooms) {
    // console.log('准备处理聊天室数据, 数量:', rooms.length);

    // 确保属性名称和值类型正确映射
    state.chatRooms = rooms.map(room => {
      // 映射 camelCase 到 snake_case
      const mappedRoom = {
        ...room,
        // 确保属性正确映射
        is_available: room.isAvailable !== undefined ? room.isAvailable : room.is_available,
        is_chat: room.isChat !== undefined ? room.isChat : room.is_chat,
      };

      // 检查字段值和转换
      let isAvailable;

      // 优先使用已映射的 is_available 字段
      if (typeof mappedRoom.is_available === 'string') {
        isAvailable = mappedRoom.is_available === 'true' || mappedRoom.is_available === '1';
        console.log(`聊天室 ${room.id} is_available为字符串: "${mappedRoom.is_available}", 转换为:`, isAvailable);
      }
      // 如果是数字，1为true，0为false
      else if (typeof mappedRoom.is_available === 'number') {
        isAvailable = mappedRoom.is_available === 1;
        console.log(`聊天室 ${room.id} is_available为数字: ${mappedRoom.is_available}, 转换为:`, isAvailable);
      }
      // 如果已经是布尔值，直接使用
      else {
        isAvailable = !!mappedRoom.is_available;
        console.log(`聊天室 ${room.id} is_available为其他类型: ${mappedRoom.is_available}, 转换为:`, isAvailable);
      }

      return {
        ...mappedRoom,
        // 将is_available强制转换为布尔值
        is_available: isAvailable,
        // 为兼容旧代码保留status字段
        status: isAvailable
      };
    });

    console.log('处理后的聊天室数据:', state.chatRooms);
  },
  SET_CURRENT_ROOM(state, room) {
    if (room) {
      state.currentRoom = {
        ...room,
        status: room.isAvailable // 为兼容旧代码保留status字段
      }
    } else {
      state.currentRoom = null
    }
  },
  SET_MESSAGES(state, messages) {
    state.messages = messages
  },
  ADD_MESSAGE(state, message) {
    state.messages.push(message)
  },
  SET_ONLINE_USERS(state, users) {
    state.onlineUsers = users
  }
}

const actions = {
  // 获取所有聊天室
  async fetchChatRooms({ commit }) {
    try {
      const res = await getChatRoomList()
      console.log('API 返回的聊天室原始数据:', res);

      if (res.code === '0') {
        console.log('聊天室数据详情:', JSON.stringify(res.data, null, 2));
        // 打印第一个聊天室的详细信息，检查字段名
        // if (res.data && res.data.length > 0) {
        //   console.log('第一个聊天室数据:');
        //   const firstRoom = res.data[0];
        //   console.log('- ID:', firstRoom.id);
        //   console.log('- isAvailable值:', firstRoom.isAvailable);
        //   console.log('- isAvailable类型:', typeof firstRoom.isAvailable);
        //   console.log('- is_available值:', firstRoom.is_available);
        //   console.log('- is_available类型:', typeof firstRoom.is_available);
        //   console.log('- 所有字段:', Object.keys(firstRoom));
        // }

        commit('SET_CHAT_ROOMS', res.data)
        return res.data
      }
      return []
    } catch (error) {
      console.error('获取聊天室列表失败:', error)
      return []
    }
  },

  // 获取单个聊天室信息
  async fetchChatRoom({ commit }, roomId) {
    try {
      const res = await getChatRoomDetail(roomId)
      if (res.code === '0') {
        commit('SET_CURRENT_ROOM', res.data)
        return res.data
      }
      return null
    } catch (error) {
      console.error('获取聊天室信息失败:', error)
      return null
    }
  },

  // 创建聊天室
  async createChatRoom(_, roomData) {
    try {
      const res = await createChatRoom(roomData)
      return res
    } catch (error) {
      console.error('创建聊天室失败:', error)
      return { code: '-1', msg: '创建聊天室失败' }
    }
  },

  // 更新聊天室开放状态
  async updateRoomAvailability(_, { roomId, isAvailable }) {
    try {
      const res = await updateChatRoomStatus(roomId, isAvailable)
      return res
    } catch (error) {
      console.error('更新聊天室状态失败:', error)
      return { code: '-1', msg: '更新聊天室状态失败' }
    }
  },

  // 更新聊天室禁言状态
  async updateRoomChatStatus(_, { roomId, isChat }) {
    try {
      const res = await updateChatRoomChatStatus(roomId, isChat)
      return res
    } catch (error) {
      console.error('更新聊天室禁言状态失败:', error)
      return { code: '-1', msg: '更新聊天室禁言状态失败' }
    }
  },

  // 删除聊天室
  async deleteChatRoom(_, roomId) {
    try {
      const res = await deleteChatRoom(roomId)
      return res
    } catch (error) {
      console.error('删除聊天室失败:', error)
      return { code: '-1', msg: '删除聊天室失败' }
    }
  },

  // 接收新消息
  receiveMessage({ commit }, message) {
    commit('ADD_MESSAGE', message)
  },

  // 更新在线用户列表
  updateOnlineUsers({ commit }, users) {
    commit('SET_ONLINE_USERS', users)
  },

  // 清除当前聊天室数据
  clearCurrentRoomData({ commit }) {
    commit('SET_CURRENT_ROOM', null)
    commit('SET_MESSAGES', [])
    commit('SET_ONLINE_USERS', [])
  }
}

const getters = {
  getChatRooms: state => state.chatRooms,
  getCurrentRoom: state => state.currentRoom,
  getMessages: state => state.messages,
  getOnlineUsers: state => state.onlineUsers
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}