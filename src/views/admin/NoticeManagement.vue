<template>
  <div class="notice-management">
    <div class="page-header">
      <h2>通知管理</h2>
      <el-button type="primary" @click="handleAdd">添加通知</el-button>
    </div>

    <!-- 搜索栏 -->
    <el-form :inline="true" :model="searchForm" class="search-form">
      <el-form-item label="标题">
        <el-input v-model="searchForm.name" placeholder="请输入通知标题" clearable></el-input>
      </el-form-item>
      <el-form-item label="内容">
        <el-input v-model="searchForm.content" placeholder="请输入通知内容" clearable></el-input>
      </el-form-item>
      <el-form-item label="时间">
        <el-date-picker
          v-model="searchForm.time"
          type="date"
          placeholder="选择日期"
          format="yyyy-MM-dd"
          value-format="yyyy-MM-dd">
        </el-date-picker>
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
        label="标题"
        width="200">
      </el-table-column>
      <el-table-column
        prop="content"
        label="内容">
        <template slot-scope="scope">
          <div class="content-cell">{{ scope.row.content }}</div>
        </template>
      </el-table-column>
      <el-table-column
        prop="time"
        label="发布时间"
        width="180">
      </el-table-column>
      <el-table-column
        label="操作"
        width="150">
        <template slot-scope="scope">
          <el-button
            size="mini"
            @click="handleEdit(scope.row)">编辑</el-button>
          <el-button
            size="mini"
            type="danger"
            @click="handleDelete(scope.row)">删除</el-button>
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

    <!-- 添加/编辑通知对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="600px">
      <el-form
        :model="form"
        :rules="rules"
        ref="form"
        label-width="80px">
        <el-form-item label="标题" prop="name">
          <el-input v-model="form.name" placeholder="请输入通知标题"></el-input>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            type="textarea"
            :rows="6"
            placeholder="请输入通知内容"
            v-model="form.content">
          </el-input>
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
  name: 'NoticeManagement',
  data() {
    return {
      // 搜索表单
      searchForm: {
        name: '',
        content: '',
        time: ''
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
      dialogTitle: '添加通知',
      // 表单
      form: {
        id: null,
        name: '',
        content: '',
        time: null
      },
      // 表单校验规则
      rules: {
        name: [
          { required: true, message: '请输入通知标题', trigger: 'blur' },
          { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
        ],
        content: [
          { required: true, message: '请输入通知内容', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.fetchNotices()
  },
  methods: {
    // 获取通知列表
    fetchNotices() {
      this.loading = true
      request.get('/notice/search', {
        params: {
          page: this.currentPage,
          size: this.pageSize,
          ...this.searchForm
        }
      }).then(res => {
        if (res.code === '0') {
          this.tableData = res.data.list || []
          this.total = res.data.total || 0
        } else {
          this.$message.error(res.msg || '获取通知列表失败')
        }
      }).catch(err => {
        //console.error('获取通知列表失败', err)
        this.$message.error('获取通知列表失败')
      }).finally(() => {
        this.loading = false
      })
    },
    // 搜索
    search() {
      this.currentPage = 1
      this.fetchNotices()
    },
    // 重置搜索
    resetSearch() {
      this.searchForm = {
        name: '',
        content: '',
        time: ''
      }
      this.search()
    },
    // 处理分页大小变化
    handleSizeChange(val) {
      this.pageSize = val
      this.fetchNotices()
    },
    // 处理页码变化
    handleCurrentChange(val) {
      this.currentPage = val
      this.fetchNotices()
    },
    // 添加通知
    handleAdd() {
      this.dialogTitle = '添加通知'
      this.form = {
        id: null,
        name: '',
        content: '',
        time: null
      }
      this.dialogVisible = true
      if (this.$refs.form) {
        this.$refs.form.resetFields()
      }
    },
    // 编辑通知
    handleEdit(row) {
      this.dialogTitle = '编辑通知'
      this.form = JSON.parse(JSON.stringify(row))
      this.dialogVisible = true
    },
    // 删除通知
    handleDelete(row) {
      this.$confirm('确认删除该通知?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        request.delete(`/notice/${row.id}`).then(res => {
          if (res.code === '0') {
            this.$message.success('删除成功')
            this.fetchNotices()
          } else {
            this.$message.error(res.msg || '删除失败')
          }
        }).catch(err => {
          //console.error('删除通知失败', err)
          this.$message.error('删除通知失败')
        })
      }).catch(() => {})
    },
    // 提交表单
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          request.post('/notice', this.form).then(res => {
            if (res.code === '0') {
              this.$message.success(this.form.id ? '更新成功' : '添加成功')
              this.dialogVisible = false
              this.fetchNotices()
            } else {
              this.$message.error(res.msg || (this.form.id ? '更新失败' : '添加失败'))
            }
          }).catch(err => {
            //console.error(this.form.id ? '更新通知失败' : '添加通知失败', err)
            this.$message.error(this.form.id ? '更新通知失败' : '添加通知失败')
          })
        } else {
          return false
        }
      })
    }
  }
}
</script>

<style scoped>
.notice-management {
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

.content-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
</style>