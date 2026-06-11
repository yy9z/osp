<template>
  <div class="page-container messages-page">
    <!-- 会话列表 -->
    <el-card class="conversations-panel">
      <template #header>
        <div class="panel-header">
          <span>消息列表</span>
        </div>
      </template>
      <div class="conversations-list" v-loading="loading">
        <el-empty v-if="!loading && conversations.length === 0" description="暂无消息" />
        <div
          v-else
          v-for="item in conversations"
          :key="item.userId"
          class="conversation-item"
          :class="{ 
            active: currentConversation === item.userId,
            pinned: item.isPinned === 1
          }"
          @click="selectConversation(item)"
        >
          <el-avatar :size="40" :src="item.avatar">{{ item.realName?.charAt(0) || 'U' }}</el-avatar>
          <div class="conversation-info">
            <div class="conversation-header">
              <span class="username">
                {{ item.realName || item.username }}
                <el-icon v-if="item.isPinned === 1" class="pin-icon"><Top /></el-icon>
              </span>
              <span class="time">{{ formatTime(item.lastMessageTime) }}</span>
            </div>
            <div class="last-message">{{ item.lastMessage }}</div>
          </div>
          <el-badge :value="item.unreadCount" :hidden="item.unreadCount === 0" />
          <el-dropdown 
            trigger="click" 
            class="conversation-dropdown"
            @click.stop
          >
            <div class="more-action-trigger" @click.stop>
              <el-icon class="more-btn-icon"><MoreFilled /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handlePin(item)">
                  {{ item.isPinned === 1 ? '取消置顶' : '置顶会话' }}
                </el-dropdown-item>
                <el-dropdown-item @click="handleDelete(item)" divided>
                  删除会话
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-card>

    <!-- 聊天区域 -->
    <el-card class="chat-panel">
      <template #header>
        <div class="panel-header">
          <span>{{ currentChatName || '请选择会话' }}</span>
        </div>
      </template>
      <div class="chat-content" v-if="currentConversation">
        <div class="messages-list" ref="messagesRef">
          <div
            v-for="msg in messages"
            :key="msg.id"
            class="message-item"
            :class="{ own: msg.senderId === userStore.userInfo?.userId }"
          >
            <el-avatar :size="36" class="message-avatar" :src="msg.senderId === userStore.userInfo?.userId ? userStore.userInfo?.avatar : (currentChatUser?.avatar || msg.senderAvatar)">
              {{ msg.senderId === userStore.userInfo?.userId
                ? userStore.userInfo?.realName?.charAt(0)
                : currentChatName?.charAt(0) }}
            </el-avatar>
            <div class="message-content">
              <div class="message-text">{{ msg.content }}</div>
              <div class="message-time">{{ msg.createTime }}</div>
            </div>
          </div>
        </div>
        <div class="chat-input">
          <el-input
            v-model="messageContent"
            placeholder="输入消息内容..."
            @keyup.enter="sendMessage"
          >
            <template #append>
              <el-button @click="sendMessage" :disabled="!messageContent.trim()">
                发送
              </el-button>
            </template>
          </el-input>
        </div>
      </div>
      <div v-else class="empty-chat">
        <el-empty description="请选择会话开始聊天" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { messageApi, userApi } from '@/api'
import { useUserStore, useMessageStore } from '@/stores'
import { messageEvents } from '@/utils/messageEvents'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Top, MoreFilled } from '@element-plus/icons-vue'

const route = useRoute()
const userStore = useUserStore()
const messageStore = useMessageStore()

// 加载状态
const loading = ref(false)

// 会话列表
const conversations = ref([])

// 当前会话ID
const currentConversation = ref(null)

// 当前聊天对象名称
const currentChatName = ref('')

// 当前聊天对象信息
const currentChatUser = ref(null)

// 消息内容
const messageContent = ref('')

// 消息列表
const messages = ref([])
const messageIdSet = ref(new Set())

// 消息容器引用
const messagesRef = ref(null)

// 轮询定时器
let pollTimer = null

const normalizeHistoryMessages = (records = []) => {
  return [...records].reverse()
}

const normalizeIncrementalMessages = (records = []) => {
  return [...records]
}

const rebuildMessageIdSet = (records = []) => {
  const nextSet = new Set()
  records.forEach((msg) => {
    if (msg?.id != null) {
      nextSet.add(String(msg.id))
    }
  })
  messageIdSet.value = nextSet
}

const mergeIncrementalMessages = (records = []) => {
  const incoming = normalizeIncrementalMessages(records)
  const delta = incoming.filter((msg) => {
    if (msg?.id == null) return false
    return !messageIdSet.value.has(String(msg.id))
  })

  if (!delta.length) return false

  delta.forEach((msg) => {
    if (msg?.id != null) {
      messageIdSet.value.add(String(msg.id))
    }
  })
  messages.value.push(...delta)
  return true
}

