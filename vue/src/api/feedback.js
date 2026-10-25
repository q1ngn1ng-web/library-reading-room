import request from '@/utils/request'

// 提交违规举报（聊天室内对消息举报）
export function submitFeedback(data) {
  return request({
    url: '/feedback',
    method: 'post',
    role: 'user',
    data
  })
}

// 我的举报记录
export function myFeedback() {
  return request({
    url: '/feedback/my',
    method: 'get',
    role: 'user'
  })
}

// 管理端：举报列表（分页+状态筛选），返回 { page, pending }
export function feedbackList(params) {
  return request({
    url: '/admin/feedback/list',
    method: 'get',
    role: 'admin',
    params
  })
}

// 管理端：处理举报（status 1已处理 / 2驳回）
export function handleFeedback(data) {
  return request({
    url: '/admin/feedback/handle',
    method: 'put',
    role: 'admin',
    data
  })
}
