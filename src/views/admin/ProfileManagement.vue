<template>
  <div class="profile-management">
    <h2>个人中心</h2>

    <el-row :gutter="20">
      <!-- 左侧头像卡片 -->
      <el-col :span="8">
        <el-card shadow="hover" class="avatar-card">
          <div class="avatar-container">
            <el-avatar :size="120" :src="form.avatar || defaultAvatar"></el-avatar>
            <h3>{{ form.name || '管理员' }}</h3>
            <p>{{ form.role || 'ADMIN' }}</p>
          </div>

          <el-upload
            class="avatar-uploader"
            action="/api/admin/avatar"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload">
            <el-button type="primary" size="small">更换头像</el-button>
          </el-upload>
        </el-card>
      </el-col>

      <!-- 右侧信息表单 -->
      <el-col :span="16">
        <el-card shadow="hover">
          <div slot="header">
            <span>基本信息</span>
          </div>

          <el-form :model="form" :rules="rules" ref="form" label-width="80px">
            <el-form-item label="用户名" prop="name">
              <el-input v-model="form.name"></el-input>
            </el-form-item>

            <el-form-item label="性别" prop="sex">
              <el-radio-group v-model="form.sex">
                <el-radio label="男">男</el-radio>
                <el-radio label="女">女</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="年龄" prop="age">
              <el-input-number v-model="form.age" :min="18" :max="100"></el-input-number>
            </el-form-item>

            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone"></el-input>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="updateProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="hover" style="margin-top: 20px;">
          <div slot="header">
            <span>修改密码</span>
          </div>

          <el-form :model="passwordForm" :rules="passwordRules" ref="passwordForm" label-width="100px">
            <el-form-item label="原密码" prop="password">
              <el-input v-model="passwordForm.password" type="password" show-password></el-input>
            </el-form-item>

            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password></el-input>
            </el-form-item>

            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password></el-input>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="updatePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'ProfileManagement',
  props: {
    adminInfo: {
      type: Object,
      required: true
    }
  },
  data() {
    // 密码确认验证
    const validatePass = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请再次输入密码'))
      } else if (value !== this.passwordForm.newPassword) {
        callback(new Error('两次输入密码不一致!'))
      } else {
        callback()
      }
    }

    return {
      defaultAvatar: require('@/assets/default-avatar.png'),
      form: {
        id: null,
        name: '',
        sex: '男',
        age: 30,
        phone: '',
        avatar: '',
        role: 'ADMIN'
      },
      rules: {
        name: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
        ],
        sex: [
          { required: true, message: '请选择性别', trigger: 'change' }
        ],
        age: [
          { required: true, message: '请输入年龄', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
        ]
      },
      passwordForm: {
        id: null,
        password: '',
        newPassword: '',
        confirmPassword: ''
      },
      passwordRules: {
        password: [
          { required: true, message: '请输入原密码', trigger: 'blur' },
          { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, validator: validatePass, trigger: 'blur' }
        ]
      },
      uploadHeaders: {
        'Authorization': localStorage.getItem('admin-token') || ''
      }
    }
  },
  created() {
    // 初始化表单数据
    if (this.adminInfo) {
      this.form = { ...this.adminInfo }
      this.passwordForm.id = this.adminInfo.id
    }
  },
  methods: {
    // 更新个人信息
    updateProfile() {
      this.$refs.form.validate(valid => {
        if (valid) {
          request.put(`/admin/update/${this.form.id}`, this.form).then(res => {
            if (res.code === '0') {
              this.$message.success('个人信息更新成功')
              // 更新 localStorage 中的管理员信息
              const updatedInfo = { ...this.form }
              this.$emit('info-updated', updatedInfo)
            } else {
              this.$message.error(res.msg || '更新失败')
            }
          }).catch(err => {
            console.error('更新个人信息失败', err)
            this.$message.error('更新个人信息失败')
          })
        } else {
          return false
        }
      })
    },
    // 修改密码
    updatePassword() {
      this.$refs.passwordForm.validate(valid => {
        if (valid) {
          request.post('/admin/password', this.passwordForm).then(res => {
            if (res.code === '0') {
              this.$message.success('密码修改成功')
              // 清空表单
              this.passwordForm.password = ''
              this.passwordForm.newPassword = ''
              this.passwordForm.confirmPassword = ''
              this.$refs.passwordForm.resetFields()
            } else {
              this.$message.error(res.msg || '密码修改失败')
            }
          }).catch(err => {
            console.error('密码修改失败', err)
            this.$message.error('密码修改失败')
          })
        } else {
          return false
        }
      })
    },
    // 上传头像成功回调
    handleAvatarSuccess(res) {
      if (res.code === '0') {
        this.form.avatar = res.data
        // 更新 localStorage 中的管理员信息
        const updatedInfo = { ...this.form }
        this.$emit('info-updated', updatedInfo)
        this.$message.success('头像上传成功')
      } else {
        this.$message.error(res.msg || '头像上传失败')
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
    }
  }
}
</script>

<style scoped>
.profile-management {
  padding: 20px;
}

.avatar-card {
  text-align: center;
}

.avatar-container {
  padding: 20px 0;
}

.avatar-container h3 {
  margin: 15px 0 5px;
}

.avatar-container p {
  margin: 0;
  color: #909399;
}

.avatar-uploader {
  margin-top: 15px;
}
</style>