import request from '@/utils/request'

// 管理员登录
export function adminLogin(data) {
  return request({
    url: '/admin/login',
    method: 'post',
    data
  })
}

// 修改管理员密码
export function updateAdminPassword(data) {
  return request({
    url: '/admin/password',
    method: 'post',
    data
  })
}

// 查询管理员信息
export function getAdminInfo(id) {
  return request({
    url: `/admin/find/${id}`,
    method: 'get'
  })
}

// 更新管理员信息
export function updateAdmin(id, data) {
  return request({
    url: `/admin/update/${id}`,
    method: 'put',
    data
  })
}

// 处理管理员登录后的信息存储
export function handleAdminLogin(adminData) {
  // 转换role为前端需要的格式
  const adminInfo = {
    ...adminData,
    name: adminData.nickname || adminData.username, // 使用nickname作为显示名
    roleType: adminData.role === 2 ? 'super' : 'normal' // 转换role为字符串表示
  }

  // 存储到本地存储
  localStorage.setItem('admin', JSON.stringify(adminInfo))
  localStorage.setItem('admin-token', adminData.token)

  return adminInfo
}