import request from '@/utils/request'

// 用户登录
export function userLogin(data) {
  return request({
    url: '/user/login',
    method: 'post',
    role: 'user',
    data
  })
}

// 用户注册
export function userRegister(data) {
  return request({
    url: '/user/register',
    method: 'post',
    role: 'user',
    data
  })
}

// 修改用户密码
export function updateUserPassword(data) {
  return request({
    url: '/user/password',
    method: 'post',
    role: 'user',
    data
  })
}

// 获取用户信息
export function getUserInfo(id) {
  return request({
    url: `/user/${id}`,
    method: 'get',
    role: 'user'
  })
}

// 更新用户信息
export function updateUser(id, data) {
  return request({
    url: `/user/${id}`,
    method: 'put',
    role: 'user',
    data
  })
}

// 处理用户登录后的信息存储
export function handleUserLogin(userData) {
  // 存储用户信息
  localStorage.setItem('user', JSON.stringify(userData))
  localStorage.setItem('user-token', userData.token)

  return userData
}

// 用户登出
export function logout() {
  localStorage.removeItem('user')
  localStorage.removeItem('user-token')
  localStorage.removeItem('activeReservation')
}