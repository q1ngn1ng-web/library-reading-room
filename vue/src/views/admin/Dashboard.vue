<template>
  <div class="admin-dashboard">
    <el-container>
      <el-header style="background-color: #409EFF; color: white;">
        <div class="header-content">
          <h2>管理员控制面板</h2>
          <div class="header-right">
            <el-dropdown @command="handleCommand">
              <span class="el-dropdown-link">
                {{ adminInfo.name }}<i class="el-icon-arrow-down el-icon--right"></i>
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
        </div>
      </el-header>
      <el-container>
        <el-aside width="200px" style="background-color: #545c64;">
          <el-menu
            :default-active="activeMenu"
            class="el-menu-vertical"
            background-color="#545c64"
            text-color="#fff"
            active-text-color="#ffd04b"
            @select="handleSelect">
            <el-menu-item index="dashboard">
              <i class="el-icon-s-home"></i>
              <span slot="title">首页</span>
            </el-menu-item>
            <el-menu-item index="users">
              <i class="el-icon-user"></i>
              <span slot="title">用户管理</span>
            </el-menu-item>
            <el-menu-item index="notices">
              <i class="el-icon-message"></i>
              <span slot="title">通知管理</span>
            </el-menu-item>
            <el-menu-item index="seats">
              <i class="el-icon-s-grid"></i>
              <span slot="title">座位管理</span>
            </el-menu-item>
            <el-menu-item index="chatrooms">
              <i class="el-icon-chat-dot-round"></i>
              <span slot="title">聊天室管理</span>
            </el-menu-item>
            <el-menu-item index="profile">
              <i class="el-icon-user"></i>
              <span slot="title">个人中心</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        <el-main>
          <!-- 仪表盘页面 -->
          <div v-if="activeMenu === 'dashboard'">
            <h2>系统概览</h2>

            <!-- 加载中 -->
            <div v-if="loading" class="loading-container">
              <el-skeleton :rows="10" animated />
            </div>

            <!-- 数据统计卡片 -->
            <div v-if="!loading">
              <el-row :gutter="20">
                <el-col :span="8">
                  <el-card shadow="hover" class="stat-card">
                    <div class="stat-card-content">
                      <div class="stat-icon">
                        <i class="el-icon-user"></i>
                      </div>
                      <div class="stat-info">
                        <div class="stat-title">用户总数</div>
                        <div class="stat-value">{{ dashboardData.userCount }}</div>
                      </div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="8">
                  <el-card shadow="hover" class="stat-card">
                    <div class="stat-card-content">
                      <div class="stat-icon">
                        <i class="el-icon-user-solid"></i>
                      </div>
                      <div class="stat-info">
                        <div class="stat-title">当前在线用户</div>
                        <div class="stat-value">{{ dashboardData.onlineUsers }}</div>
                      </div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="8">
                  <el-card shadow="hover" class="stat-card">
                    <div class="stat-card-content">
                      <div class="stat-icon">
                        <i class="el-icon-chat-dot-round"></i>
                      </div>
                      <div class="stat-info">
                        <div class="stat-title">活跃聊天室</div>
                        <div class="stat-value">{{ dashboardData.activeRooms }}</div>
                      </div>
                    </div>
                  </el-card>
                </el-col>
              </el-row>
              <el-row :gutter="20" class="second-row" style="margin-top: 20px;">
                <el-col :span="12">
                  <el-card shadow="hover" class="stat-card">
                    <div class="stat-card-content">
                      <div class="stat-icon">
                        <i class="el-icon-s-order"></i>
                      </div>
                      <div class="stat-info">
                        <div class="stat-title">总预约数</div>
                        <div class="stat-value">{{ dashboardData.totalReservations }}</div>
                      </div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="12">
                  <el-card shadow="hover" class="stat-card">
                    <div class="stat-card-content">
                      <div class="stat-icon">
                        <i class="el-icon-time"></i>
                      </div>
                      <div class="stat-info">
                        <div class="stat-title">活跃预约</div>
                        <div class="stat-value">{{ dashboardData.activeReservations }}</div>
                      </div>
                    </div>
                  </el-card>
                </el-col>
              </el-row>
            </div>
          </div>

          <!-- 用户管理页面 -->
          <user-management v-else-if="activeMenu === 'users'" />

          <!-- 通知管理页面 -->
          <notice-management v-else-if="activeMenu === 'notices'" />

          <!-- 座位管理页面 -->
          <seat-management v-else-if="activeMenu === 'seats'" />

          <!-- 聊天室管理页面 -->
          <chatroom-management v-else-if="activeMenu === 'chatrooms'" />

          <!-- 个人中心页面 -->
          <profile-management v-else-if="activeMenu === 'profile'" :admin-info="adminInfo" @info-updated="handleProfileUpdated" />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import UserManagement from './UserManagement.vue'
