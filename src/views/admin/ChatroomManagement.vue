<template>
  <div class="chatroom-management">
    <div class="page-header">
      <h2>聊天室管理</h2>
      <el-button type="primary" @click="handleAdd">创建聊天室</el-button>
    </div>

    <!-- 聊天室列表 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      border
      style="width: 100%">
      <el-table-column
        prop="id"
        label="ID"
        width="60">
      </el-table-column>
      <el-table-column
        prop="floor"
        label="楼层"
        width="100">
      </el-table-column>
      <el-table-column
        prop="category"
        label="分类"
        width="120">
      </el-table-column>
      <el-table-column
        label="状态"
        width="120">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isAvailable ? 'success' : 'danger'">
            {{ scope.row.isAvailable ? '开放' : '关闭' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="聊天权限"
        width="120">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isChat ? 'success' : 'warning'">
            {{ scope.row.isChat ? '允许发言' : '禁言' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="人数"
        width="120">
        <template slot-scope="scope">
          <div>{{ scope.row.currentUser }}/{{ scope.row.maxUser }}</div>
        </template>
      </el-table-column>
      <el-table-column
        prop="createTime"
        label="创建时间"
        width="180">
      </el-table-column>
      <el-table-column
        label="操作">
        <template slot-scope="scope">
          <el-button
            size="mini"
            @click="handleViewUsers(scope.row)">查看用户</el-button>
          <el-button
            size="mini"
            :type="scope.row.isAvailable ? 'danger' : 'success'"
            @click="toggleRoomStatus(scope.row)">
            {{ scope.row.isAvailable ? '关闭' : '开放' }}
          </el-button>
          <el-button
            size="mini"
            :type="scope.row.isChat ? 'warning' : 'success'"
            @click="toggleChatStatus(scope.row)">
            {{ scope.row.isChat ? '禁言' : '解禁' }}
          </el-button>
          <el-button
            size="mini"
            type="danger"
            @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建聊天室对话框 -->
    <el-dialog
      title="创建聊天室"
      :visible.sync="roomDialogVisible"
      width="500px">
      <el-form
        :model="roomForm"
        :rules="roomRules"
        ref="roomForm"
        label-width="80px">
        <el-form-item label="楼层" prop="floor">
          <el-input v-model="roomForm.floor" placeholder="例如: 1楼"></el-input>
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="roomForm.category" placeholder="请选择分类">
            <el-option label="学习交流" value="学习交流"></el-option>
            <el-option label="休闲娱乐" value="休闲娱乐"></el-option>
            <el-option label="生活分享" value="生活分享"></el-option>
            <el-option label="技术讨论" value="技术讨论"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="最大人数" prop="maxUser">
          <el-input-number v-model="roomForm.maxUser" :min="2" :max="1000"></el-input-number>
        </el-form-item>
        <el-form-item label="状态" prop="isAvailable">
          <el-switch
            v-model="roomForm.isAvailable"
            active-text="开放"
            inactive-text="关闭">
          </el-switch>
        </el-form-item>
        <el-form-item label="聊天权限" prop="isChat">
          <el-switch
            v-model="roomForm.isChat"
            active-text="允许发言"
            inactive-text="禁言">
          </el-switch>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="roomDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitRoomForm">确 定</el-button>
      </span>
    </el-dialog>

    <!-- 聊天室用户列表对话框 -->
    <el-dialog
      :title="`聊天室用户 - ${selectedRoom.floor || ''}`"
      :visible.sync="userDialogVisible"
      width="600px">
      <el-table
        v-loading="userLoading"
        :data="roomUsers"
        border
        style="width: 100%">
        <el-table-column
          prop="userId"
          label="用户ID"
          width="80">
        </el-table-column>
        <el-table-column
          prop="name"
          label="用户名"
          width="120">
        </el-table-column>
        <el-table-column
          label="头像"
          width="80">
          <template slot-scope="scope">
            <el-avatar :size="40" :src="scope.row.avatar"></el-avatar>
          </template>
        </el-table-column>
        <el-table-column
          label="禁言状态"
          width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.isMuted ? 'danger' : 'success'">
              {{ scope.row.isMuted ? '已禁言' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="操作">
          <template slot-scope="scope">
            <el-button
              size="mini"
              :type="scope.row.isMuted ? 'success' : 'danger'"
              @click="toggleUserMuteStatus(scope.row)">
              {{ scope.row.isMuted ? '解除禁言' : '禁言' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="roomUsers.length === 0" class="no-data">
        暂无在线用户
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'ChatroomManagement',
  data() {
    return {
      // 聊天室列表
      tableData: [],
      loading: false,

      // 创建聊天室
      roomDialogVisible: false,
      roomForm: {
        floor: '',
        category: '',
        maxUser: 50,
        isAvailable: true,
        isChat: true
      },
      roomRules: {
        floor: [
          { required: true, message: '请输入楼层', trigger: 'blur' }
        ],
        category: [
          { required: true, message: '请选择分类', trigger: 'change' }
        ],
        maxUser: [
          { required: true, message: '请设置最大人数', trigger: 'blur' }
        ]
      },

      // 聊天室用户
      userDialogVisible: false,
      userLoading: false,
      selectedRoom: {},
      roomUsers: []
    }
  },
  created() {
    this.fetchChatRooms()
  },
  methods: {
    // 获取聊天室列表
    fetchChatRooms() {
      this.loading = true
      request.get('/chatRoom/list').then(res => {
        if (res.code === '0') {
          this.tableData = res.data || []
        } else {
          this.$message.error(res.msg || '获取聊天室列表失败')
        }
      }).catch(err => {
        //console.error('获取聊天室列表失败', err)
        this.$message.error('获取聊天室列表失败')
      }).finally(() => {
        this.loading = false
      })
    },

    // 添加聊天室
    handleAdd() {
      this.roomForm = {
        floor: '',
        category: '',
        maxUser: 50,
        isAvailable: true,
        isChat: true
      }
      this.roomDialogVisible = true
      if (this.$refs.roomForm) {
        this.$refs.roomForm.resetFields()
      }
    },

    // 提交聊天室表单
    submitRoomForm() {
      this.$refs.roomForm.validate(valid => {
        if (valid) {
          request.post('/chatRoom', this.roomForm).then(res => {
            if (res.code === '0') {
              this.$message.success('创建聊天室成功')
              this.roomDialogVisible = false
              this.fetchChatRooms()
            } else {
              this.$message.error(res.msg || '创建聊天室失败')
            }
          }).catch(err => {
            //console.error('创建聊天室失败', err)
            this.$message.error('创建聊天室失败')
          })
        } else {
          return false
        }
      })
    },

    // 切换聊天室状态
    toggleRoomStatus(room) {
      const status = !room.isAvailable
      const actionText = status ? '开放' : '关闭'

      this.$confirm(`确认${actionText}该聊天室?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        request.put(`/chatRoom/${room.id}/available?status=${status}`).then(res => {
          if (res.code === '0') {
            this.$message.success(`${actionText}成功`)
            // 更新本地数据
            room.isAvailable = status
          } else {
            this.$message.error(res.msg || `${actionText}失败`)
          }
        }).catch(err => {
          //console.error(`${actionText}聊天室失败`, err)
          this.$message.error(`${actionText}聊天室失败`)
        })
      }).catch(() => {})
    },

    // 切换聊天权限
    toggleChatStatus(room) {
      const status = !room.isChat
      const actionText = status ? '解除禁言' : '禁言'

      this.$confirm(`确认对该聊天室${actionText}?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        request.put(`/chatRoom/${room.id}/chat?status=${status}`).then(res => {
          if (res.code === '0') {
            this.$message.success(`${actionText}成功`)
            // 更新本地数据
            room.isChat = status
          } else {
            this.$message.error(res.msg || `${actionText}失败`)
          }
        }).catch(err => {
          //console.error(`聊天室${actionText}失败`, err)
          this.$message.error(`聊天室${actionText}失败`)
        })
      }).catch(() => {})
    },

    // 删除聊天室
    handleDelete(room) {
      this.$confirm(`确认删除聊天室 "${room.floor} - ${room.category}"?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        request.delete(`/chatRoom/${room.id}`).then(res => {
          if (res.code === '0') {
            this.$message.success('删除成功')
            this.fetchChatRooms()
          } else {
            this.$message.error(res.msg || '删除失败')
          }
        }).catch(err => {
          //console.error('删除聊天室失败', err)
          this.$message.error('删除聊天室失败')
        })
      }).catch(() => {})
    },

    // 查看聊天室用户
    handleViewUsers(room) {
      this.selectedRoom = room
      this.userDialogVisible = true
      this.fetchRoomUsers(room.id)
    },

    // 获取聊天室用户列表
    fetchRoomUsers(roomId) {
      this.userLoading = true
      this.roomUsers = []

      request.get(`/chatRoom/${roomId}/users`).then(res => {
        if (res.code === '0') {
          this.roomUsers = res.data || []
        } else {
          this.$message.error(res.msg || '获取聊天室用户列表失败')
        }
      }).catch(err => {
        //console.error('获取聊天室用户列表失败', err)
        this.$message.error('获取聊天室用户列表失败')
      }).finally(() => {
        this.userLoading = false
      })
    },

    // 切换用户禁言状态
    toggleUserMuteStatus(user) {
      const status = !user.isMuted
      const actionText = status ? '禁言' : '解除禁言'

      this.$confirm(`确认${actionText}用户 "${user.name}"?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        request.put(`/chatRoom/${this.selectedRoom.id}/user/${user.userId}/mute?muted=${status}`).then(res => {
          if (res.code === '0') {
            this.$message.success(`${actionText}成功`)
            // 更新本地数据
            user.isMuted = status
          } else {
            this.$message.error(res.msg || `${actionText}失败`)
          }
        }).catch(err => {
          //console.error(`用户${actionText}失败`, err)
          this.$message.error(`用户${actionText}失败`)
        })
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.chatroom-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.no-data {
  text-align: center;
  margin: 20px 0;
  color: #909399;
}
</style>