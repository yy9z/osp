import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { messageApi } from '@/api'

export const useMessageStore = defineStore('message', () => {
  const unreadCount = ref(0)
  const conversations = ref([])
  let isRefreshing = false

  const totalUnreadCount = computed(() => {
    return conversations.value.reduce((sum, conv) => sum + (conv.unreadCount || 0), 0)
  })

  async function refreshUnreadCount() {
    if (isRefreshing) return
    isRefreshing = true
    try {
      const res = await messageApi.getUnreadCount()
      unreadCount.value = res.data || 0
    } catch (error) {
      console.error('获取未读消息数失败:', error)
    } finally {
      isRefreshing = false
    }
  }

  async function refreshConversations() {
    try {
      const res = await messageApi.getConversations()
      conversations.value = res.data || []
      unreadCount.value = totalUnreadCount.value
    } catch (error) {
      console.error('获取会话列表失败:', error)
    }
  }

  function updateConversationUnread(userId, count) {
    const conv = conversations.value.find(c => c.userId === userId)
    if (conv) {
      conv.unreadCount = count
    }
    unreadCount.value = totalUnreadCount.value
  }

  function incrementUnread(userId) {
    const conv = conversations.value.find(c => c.userId === userId)
    if (conv) {
      conv.unreadCount = (conv.unreadCount || 0) + 1
      console.log('[Store] 会话', userId, '未读数增加到', conv.unreadCount)
    } else {
      console.log('[Store] 会话不存在，需要刷新列表')
      refreshConversations()
    }
    unreadCount.value = totalUnreadCount.value
    console.log('[Store] 总未读数:', unreadCount.value)
  }

  function decrementUnread(count = 1) {
    unreadCount.value = Math.max(0, unreadCount.value - count)
  }

  function clearConversationUnread(userId) {
    const conv = conversations.value.find(c => c.userId === userId)
    if (conv && conv.unreadCount > 0) {
      const decrease = conv.unreadCount
      conv.unreadCount = 0
      unreadCount.value = Math.max(0, unreadCount.value - decrease)
      console.log('[Store] 清除会话', userId, '未读数，减少', decrease, '，剩余', unreadCount.value)
    }
  }

  function clearUnread() {
    unreadCount.value = 0
    conversations.value.forEach(c => c.unreadCount = 0)
  }

  function forceSetUnread(count) {
    unreadCount.value = count
  }

  function setConversations(list) {
    conversations.value = list
    unreadCount.value = totalUnreadCount.value
    console.log('[Store] 设置会话列表，总未读数:', unreadCount.value)
  }

  return {
    unreadCount,
    conversations,
    totalUnreadCount,
    refreshUnreadCount,
    refreshConversations,
    updateConversationUnread,
    incrementUnread,
    decrementUnread,
    clearConversationUnread,
    clearUnread,
    forceSetUnread,
    setConversations
  }
})
