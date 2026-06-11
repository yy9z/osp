<template>
  <div class="context-panel">
    <div class="panel-header">
      <span class="panel-title">理解状态</span>
      <el-tag v-if="intent" size="small" :type="intentTagType" effect="dark">
        {{ intentLabel }}
      </el-tag>
    </div>

    <!-- 已识别意图 -->
    <div v-if="intent" class="section">
      <div class="section-label">意图识别</div>
      <div class="intent-chip">
        <el-icon><Cpu /></el-icon>
        <span>{{ intentLabel }}</span>
      </div>
    </div>

    <!-- 已提取参数 -->
    <div v-if="hasSlots" class="section">
      <div class="section-label">已提取参数</div>
      <div class="slot-list">
        <div
          v-for="(value, key) in filledSlots"
          :key="key"
          class="slot-item filled"
        >
          <span class="slot-key">{{ slotLabel(key) }}</span>
          <span class="slot-value">{{ value }}</span>
        </div>
      </div>
    </div>

    <!-- 待补充参数 -->
    <div v-if="pendingAskFor" class="section">
      <div class="section-label">待补充参数</div>
      <div class="slot-item pending">
        <el-icon><QuestionFilled /></el-icon>
        <span>{{ slotLabel(pendingAskFor) }}</span>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!intent" class="empty-state">
      <el-icon size="32" color="#c0c4cc"><ChatRound /></el-icon>
      <p>向助手发送消息后，这里将显示意图识别和参数提取的实时状态。</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Cpu, QuestionFilled, ChatRound } from '@element-plus/icons-vue'

const props = defineProps({
  intent: { type: String, default: null },
  extractedSlots: { type: Object, default: () => ({}) },
  pendingAskFor: { type: String, default: null }
})

const INTENT_LABELS = {
  DORM_REPAIR: '宿舍报修',
  DORM_QUERY: '宿舍查询',
  REPAIR_QUERY: '工单查询',
  SECONDHAND_SEARCH: '二手搜索',
  SECONDHAND_PUBLISH: '发布商品',
  LOSTFOUND_LOST: '登记失物',
  LOSTFOUND_FOUND: '登记拾物',
  NAVIGATION: '校园导航',
  MESSAGE_QUERY: '消息查询',
  GENERAL_CHAT: '通用问答',
  UNKNOWN: '未知意图'
}

const SLOT_LABELS = {
  fault_type: '故障类型',
  dorm_no: '宿舍号',
  description: '详细描述',
  keyword: '关键词',
  category: '商品分类',
  max_price: '价格上限',
  sort_preference: '排序偏好',
  destination: '目的地',
  origin: '出发地',
  item_name: '物品名称',
  color: '颜色',
  location: '地点',
  time: '时间'
}

const intentLabel = computed(() => INTENT_LABELS[props.intent] || props.intent)

const intentTagType = computed(() => {
  if (props.intent === 'GENERAL_CHAT') return 'primary'
  if (props.intent === 'UNKNOWN') return 'info'
  if (props.pendingAskFor) return 'warning'
  return 'success'
})

const filledSlots = computed(() => {
  const result = {}
  for (const [k, v] of Object.entries(props.extractedSlots || {})) {
    if (v !== null && v !== undefined && v !== '') {
      result[k] = v
    }
  }
  return result
})

const hasSlots = computed(() => Object.keys(filledSlots.value).length > 0)

function slotLabel(key) {
  return SLOT_LABELS[key] || key
}
</script>

<style scoped>
.context-panel {
  height: 100%;
  padding: 16px;
  overflow-y: auto;
  background: #fff;
  border-left: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-label {
  font-size: 11px;
  color: #909399;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.intent-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 8px;
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 500;
}

.slot-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.slot-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 12px;
  gap: 8px;
}

.slot-item.filled {
  background: #f0f9eb;
  color: #67c23a;
}

.slot-key {
  color: #606266;
  font-weight: 500;
  min-width: 70px;
}

.slot-value {
  color: #303133;
  font-weight: 600;
  text-align: right;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.slot-item.pending {
  background: #fdf6ec;
  color: #e6a23c;
  gap: 8px;
  justify-content: flex-start;
  font-weight: 500;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  text-align: center;
  padding: 24px;
}

.empty-state p {
  font-size: 12px;
  color: #c0c4cc;
  line-height: 1.6;
  margin: 0;
}
</style>
