import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '@/api'
import { useAgentStore } from './agent'

export const useUserStore = defineStore('user', () => {
  const token = ref(sessionStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(sessionStorage.getItem('userInfo') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  
  const role = computed(() => userInfo.value?.role?.toUpperCase() || '')
  
  const isStudent = computed(() => role.value === 'STUDENT')
  
  const isTeacher = computed(() => role.value === 'TEACHER')
  
  const isAdmin = computed(() => role.value === 'ADMIN')
  
  const isDormManager = computed(() => role.value === 'DORM_MANAGER')
  
  const isUser = computed(() => role.value === 'USER')
  
  const isNormalUser = computed(() => isStudent.value || isTeacher.value)
  
  const isManager = computed(() => isAdmin.value || isDormManager.value)

  async function login(username, password) {
    const agentStore = useAgentStore()
    const res = await userApi.login({ username, password })
    // 每次登录后都强制开启新对话
    agentStore.resetState()
    token.value = res.data.token
    userInfo.value = res.data
    sessionStorage.setItem('token', res.data.token)
    sessionStorage.setItem('userInfo', JSON.stringify(res.data))
    // 清理历史版本遗留，避免多窗口仍共享本地持久态
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    return res.data
  }

  async function register(data) {
    return await userApi.register(data)
  }

  async function fetchUserInfo() {
    try {
      const res = await userApi.getUserInfo()
      userInfo.value = res.data
      sessionStorage.setItem('userInfo', JSON.stringify(res.data))
      return res.data
    } catch (error) {
      logout()
      throw error
    }
  }

  async function changePassword(oldPassword, newPassword) {
    return await userApi.changePassword({ oldPassword, newPassword })
  }

  function logout() {
    const agentStore = useAgentStore()
    agentStore.resetState()
    token.value = ''
    userInfo.value = null
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userInfo')
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    role,
    isStudent,
    isTeacher,
    isAdmin,
    isDormManager,
    isUser,
    isNormalUser,
    isManager,
    login,
    register,
    fetchUserInfo,
    changePassword,
    logout
  }
})
