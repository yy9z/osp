<template>
  <div class="dashboard">
    <!-- 背景水印 -->
    <div class="bg-watermark">
      <div class="watermark-plum"></div>
    </div>

    <!-- Hero Banner 区域 -->
    <section class="hero-banner">
      <div class="hero-content">
        <div class="hero-text">
          <h1 class="hero-title">
            欢迎回来，<span class="username">{{ userStore.userInfo?.realName || userStore.userInfo?.username || '同学' }}</span>
          </h1>
          <p class="hero-subtitle">{{ currentDate }} | 祝你学习愉快</p>
        </div>
        <div class="hero-decoration">
          <div class="decoration-ring"></div>
          <div class="decoration-ring delay-1"></div>
          <div class="decoration-ring delay-2"></div>
        </div>
      </div>
      <div class="hero-gradient"></div>
    </section>

    <!-- 功能入口 - 横向小卡片 -->
    <section class="quick-entry-section">
      <div class="entry-grid">
        <div 
          v-for="(item, index) in quickEntries" 
          :key="item.path"
          class="entry-card"
          :class="`entry-${item.size}`"
          @click="router.push(item.path)"
          :style="{ animationDelay: `${index * 0.05}s` }"
        >
          <div class="entry-icon-wrap" :style="{ background: item.gradient }">
            <el-icon :size="item.iconSize || 20">
              <component :is="item.icon" />
            </el-icon>
          </div>
          <div class="entry-info">
            <div class="entry-title">{{ item.title }}</div>
            <div class="entry-desc">{{ item.desc }}</div>
          </div>
          <el-icon class="entry-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
    </section>

    <!-- Bento Box 布局 -->
    <section class="bento-grid">
      <!-- 左侧大区域：二手交易 + 失物招领 -->
      <div class="bento-left">
        <!-- 二手交易模块 -->
        <div class="bento-card card-secondhand">
          <div class="card-header">
            <div class="card-title-wrap">
              <el-icon class="card-icon" :size="22"><ShoppingBag /></el-icon>
              <span class="card-title">二手交易</span>
            </div>
            <div class="card-header-right">
              <div class="stat-tag">
                <el-icon><TrendCharts /></el-icon>
                <span>{{ stats.secondHandCount }} 件在售</span>
              </div>
              <span class="more-link" @click.stop="router.push('/secondhand?tab=market')">更多 ></span>
            </div>
          </div>
          <div class="card-content">
            <div class="items-preview text-only" v-if="secondHandPreview.length > 0">
              <div v-for="item in secondHandPreview" :key="item.id" class="preview-item text-card" @click="router.push('/secondhand/' + item.id)">
                <div class="text-card-main">
                  <div class="text-card-title">{{ item.title }}</div>
                  <div class="text-card-desc">{{ item.description || '暂无简介' }}</div>
                </div>
                <div class="text-card-price">¥{{ item.price }}</div>
              </div>
            </div>
            <div v-else class="empty-state">
              <el-icon :size="40"><ShoppingBag /></el-icon>
              <p>暂无在售宝贝</p>
            </div>
          </div>
        </div>

        <!-- 失物招领模块 -->
        <div class="bento-card card-lostfound" @click="router.push('/lostfound')">
          <div class="card-header">
            <div class="card-title-wrap">
              <el-icon class="card-icon" :size="22"><Search /></el-icon>
              <span class="card-title">失物招领</span>
            </div>
            <div class="stat-tag warning">
              <el-icon><Warning /></el-icon>
              <span>{{ stats.lostFoundCount }} 条待认领</span>
            </div>
          </div>
          <div class="card-content">
            <div class="items-preview">
              <div v-for="item in lostFoundPreview" :key="item.id" class="preview-item">
                <img 
                  :src="getLostFoundCover(item)" 
                  :alt="item.title" 
                  class="preview-img" 
                  @error="(e) => (e.target as HTMLImageElement).src = placeholderImg"
                />
                <div class="preview-info">
                  <div class="preview-title">{{ item.title }}</div>
                  <div class="preview-time">{{ formatTime(item.createTime) }}</div>
                </div>
              </div>
              <div v-if="lostFoundPreview.length === 0" class="empty-state">
                <el-icon :size="40"><Search /></el-icon>
                <p>暂无失物信息</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧小组件区域 -->
      <div class="bento-right">
        <!-- 主动提醒 Widget -->
        <div class="bento-card card-proactive">
          <div class="card-header">
            <div class="card-title-wrap">
              <el-icon class="card-icon" :size="20"><Bell /></el-icon>
              <span class="card-title">智能提醒</span>
            </div>
          </div>
          <div class="card-content proactive-content">
            <div v-if="proactiveTips.length" class="tip-list">
              <div v-for="tip in proactiveTips" :key="`${tip.title}-${tip.actionPath}`" class="tip-item" @click="handleTipClick(tip)">
                <div class="tip-main">
                  <div class="tip-title">{{ tip.title }}</div>
                  <div class="tip-desc">{{ tip.content }}</div>
                </div>
                <el-tag size="small" :type="getTipLevelType(tip.level)">
                  {{ getTipLevelText(tip.level) }}
                </el-tag>
              </div>
            </div>
            <div v-else class="empty-state small">
              <p>当前没有待处理提醒</p>
            </div>
          </div>
        </div>

        <!-- 消息流 Widget -->
        <div class="bento-card card-messages">
          <div class="card-header">
            <div class="card-title-wrap">
              <el-icon class="card-icon" :size="20"><Bell /></el-icon>
              <span class="card-title">最新动态</span>
            </div>
          </div>
          <div class="card-content messages-content">
            <div class="message-list">
              <div 
                v-for="msg in messageList" 
                :key="msg.id"
                class="message-item"
                @click="handleNotificationClick(msg)"
              >
                <el-icon class="message-type-icon" :size="16">
                  <component :is="getNotificationIcon(msg.type)" />
                </el-icon>
                <div class="message-info">
                  <div class="message-text">{{ msg.title }}</div>
                  <div class="message-desc">{{ msg.content }}</div>
                  <div class="message-time">{{ msg.time }}</div>
                </div>
              </div>
              <div v-if="messageList.length === 0" class="empty-state small">
                <p>暂无最新消息</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 消息中心入口（带角标） -->
        <div class="bento-card card-messages-center" @click="router.push('/messages')">
          <Badge :count="stats.unreadCount" :animate="true" :show-dot="false">
            <div class="messages-center-content">
              <div class="messages-center-icon">
                <el-icon :size="28"><Message /></el-icon>
              </div>
              <div class="messages-center-text">
                <div class="messages-center-title">消息中心</div>
                <div class="messages-center-desc">
                  {{ stats.unreadCount > 0 ? `${stats.unreadCount} 条未读消息` : '暂无新消息' }}
                </div>
              </div>
            </div>
          </Badge>
        </div>
      </div>
    </section>

    <!-- 底部统计区域 -->
    <section class="stats-section">
      <div class="stats-grid">
        <div class="stat-item">
          <div class="stat-icon blue">
            <el-icon :size="24"><ShoppingBag /></el-icon>
          </div>
          <div class="stat-data">
            <div class="stat-value">{{ stats.secondHandCount }}</div>
            <div class="stat-label">二手商品</div>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon green">
            <el-icon :size="24"><Search /></el-icon>
          </div>
          <div class="stat-data">
            <div class="stat-value">{{ stats.lostFoundCount }}</div>
            <div class="stat-label">失物招领</div>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon orange">
            <el-icon :size="24"><Message /></el-icon>
          </div>
          <div class="stat-data">
            <div class="stat-value">{{ stats.unreadCount }}</div>
            <div class="stat-label">未读消息</div>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon red">
            <el-icon :size="24"><Tools /></el-icon>
          </div>
          <div class="stat-data">
            <div class="stat-value">{{ stats.repairCount }}</div>
            <div class="stat-label">宿舍报修</div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores'