import NoticeManagement from './NoticeManagement.vue'
import SeatManagement from './SeatManagement.vue'
import ChatroomManagement from './ChatroomManagement.vue'
import ProfileManagement from './ProfileManagement.vue'
import request from '@/utils/request'

export default {
  name: 'AdminDashboard',
  components: {
    UserManagement,
    NoticeManagement,
    SeatManagement,
    ChatroomManagement,
    ProfileManagement
  },
  data() {
    return {
      adminInfo: {},
      activeMenu: 'dashboard',
      loading: true,
      dashboardData: {
        userCount: 0,
        onlineUsers: 0,
        activeRooms: 0,
        totalReservations: 0,
        activeReservations: 0,
        statistics: {
          daily: [],
          weekly: [],
          monthly: []
        }
      }
    }
  },
  created() {
    // 获取管理员信息
    const adminInfo = localStorage.getItem('admin')
    if (adminInfo) {
      this.adminInfo = JSON.parse(adminInfo)
    } else {
      this.$router.push('/login')
    }

    // 获取仪表盘数据
    this.fetchDashboardData()
  },
  methods: {
    fetchDashboardData() {
      this.loading = true
      request.get('/admin/dashboard', { role: 'admin' }).then(res => {
        if (res.code === '0') {
          this.dashboardData = res.data
        } else {
          this.$message.error(res.msg || '获取仪表盘数据失败')
        }
      }).catch(err => {
        //console.error('获取仪表盘数据失败', err)
        this.$message.error('获取仪表盘数据失败')
      }).finally(() => {
        this.loading = false
      })
    },
    handleSelect(key) {
      this.activeMenu = key
    },
    handleCommand(command) {
      if (command === 'logout') {
        this.handleLogout()
      } else if (command === 'profile') {
        this.activeMenu = 'profile'
      }
    },
    handleLogout() {
      this.$confirm('确认退出登录吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        localStorage.removeItem('admin')
        localStorage.removeItem('admin-token')
        this.$router.push('/login')
        this.$message.success('已退出登录')
      }).catch(() => {})
    },
    handleProfileUpdated(updatedInfo) {
      this.adminInfo = updatedInfo
      localStorage.setItem('admin', JSON.stringify(updatedInfo))
      this.$message.success('个人信息更新成功')
    }
  }
}
</script>

<style scoped>
.admin-dashboard {
  height: 100vh;
}
.el-header {
  line-height: 60px;
}
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}
.header-right {
  color: white;
}
.el-dropdown-link {
  color: white;
  cursor: pointer;
}
.el-aside {
  height: calc(100vh - 60px);
}

.loading-container {
  padding: 20px;
}

.stat-card {
  margin-bottom: 20px;
}
.stat-card-content {
  display: flex;
  align-items: center;
}
.stat-icon {
  font-size: 48px;
  margin-right: 15px;
  color: #409EFF;
}
.stat-info {
  flex: 1;
}
.stat-title {
  font-size: 14px;
  color: #909399;
}
.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}
.second-row {
  margin-top: 20px;
}
</style>