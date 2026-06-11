<template>
  <div class="repair-card">
    <div class="repair-card-header">
      <el-icon class="repair-icon"><Tools /></el-icon>
      <span>报修工单</span>
      <el-tag :type="tagType" size="small" class="repair-status">{{ statusText }}</el-tag>
    </div>
    <div class="repair-card-body">
      <div class="repair-row">
        <span class="label">工单号</span>
        <span class="value">#{{ data.orderId }}</span>
      </div>
      <div class="repair-row" v-if="data.faultType">
        <span class="label">故障类型</span>
        <span class="value">{{ data.faultType }}</span>
      </div>
      <div class="repair-row" v-if="data.building || data.roomNo">
        <span class="label">宿舍位置</span>
        <span class="value">{{ data.building }} {{ data.roomNo }}</span>
      </div>
      <div class="repair-row" v-if="data.submitTime">
        <span class="label">提交时间</span>
        <span class="value">{{ formatTime(data.submitTime) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Tools } from '@element-plus/icons-vue'

const props = defineProps({
  data: { type: Object, required: true }
})

const statusText = computed(() => {
  const s = props.data.status
  return { PENDING: '等待处理', PROCESSING: '处理中', COMPLETED: '已完成', REJECTED: '已驳回' }[s] || s
})

const tagType = computed(() => {
  const s = props.data.status
  return { PENDING: 'warning', PROCESSING: 'primary', COMPLETED: 'success', REJECTED: 'danger' }[s] || 'info'
})

function formatTime(t) {
  if (!t) return ''
  return t.slice(0, 16).replace('T', ' ')
}
</script>

<style scoped>
.repair-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  overflow: hidden;
  margin-top: 8px;
}
.repair-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-weight: 600;
  font-size: 14px;
}
.repair-icon { font-size: 16px; }
.repair-status { margin-left: auto; }
.repair-card-body { padding: 12px 14px; }
.repair-row {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 13px;
  color: #606266;
}
.repair-row .label { color: #909399; }
.repair-row .value { font-weight: 500; color: #303133; }
</style>