import { homeApi } from '@/api/home'
import Badge from '@/components/Badge.vue'
import placeholderImg from '@/assets/placeholder.png'
import { ElMessage } from 'element-plus'
import {
  ShoppingBag,
  Search,
  Message,
  MapLocation,
  House,
  ArrowRight,
  Bell,
  TrendCharts,
  Warning,
  Tools
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

interface DashboardStatistics {
  secondHandCount?: number
  lostFoundCount?: number
  unreadMsgCount?: number
  dormRepairCount?: number
}

interface SecondHandPreviewItem {
  id: number | string
  title: string
  description?: string
  price?: number | string
}

interface LostFoundPreviewItem {
  id: number | string
  title: string
  createTime?: string
  images?: string[] | string | null
  imageList?: string[] | null
}

interface DashboardNotificationRaw {
  id: number | string
  type?: string
  title?: string
  content?: string
  relatedId?: number | string
  relatedType?: string
  createTime?: string
}

interface DashboardNotificationItem {
  id: number | string
  type: string
  title: string
  content: string
  relatedId?: number | string
  relatedType?: string
  time: string
}

interface ProactiveTip {
  title: string
  content: string
  actionPath?: string
  level?: string
  priority?: number
}

interface HomeDashboardData {
  statistics?: DashboardStatistics
  notifications?: DashboardNotificationRaw[]
  secondHandItems?: SecondHandPreviewItem[]
  lostFoundItems?: LostFoundPreviewItem[]
  proactiveTips?: ProactiveTip[]
}

interface ApiResponse<T> {
  code: number
  data?: T | null
}

// 当前日期
const currentDate = computed(() => {
  const now = new Date()
  return now.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
})

// 统计数据
const stats = ref({
  secondHandCount: 0,
  lostFoundCount: 0,
  unreadCount: 0,
  repairCount: 0
})

// 二手商品预览
const secondHandPreview = ref<SecondHandPreviewItem[]>([])

// 失物招领预览
const lostFoundPreview = ref<LostFoundPreviewItem[]>([])
const normalizeImages = (images: unknown): string[] => {
  if (!images) return []
  if (Array.isArray(images)) {
    return images
      .filter((item): item is string => typeof item === 'string')
      .map((item) => item.trim())
      .filter((item) => item.length > 0)
  }
  if (typeof images === 'string') {
    try {
      const parsed: unknown = JSON.parse(images)
      if (Array.isArray(parsed)) {
        return parsed
          .filter((item): item is string => typeof item === 'string')
          .map((item) => item.trim())
          .filter((item) => item.length > 0)
      }
    } catch (_) {
      const trimmed = images.trim()
      return trimmed ? [trimmed] : []
    }
  }
  return []
}
const getLostFoundCover = (item: LostFoundPreviewItem) => {
  const list = normalizeImages(item?.imageList || item?.images)
  return list[0] || placeholderImg
}

// 功能入口配置
const quickEntries = [
  { 
    title: '二手交易', 
    path: '/secondhand', 
    icon: ShoppingBag, 
    desc: '买卖二手好物',
    gradient: 'linear-gradient(135deg, #409EFF 0%, #67C23A 100%)',
    size: 'large',
    iconSize: 22
  },
  { 
    title: '失物招领', 
    path: '/lostfound', 
    icon: Search, 
    desc: '寻找丢失物品',
    gradient: 'linear-gradient(135deg, #67C23A 0%, #E6A23C 100%)',
    size: 'large',
    iconSize: 22
  },
  { 
    title: '消息中心', 
    path: '/messages', 
    icon: Message, 
    desc: '查看通知消息',
    gradient: 'linear-gradient(135deg, #E6A23C 0%, #F56C6C 100%)',
    size: 'medium',
    iconSize: 20
  },
  { 
    title: '宿舍管理', 
    path: '/dormitory', 
    icon: House, 
    desc: '报修与查询',
    gradient: 'linear-gradient(135deg, #F56C6C 0%, #909399 100%)',
    size: 'medium',
    iconSize: 20
  },
  { 
    title: '校园导航', 
    path: '/navigation', 
    icon: MapLocation, 
    desc: '校园地图导览',
    gradient: 'linear-gradient(135deg, #909399 0%, #004191 100%)',
    size: 'small',
    iconSize: 18
  }
]

// 消息流列表
const messageList = ref<DashboardNotificationItem[]>([
  { id: 1, type: 'SYSTEM', title: '二手交易动态', content: '有人发布了新的二手商品', time: '刚刚' },
  { id: 2, type: 'SYSTEM', title: '失物招领动态', content: '您的失物已被人捡到', time: '5分钟前' },
  { id: 3, type: 'SYSTEM', title: '宿舍报修动态', content: '宿舍维修已完成', time: '1小时前' },
  { id: 4, type: 'SYSTEM', title: '教务通知', content: '新学期选课开始了', time: '昨天' }
])
const proactiveTips = ref<ProactiveTip[]>([])

// 格式化时间
const formatTime = (time?: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const hours = Math.floor(diff / (1000 * 60 * 60))
  if (hours < 1) return '刚刚'
  if (hours < 24) return `${hours}小时前`
  if (hours < 48) return '昨天'
  return date.toLocaleDateString('zh-CN')
}

// 格式化时间 ago
const formatTimeAgo = (timeStr?: string) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / (1000 * 60))
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

