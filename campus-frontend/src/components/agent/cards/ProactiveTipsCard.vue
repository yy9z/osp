<template>
  <div class="tips-card">
    <div class="tips-header">
      <div class="header-title">
        <el-icon><Bell /></el-icon>
        <span>智能提醒</span>
      </div>
      <el-tag v-if="highPriorityCount > 0" size="small" type="danger">
        {{ highPriorityCount }} 条高优先
      </el-tag>
      <el-tag v-else size="small" type="success">状态良好</el-tag>
    </div>

    <div class="tips-body">
      <button
        v-for="tip in tips"
        :key="`${tip.title}-${tip.actionPath}`"
        type="button"
        class="tip-row"
        @click="openTip(tip)"
      >
        <div class="tip-main">
          <div class="tip-title">{{ tip.title || '待处理提醒' }}</div>
          <div class="tip-content">{{ tip.content || '暂无详细说明' }}</div>
        </div>
        <div class="tip-side">
          <el-tag size="small" :type="levelType(tip.level)">
            {{ levelText(tip.level) }}
          </el-tag>
          <el-icon v-if="resolvePath(tip)"><ArrowRight /></el-icon>
        </div>
      </button>

      <div v-if="tips.length === 0" class="empty-state">
        当前没有需要优先处理的校园事务
      </div>
    </div>

    <div class="tips-footer">
      <el-button size="small" type="primary" plain @click="router.push('/')">
        <el-icon><House /></el-icon>
        回到首页
      </el-button>
      <el-button size="small" @click="router.push('/messages')">
        <el-icon><ChatDotRound /></el-icon>
        消息中心
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Bell, ChatDotRound, House } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const props = defineProps({
  data: { type: [Object, Array], required: true }
})

const safeData = computed(() => Array.isArray(props.data) ? {} : (props.data || {}))
const tips = computed(() => Array.isArray(safeData.value.tips) ? safeData.value.tips : [])
const highPriorityCount = computed(() => Number(safeData.value.highPriorityCount || 0))

function levelType(level) {
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  return 'info'
}

function levelText(level) {
  if (level === 'HIGH') return '高优先'
  if (level === 'MEDIUM') return '中优先'
  return '低优先'
}

function resolvePath(tip) {
  const rawPath = String(tip?.actionPath || '').trim()
  if (!rawPath) return ''
  if (rawPath === '/lostfound?tab=my-lost') return '/lostfound/my-lost'
  if (rawPath === '/lostfound?tab=my-found') return '/lostfound/my-found'
  return rawPath
}

async function openTip(tip) {
  const path = resolvePath(tip)
  if (!path) {
    ElMessage.warning('该提醒暂未配置跳转路径')
    return
  }
  await router.push(path)
}
</script>

<style scoped>
.tips-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  margin-top: 8px;
}

.tips-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border-bottom: 1px solid #edf0f5;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #303133;
  font-weight: 600;
  font-size: 14px;
}

.tips-body {
  padding: 10px 12px;
}

.tip-row {
  width: 100%;
  border: 0;
  border-radius: 6px;
  background: #f7f9fc;
  padding: 9px;
  margin: 0 0 6px;
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: left;
  cursor: pointer;
}

.tip-row:hover {
  background: #eef5ff;
}

.tip-main {
  min-width: 0;
  flex: 1;
}

.tip-title {
  color: #303133;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
}

.tip-content {
  color: #606266;
  font-size: 12px;
  line-height: 1.45;
  margin-top: 2px;
}

.tip-side {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
}

.empty-state {
  color: #909399;
  font-size: 13px;
  text-align: center;
  padding: 16px 0;
}

.tips-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 8px 12px 10px;
  border-top: 1px solid #edf0f5;
}
</style>
