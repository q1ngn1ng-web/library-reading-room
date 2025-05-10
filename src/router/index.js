import Vue from 'vue'
import VueRouter from 'vue-router'
import Home from '@/views/Home.vue'
import Login from "@/views/Login.vue";
import User from "@/views/User.vue";
import Layout from "@/views/user/Layout.vue";
import Register from "@/views/Register";
import ChatRoom from "@/components/ChatRoom.vue";
import Person from "@/views/user/Profile.vue";
import Notice from "@/views/Notice";
import AdminDashboard from "@/views/admin/Dashboard.vue";

Vue.use(VueRouter)

const routes = [
  { path: '/login', component: Login },
  { path: '/register', component: Register },
  { path: '/', component: Layout,
    children: [
      { path: '', component: Home },
      { path: 'user', component: User },
      { path: 'notice', component: Notice },
      { path: 'ChatRoom', component: ChatRoom },
      { path: 'person', component: Person },
    ]
  },
  { path: '/admin', component: AdminDashboard },
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
  if (to.path === '/admin') {
    if (!adminToken) {
      return next("/login");
    }
    return next();
  }

  // 其他页面的访问控制
  if (!userToken && !adminToken && to.path !== '/login' && to.path !== '/register') {
    return next("/login");
  }
  next();
})

export default router
