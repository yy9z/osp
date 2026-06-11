<template>
  <el-container class="app-container">
    <!-- 侧边栏 - 毛玻璃效果 -->
    <el-aside :width="isCollapse ? '72px' : '220px'" class="app-aside">
      <div class="sidebar-glass">
        <!-- Logo区域 -->
        <div class="logo">
          <img src="/images/ustc-logo.png" alt="USTC" class="logo-img" />
          <transition name="fade">
            <span v-if="!isCollapse" class="logo-text">{{ isAdminMode ? '管理后台' : '一站式平台' }}</span>
          </transition>
        </div>

        <!-- 导航菜单 -->
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          router
          class="app-menu"
          :background-color="'transparent'"
          text-color="var(--ustc-text-secondary)"
          active-text-color="#fff"
        >
          <!-- 管理员菜单 -->
          <template v-if="isAdminMode">
            <el-menu-item index="/admin/secondhand">
              <el-icon><ShoppingBag /></el-icon>
              <template #title>
                <span class="menu-title">二手交易审核</span>
              </template>
            </el-menu-item>

            <el-menu-item index="/admin/lostfound">
              <el-icon><Search /></el-icon>
              <template #title>
                <span class="menu-title">失物招领管理</span>
              </template>
            </el-menu-item>

            <el-menu-item index="/admin/message">
              <el-icon><Message /></el-icon>
              <template #title>
                <span class="menu-title">系统公告</span>
              </template>
            </el-menu-item>

            <el-menu-item index="/admin/users">
              <el-icon><User /></el-icon>
              <template #title>
                <span class="menu-title">账号管理</span>
              </template>
            </el-menu-item>
          </template>

          <!-- 普通用户菜单（包括DORM_MANAGER） -->
          <template v-else>
            <el-menu-item index="/">
              <el-icon><House /></el-icon>
              <template #title>
                <span class="menu-title">首页</span>
              </template>
            </el-menu-item>

            <el-menu-item index="/agent">
              <el-icon><ChatDotSquare /></el-icon>
              <template #title>
                <span class="menu-title">智能助手</span>
              </template>
            </el-menu-item>

            <!-- 二手交易 - 可折叠二级菜单 -->
            <el-sub-menu index="/secondhand-group">
              <template #title>
                <el-icon><ShoppingBag /></el-icon>
                <span class="menu-title">二手交易</span>
              </template>
              <el-menu-item index="/secondhand?tab=market">
                <el-icon><Shop /></el-icon>
                <span class="sub-menu-title">逛逛市场</span>
              </el-menu-item>
              <el-menu-item index="/secondhand?tab=mystore">
                <el-icon><Box /></el-icon>
                <span class="sub-menu-title">我的发布</span>
              </el-menu-item>
              <el-menu-item index="/secondhand?tab=favorites">
                <el-icon><Star /></el-icon>
                <span class="sub-menu-title">我的收藏</span>
              </el-menu-item>
            </el-sub-menu>

            <!-- 失物招领 - 可折叠二级菜单 -->
            <el-sub-menu index="/lostfound-group">
              <template #title>
                <el-icon><Search /></el-icon>
                <span class="menu-title">失物招领</span>
              </template>
              <el-menu-item index="/lostfound/lost-board">
                <el-icon><Document /></el-icon>
                <span class="sub-menu-title">寻物公告</span>
              </el-menu-item>
              <el-menu-item index="/lostfound/found-board">
                <el-icon><Collection /></el-icon>
                <span class="sub-menu-title">招领公告</span>
              </el-menu-item>
              <el-menu-item index="/lostfound/my-lost">
                <el-icon><Bell /></el-icon>
                <span class="sub-menu-title">我的失物</span>
              </el-menu-item>
              <el-menu-item index="/lostfound/my-found">
                <el-icon><Checked /></el-icon>
                <span class="sub-menu-title">我的招领</span>
              </el-menu-item>
            </el-sub-menu>

            <el-menu-item index="/messages">
              <Badge v-if="!forceHideBadge" :count="unreadCount" :show-dot="false" size="small">
                <el-icon><Message /></el-icon>
              </Badge>
              <el-icon v-else><Message /></el-icon>
              <template #title>
                <span class="menu-title">消息中心</span>
              </template>
            </el-menu-item>

            <el-sub-menu index="/dormitory-group">
              <template #title>
                <el-icon><House /></el-icon>
                <span class="menu-title">宿舍管理</span>
              </template>
              <el-menu-item index="/dormitory/info">
                <el-icon><OfficeBuilding /></el-icon>
                <span class="sub-menu-title">宿舍信息</span>
              </el-menu-item>
              <el-menu-item v-if="!userStore.isDormManager" index="/dormitory/repair">
                <el-icon><EditPen /></el-icon>
                <span class="sub-menu-title">报修申请</span>
              </el-menu-item>
              <el-menu-item v-if="userStore.isAdmin || userStore.isDormManager" index="/dormitory/manage">
                <el-icon><Tools /></el-icon>
                <span class="sub-menu-title">报修管理</span>
              </el-menu-item>
              <el-menu-item v-if="userStore.isAdmin || userStore.isDormManager" index="/dormitory/assignment">
                <el-icon><Tools /></el-icon>
                <span class="sub-menu-title">宿舍管理</span>
              </el-menu-item>
            </el-sub-menu>

            <el-menu-item index="/navigation">
              <el-icon><MapLocation /></el-icon>
              <template #title>
                <span class="menu-title">校园导航</span>
              </template>
            </el-menu-item>
          </template>
        </el-menu>
      </div>
    </el-aside>

    <!-- 主体区域 -->
    <el-container>
      <!-- 头部导航栏 -->
      <el-header class="app-header">
        <div class="header-left">
          <el-icon class="collapse-icon" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>

          <!-- 面包屑导航 -->
          <el-breadcrumb separator="/" class="header-breadcrumb">
            <el-breadcrumb-item :to="{ path: isAdminMode ? '/admin/secondhand' : '/' }">{{ isAdminMode ? '管理后台' : '首页' }}</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRouteName">
              {{ currentRouteName }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <!-- 模式切换按钮（仅系统管理员可见） -->
          <el-button 
            v-if="userStore.isAdmin" 
            type="primary" 
            size="small"
            @click="toggleMode"
            class="mode-switch-btn"
          >
            <el-icon><Switch /></el-icon>
            {{ isAdminMode ? '用户端' : '管理端' }}
          </el-button>

          <!-- 微型校徽标识 -->
          <div class="ustc-badge-header">
            <img src="/images/ustc-logo.png" alt="USTC" class="header-logo" />
            <span class="header-ustc">USTC</span>
          </div>

          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="36" :src="userStore.userInfo?.avatar" class="user-avatar">
                {{ userStore.userInfo?.realName?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</span>
              <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu class="ustc-dropdown">
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item command="password">
                  <el-icon><Lock /></el-icon>
                  修改密码
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区域 -->
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore, useMessageStore } from '@/stores'
import { storeToRefs } from 'pinia'
import { messageEvents } from '@/utils/messageEvents'
import Badge from '@/components/Badge.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  House,
  ShoppingBag,
  Shop,
  Box,
  Star,
  Search,
  Message,
  MapLocation,
  Fold,
  Expand,
  ArrowDown,
  User,
  Lock,
  SwitchButton,
  OfficeBuilding,
  Tools,
  Switch,
  EditPen,
  ChatDotSquare,
  Document,
  Collection,
  Bell,
  Checked
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const messageStore = useMessageStore()
const { unreadCount } = storeToRefs(messageStore)

const isCollapse = ref(false)
const forceHideBadge = ref(false)

const activeMenu = computed(() => route.path)

const isAdminMode = computed(() => {
  return route.path.startsWith('/admin')
})

let removeClearBadgeListener = null

onMounted(() => {
  messageStore.refreshUnreadCount()
  
  removeClearBadgeListener = messageEvents.onClearBadge(() => {
    forceHideBadge.value = true
    setTimeout(() => {
      forceHideBadge.value = false
    }, 100)
  })
})

onUnmounted(() => {
  if (removeClearBadgeListener) {
    removeClearBadgeListener()
  }
})

watch(unreadCount, (newVal, oldVal) => {
})

const currentRouteName = computed(() => {
  const routeNames = {
    '/': '',
    '/agent': '智能助手',
    '/secondhand': '二手交易',
    '/lostfound/lost-board': '寻物公告',
    '/lostfound/found-board': '招领公告',
    '/lostfound/my-lost': '我的失物',
    '/lostfound/my-found': '我的招领',
    '/messages': '消息中心',
    '/dormitory/info': '宿舍信息',
    '/dormitory/repair': '报修申请',
    '/dormitory/manage': '报修管理',
    '/navigation': '校园导航',
    '/profile': '个人中心',
    '/admin/secondhand': '二手交易审核',
    '/admin/lostfound': '失物招领管理',
    '/dormitory/assignment': '宿舍管理',
    '/admin/message': '系统公告',
    '/admin/users': '账号管理'
  }
  return routeNames[route.path] || ''
})

const toggleMode = () => {
  if (isAdminMode.value) {
    router.push('/')
  } else {
    router.push('/admin/secondhand')
  }
}

const handleCommand = async (command) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      userStore.logout()
      router.push('/login')
      ElMessage.success('已退出登录')
    } catch {
    }
  } else if (command === 'password') {
    router.push('/profile?tab=security')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}
