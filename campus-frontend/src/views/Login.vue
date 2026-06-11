<template>
  <div class="login-container">
    <!-- 左侧实景区域 -->
    <div class="login-scene">
      <div class="scene-overlay"></div>
      <div class="scene-content">
        <div class="ustc-badge">
          <img src="/images/ustc-logo.png" alt="USTC" class="ustc-logo" />
          <span class="ustc-name">中国科学技术大学</span>
        </div>
        <h2 class="scene-title">校园一站式平台</h2>
        <p class="scene-subtitle">University of Science and Technology of China</p>
        <div class="scene-features">
          <div class="feature-item">
            <span class="feature-icon">🏠</span>
            <span>宿舍管理</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">🔄</span>
            <span>二手交易</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">📍</span>
            <span>失物招领</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">🗺️</span>
            <span>校园导航</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧表单区域 -->
    <div class="login-form-area">
      <div class="form-wrapper">
        <div class="form-header">
          <img src="/images/ustc-logo.png" alt="USTC" class="form-logo" />
          <h1>统一身份认证</h1>
          <p class="cas-subtitle">CAS Unified Identity Authentication</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              size="large"
              class="ustc-input"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
              class="ustc-input"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="login-button"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>

          <div class="login-footer">
            <span>还没有账号？</span>
            <router-link to="/register" class="register-link">立即注册</router-link>
            <span style="margin: 0 8px; color: #c0c4cc;">|</span>
            <router-link to="/register-admin" class="register-link">管理员注册</router-link>
          </div>
        </el-form>

        <div class="form-footer">
          <p>© 2024 中国科学技术大学 · 校园一站式平台</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 表单引用
const loginFormRef = ref()

// 加载状态
const loading = ref(false)

// 登录表单
const loginForm = reactive({
  username: '',
  password: ''
})

// 表单验证规则
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ]
}

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await userStore.login(loginForm.username, loginForm.password)
      ElMessage.success('登录成功')

      const redirect = route.query.redirect || '/'
      router.push(redirect)
    } catch (error) {
      console.error('登录失败:', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  width: 100%;
}

/* 左侧实景区域 */
.login-scene {
  flex: 1;
  position: relative;
  background-image: url('/images/campus-bg.jpg');
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
}

.scene-overlay {
  position: absolute;
  inset: 0;
  background: var(--ustc-gradient-overlay);
  backdrop-filter: blur(2px);
}

.scene-content {
  position: relative;
  z-index: 1;
  text-align: center;
  color: #fff;
  padding: 40px;
}

.ustc-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 32px;
}

.ustc-logo {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #fff;
  padding: 4px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

.ustc-name {
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 2px;
}

.scene-title {
  font-size: 42px;
  font-weight: 700;
  margin: 0 0 12px 0;
  letter-spacing: 4px;
}

.scene-subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin: 0 0 48px 0;
  letter-spacing: 1px;
}

.scene-features {
  display: flex;
  justify-content: center;
  gap: 32px;
  flex-wrap: wrap;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: var(--ustc-radius-md);
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
}

.feature-icon {
  font-size: 20px;
}

/* 右侧表单区域 */
.login-form-area {
  width: 520px;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ustc-card);
  padding: 40px;
}

.form-wrapper {
  width: 100%;
  max-width: 380px;
}

.form-header {
  text-align: center;
  margin-bottom: 40px;
}

.form-logo {
  width: 64px;
  height: 64px;
  margin-bottom: 20px;
}

.form-header h1 {
  font-size: 28px;
  color: var(--ustc-primary);
  margin: 0 0 8px 0;
  font-weight: 600;
}

.cas-subtitle {
  font-size: 13px;
  color: var(--ustc-text-placeholder);
  margin: 0;
  letter-spacing: 0.5px;
}

.login-form {
  margin-top: 20px;
}

.login-form :deep(.el-input__wrapper) {
  padding: 4px 15px;
  border-radius: var(--ustc-radius-sm);
  box-shadow: var(--ustc-shadow-sm);
  border: 1px solid rgba(0, 0, 0, 0.08);
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: var(--ustc-primary);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--ustc-primary);
  box-shadow: 0 0 0 3px rgba(0, 65, 145, 0.1);
}

.login-form :deep(.el-input__inner) {
  height: 44px;
  font-size: 15px;
}

.login-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  background: var(--ustc-gradient-primary);
  border: none;
  border-radius: var(--ustc-radius-sm);
  letter-spacing: 2px;
  transition: all 0.3s ease;
}

.login-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(0, 65, 145, 0.3);
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  color: var(--ustc-text-secondary);
}

.register-link {
  color: var(--ustc-primary);
  text-decoration: none;
  margin-left: 6px;
  font-weight: 500;
  transition: color 0.3s ease;
}

.register-link:hover {
  color: var(--ustc-primary-light);
  text-decoration: underline;
}

.form-footer {
  text-align: center;
  margin-top: 40px;
}

.form-footer p {
  font-size: 12px;
  color: var(--ustc-text-placeholder);
  margin: 0;
}

/* 响应式适配 */
@media (max-width: 1024px) {
  .login-scene {
    display: none;
  }

  .login-form-area {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .login-form-area {
    padding: 24px;
  }

  .form-header h1 {
    font-size: 22px;
  }
}
</style>
