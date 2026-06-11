import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 30000
})

const WHITE_LIST = ['/user/login', '/user/register']

let isRefreshing = false
let redirectTimer = null
let hasShownExpiredMessage = false

const handleTokenExpired = () => {
  if (isRefreshing) return

  isRefreshing = true

  sessionStorage.removeItem('token')
  sessionStorage.removeItem('userInfo')
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')

  if (!hasShownExpiredMessage) {
    hasShownExpiredMessage = true
    ElMessage.error('登录已过期，请重新登录')
  }

  if (redirectTimer) {
    clearTimeout(redirectTimer)
  }

  redirectTimer = setTimeout(() => {
    isRefreshing = false
    hasShownExpiredMessage = false
    redirectTimer = null

    const currentPath = window.location.pathname + window.location.search
    if (!currentPath.startsWith('/login') && !currentPath.startsWith('/register')) {
      window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`
    }
  }, 100)
}

service.interceptors.request.use(
  config => {
    const token = sessionStorage.getItem('token')

    if (!token && !WHITE_LIST.some(path => config.url?.includes(path))) {
      handleTokenExpired()
      return Promise.reject(new Error('NO_TOKEN'))
    }

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    const res = response.data

    if (res.code === 200) {
      return res
    } else if (res.code === 500) {
      ElMessage.error(res.message || '服务器内部错误')
      return Promise.reject(new Error(res.message || '服务器内部错误'))
    } else if (res.code === 401 || res.code === 403) {
      ElMessage.error(res.message || '操作失败')
      return Promise.reject(new Error(res.message || '操作失败'))
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  error => {
    if (error.message === 'NO_TOKEN') {
      return Promise.reject(error)
    }

    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 401:
          handleTokenExpired()
          break
        case 403:
          ElMessage.error('没有权限执行此操作')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(data.message || '请求失败')
      }
    } else {
      ElMessage.error('网络连接失败，请检查网络')
    }
    return Promise.reject(error)
  }
)

export const resetAuthState = () => {
  isRefreshing = false
  hasShownExpiredMessage = false
  if (redirectTimer) {
    clearTimeout(redirectTimer)
    redirectTimer = null
  }
}

export default service

export const uploadFile = (file, options = {}) => {
  const formData = new FormData()
  formData.append('file', file)
  const isLocalDev = typeof window !== 'undefined' &&
    (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')
  const mode = options.mode || (isLocalDev ? 'local' : '')

  return service.post('/file/upload', formData, {
    params: mode ? { mode } : undefined,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