</script>

<style scoped>
.app-container {
  height: 100vh;
}

/* 侧边栏 - 毛玻璃效果 */
.app-aside {
  background: linear-gradient(180deg, #f8fafc 0%, #eef2f6 100%);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.sidebar-glass {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--ustc-glass-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-right: 1px solid var(--ustc-glass-border);
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.03);
}

/* Logo区域 */
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  gap: 10px;
}

.logo-img {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #fff;
  padding: 2px;
  box-shadow: 0 2px 8px rgba(0, 65, 145, 0.15);
  flex-shrink: 0;
}

.logo-text {
  font-size: 15px;
  font-weight: 600;
  color: var(--ustc-primary);
  white-space: nowrap;
  letter-spacing: 0.5px;
}

/* 菜单样式 */
.app-menu {
  border-right: none;
  flex: 1;
  padding: 12px 8px;
}

.app-menu :deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  border-radius: var(--ustc-radius-md);
  margin: 4px 0;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.app-menu :deep(.el-menu-item::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 0;
  height: 0;
  background: var(--ustc-gradient-active);
  border-radius: 0 var(--ustc-radius-sm) var(--ustc-radius-sm) 0;
  transition: width 0.3s ease;
  z-index: -1;
}

.app-menu :deep(.el-menu-item:hover) {
  background: rgba(0, 65, 145, 0.05);
  color: var(--ustc-primary);
}

