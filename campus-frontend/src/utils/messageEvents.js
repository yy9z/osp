const EVENT_CLEAR_BADGE = 'message:clear-badge'
const EVENT_UPDATE_BADGE = 'message:update-badge'

export const messageEvents = {
  emitClearBadge() {
    console.log('[EventBus] 发送 clear-badge 事件')
    window.dispatchEvent(new CustomEvent(EVENT_CLEAR_BADGE))
  },

  emitUpdateBadge(count) {
    console.log('[EventBus] 发送 update-badge 事件, count:', count)
    window.dispatchEvent(new CustomEvent(EVENT_UPDATE_BADGE, { detail: count }))
  },

  onClearBadge(callback) {
    window.addEventListener(EVENT_CLEAR_BADGE, callback)
    return () => window.removeEventListener(EVENT_CLEAR_BADGE, callback)
  },

  onUpdateBadge(callback) {
    const handler = (e) => callback(e.detail)
    window.addEventListener(EVENT_UPDATE_BADGE, handler)
    return () => window.removeEventListener(EVENT_UPDATE_BADGE, handler)
  }
}
