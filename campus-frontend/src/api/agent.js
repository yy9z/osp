import request from './request'

/**
 * 发送 Agent 对话消息
 * @param {Object} data - { sessionId, message, context }
 */
export function chatWithAgent(data) {
  return request({
    url: '/agent/chat',
    method: 'post',
    data,
    timeout: 90000  // LLM 调用可能较慢，延长超时
  })
}

/**
 * 清除 Agent 会话（新建对话）
 * @param {string} sessionId
 */
export function clearAgentSession(sessionId) {
  return request({
    url: `/agent/session/${sessionId}`,
    method: 'delete'
  })
}

/**
 * 获取当前用户的 Agent 历史会话列表
 */
export function getAgentSessions(limit = 30) {
  return request({
    url: '/agent/sessions',
    method: 'get',
    params: { limit }
  })
}

/**
 * 获取指定会话的历史消息
 */
export function getAgentSessionHistory(sessionId, limit = 200) {
  return request({
    url: `/agent/session/${sessionId}/history`,
    method: 'get',
    params: { limit }
  })
}