.app-menu :deep(.el-menu-item.is-active) {
  background: var(--ustc-gradient-active);
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 65, 145, 0.3);
  animation: breathe 2s ease-in-out infinite;
}

@keyframes breathe {
  0%, 100% {
    box-shadow: 0 4px 12px rgba(0, 65, 145, 0.3);
  }
  50% {
    box-shadow: 0 4px 20px rgba(0, 65, 145, 0.4);
  }
}

.app-menu :deep(.el-menu-item .el-icon) {
  font-size: 18px;
}

.menu-title {
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.3px;
}

/* 二级菜单样式 */
.app-menu :deep(.el-sub-menu__title) {
  height: 48px;
  line-height: 48px;
  border-radius: var(--ustc-radius-md);
  margin: 4px 0;
  transition: all 0.3s ease;
}

.app-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(0, 65, 145, 0.05);
  color: var(--ustc-primary);
}

.app-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  background: var(--ustc-gradient-active);
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 65, 145, 0.3);
}

.app-menu :deep(.el-menu--inline) {
  padding-left: 8px;
}

.app-menu :deep(.el-menu--inline .el-menu-item) {
  height: 40px;
  line-height: 40px;
  border-radius: var(--ustc-radius-sm);
  margin: 2px 0;
  font-size: 13px;
}

.app-menu :deep(.el-menu--inline .el-menu-item:hover) {
  background: rgba(0, 65, 145, 0.08);
}

.app-menu :deep(.el-menu--inline .el-menu-item.is-active) {
  background: rgba(0, 65, 145, 0.12);
  color: var(--ustc-primary);
}

.sub-menu-title {
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.2px;
  padding-left: 4px;
}

