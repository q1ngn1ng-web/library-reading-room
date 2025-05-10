<template>
  <div>
    <div style="color: #5050bb; font-weight: bold; font-size: 20px">
      欢迎您 {{user.name}}，这里是 图书馆阅览室系统 ！
    </div>
    <div style="display: flex; margin-top: 20px">
      <div style="flex: 1">
        <div style="margin-bottom: 15px; font-weight: bold; font-size: 18px; color: #344675">系统公告</div>
        <el-collapse v-model="activeName" accordion>
          <el-collapse-item v-for="item in data" :title="item.name" :name="item.id">
            <template slot="title">
              <i class="header-icon el-icon-info"> {{item.name}} </i>
            </template>
            <div style="padding: 0 20px">{{item.content}}</div>
          </el-collapse-item>
        </el-collapse>
      </div>
      <div style="flex: 1;">
      </div>
    </div>
  </div>
</template>

<script>

import request from "@/utils/request";

export default {
  name: 'HomeView',

  data() {
    return {
      activeName: '1',
      data: [],
      user: localStorage.getItem("user") ? JSON.parse(localStorage.getItem("user")) : {},
      admin: localStorage.getItem("admin") ? JSON.parse(localStorage.getItem("admin")) : {},
      baseURL: process.env.VUE_APP_BASE_API,
    };
  },
  mounted() {
    this.findNotice();
  },
  methods: {
    findNotice() {
      request.get("/notice").then(res => {
        if (res.code === '0') {
          this.data = res.data;
          this.activeName = res.data[0].id;
        } else {
          this.$message.error(res.msg)
        }
      })
    }
  }
}
</script>

<style scoped>
.el-collapse-item__header {
  color: #344675 !important;
}
</style>
