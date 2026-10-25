<template>
  <div class="change-password">
    <el-form :model="passwordForm" ref="passwordForm">
      <el-form-item label="原密码" label-width="15%">
        <el-input v-model="passwordForm.password" show-password autocomplete="off" style="width: 90%"></el-input>
      </el-form-item>
      <el-form-item label="新密码" label-width="15%">
        <el-input v-model="passwordForm.newPassword" type="password"  show-password autocomplete="off" style="width: 90%"></el-input>
      </el-form-item>
      <el-form-item label="确认密码" label-width="15%">
        <el-input v-model="passwordForm.confirmPassword" type="password"  show-password autocomplete="off" style="width: 90%"></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submitForm">确认修改</el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>

  </div>
</template>

<script>
import request from "@/utils/request";
//使用方法
/* <change-password role="user" :user="userData" />
<change-password role="admin" :admin="adminData" /> */
// 这里的userData和adminData是从父组件传递过来的数据

export default {
  name: 'ChangePassword',
  props: {
    // 用户角色，可以是 'user' 或 'admin'
    userData: {  // 接收父组件传递的用户数据
      type: Object,
      required: true
    },
    role: {
      type: String,
      required: true
    },
  },
  data() {
    return {
      passwordForm: {
        password: '',
        newPassword: '',
        confirmPassword: ''
      },

    }
  },
  computed: {
    // 根据角色返回当前用户对象
    currentUser() {
      if (this.role == null) {
        return;
      }
      return this.userData;
    }
  },
  // 页面加载的时候，做一些事情，在created里面



  // 定义一些页面上控件出发的事件调用的方法
  methods: {
    submitForm() {
      // 原密码取表单输入（不再依赖 localStorage 中缓存的旧密码对象）
      if (!this.passwordForm.password) {
        this.$message.warning("请输入原密码");
        return;
      }
      if (!this.passwordForm.newPassword) {
        this.$message.warning("请输入新密码");
        return;
      }
      if (!this.passwordForm.confirmPassword) {
        this.$message.warning("请确认新密码");
        return;
      }
      if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
        this.$message.warning("两次输入的密码不一致");
        return;
      }

      // 只提交必要字段，原密码由服务端 BCrypt 复核
      const payload = {
        password: this.passwordForm.password,
        newPassword: this.passwordForm.newPassword
      };

      // 根据角色选择不同的API端点
      const apiUrl = this.role === 'admin' ? '/admin/password' : '/user/password';

      request.post(apiUrl, payload).then(res => {
        if (res.code === '0') {
          this.$message.success("修改成功，请重新登录");
          // 密码已变更，清除本地登录态（含token）并回到登录页
          localStorage.removeItem(this.role);
          localStorage.removeItem(`${this.role}-token`);
          this.$router.push("/login");
        } else {
          this.$message.error(res.msg);
        }
      });
    },
    txSuccessUpload(res) {
      // 根据角色设置头像
      if (this.role === 'admin') {
        this.admin.avatar = res.data;
      } else {
        this.user.avatar = res.data;
      }
    },
    resetForm() {
      // 重置表单
      this.$refs.passwordForm.resetFields();
    },
  }
}

</script>

<style scoped>
.change-password {
  max-width: 500px;
  margin: 0 auto;
  padding: 20px;
}
</style>