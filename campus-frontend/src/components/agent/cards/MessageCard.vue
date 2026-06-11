<template>
  <div class="message-card">
    <div class="message-header">
      <div class="header-title">
        <el-icon><Bell /></el-icon>
        <span>消息动态</span>
      </div>
      <el-tag v-if="unreadCount > 0" size="small" type="danger">{{ unreadCount }} 未读</el-tag>
      <el-tag v-else size="small" type="success">无未读</el-tag>
    </div>

    <div class="message-body">
      <section v-if="notifications.length" class="message-section">
        <div class="section-title">最新通知</div>
        <button
          v-for="item in notifications"
          :key="item.id || `${item.title}-${item.createTime}`"
          type="button"
          class="notification-row"
          @click="openNotification(item)"
        >
          <div class="row-main">
            <div class="row-title">{{ item.title || '系统通知' }}</div>
            <div class="row-content">{{ item.content || '暂无内容' }}</div>
          </div>
          <div class="row-side">
            <span class="row-time">{{ formatTime(item.createTime) }}</span>
            <el-icon v-if="notificationTarget(item)"><ArrowRight /></el-icon>
          </div>
        </button>
      </section>

      <section v-if="conversations.length" class="message-section">
        <div class="section-title">最近会话</div>
        <button
          v-for="item in conversations"
          :key="item.userId"
          type="button"
          class="conversation-row"
          @click="openConversation(item.userId)"
        >
          <el-avatar :size="32" :src="item.avatar">
            {{ displayName(item).charAt(0) || 'U' }}
          </el-avatar>
          <div class="row-main">
            <div class="row-title">
              {{ displayName(item) }}
              <el-tag v-if="item.isPinned === 1" size="small" effect="plain">置顶</el-tag>
            </div>
            <div class="row-content">{{ item.lastMessage || '暂无消息内容' }}</div>
          </div>
          <div class="row-side">
            <el-badge :value="item.unreadCount" :hidden="!item.unreadCount" />
            <span class="row-time">{{ formatTime(item.lastMessageTime) }}</span>
          </div>
        </button>
      </section>

      <div v-if="!hasContent" class="empty-state">暂无通知和最近会话</div>
    </div>

    <div class="message-footer">
      <el-button type="primary" size="small" @click="openMessageCenter">
        <el-icon><ChatDotRound /></el-icon>
        消息中心
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Bell, ChatDotRound } from '@element-plus/icons-vue'

const router = useRouter()
const props = defineProps({
  data: { type: [Object, Array], required: true }
})

const safeData = computed(() => Array.isArray(props.data) ? {} : (props.data || {}))
const notifications = computed(() => Array.isArray(safeData.value.notifications) ? safeData.value.notifications : [])
const conversations = computed(() => Array.isArray(safeData.value.conversations) ? safeData.value.conversations : [])
const unreadCount = computed(() => Number(safeData.value.unreadCount || 0))
const hasContent = computed(() => notifications.value.length > 0 || conversations.value.length > 0)

function displayName(item) {
  return item?.realName || item?.username || `用户 ${item?.userId || ''}`.trim()
}

function formatTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 16).replace('T', ' ')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

function notificationTarget(item) {
  const relatedType = String(item?.relatedType || item?.type || '').toUpperCase()
  const relatedId = item?.relatedId
  if (relatedType === 'SECONDHAND' && relatedId) return `/secondhand/${relatedId}`
  if (relatedType === 'LOSTFOUND') return '/lostfound'
  if (relatedType === 'MESSAGE' && relatedId) return `/messages?userId=${relatedId}`
  return ''
}

function openNotification(item) {
  const target = notificationTarget(item)
  if (target) router.push(target)
}

function openConversation(userId) {
  if (!userId) {
    openMessageCenter()
    return
  }
  router.push({ path: '/messages', query: { userId } })
}

function openMessageCenter() {
  router.push('/messages')
}
</script>

<style scoped>
.message-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  margin-top: 8px;
}

.message-header {
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

.message-body {
  padding: 10px 12px;
}

.message-section + .message-section {
  margin-top: 12px;
}

.section-title {
  color: #909399;
  font-size: 12px;
  margin-bottom: 6px;
}

.notification-row,
.conversation-row {
  width: 100%;
  border: 0;
  border-radius: 6px;
  background: #f7f9fc;
  padding: 8px;
  margin: 0 0 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: left;
  cursor: pointer;
}

.notification-row:hover,
.conversation-row:hover {
  background: #eef5ff;
}

.row-main {
  min-width: 0;
  flex: 1;
}

.row-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #303133;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
}

.row-content {
  color: #606266;
  font-size: 12px;
  line-height: 1.4;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-side {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 11px;
}

.row-time {
  white-space: nowrap;
}

.empty-state {
  color: #909399;
  font-size: 13px;
  text-align: center;
  padding: 16px 0;
}

.message-footer {
  display: flex;
  justify-content: flex-end;
  padding: 8px 12px 10px;
  border-top: 1px solid #edf0f5;
}
</style>
