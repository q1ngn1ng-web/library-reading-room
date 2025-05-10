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
            <el-menu-item index="system">
              <i class="el-icon-s-order"></i>
              <span slot="title">系统管理</span>
            </el-menu-item>
            <el-menu-item index="password">
              <i class="el-icon-lock"></i>
              <span slot="title">修改密码</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        <el-main>
          <!-- 根据选中的菜单显示不同内容 -->
          <el-card v-if="activeMenu === 'dashboard'" class="welcome-card">
            <div slot="header">
              <span>欢迎使用管理员系统</span>
            </div>
            <div>
              <p>这里是管理员控制面板，您可以在这里管理系统的各项功能。</p>
            </div>
          </el-card>

          <change-password
            v-else-if="activeMenu === 'password'"
            role="admin"
            :user-data="adminInfo"
            @password-changed="handlePasswordChanged"
          />

          <!-- 其他菜单对应的内容 -->
          <div v-else-if="activeMenu === 'users'">
            用户管理内容
          </div>

          <div v-else-if="activeMenu === 'system'">
            系统管理内容
          </div>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>




<script>
import ChangePassword from '@/components/ChangePassword.vue'
export default {
  name: 'AdminDashboard',
  components: {
    ChangePassword
  },
  data() {
    return {
      adminInfo: null,
      activeMenu: 'dashboard' // 当前选中的菜单
    }
  },
  created() {
    // 获取管理员信息
    const adminInfo = localStorage.getItem('admin')
    if (adminInfo) {
      this.adminInfo = JSON.parse(adminInfo)
    }
  },
  methods: {
    handleSelect(key) {
      this.activeMenu = key
    },
    handleCommand(command) {
      if (command === 'logout') {
        this.handleLogout()
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
    handlePasswordChanged() {
      this.$message.success('密码修改成功')
      this.activeMenu = 'dashboard' // 修改成功后返回首页
    }
  },
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
.welcome-card {
  margin: 20px;
}
</style>