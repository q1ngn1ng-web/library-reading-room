<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
import { mapActions } from 'vuex'

export default {
  name: 'App',
  methods: {
    ...mapActions('seat', ['fetchCurrentReservation']),
  },
  created() {
    // 应用启动时获取当前预约状态，确保聊天室访问权限正确设置
    this.fetchCurrentReservation().catch(error => {
      console.error('获取当前预约状态失败:', error)
    })
  }
}
</script>

<style>

</style>