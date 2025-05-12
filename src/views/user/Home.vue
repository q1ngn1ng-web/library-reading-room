<template>
  <div>
    <div style="color: #5050bb; font-weight: bold; font-size: 20px">
      欢迎您 {{user.name}}，这里是图书馆阅览室系统 ！
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
      <div style="flex: 1; margin-left: 20px;">
        <el-card class="reservation-card">
          <div slot="header">
            <span>🕒 我的当前预约</span>
            <el-button
              size="mini"
              @click="$router.push('/user/seats')"
              style="float: right; padding: 3px 0"
            >
              {{ activeReservation ? '修改预约' : '去预约' }}
            </el-button>
          </div>

          <el-table
            :data="activeReservation ? [formatReservation(activeReservation)] : []"
            style="width: 100%"
            v-loading="loading"
          >
            <el-table-column label="楼层/座位" width="120">
              <template slot-scope="{row}">
                {{ row.floorNumber }}楼{{ row.seatNumber }}号
              </template>
            </el-table-column>

            <el-table-column label="使用时段">
              <template slot-scope="{row}">
                {{ row.date }} {{ row.timeRange }}
              </template>
            </el-table-column>

            <el-table-column label="状态" width="120">
              <template slot-scope="{row}">
                <el-tag :type="getStatusTagType(row.status)" size="small">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="180" v-if="activeReservation">
              <template slot-scope="{row}">
                <el-button
                  size="mini"
                  type="success"
                  @click="handleCheckIn()"
                  v-if="row.status === 'reserved'"
                >
                  签到
                </el-button>
                <el-button
                  size="mini"
                  type="danger"
                  @click="handleCancelReservation()"
                  v-if="row.status === 'reserved'"
                >
                  取消预约
                </el-button>
                <el-button
                  size="mini"
                  type="warning"
                  @click="handleCheckOut()"
                  v-if="row.status === 'checked_in' || row.status === 'occupied'"
                >
                  退座
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="!activeReservation && !loading" class="empty-tip">
            <i class="el-icon-info"></i> 当前没有进行中的预约
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script>
import request from "@/utils/request";
import { mapState, mapActions } from 'vuex';
import { getStatusText, getStatusTagType } from '@/utils/seatUtils';
import { formatDateTime } from '@/utils/dateUtils';

export default {
  name: 'HomeView',

  data() {
    return {
      activeName: '1',
      data: [],
      user: localStorage.getItem("user") ? JSON.parse(localStorage.getItem("user")) : {},
      admin: localStorage.getItem("admin") ? JSON.parse(localStorage.getItem("admin")) : {},
      baseURL: process.env.VUE_APP_BASE_API,
      loading: false
    };
  },
  computed: {
    ...mapState('seat', ['activeReservation'])
  },
  mounted() {
    this.findNotice();
    this.fetchCurrentReservation();
  },
  methods: {
    ...mapActions('seat', [
      'fetchCurrentReservation',
      'cancelReservation',
      'checkIn',
      'checkOut'
    ]),
    findNotice() {
      request.get("/notice").then(res => {
        if (res.code === '0') {
          this.data = res.data;
          this.activeName = res.data[0]?.id;
        } else {
          this.$message.error(res.msg)
        }
      })
    },
    // 格式化预约信息用于表格显示
    formatReservation(reservation) {
      if (!reservation) return {};

      // 提取日期和时间范围
      let startTime = reservation.startTime ? new Date(reservation.startTime) : null;
      let endTime = reservation.endTime ? new Date(reservation.endTime) : null;

      return {
        id: reservation.reservationId,
        floorNumber: reservation.floorNumber,
        seatNumber: reservation.seatNumber,
        date: startTime ? formatDateTime(startTime, 'yyyy-MM-dd') : '未知日期',
        timeRange: (startTime && endTime)
          ? `${formatDateTime(startTime, 'HH:mm')} - ${formatDateTime(endTime, 'HH:mm')}`
          : '未知时间段',
        status: reservation.status
      };
    },
    // 签到
    async handleCheckIn() {
      try {
        const result = await this.checkIn();
        if (result.success) {
          this.$message.success('签到成功');
          this.fetchCurrentReservation();
        } else {
          this.$message.error(result.message || '签到失败');
        }
      } catch (error) {
        console.error('签到失败:', error);
        this.$message.error('签到失败，请重试');
      }
    },
    // 取消预约
    async handleCancelReservation() {
      try {
        this.$confirm('确定要取消当前预约吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(async () => {
          const result = await this.cancelReservation();
          if (result.success) {
            this.$message.success('预约已取消');
            this.fetchCurrentReservation();
          } else {
            this.$message.error(result.message || '取消预约失败');
          }
        }).catch(() => {});
      } catch (error) {
        console.error('取消预约失败:', error);
        this.$message.error('取消预约失败，请重试');
      }
    },
    // 退座
    async handleCheckOut() {
      try {
        this.$confirm('确定要退座吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(async () => {
          const result = await this.checkOut();
          if (result.success) {
            this.$message.success('已退座');
            this.fetchCurrentReservation();
          } else {
            this.$message.error(result.message || '退座失败');
          }
        }).catch(() => {});
      } catch (error) {
        console.error('退座失败:', error);
        this.$message.error('退座失败，请重试');
      }
    },
    // 导出工具函数
    getStatusText,
    getStatusTagType
  }
}
</script>

<style scoped>
.el-collapse-item__header {
  color: #344675 !important;
}

.reservation-card {
  margin-bottom: 20px;
}

.empty-tip {
  color: #909399;
  text-align: center;
  padding: 20px 0;
  font-size: 14px;
}

.empty-tip i {
  margin-right: 5px;
  font-size: 16px;
}
</style>
