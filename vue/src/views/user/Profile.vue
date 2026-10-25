<template>
  <div style="width: 50%; margin-top: 20px">
    <el-form :model="user">
      <el-form-item label="头像" label-width="15%">
        <el-upload :action="baseURL+'/files/upload'" ref="tx" :on-success="txSuccessUpload">
          <el-button size="small" type="success">点击上传</el-button>
        </el-upload>
      </el-form-item>
      <el-form-item label="姓名" label-width="15%">
        <el-input v-model="user.username" disabled autocomplete="off" style="width: 90%"></el-input>
      </el-form-item>
      <el-form-item label="昵称" label-width="15%">
        <el-input v-model="user.nickname" autocomplete="off" style="width: 90%"></el-input>
      </el-form-item>
      <el-form-item label="性别" label-width="15%">
        <el-radio v-model="user.sex" label="男">男</el-radio>
        <el-radio v-model="user.sex" label="女">女</el-radio>
      </el-form-item>
      <el-form-item label="年龄" label-width="15%">
        <el-input v-model="user.age" autocomplete="off" style="width: 90%"></el-input>
      </el-form-item>
      <el-form-item label="电话" label-width="15%">
        <el-input v-model="user.phone" autocomplete="off" style="width: 90%"></el-input>
      </el-form-item>
    </el-form>
    <div style="text-align: center;">
      <el-button type="primary" @click="submit()">保 存</el-button>
    </div>
  </div>
</template>

<script>
import request from "@/utils/request";

export default {
  props: {
    userData: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      user: this.getUserData(),
      baseURL: process.env.VUE_APP_BASE_API,
    }
  },
  created() {
    // 确保我们有最新的用户数据
    this.refreshUserData();
  },
  methods: {
    getUserData() {
      // 如果有传入的用户数据，优先使用它
      if (this.userData && Object.keys(this.userData).length > 0) {
        return JSON.parse(JSON.stringify(this.userData)); // 深拷贝避免直接修改props
      }
      // 否则从localStorage获取
      const storedUser = localStorage.getItem("user");
      return storedUser ? JSON.parse(storedUser) : {};
    },
    refreshUserData() {
      // 重新从localStorage获取最新数据
      const storedUser = localStorage.getItem("user");
      if (storedUser) {
        this.user = JSON.parse(storedUser);
      }
    },
    submit() {
      request.post("/user", this.user).then(res => {
        if (res.code === '0') {
          // 更新本地存储
          localStorage.setItem("user", JSON.stringify(this.user));

          // 通知父组件更新用户信息
          this.$emit('profile-updated');

          // 全局事件总线通知应用其他部分
          if (this.$bus) {
            this.$bus.$emit('updateAvatar', this.user.avatar);
            this.$bus.$emit('user-updated');
          }

          this.$message.success("个人信息更新成功");
        } else {
          this.$message.error(res.msg || "更新失败");
        }
      }).catch(err => {
        console.error("更新个人信息失败:", err);
        this.$message.error("更新失败，请稍后重试");
      });
    },
    txSuccessUpload(res) {
      if (res.code === '0' && res.data) {
        this.user.avatar = res.data;
      } else {
        this.$message.error("头像上传失败");
      }
    }
  }
}
</script>

<style scoped>
.el-form {
  margin-bottom: 20px;
}
</style>