// 获取首页数据
const fetchHomeData = async () => {
  try {
    const res = await homeApi.getHomeData() as ApiResponse<HomeDashboardData>
    if (res.code === 200 && res.data) {
      const data = res.data
      // 绑定统计数据
      stats.value = {
        secondHandCount: data.statistics?.secondHandCount || 0,
        lostFoundCount: data.statistics?.lostFoundCount || 0,
        unreadCount: data.statistics?.unreadMsgCount || 0,
        repairCount: data.statistics?.dormRepairCount || 0
      }
      // 绑定二手商品预览
      secondHandPreview.value = (data.secondHandItems || []).slice(0, 4)
      // 绑定失物招领预览
      lostFoundPreview.value = (data.lostFoundItems || []).slice(0, 4)
      // 绑定最新动态
      messageList.value = (data.notifications || []).map((item) => ({
        id: item.id,
        type: item.type || 'SYSTEM',
        content: item.content || '',
        title: item.title || '系统通知',
        relatedId: item.relatedId,
        relatedType: item.relatedType,
        time: formatTimeAgo(item.createTime)
      }))
      proactiveTips.value = data.proactiveTips || []
    }
  } catch (error) {
    console.error('获取首页数据失败:', error)
  }
}

const fetchProactiveTips = async () => {
  try {
    const res = await homeApi.getProactiveTips() as ApiResponse<ProactiveTip[]>
    if (res.code === 200) {
      proactiveTips.value = res.data || []
    }
  } catch (error) {
    console.error('获取主动提醒失败:', error)
  }
}

