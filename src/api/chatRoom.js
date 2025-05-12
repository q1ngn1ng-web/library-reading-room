import request from '@/utils/request'

// 创建聊天室
export function createChatRoom(data) {
  return request({
    url: '/chatRoom',
    method: 'post',
    data
  })
}

// 更新聊天室
export function updateChatRoom(id, data) {
  return request({
    url: `/chatRoom/${id}`,
    method: 'put',
    data
  })
}

// 更新聊天室开放状态
export function updateChatRoomStatus(id, isAvailable) {
  return request({
    url: `/chatRoom/${id}/status`,
    method: 'put',
    params: { status: isAvailable ? 1 : 0 }
  })
}

// 更新聊天室禁言状态
export function updateChatRoomChatStatus(id, isChat) {
  return request({
    url: `/chatRoom/${id}/chat`,
    method: 'put',
    params: { isChat: isChat ? 1 : 0 }
  })
}

// 获取所有聊天室
export function getChatRoomList() {
  return request({
    url: '/chatRoom/list',
    method: 'get'
  })
}

// 获取聊天室详情
export function getChatRoomDetail(id) {
  return request({
    url: `/chatRoom/${id}`,
    method: 'get'
  })
}

// 删除聊天室
export function deleteChatRoom(id) {
  return request({
    url: `/chatRoom/${id}`,
    method: 'delete'
  })
}