import { defineStore } from 'pinia'
import { ref } from 'vue'
import { chatWithAgent, clearAgentSession, getAgentSessions, getAgentSessionHistory } from '@/api/agent'
import { ElMessage } from 'element-plus'

export const useAgentStore = defineStore('agent', () => {
  const sessionId = ref(null)
  const activeSessionId = ref(null)
  const sessions = ref([])
  const messages = ref([])  // { role: 'user'|'agent', content, cards, timestamp }
  const loading = ref(false)

  // 右侧 ContextPanel 状态 (FR-A-05, FR-A-08)
  const currentIntent = ref(null)
  const extractedSlots = ref({})
  const pendingAskFor = ref(null)
  const followUpType = ref(null)
  const followUpOptions = ref([])
  const pendingConfirmationId = ref(null)

  function resolveAgentErrorMessage(error, fallbackMessage) {
    const rawMessage = String(error?.message || '')
    const lower = rawMessage.toLowerCase()
    if (rawMessage === 'NO_TOKEN' || lower.includes('未登录') || lower.includes('登录已过期')) {
      return '登录状态已失效，请重新登录后再试'
    }
    if (lower.includes('timeout') || lower.includes('超时')) {
      return '智能助手响应超时，请稍后重试'
    }
    if (lower.includes('network') || lower.includes('网络')) {
      return '网络连接异常，请检查网络后重试'
    }
    if (lower.includes('权限') || lower.includes('forbidden') || lower.includes('403')) {
      return '当前账号没有权限执行该操作'
    }
    if (lower.includes('401') || lower.includes('unauthorized')) {
      return '登录状态已失效，请重新登录后再试'
    }
    return rawMessage || fallbackMessage
  }

  function resetState() {
    sessionId.value = null
    activeSessionId.value = null
    sessions.value = []
    messages.value = []
    currentIntent.value = null
    extractedSlots.value = {}
    pendingAskFor.value = null
    followUpType.value = null
    followUpOptions.value = []
    pendingConfirmationId.value = null
    loading.value = false
  }

  async function sendMessage(userMessage, context = null, confirmation = null) {
    if (loading.value) return

    messages.value.push({ role: 'user', content: userMessage, timestamp: new Date() })

    loading.value = true
    try {
      const res = await chatWithAgent({
        sessionId: sessionId.value || null,
        message: userMessage,
        context,
        confirmationId: confirmation?.confirmationId || null,
        confirmationDecision: confirmation?.confirmationDecision || null
      })

      const data = res.data
      sessionId.value = data.sessionId
      activeSessionId.value = data.sessionId

      currentIntent.value = data.intent
      extractedSlots.value = data.extractedSlots || {}
      pendingAskFor.value = data.askFor || null
      followUpType.value = data.followUpType || null
      followUpOptions.value = data.followUpOptions || []
      pendingConfirmationId.value = data.confirmationRequired ? data.confirmationId : null

      messages.value.push({
        role: 'agent',
        content: data.reply,
        cards: data.cards || [],
        usedTools: data.usedTools || [],
        intent: data.intent,
        taskCompleted: data.taskCompleted,
        suggestions: data.taskCompleted ? (data.followUpSuggestions || []) : [],
        followUpType: data.followUpType || null,
        followUpOptions: data.followUpOptions || [],
        confirmationRequired: Boolean(data.confirmationRequired),
        confirmationId: data.confirmationId || null,
        confirmationPreview: data.confirmationPreview || null,
        confirmationResolved: false,
        timestamp: new Date()
      })

      if (data.taskCompleted) {
        pendingAskFor.value = null
        followUpType.value = null
        followUpOptions.value = []
        pendingConfirmationId.value = null
      }

      // 历史列表异步刷新，不阻塞本次消息返回速度
      loadSessions()

      return data
    } catch (e) {
      const errorMessage = resolveAgentErrorMessage(e, '助手暂时无法响应，请稍后再试')
      ElMessage.error(errorMessage)
      messages.value.push({
        role: 'agent',
        content: '抱歉，智能助手暂时繁忙，请稍后再试。',
        cards: [],
        timestamp: new Date()
      })
    } finally {
      loading.value = false
    }
  }

  async function sendConfirmation({ messageIndex, confirmationId, decision }) {
    if (loading.value || !confirmationId) return
    const target = messages.value[messageIndex]
    if (target?.confirmationResolved) return
    if (target) target.confirmationResolved = true

    const label = decision === 'APPROVE' ? '确认执行' : '取消'
    const result = await sendMessage(label, null, {
      confirmationId,
      confirmationDecision: decision
    })
    if (!result && target) target.confirmationResolved = false
    return result
  }

  async function newSession() {
    // 新建会话只切换到空白对话，历史记录由用户通过删除按钮单独管理。
    sessionId.value = null
    activeSessionId.value = null
    messages.value = []
    currentIntent.value = null
    extractedSlots.value = {}
    pendingAskFor.value = null
    followUpType.value = null
    followUpOptions.value = []
    pendingConfirmationId.value = null
    loading.value = false
    await loadSessions()
  }

  async function deleteSession(targetSessionId) {
    if (!targetSessionId || loading.value) return
    try {
      await clearAgentSession(targetSessionId)
      sessions.value = sessions.value.filter(item => item.sessionId !== targetSessionId)
      if (activeSessionId.value === targetSessionId || sessionId.value === targetSessionId) {
        sessionId.value = null
        activeSessionId.value = null
        messages.value = []
        currentIntent.value = null
        extractedSlots.value = {}
        pendingAskFor.value = null
        followUpType.value = null
        followUpOptions.value = []
        pendingConfirmationId.value = null
      }
      await loadSessions()
    } catch (e) {
      ElMessage.error(resolveAgentErrorMessage(e, '删除历史会话失败'))
      throw e
    }
  }

  async function loadSessions() {
    try {
      const res = await getAgentSessions(50)
      sessions.value = res.data || []
    } catch (e) {
      ElMessage.error(resolveAgentErrorMessage(e, '加载历史会话失败'))
      sessions.value = []
    }
  }

  async function openSession(targetSessionId) {
    if (!targetSessionId || loading.value) return
    loading.value = true
    try {
      const res = await getAgentSessionHistory(targetSessionId, 300)
      const data = res.data || {}
      sessionId.value = data.sessionId || targetSessionId
      activeSessionId.value = sessionId.value
      messages.value = (data.messages || []).map(msg => ({
        role: msg.role,
        content: msg.content,
        cards: Array.isArray(msg.cards) ? msg.cards : [],
        usedTools: Array.isArray(msg.usedTools) ? msg.usedTools : [],
        confirmationRequired: Boolean(msg.confirmationRequired),
        confirmationId: msg.confirmationId || null,
        confirmationPreview: msg.confirmationPreview || null,
        confirmationResolved: false,
        timestamp: msg.timestamp ? new Date(msg.timestamp) : new Date()
      }))
      currentIntent.value = null
      extractedSlots.value = {}
      pendingAskFor.value = null
      followUpType.value = null
      followUpOptions.value = []
      pendingConfirmationId.value = messages.value.findLast(msg => msg.confirmationRequired)?.confirmationId || null
    } catch (e) {
      ElMessage.error(resolveAgentErrorMessage(e, '加载历史会话失败'))
    } finally {
      loading.value = false
    }
  }

  return {
    sessionId,
    activeSessionId,
    sessions,
    messages,
    loading,
    currentIntent,
    extractedSlots,
    pendingAskFor,
    followUpType,
    followUpOptions,
    pendingConfirmationId,
    resetState,
    sendMessage,
    sendConfirmation,
    newSession,
    deleteSession,
    loadSessions,
    openSession
  }
})
