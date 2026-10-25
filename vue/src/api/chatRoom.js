import request from '@/utils/request'

// 创建聊天室
export function createChatRoom(data) {
  return request({
    url: '/chatRoom',
    method: 'post',
    role: 'admin',  // 创建聊天室通常是管理员操作
    data
  })
}

// 更新聊天室
export function updateChatRoom(id, data) {
  return request({
    url: `/chatRoom/${id}`,
    method: 'put',
    role: 'admin',
    data
  })
}

// 更新聊天室开放状态
export function updateChatRoomStatus(id, isAvailable) {
  return request({
    url: `/chatRoom/${id}/status`,
    method: 'put',
    role: 'admin',
    params: { status: isAvailable ? 1 : 0 }
  })
}

// 更新聊天室禁言状态
export function updateChatRoomChatStatus(id, isChat) {
  return request({
    url: `/chatRoom/${id}/chat`,
    method: 'put',
    role: 'admin',
    params: { isChat: isChat ? 1 : 0 }
  })
}

// 获取所有聊天室
export function getChatRoomList() {
  return request({
    url: '/chatRoom/list',
    method: 'get',
    role: 'user'  // 用户通常也需要查看聊天室列表
  })
}

// 获取聊天室详情
export function getChatRoomDetail(id) {
  return request({
    url: `/chatRoom/${id}`,
    method: 'get',
    role: 'user'  // 用户需要查看聊天室详情
  })
}

// 删除聊天室
export function deleteChatRoom(id) {
  return request({
    url: `/chatRoom/${id}`,
    method: 'delete',
    role: 'admin'  // 删除聊天室通常是管理员操作
  })
}

// 获取聊天室历史消息
export function getChatRoomHistory(roomId, params) {
  return request({
    url: `/chat/history/${roomId}`,
    method: 'get',
    role: 'user',
    params
  })
}