// 获取通知图标
const getNotificationIcon = (type: string) => {
  switch (type) {
    case 'SECONDHAND': return ShoppingBag
    case 'LOSTFOUND': return Search
    case 'DORMITORY_REPAIR': return Tools
    case 'REPAIR_COMPLETED': return Tools
    default: return Bell
  }
}

// 处理通知点击
const handleNotificationClick = (item: DashboardNotificationItem) => {
  if (item.relatedType === 'SECONDHAND') {
    router.push(`/secondhand/${item.relatedId}`)
  } else if (item.relatedType === 'LOSTFOUND') {
    router.push(`/lostfound/${item.relatedId}`)
  } else if (item.relatedType === 'DORMITORY_REPAIR') {
    if (userStore.isAdmin || userStore.isDormManager) {
      router.push('/dormitory/manage')
    } else {
      router.push('/dormitory/repair')
    }
  }
}

const getTipLevelType = (level: string) => {
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  return 'info'
}

const getTipLevelText = (level: string) => {
  if (level === 'HIGH') return '高优先'
  if (level === 'MEDIUM') return '中优先'
  return '低优先'
}

const resolveTipActionPath = (tip: ProactiveTip) => {
  const rawPath = String(tip?.actionPath || '').trim()
  if (!rawPath) return ''

  // 兼容历史路径：失物招领 query tab
  if (rawPath === '/lostfound?tab=my-lost') return '/lostfound/my-lost'
  if (rawPath === '/lostfound?tab=my-found') return '/lostfound/my-found'

  return rawPath
}

const handleTipClick = async (tip: ProactiveTip) => {
  const targetPath = resolveTipActionPath(tip)
  if (!targetPath) {
    ElMessage.warning('该提醒暂未配置跳转路径')
    return
  }

  try {
    await router.push(targetPath)
  } catch (error) {
    console.error('提醒跳转失败:', error)
    ElMessage.error('跳转失败，请稍后重试')
  }
}

// 模拟加载数据
onMounted(() => {
  fetchHomeData()
  fetchProactiveTips()
})
</script>

<style scoped>
.dashboard {
  max-width: 1440px;
  margin: 0 auto;
  position: relative;
  padding-bottom: 40px;
}

