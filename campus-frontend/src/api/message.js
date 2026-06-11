import request from './request'

export const messageApi = {
  send(data) {
    return request({
      url: '/message/send',
      method: 'post',
      data
    })
  },

  getConversations() {
    return request({
      url: '/message/conversations',
      method: 'get'
    })
  },

  getHistory(userId, params) {
    return request({
      url: `/message/history/${userId}`,
      method: 'get',
      params
    })
  },

  getHistorySince(userId, params) {
    return request({
      url: `/message/history/${userId}/since`,
      method: 'get',
      params
    })
  },

  markRead(userId) {
    return request({
      url: `/message/read/${userId}`,
      method: 'put'
    })
  },

  getUnreadCount() {
    return request({
      url: '/message/unread-count',
      method: 'get'
    })
  },

  getOrCreateConversation(otherUserId) {
    return request({
      url: `/message/conversation/${otherUserId}`,
      method: 'get'
    })
  },

  pinConversation(otherUserId, pinned = true) {
    return request({
      url: `/message/conversation/${otherUserId}/pin`,
      method: 'put',
      params: { pinned }
    })
  },

  deleteConversation(otherUserId) {
    return request({
      url: `/message/conversation/${otherUserId}`,
      method: 'delete'
    })
  }
}