/* 折叠状态下的样式 */
.app-aside :deep(.el-menu--collapse) .sub-menu-title {
  display: none;
}

.app-aside :deep(.el-menu--collapse) .el-sub-menu__title {
  padding: 0 24px !important;
}

.app-aside :deep(.el-menu--collapse) .el-sub-menu__title .el-icon {
  margin-right: 0;
}

.app-aside :deep(.el-menu--collapse) .el-sub-menu__title .menu-title {
  display: none;
}

/* 折叠时显示为图标模式，hover 展开 */
.app-aside :deep(.el-menu--collapse) .el-sub-menu {
  position: relative;
}

.app-aside :deep(.el-menu--collapse) .el-sub-menu:hover > .el-menu--inline {
  display: block !important;
}

.app-aside :deep(.el-menu--collapse) .el-sub-menu .el-menu--inline {
  position: absolute;
  left: 100%;
  top: 0;
  min-width: 160px;
  display: none;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  padding: 8px;
}

.app-aside :deep(.el-menu--collapse) .el-sub-menu .el-menu--inline .el-menu-item {
  border-radius: 6px;
  margin: 2px 0;
}

/* 折叠状态 */
.app-aside :deep(.el-menu--collapse) .logo-text {
  display: none;
}

.app-aside :deep(.el-menu--collapse) .logo {
  padding: 0;
  justify-content: center;
}

/* 头部导航 */
.app-header {
  background: var(--ustc-card);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: var(--ustc-shadow-sm);
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.collapse-icon {
  font-size: 20px;
  cursor: pointer;
  color: var(--ustc-text-secondary);
  transition: color 0.3s ease;
}

.collapse-icon:hover {
  color: var(--ustc-primary);
}

/* 面包屑 */
.header-breadcrumb :deep(.el-breadcrumb__item) {
  opacity: 0.8;
  transition: opacity 0.3s ease;
}

.header-breadcrumb :deep(.el-breadcrumb__item:hover) {
  opacity: 1;
}

.header-breadcrumb :deep(.el-breadcrumb__inner) {
  font-size: 14px;
  color: var(--ustc-text-secondary);
}

.header-breadcrumb :deep(.el-breadcrumb__inner.is-link:hover) {
  color: var(--ustc-primary);
}

.header-breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--ustc-text);
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

/* 模式切换按钮 */
.mode-switch-btn {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 微型校徽标识 */
.ustc-badge-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: linear-gradient(135deg, rgba(0, 65, 145, 0.08) 0%, rgba(0, 65, 145, 0.04) 100%);
  border-radius: 20px;
  border: 1px solid rgba(0, 65, 145, 0.1);
}

.header-logo {
  width: 24px;
  height: 24px;
  border-radius: 50%;
}

.header-ustc {
  font-size: 13px;
  font-weight: 600;
  color: var(--ustc-primary);
  letter-spacing: 1px;
}

/* 用户信息 */
.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 24px;
  transition: background 0.3s ease;
}

.user-info:hover {
  background: var(--ustc-bg);
}

.user-avatar {
  background: var(--ustc-gradient-primary);
  font-size: 14px;
}

.username {
  margin-left: 10px;
  color: var(--ustc-text);
  font-size: 14px;
  font-weight: 500;
}

.dropdown-arrow {
  margin-left: 6px;
  color: var(--ustc-text-placeholder);
  font-size: 12px;
}

/* 下拉菜单 */
.ustc-dropdown :deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
}

.ustc-dropdown :deep(.el-dropdown-menu__item .el-icon) {
  color: var(--ustc-text-secondary);
}

.ustc-dropdown :deep(.el-dropdown-menu__item:hover) {
  background: var(--ustc-bg);
  color: var(--ustc-primary);
}

.ustc-dropdown :deep(.el-dropdown-menu__item:hover .el-icon) {
  color: var(--ustc-primary);
}

/* 内容区域 */
.app-main {
  background: var(--ustc-bg);
  padding: 16px;
  min-height: calc(100vh - 64px);
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .ustc-badge-header {
    display: none;
  }

  .username {
    display: none;
  }

  .header-breadcrumb {
    display: none;
  }
  
  .mode-switch-btn {
    display: none;
  }
}
</style>
