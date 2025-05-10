<template>
  <div class="chat-room-admin">
    <div class="admin-header">
      <h2>聊天室管理</h2>
      <el-button type="primary" @click="showCreateDialog">创建聊天室</el-button>
    </div>

    <el-card class="admin-card">
      <el-table
        :data="chatRooms"
        style="width: 100%"
        border
        v-loading="loading"
      >
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="floor" label="楼层" width="100"></el-table-column>
        <el-table-column prop="category" label="学科类型"></el-table-column>
        <el-table-column label="使用情况" width="160">
          <template slot-scope="scope">
            {{ scope.row.currentUser }}/{{ scope.row.maxUser }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template slot-scope="scope">
            <el-switch
              v-model="scope.row.status"
              active-color="#13ce66"
              inactive-color="#ff4949"
              @change="handleStatusChange(scope.row)"
            ></el-switch>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template slot-scope="scope">
            <el-button
              size="mini"
              @click="viewRoom(scope.row)"
            >
              查看
            </el-button>
            <el-button
              size="mini"
              type="danger"
              @click="confirmDeleteRoom(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建聊天室对话框 -->
    <el-dialog title="创建聊天室" :visible.sync="dialogVisible" width="30%">
      <el-form :model="roomForm" :rules="rules" ref="roomForm" label-width="80px">
        <el-form-item label="楼层号" prop="floor">
          <el-input-number v-model="roomForm.floor" :min="1" controls-position="right"></el-input-number>
        </el-form-item>
        <el-form-item label="学科类型" prop="category">
          <el-input v-model="roomForm.category" placeholder="请输入学科类型"></el-input>
        </el-form-item>
        <el-form-item label="最大人数" prop="maxUser">
          <el-input-number v-model="roomForm.maxUser" :min="5" :max="100" controls-position="right"></el-input-number>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="roomForm.status" active-text="开放" inactive-text="关闭"></el-switch>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitCreateRoom">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { mapActions, mapGetters } from 'vuex'

export default {
  name: 'ChatRoomAdmin',
  data() {
    return {
      loading: false,
      dialogVisible: false,
      roomForm: {
        floor: 1,
        category: '',
        maxUser: 50,
        status: true
      },
      rules: {
        floor: [
          { required: true, message: '请输入楼层号', trigger: 'blur' },
          { type: 'number', message: '楼层号必须为数字' }
        ],
        category: [
          { required: true, message: '请输入学科类型', trigger: 'blur' },
          { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
        ],
        maxUser: [
          { required: true, message: '请输入最大人数', trigger: 'blur' },
          { type: 'number', message: '最大人数必须为数字' }
        ]
      }
    }
  },
  computed: {
    ...mapGetters('chat', ['getChatRooms']),
    chatRooms() {
      return this.getChatRooms
    }
  },
  methods: {
    ...mapActions('chat', [
      'fetchChatRooms',
      'createChatRoom',
      'updateRoomStatus',
      'deleteChatRoom'
    ]),
    async loadRooms() {
      this.loading = true
      try {
        await this.fetchChatRooms()
      } catch (error) {
        this.$message.error('获取聊天室列表失败')
      } finally {
        this.loading = false
      }
    },
    showCreateDialog() {
      this.dialogVisible = true
      if (this.$refs.roomForm) {
        this.$refs.roomForm.resetFields()
      }
    },
    submitCreateRoom() {
      this.$refs.roomForm.validate(async (valid) => {
        if (valid) {
          try {
            const res = await this.createChatRoom(this.roomForm)
            if (res.code === '0') {
              this.$message.success('创建聊天室成功')
              this.dialogVisible = false
              this.loadRooms()
            } else {
              this.$message.error(res.msg || '创建聊天室失败')
            }
          } catch (error) {
            this.$message.error('创建聊天室失败')
          }
        }
      })
    },
    async handleStatusChange(room) {
      try {
        const res = await this.updateRoomStatus({
          roomId: room.id,
          status: room.status
        })
        if (res.code === '0') {
          this.$message.success(`聊天室状态已${room.status ? '开启' : '关闭'}`)
        } else {
          // 恢复原状态
          room.status = !room.status
          this.$message.error(res.msg || '更新状态失败')
        }
      } catch (error) {
        // 恢复原状态
        room.status = !room.status
        this.$message.error('更新状态失败')
      }
    },
    viewRoom(room) {
      this.$router.push(`/chat-room/${room.id}`)
    },
    confirmDeleteRoom(room) {
      this.$confirm('确定要删除该聊天室吗？删除后不可恢复', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.deleteRoom(room)
      }).catch(() => {})
    },
    async deleteRoom(room) {
      try {
        const res = await this.deleteChatRoom(room.id)
        if (res.code === '0') {
          this.$message.success('删除聊天室成功')
          this.loadRooms()
        } else {
          this.$message.error(res.msg || '删除聊天室失败')
        }
      } catch (error) {
        this.$message.error('删除聊天室失败')
      }
    }
  },
  created() {
    // 验证是否是管理员
    const adminToken = localStorage.getItem('admin-token')
    if (!adminToken) {
      this.$router.push('/login')
      return
    }

    this.loadRooms()
  }
}
</script>

<style scoped>
.chat-room-admin {
  padding: 20px;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.admin-header h2 {
  margin: 0;
}

.admin-card {
  margin-bottom: 20px;
}
</style>