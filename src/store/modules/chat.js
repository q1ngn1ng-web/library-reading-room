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
    // 添加兼容性处理
    state.chatRooms = rooms.map(room => ({
      ...room,
      status: room.is_available // 为兼容旧代码保留status字段
    }))
  },
  SET_CURRENT_ROOM(state, room) {
    if (room) {
      state.currentRoom = {
        ...room,
        status: room.is_available // 为兼容旧代码保留status字段
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
      if (res.code === '0') {
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