<template>
  <el-container>
    <el-header>
      <div class="header-content">
        <div class="logo">图书馆阅览室系统</div>
        <div class="user-info">
          <el-dropdown @command="handleCommand">
            <span class="el-dropdown-link">
              {{ userInfo.name }}<i class="el-icon-arrow-down el-icon--right"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="person">
                <i class="el-icon-user"></i>个人信息
              </el-dropdown-item>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <el-container>
      <el-aside width="200px">
        <el-menu
          :default-active="activeMenu"
          class="el-menu-vertical"
          @select="handleSelect">
          <el-menu-item index="home">
            <i class="el-icon-s-home"></i>
            <span slot="title">首页</span>
          </el-menu-item>
          <el-menu-item index="chat">
            <i class="el-icon-chat-dot-round"></i>
            <span slot="title">聊天室</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main>
        <!-- 根据选中的菜单显示不同内容 -->
        <component
          :is="currentView"
          v-if="currentView !== 'password' && currentView !== 'Person'"
        ></component>

        <change-password
          v-else-if="currentView === 'password'"
          role="user"
          :userData=this.userInfo
          @password-changed="handlePasswordChanged"
        />

        <person
          v-else-if="currentView === 'Person'"
          :userData=this.userInfo
          @profile-updated="handleProfileUpdated"
        />
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import ChangePassword from '@/components/ChangePassword.vue'
import Home from '@/views/user/Home.vue'
import ChatRoom from '@/components/ChatRoom.vue'
import Person from '@/views/user/Profile.vue'

export default {
  name: 'Layout',
  components: {
    ChangePassword,
    Home,
    ChatRoom,
    Person
  },
  data() {
    return {
      userInfo: null,
      activeMenu: 'home',
      currentView: 'Home'
    }
  },
  created() {
    // 获取用户信息
    const userInfo = localStorage.getItem('user')
    if (userInfo) {
      this.userInfo = JSON.parse(userInfo)
    }
  },
  methods: {
    handleSelect(key) {
      this.activeMenu = key
      // 根据菜单项设置当前视图
      switch(key) {
        case 'home':
          this.currentView = 'Home'
          break
        case 'chat':
          this.currentView = 'ChatRoom'
          break
        case 'notice':
          this.currentView = 'Notice'
          break

      }
    },
    handleCommand(command) {
      switch(command) {
        case 'person':
          this.currentView = 'Person'
          break
        case 'password':
          this.currentView = 'password'
          break
        case 'logout':
          this.handleLogout()
          break
      }
    },
    handleLogout() {
      this.$confirm('确认退出登录吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        localStorage.removeItem('user')
        localStorage.removeItem('user-token')
        this.$router.push('/login')
        this.$message.success('已退出登录')
      }).catch(() => {})
    },
    handlePasswordChanged() {
      this.$message.success('密码修改成功')
      this.currentView = 'Home' // 修改成功后返回首页
      this.activeMenu = 'home'
    },
    handlePasswordChanged() {
      this.$message.success('密码修改成功')
      this.currentView = 'Home'
      this.activeMenu = 'home'
    },
    handleProfileUpdated() {
      this.$message.success('个人信息更新成功')
      // 更新本地存储的用户信息
      const userInfo = localStorage.getItem('user')
      if (userInfo) {
        this.userInfo = JSON.parse(userInfo)
      }
      this.currentView = 'Home'
      this.activeMenu = 'home'
    },
  }
}
</script>

<style scoped>
.el-container {
  height: 100vh;
}
.el-header {
  background-color: #409EFF;
  color: white;
  line-height: 60px;
}
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}
.user-info {
  color: white;
}
.el-dropdown-link {
  color: white;
  cursor: pointer;
}
.el-aside {
  background-color: #545c64;
}
.el-menu {
  border-right: none;
}
</style>