/* 背景水印 */
.bg-watermark {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.watermark-plum {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 600px;
  height: 600px;
  opacity: 0.03;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 200'%3E%3Cpath fill='%23004191' d='M100 20c-5 0-10 20-10 40s5 30 10 30 10-10 10-30-5-40-10-40zm0 80c-20 0-40 10-50 30 10 10 30 20 50 20s40-10 50-20c-10-20-30-30-50-30z'/%3E%3C/svg%3E");
  background-size: contain;
  background-repeat: no-repeat;
}

/* Hero Banner */
.hero-banner {
  position: relative;
  height: 160px;
  border-radius: 20px;
  overflow: hidden;
  margin-bottom: 20px;
  z-index: 1;
}

.hero-content {
  position: relative;
  height: 100%;
  padding: 32px 36px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 2;
}

.hero-gradient {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #004191 0%, #1a5cb8 50%, #2d7dd2 100%);
  z-index: 0;
}

.hero-text {
  color: #fff;
}

.hero-title {
  font-size: 32px;
  font-weight: 600;
  margin: 0 0 8px 0;
  letter-spacing: 0.5px;
}

.username {
  background: linear-gradient(90deg, #fff, #a8d8ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 14px;
  opacity: 0.85;
  margin: 0;
  font-weight: 300;
}

.hero-decoration {
  position: relative;
  width: 120px;
  height: 120px;
}

.decoration-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 60px;
  height: 60px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  animation: ringPulse 3s ease-in-out infinite;
}

.decoration-ring.delay-1 {
  animation-delay: 1s;
  width: 90px;
  height: 90px;
}

.decoration-ring.delay-2 {
  animation-delay: 2s;
  width: 120px;
  height: 120px;
}

@keyframes ringPulse {
  0%, 100% {
    opacity: 0.3;
    transform: translate(-50%, -50%) scale(1);
  }
  50% {
    opacity: 0.6;
    transform: translate(-50%, -50%) scale(1.1);
  }
}

/* 功能入口 */
.quick-entry-section {
  margin-bottom: 24px;
  z-index: 1;
  position: relative;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.entry-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.3s ease;
  animation: fadeInUp 0.5s ease-out both;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.entry-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
}

.entry-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  margin-right: 12px;
}

.entry-info {
  flex: 1;
  min-width: 0;
}

.entry-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 2px;
}

.entry-desc {
  font-size: 12px;
  color: #909399;
}

.entry-arrow {
  color: #c0c4cc;
  margin-left: 8px;
  transition: transform 0.3s ease;
}

.entry-card:hover .entry-arrow {
  transform: translateX(4px);
  color: var(--ustc-primary);
}

/* Bento Grid */
.bento-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 16px;
  margin-bottom: 20px;
  z-index: 1;
  position: relative;
}

.bento-left {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.bento-right {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 通用卡片样式 */
.bento-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  min-height: 180px;
}

.bento-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.more-link {
  font-size: 13px;
  color: #909399;
  cursor: pointer;
  transition: color 0.3s ease;
}

.more-link:hover {
  color: var(--ustc-primary);
}

.card-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-icon {
  color: var(--ustc-primary);
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.stat-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: rgba(64, 158, 255, 0.1);
  color: #409EFF;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.stat-tag.warning {
  background: rgba(230, 162, 60, 0.1);
  color: #E6A23C;
}

.card-content {
  min-height: 120px;
}

/* 二手交易/失物招领卡片 */
.items-preview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.items-preview.text-only {
  display: flex;
  gap: 16px;
  overflow: hidden;
}

.preview-item {
  background: #f5f7fa;
  border-radius: 12px;
  overflow: hidden;
  transition: transform 0.3s ease;
}

.preview-item.text-card {
  flex: 1;
  min-width: 0;
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  cursor: pointer;
  transition: all 0.3s ease;
}

.preview-item.text-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  border-color: #dcdfe6;
}

.text-card-main {
  flex: 1;
  min-width: 0;
}

.text-card-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 6px;
}

.text-card-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.text-card-price {
  font-size: 16px;
  font-weight: 600;
  color: #004191;
  margin-top: 12px;
}

.preview-item:hover {
  transform: scale(1.02);
}

