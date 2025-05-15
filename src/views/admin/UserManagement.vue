<template>
  <div class="user-management">
    <div class="page-header">
      <h2>用户管理</h2>
      <el-button type="primary" @click="handleAdd">添加用户</el-button>
    </div>

    <!-- 搜索栏 -->
    <el-form :inline="true" :model="searchForm" class="search-form">
      <el-form-item label="用户名">
        <el-input v-model="searchForm.name" placeholder="请输入用户名" clearable></el-input>
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="searchForm.role" placeholder="请选择角色" clearable>
          <el-option label="管理员" value="ADMIN"></el-option>
          <el-option label="教师" value="TEACHER"></el-option>
          <el-option label="学生" value="STUDENT"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="电话">
        <el-input v-model="searchForm.phone" placeholder="请输入电话" clearable></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
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
        prop="name"
        label="用户名"
        width="120">
      </el-table-column>
      <el-table-column
        prop="sex"
        label="性别"
        width="60">
      </el-table-column>
      <el-table-column
        prop="age"
        label="年龄"
        width="60">
      </el-table-column>
      <el-table-column
        prop="phone"
        label="电话"
        width="130">
      </el-table-column>
      <el-table-column
        prop="role"
        label="角色"
        width="100">
        <template slot-scope="scope">
          <el-tag :type="getRoleType(scope.row.role)">{{ getRoleName(scope.row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="头像"
        width="80">
        <template slot-scope="scope">
          <el-avatar :size="40" :src="scope.row.avatar"></el-avatar>
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        width="280">
        <template slot-scope="scope">
          <el-button
            size="mini"
            @click="handleEdit(scope.row)">编辑</el-button>
          <el-button
            size="mini"
            type="danger"
            @click="handleDelete(scope.row)">删除</el-button>
          <el-button
            size="mini"
            type="warning"
            @click="handleResetPassword(scope.row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total">
      </el-pagination>
    </div>

    <!-- 添加/编辑用户对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="500px">
      <el-form
        :model="form"
        :rules="rules"
        ref="form"
        label-width="80px">
        <el-form-item label="用户名" prop="name">
          <el-input v-model="form.name"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!form.id">
          <el-input v-model="form.password" type="password"></el-input>
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="form.sex">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="form.age" :min="1" :max="120"></el-input-number>
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone"></el-input>
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色">
            <el-option label="管理员" value="ADMIN"></el-option>
            <el-option label="教师" value="TEACHER"></el-option>
            <el-option label="学生" value="STUDENT"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="头像">
          <el-upload
            class="avatar-uploader"
            action="/api/file/upload"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload">
            <img v-if="form.avatar" :src="form.avatar" class="avatar">
            <i v-else class="el-icon-plus avatar-uploader-icon"></i>
          </el-upload>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'UserManagement',
  data() {
    return {
      // 搜索表单
      searchForm: {
        name: '',
        role: '',
        phone: ''
      },
      // 表格数据
      tableData: [],
      loading: false,
      // 分页
      currentPage: 1,
      pageSize: 10,
      total: 0,
      // 对话框
      dialogVisible: false,
      dialogTitle: '添加用户',
      // 表单
      form: {
        id: null,
        name: '',
        password: '',
        sex: '男',
        age: 20,
        phone: '',
        role: 'STUDENT',
        avatar: ''
      },
      // 表单校验规则
      rules: {
        name: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
        ],
        sex: [
          { required: true, message: '请选择性别', trigger: 'change' }
        ],
        age: [
          { required: true, message: '请输入年龄', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '请输入电话', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
        ],
        role: [
          { required: true, message: '请选择角色', trigger: 'change' }
        ]
      },
      // 上传头像的headers
      uploadHeaders: {
        'token': localStorage.getItem('admin-token') || '',
        'X-Role': 'admin'
      }
    }
  },
  created() {
    this.fetchUsers()
  },
  methods: {
    // 获取用户列表
    fetchUsers() {
      this.loading = true
      request.get('/user/search', {
        params: {
          page: this.currentPage,
          size: this.pageSize,
          ...this.searchForm,
        },
        role: 'admin'
      }).then(res => {
        if (res.code === '0') {
          this.tableData = res.data.list || []
          this.total = res.data.total || 0
        } else {
          this.$message.error(res.msg || '获取用户列表失败')
        }
      }).catch(err => {
        console.error('获取用户列表失败', err)
        this.$message.error('获取用户列表失败')
      }).finally(() => {
        this.loading = false
      })
    },
    // 搜索
    search() {
      this.currentPage = 1
      this.fetchUsers()
    },
    // 重置搜索
    resetSearch() {
      this.searchForm = {
        name: '',
        role: '',
        phone: ''
      }
      this.search()
    },
    // 处理分页大小变化
    handleSizeChange(val) {
      this.pageSize = val
      this.fetchUsers()
    },
    // 处理页码变化
    handleCurrentChange(val) {
      this.currentPage = val
      this.fetchUsers()
    },
    // 添加用户
    handleAdd() {
      this.dialogTitle = '添加用户'
      this.form = {
        id: null,
        name: '',
        password: '',
        sex: '男',
        age: 20,
        phone: '',
        role: 'STUDENT',
        avatar: ''
      }
      this.dialogVisible = true
      if (this.$refs.form) {
        this.$refs.form.resetFields()
      }
    },
    // 编辑用户
    handleEdit(row) {
      this.dialogTitle = '编辑用户'
      this.form = JSON.parse(JSON.stringify(row))
      this.dialogVisible = true
    },
    // 删除用户
    handleDelete(row) {
      this.$confirm('确认删除该用户?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        request.delete(`/user/${row.id}`, { role: 'admin' }).then(res => {
          if (res.code === '0') {
            this.$message.success('删除成功')
            this.fetchUsers()
          } else {
            this.$message.error(res.msg || '删除失败')
          }
        }).catch(err => {
          console.error('删除用户失败', err)
          this.$message.error('删除用户失败')
        })
      }).catch(() => {})
    },
    // 重置密码
    handleResetPassword(row) {
      this.$confirm('确认重置该用户的密码?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        request.put(`/user/${row.id}/resetPassword`, null, { role: 'admin' }).then(res => {
          if (res.code === '0') {
            this.$message.success(`密码重置成功，新密码为: ${res.data}`)
          } else {
            this.$message.error(res.msg || '重置密码失败')
          }
        }).catch(err => {
          console.error('重置密码失败', err)
          this.$message.error('重置密码失败')
        })
      }).catch(() => {})
    },
    // 提交表单
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          request.post('/user', this.form, { role: 'admin' }).then(res => {
            if (res.code === '0') {
              this.$message.success(this.form.id ? '更新成功' : '添加成功')
              this.dialogVisible = false
              this.fetchUsers()
            } else {
              this.$message.error(res.msg || (this.form.id ? '更新失败' : '添加失败'))
            }
          }).catch(err => {
            console.error(this.form.id ? '更新用户失败' : '添加用户失败', err)
            this.$message.error(this.form.id ? '更新用户失败' : '添加用户失败')
          })
        } else {
          return false
        }
      })
    },
    // 上传头像成功回调
    handleAvatarSuccess(res, file) {
      if (res.code === '0') {
        this.form.avatar = res.data
      } else {
        this.$message.error(res.msg || '上传头像失败')
      }
    },
    // 上传头像前的校验
    beforeAvatarUpload(file) {
      const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isJPG) {
        this.$message.error('上传头像图片只能是 JPG 或 PNG 格式!')
      }
      if (!isLt2M) {
        this.$message.error('上传头像图片大小不能超过 2MB!')
      }
      return isJPG && isLt2M
    },
    // 获取角色类型（用于标签颜色）
    getRoleType(role) {
      switch(role) {
        case 'ADMIN':
          return 'danger'
        case 'TEACHER':
          return 'warning'
        case 'STUDENT':
          return 'success'
        default:
          return 'info'
      }
    },
    // 获取角色名称
    getRoleName(role) {
      switch(role) {
        case 'ADMIN':
          return '管理员'
        case 'TEACHER':
          return '教师'
        case 'STUDENT':
          return '学生'
        default:
          return '未知'
      }
    }
  }
}
</script>

<style scoped>
.user-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-form {
  margin-bottom: 20px;
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}
.avatar-uploader .el-upload:hover {
  border-color: #409EFF;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 80px;
  height: 80px;
  line-height: 80px;
  text-align: center;
}
.avatar {
  width: 80px;
  height: 80px;
  display: block;
}
</style>