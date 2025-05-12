import Vue from 'vue'
import VueRouter from 'vue-router'

import Login from "@/views/Login.vue";
import ChatRoom from "@/views/chat/ChatRoom.vue";

import Home from '@/views/user/Home.vue'
import Layout from "@/views/user/Layout.vue";
import Register from "@/views/user/Register";

import Person from "@/views/user/Profile.vue";
import SeatReservation from "@/views/user/SeatReservation.vue";

import AdminDashboard from "@/views/admin/Dashboard";
import Notice from "@/views/admin/Notice";
import User from "@/views/admin/User.vue";

// 聊天室相关组件
import ChatRoomList from "@/views/chat/ChatRoomList.vue";
import ChatRoomAdmin from "@/views/chat/ChatRoomAdmin.vue";

Vue.use(VueRouter)

const routes = [
  { path: '/login', component: Login },
  { path: '/register', component: Register },
  { path: '/', component: Layout,
    children: [
      { path: '', component: Home },
      { path: 'user', component: User },
      { path: 'notice', component: Notice },
      // 聊天室列表
      { path: 'chat-rooms', component: ChatRoomList },
      // 单个聊天室
      { path: 'chat-room/:id', component: ChatRoom },
      { path: 'person', component: Person },
      // 座位预约
      { path: 'seat-reservation', component: SeatReservation },
    ]
  },
  { path: '/admin', component: AdminDashboard },
  // 管理员聊天室管理
  { path: '/admin/chat-rooms', component: ChatRoomAdmin },
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

const originalPush = VueRouter.prototype.push
VueRouter.prototype.push = function push(location) {
  return originalPush.call(this, location).catch(err => err)
}

// 路由守卫
router.beforeEach((to, from, next) => {
  const userToken = localStorage.getItem("user-token");
  const adminToken = localStorage.getItem("admin-token");

  // 如果是访问管理员页面
  if (to.path === '/admin' || to.path === '/admin/chat-rooms') {
    if (!adminToken) {
      return next("/login");
    }
    return next();
  }

  // 聊天室相关路由
  if (to.path.startsWith('/chat-room/')) {
    // 检查是否有有效的预约
    const activeReservation = localStorage.getItem('activeReservation');
    if (!activeReservation && userToken) {
      // 如果没有活跃预约，重定向到座位预约页面
      return next('/seat-reservation');
    }
  }

  // 其他页面的访问控制
  if (!userToken && !adminToken && to.path !== '/login' && to.path !== '/register') {
    return next("/login");
  }
  next();
})

export default router
