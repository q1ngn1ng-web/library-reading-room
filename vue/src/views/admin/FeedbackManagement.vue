<template>
  <div class="feedback-management">
    <div class="page-header">
      <h2>举报管理</h2>
      <el-tag v-if="pending > 0" type="danger" size="medium">待处理 {{ pending }} 条</el-tag>
    </div>

    <!-- 状态筛选 -->
    <el-radio-group v-model="statusFilter" size="small" @change="loadData" style="margin-bottom: 12px">
      <el-radio-button label="">全部</el-radio-button>
      <el-radio-button label="0">待处理</el-radio-button>
      <el-radio-button label="1">已处理</el-radio-button>
      <el-radio-button label="2">已驳回</el-radio-button>
    </el-radio-group>

    <el-table v-loading="loading" :data="tableData" border style="width: 100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="类型" width="90">
        <template slot-scope="s">
          <el-tag :type="typeTag(s.row.type)" size="small">{{ typeText(s.row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="楼层" width="70">
        <template slot-scope="s">{{ s.row.roomName ? s.row.roomName + '楼' : '-' }}</template>
      </el-table-column>
      <el-table-column prop="reporterName" label="举报人" width="100" />
      <el-table-column prop="targetName" label="被举报人" width="100">
        <template slot-scope="s">{{ s.row.targetName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="content" label="描述" min-width="140" show-overflow-tooltip />
      <el-table-column label="提交时间" width="150">
        <template slot-scope="s">{{ formatTime(s.row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template slot-scope="s">
          <el-tag :type="['warning','success','info'][s.row.status]" size="small">
            {{ ['待处理','已处理','已驳回'][s.row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template slot-scope="s">
          <template v-if="s.row.status === 0">
            <el-button size="mini" type="primary" @click="handle(s.row, 1)">处理</el-button>
            <el-button size="mini" @click="handle(s.row, 2)">驳回</el-button>
          </template>
          <span v-else style="color:#909399;font-size:12px">{{ s.row.handleNote || '-' }}</span>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 12px; text-align: right"
      background layout="prev, pager, next"
      :page-size="size" :current-page="page"
      :total="total" @current-change="p => { page = p; loadData() }" />
  </div>
</template>

<script>
import { feedbackList, handleFeedback } from '@/api/feedback'

export default {
  name: 'FeedbackManagement',
  data() {
    return { loading: false, tableData: [], page: 1, size: 10, total: 0, pending: 0, statusFilter: '' }
  },
  created() { this.loadData() },
  methods: {
    loadData() {
      this.loading = true
      const params = { page: this.page, size: this.size }
      if (this.statusFilter !== '') params.status = Number(this.statusFilter)
      feedbackList(params).then(res => {
        if (res.code === '0') {
          this.tableData = res.data.page.list || []
          this.total = res.data.page.total || 0
          this.pending = res.data.pending || 0
        } else { this.$message.error(res.msg) }
      }).finally(() => { this.loading = false })
    },
    handle(row, status) {
      this.$prompt(status === 1 ? '处理备注（可选）' : '驳回原因（可选）', '确认' + (status === 1 ? '处理' : '驳回'), {
        confirmButtonText: '确定', cancelButtonText: '取消', inputPlaceholder: '选填'
      }).then(({ value }) => {
        handleFeedback({ id: row.id, status, handleNote: value || '' }).then(res => {
          if (res.code === '0') { this.$message.success('已处理'); this.loadData() }
          else this.$message.error(res.msg)
        })
      }).catch(() => {})
    },
    typeText(t) { return { TALK: '喧哗', EAT: '饮食', SEAT_OCCUPY: '占座', OTHER: '其他' }[t] || t },
    typeTag(t) { return { TALK: 'warning', EAT: 'danger', SEAT_OCCUPY: 'primary', OTHER: 'info' }[t] || 'info' },
    formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN', { hour12: false }) : '-' }
  }
}
</script>

<style scoped>
.feedback-management { padding: 8px; }
.page-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.page-header h2 { margin: 0; }
</style>