.preview-img {
  width: 100%;
  height: 90px;
  object-fit: cover;
  background: linear-gradient(135deg, #e4e7ed, #d4d7dd);
}

.preview-info {
  padding: 10px;
}

.preview-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.preview-price {
  font-size: 14px;
  font-weight: 600;
  color: #F56C6C;
}

.preview-time {
  font-size: 12px;
  color: #909399;
}

/* 校车时间 Widget */
.card-shuttle {
  flex: 1;
}

.shuttle-content {
  padding: 0;
}

.shuttle-timeline {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.shuttle-item {
  padding: 10px 8px;
  background: #f5f7fa;
  border-radius: 10px;
  text-align: center;
  position: relative;
  transition: all 0.3s ease;
}

.shuttle-item:hover {
  background: #ecf5ff;
}

.shuttle-item.next-bus {
  background: linear-gradient(135deg, #004191 0%, #1a5cb8 100%);
  color: #fff;
}

.shuttle-time {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.shuttle-route {
  font-size: 11px;
  opacity: 0.8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.shuttle-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background: #F56C6C;
  color: #fff;
  font-size: 9px;
  padding: 2px 6px;
  border-radius: 10px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

/* 消息流 Widget */
.card-proactive {
  min-height: 220px;
}

.proactive-content {
  padding: 0;
}

.tip-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 180px;
  overflow-y: auto;
}

.tip-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  background: #f8fafc;
  border: 1px solid #edf2f7;
  border-radius: 10px;
  padding: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tip-item:hover {
  border-color: #d9ecff;
  background: #f5f9ff;
}

.tip-main {
  min-width: 0;
}

.tip-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.tip-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.card-messages {
  flex: 2;
  min-height: 280px;
}

.messages-content {
  padding: 0;
}

.message-list {
  max-height: 220px;
  overflow-y: auto;
}

.message-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.message-item:last-child {
  border-bottom: none;
}

.message-type-icon {
  color: var(--ustc-primary);
  margin-right: 10px;
  flex-shrink: 0;
  margin-top: 2px;
}

.message-info {
  flex: 1;
  min-width: 0;
}

.message-text {
  font-size: 13px;
  color: #303133;
  line-height: 1.4;
  margin-bottom: 4px;
}

.message-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.message-time {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}

/* 消息中心入口 */
.card-messages-center {
  background: linear-gradient(135deg, #fff 0%, #f8fafc 100%);
}

.messages-center-content {
  display: flex;
  align-items: center;
  padding: 8px;
}

.messages-center-icon {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #E6A23C 0%, #F56C6C 100%);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-right: 16px;
}

.messages-center-text {
  flex: 1;
}

.messages-center-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.messages-center-desc {
  font-size: 13px;
  color: #909399;
}

/* 空状态 */
.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px 20px;
  color: #c0c4cc;
}

.empty-state p {
  margin: 12px 0 0;
  font-size: 14px;
}

.empty-state.small {
  padding: 20px;
}

/* 统计区域 */
.stats-section {
  z-index: 1;
  position: relative;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  padding: 20px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-right: 14px;
}

.stat-icon.blue {
  background: linear-gradient(135deg, #409EFF, #67C23A);
}

.stat-icon.green {
  background: linear-gradient(135deg, #67C23A, #E6A23C);
}

.stat-icon.orange {
  background: linear-gradient(135deg, #E6A23C, #F56C6C);
}

.stat-icon.red {
  background: linear-gradient(135deg, #F56C6C, #909399);
}

.stat-data {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

/* 响应式布局 */
@media (max-width: 1200px) {
  .entry-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  
  .bento-grid {
    grid-template-columns: 1fr;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hero-banner {
    height: 120px;
  }
  
  .hero-title {
    font-size: 20px;
  }
  
  .hero-content {
    padding: 20px;
  }
  
  .hero-decoration {
    display: none;
  }
  
  .entry-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  
  .entry-card {
    padding: 12px;
  }
  
  .entry-icon-wrap {
    width: 36px;
    height: 36px;
    margin-right: 10px;
  }
  
  .entry-desc {
    display: none;
  }
  
  .items-preview {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .shuttle-timeline {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  
  .stat-item {
    padding: 16px;
  }
  
  .stat-value {
    font-size: 20px;
  }
  
  .stat-icon {
    width: 40px;
    height: 40px;
  }
}

@media (max-width: 480px) {
  .entry-grid {
    grid-template-columns: 1fr;
  }
  
  .bento-card {
    padding: 16px;
  }
  
  .items-preview {
    grid-template-columns: 1fr;
  }
  
  .preview-item {
    display: flex;
    align-items: center;
  }
  
  .preview-img {
    width: 60px;
    height: 60px;
  }
}
</style>
