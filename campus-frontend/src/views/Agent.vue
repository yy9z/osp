<template>
  <div class="agent-page">
    <!-- 页面头部 -->
    <div class="agent-header">
      <div class="header-left">
        <div class="agent-avatar-wrap">
          <span class="agent-emoji">🤖</span>
        </div>
        <div>
          <h2 class="agent-title">校园智能助手</h2>
          <p class="agent-sub">自然语言驱动，一句话搞定校园事务</p>
        </div>
      </div>
      <el-button
        size="small"
        plain
        @click="handleNewSession"
        :loading="agentStore.loading"
      >
        <el-icon><Plus /></el-icon>
        新建对话
      </el-button>
    </div>

    <!-- 主体：历史会话 + 对话 + 上下文 -->
    <div class="agent-body">
      <!-- 会话历史 -->
      <div class="history-column">
        <div class="history-header">历史会话</div>
        <div v-if="agentStore.sessions.length === 0" class="history-empty">暂无历史会话</div>
        <div
          v-for="item in agentStore.sessions"
          :key="item.sessionId"
          role="button"
          tabindex="0"
          class="history-item"
          :class="{ active: agentStore.activeSessionId === item.sessionId }"
          @click="handleOpenSession(item.sessionId)"
          @keydown.enter.prevent="handleOpenSession(item.sessionId)"
          @keydown.space.prevent="handleOpenSession(item.sessionId)"
        >
          <div class="history-title-row">
            <div class="history-title">{{ item.title || '新会话' }}</div>
            <el-button
              class="history-delete"
              text
              circle
              size="small"
              :disabled="agentStore.loading"
              @click.stop="handleDeleteSession(item)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          <div class="history-meta">
            <span class="history-time">{{ formatSessionTime(item.updatedAt) }}</span>
            <span class="history-count">{{ item.messageCount || 0 }}条</span>
          </div>
          <div v-if="item.lastMessage" class="history-last">{{ item.lastMessage }}</div>
        </div>
      </div>

      <!-- 左侧聊天列 -->
      <div class="chat-column">
        <ChatWindow
          :messages="agentStore.messages"
          :loading="agentStore.loading"
          class="chat-area"
          @send="handleSend"
          @confirm="handleConfirmation"
        />
        <AgentInputBar
          :loading="agentStore.loading"
          @send="handleSend"
        />
      </div>

      <!-- 右侧上下文面板 (FR-A-05, FR-A-08) -->
      <div class="context-column">
        <ContextPanel
          :intent="agentStore.currentIntent"
          :extracted-slots="agentStore.extractedSlots"
          :pending-ask-for="agentStore.pendingAskFor"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useAgentStore } from '@/stores/agent'
import ChatWindow from '@/components/agent/ChatWindow.vue'
import AgentInputBar from '@/components/agent/AgentInputBar.vue'
import ContextPanel from '@/components/agent/ContextPanel.vue'
import { Delete, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const agentStore = useAgentStore()

onMounted(async () => {
  await agentStore.loadSessions()
})

async function handleSend(message) {
  await agentStore.sendMessage(message)
}

async function handleConfirmation(payload) {
  await agentStore.sendConfirmation(payload)
}

async function handleNewSession() {
  if (agentStore.messages.length === 0) return
  await agentStore.newSession()
  ElMessage.success('已新建对话，原对话已保存在历史会话中')
}

async function handleOpenSession(sessionId) {
  await agentStore.openSession(sessionId)
}

async function handleDeleteSession(item) {
  if (!item?.sessionId) return
  const title = item.title || '该会话'
  await ElMessageBox.confirm(`确定删除“${title}”吗？删除后无法恢复。`, '删除历史会话', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
    confirmButtonClass: 'el-button--danger'
  }).then(async () => {
    await agentStore.deleteSession(item.sessionId)
    ElMessage.success('历史会话已删除')
  }).catch(() => {})
}

function formatSessionTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hour = String(d.getHours()).padStart(2, '0')
  const minute = String(d.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}
</script>

<style scoped>
.agent-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  background: #f5f7fa;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.06);
}

/* 头部 */
.agent-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}
.header-left { display: flex; align-items: center; gap: 14px; }
.agent-avatar-wrap {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}
.agent-title { margin: 0; font-size: 18px; color: #303133; font-weight: 600; }
.agent-sub { margin: 2px 0 0; font-size: 12px; color: #909399; }

/* 主体：三列布局 */
.agent-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 历史会话列 */
.history-column {
  width: 240px;
  flex-shrink: 0;
  border-right: 1px solid #e4e7ed;
  background: #fff;
  overflow-y: auto;
  padding: 10px;
}

.history-header {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  padding: 8px 6px 10px;
}

.history-empty {
  color: #c0c4cc;
  font-size: 12px;
  padding: 6px;
}

.history-item {
  width: 100%;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 8px;
  margin-bottom: 8px;
  text-align: left;
  background: #fff;
  cursor: pointer;
}

.history-item:hover {
  border-color: #c6e2ff;
  background: #f8fbff;
}

.history-title-row {
  display: flex;
  align-items: flex-start;
  gap: 6px;
}

.history-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.history-title {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: #303133;
  font-weight: 600;
  line-height: 1.4;
}

.history-delete {
  flex-shrink: 0;
  color: #909399;
}

.history-delete:hover {
  color: #f56c6c;
  background: #fef0f0;
}

.history-meta {
  margin-top: 4px;
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 11px;
}

.history-last {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 左侧聊天列 */
.chat-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}
.chat-area { flex: 1; overflow-y: auto; }

/* 右侧上下文面板 */
.context-column {
  width: 260px;
  flex-shrink: 0;
  overflow: hidden;
}
</style>
