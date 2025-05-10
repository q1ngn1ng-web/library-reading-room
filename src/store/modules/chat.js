import request from '@/utils/request'

const state = {
  chatRooms: [],
  currentRoom: null,
  messages: [],
  onlineUsers: []
}

const mutations = {
  SET_CHAT_ROOMS(state, rooms) {
    state.chatRooms = rooms
  },
  SET_CURRENT_ROOM(state, room) {
    state.currentRoom = room
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
      const res = await request.get('/chatRoom/list')
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
      const res = await request.get(`/chatRoom/${roomId}`)
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
      const res = await request.post('/chatRoom', roomData)
      return res
    } catch (error) {
      console.error('创建聊天室失败:', error)
      return { code: '-1', msg: '创建聊天室失败' }
    }
  },

  // 更新聊天室状态
  async updateRoomStatus(_, { roomId, status }) {
    try {
      const res = await request.put(`/chatRoom/${roomId}/status`, { status })
      return res
    } catch (error) {
      console.error('更新聊天室状态失败:', error)
      return { code: '-1', msg: '更新聊天室状态失败' }
    }
  },

  // 删除聊天室
  async deleteChatRoom(_, roomId) {
    try {
      const res = await request.delete(`/chatRoom/${roomId}`)
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