import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 校园导航专用 Store
 * 主要职责：在 Agent 页和 Navigation 页之间传递已解析的导航任务数据
 */
export const useNavigationStore = defineStore('navigation', () => {
  const pendingTask = ref(null)
  const latestAgentRouteTask = ref(null)
  const preferredMode = ref('walking')
  const userLocation = ref(null)
  const clarification = ref(null)

  function setPendingTask(task) {
    pendingTask.value = task
    latestAgentRouteTask.value = task
  }

  function consumePendingTask() {
    const task = pendingTask.value
    pendingTask.value = null
    return task
  }

  function setLatestAgentRouteTask(task) {
    latestAgentRouteTask.value = task
  }

  function setPreferredMode(mode) {
    preferredMode.value = mode
  }

  function setUserLocation(location) {
    userLocation.value = location
  }

  function setClarification(payload) {
    clarification.value = payload
  }

  function clearClarification() {
    clarification.value = null
  }

  return {
    pendingTask,
    latestAgentRouteTask,
    preferredMode,
    userLocation,
    clarification,
    setPendingTask,
    consumePendingTask,
    setLatestAgentRouteTask,
    setPreferredMode,
    setUserLocation,
    setClarification,
    clearClarification
  }
})