const getLatestMessageId = () => {
  if (!messages.value.length) return 0
  const last = messages.value[messages.value.length - 1]
  const numeric = Number(last?.id)
  return Number.isFinite(numeric) && numeric > 0 ? numeric : 0
}

// 获取会话列表
const fetchConversations = async () => {
  loading.value = true
  try {
    const res = await messageApi.getConversations()
    conversations.value = res.data || []
    messageStore.setConversations(conversations.value)

    // 检查是否有指定的用户需要开始对话
    const userId = route.query.userId
    if (userId && !currentConversation.value) {
      // 先在本地列表中查找是否已有会话
      const existingConv = conversations.value.find(c => String(c.userId) === String(userId))
      if (existingConv) {
        selectConversation(existingConv)
      } else {
        // 本地没有，创建新会话
        await createNewConversation(userId)
      }
    }
  } catch (error) {
    console.error('获取会话列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 创建新会话
const createNewConversation = async (userId) => {
  // 再次检查本地列表，防止重复添加
  const existingConv = conversations.value.find(c => String(c.userId) === String(userId))
  if (existingConv) {
    selectConversation(existingConv)
    return
  }

  try {
    // 调用后端幂等接口获取或创建会话
    const res = await messageApi.getOrCreateConversation(userId)
    const convData = res.data

    // 检查本地是否已存在（防止并发）
    const checkConv = conversations.value.find(c => String(c.userId) === String(userId))
    if (checkConv) {
      selectConversation(checkConv)
      return
    }

    // 添加到会话列表
    const newConv = {
      userId: Number(userId),
      username: convData.username,
      realName: convData.realName,
      avatar: convData.avatar,
      lastMessage: convData.lastMessage || '',
      lastMessageTime: convData.lastMessageTime || null,
      unreadCount: convData.unreadCount || 0
    }
    conversations.value.unshift(newConv)
    selectConversation(newConv)
  } catch (error) {
    console.error('创建会话失败:', error)
    ElMessage.error('无法开始对话')
  }
}

// 选择会话
const selectConversation = async (item) => {
  currentConversation.value = item.userId
  currentChatName.value = item.realName || item.username
  currentChatUser.value = item

  // 乐观更新：立即清除该会话的未读数
  const unreadBefore = item.unreadCount || 0
  if (unreadBefore > 0) {
    messageStore.clearConversationUnread(item.userId)
    messageEvents.emitClearBadge()
    item.unreadCount = 0
  }

  try {
    const res = await messageApi.getHistory(item.userId, { page: 1, size: 50 })
    messages.value = normalizeHistoryMessages(res.data.records || [])
    rebuildMessageIdSet(messages.value)

    // 标记已读
    await messageApi.markRead(item.userId)

    // 滚动到底部
    await nextTick()
    scrollToBottom()

    // 开始轮询新消息
    startPolling()
  } catch (error) {
    console.error('获取消息历史失败:', error)
  }
}

// 发送消息
const sendMessage = async () => {
  if (!messageContent.value.trim() || !currentConversation.value) return

  try {
    const relatedId = route.query.itemId ? Number(route.query.itemId) : null
    const sendRes = await messageApi.send({
      receiverId: currentConversation.value,
      content: messageContent.value.trim(),
      relatedType: relatedId ? 'SECONDHAND' : null,
      relatedId: relatedId || null
    })

    // 添加到消息列表
    const messageId = sendRes?.data?.messageId || Date.now()
    messages.value.push({
      id: messageId,
      senderId: userStore.userInfo.userId,
      content: messageContent.value.trim(),
      createTime: new Date().toLocaleString()
    })
    messageIdSet.value.add(String(messages.value[messages.value.length - 1].id))

    messageContent.value = ''

    // 滚动到底部
    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送失败，请重试')
  }
}

const applyDraftFromRoute = () => {
  const draft = route.query.draft
  if (typeof draft === 'string' && draft.trim()) {
    messageContent.value = draft.trim()
  }
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

// 开始轮询新消息
const startPolling = () => {
  // 清除之前的轮询
  if (pollTimer) {
    clearInterval(pollTimer)
  }

  // 每3秒轮询一次新消息
  pollTimer = setInterval(async () => {
    if (!currentConversation.value) return

    try {
      const sinceId = getLatestMessageId()
      const res = await messageApi.getHistorySince(currentConversation.value, { sinceId, size: 20 })
      const hasDelta = mergeIncrementalMessages(res.data || [])
      if (hasDelta) {
        await nextTick()
        scrollToBottom()
      }
    } catch (error) {
      console.error('轮询消息失败:', error)
    }
  }, 3000)
}

// 停止轮询
const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

// 置顶/取消置顶会话
const handlePin = async (item) => {
  try {
    const newPinned = item.isPinned === 1 ? false : true
    await messageApi.pinConversation(item.userId, newPinned)
    ElMessage.success(newPinned ? '置顶成功' : '取消置顶成功')
    await fetchConversations()
  } catch (error) {
    console.error('置顶操作失败:', error)
    ElMessage.error(error.message || '操作失败')
  }
}

// 删除会话
const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm(
      '确定删除该会话吗？这将永久删除所有聊天记录，且不可恢复。',
      '删除会话',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    // 先清除该会话的未读数
    if (item.unreadCount > 0) {
      messageStore.clearConversationUnread(item.userId)
    }
    
    await messageApi.deleteConversation(item.userId)
    
    // 立即从本地列表中移除该会话
    conversations.value = conversations.value.filter(c => c.userId !== item.userId)
    
    // 同步更新 Store
    messageStore.setConversations(conversations.value)
    
    // 如果删除的是当前会话，清空聊天窗口
    if (currentConversation.value === item.userId) {
      currentConversation.value = null
      currentChatName.value = ''
      currentChatUser.value = null
      messages.value = []
      messageIdSet.value = new Set()
      stopPolling()
    }
    
    ElMessage.success('会话已删除')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除会话失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 格式化时间显示
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  // 当天消息显示时间
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }

  // 昨天
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) {
    return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }

  // 一周内
  if (diff < 7 * 24 * 60 * 60 * 1000) {
    const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    return weekDays[date.getDay()] + ' ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }

  // 更早
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// 组件挂载时获取会话列表
onMounted(() => {
  applyDraftFromRoute()
  fetchConversations()
})

// 组件卸载时停止轮询
onUnmounted(() => {
  stopPolling()
})

// 监听消息变化，自动滚动到底部
watch(() => messages.value.length, () => {
  scrollToBottom()
})

watch(() => route.query.draft, () => {
  applyDraftFromRoute()
})
</script>

<style scoped>
.page-container {
  display: flex;
  gap: 20px;
  height: calc(100vh - 140px);
}

.conversations-panel {
  width: 320px;
  display: flex;
  flex-direction: column;
}

.conversations-panel :deep(.el-card__body) {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

.conversations-list {
  height: 100%;
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 15px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.3s;
  position: relative;
}

.conversation-item:hover {
  background: #f5f7fa;
}

.conversation-item.active {
  background: #ecf5ff;
}

.conversation-item.pinned {
  background: rgba(0, 65, 145, 0.05);
}

.conversation-item.pinned:hover {
  background: rgba(0, 65, 145, 0.08);
}

.conversation-item.pinned.active {
  background: rgba(0, 65, 145, 0.12);
}

.pin-icon {
  color: var(--ustc-primary);
  margin-left: 4px;
  font-size: 12px;
}

.conversation-dropdown {
  opacity: 0;
  transition: opacity 0.2s;
  margin-left: 8px;
}

.conversation-item:hover .conversation-dropdown {
  opacity: 1;
}

.more-action-trigger {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: transparent;
  cursor: pointer;
  transition: all 0.3s ease;
}

.more-action-trigger:hover {
  background: rgba(0, 65, 145, 0.1);
}

.more-btn-icon {
  font-size: 20px;
  width: 20px;
  height: 20px;
  color: #909399;
  transition: color 0.3s ease;
}

.more-action-trigger:hover .more-btn-icon {
  color: var(--ustc-primary);
}

.conversation-info {
  flex: 1;
  margin-left: 12px;
  overflow: hidden;
}

.conversation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.username {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.time {
  font-size: 12px;
  color: #999;
}

.last-message {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-panel :deep(.el-card__header) {
  flex-shrink: 0;
  padding: 15px 20px;
  border-bottom: 1px solid #ebeef5;
}

.chat-panel :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
  min-height: 0;
}

.chat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.messages-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  min-height: 0;
}

.messages-list::-webkit-scrollbar {
  width: 6px;
}

.messages-list::-webkit-scrollbar-track {
  background: transparent;
}

.messages-list::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.messages-list::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
  align-items: flex-start;
}

.message-item.own {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  max-width: 70%;
  margin: 0 12px;
}

.message-item.own .message-content {
  text-align: right;
}

.message-text {
  display: inline-block;
  padding: 10px 15px;
  border-radius: 8px;
  background: #f5f7fa;
  color: #333;
  font-size: 14px;
  line-height: 1.5;
}

.message-item.own .message-text {
  background: #409EFF;
  color: #fff;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.chat-input {
  flex-shrink: 0;
  padding: 15px;
  border-top: 1px solid #eee;
  background: #fff;
}

.empty-chat {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-input :deep(.el-input-group__append) {
  background: #409EFF;
  border-color: #409EFF;
  color: #fff;
}

.empty-chat {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 768px) {
  .page-container {
    flex-direction: column;
    height: auto;
  }

  .conversations-panel {
    width: 100%;
    max-height: 300px;
  }
}
</style>
