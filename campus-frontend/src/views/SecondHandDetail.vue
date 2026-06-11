<template>
  <div class="detail-page">
    <!-- 返回按钮 -->
    <div class="back-nav">
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
    </div>

    <div class="detail-container" v-loading="loading">
      <!-- 商品不存在 -->
      <el-empty v-if="!loading && !goodsDetail" description="商品不存在或已下架" />

      <template v-else-if="goodsDetail">
        <div class="detail-main">
          <!-- 左侧图片区域 -->
          <div class="image-section">
            <!-- 图片轮播 -->
            <el-carousel
              v-if="goodsDetail.images?.length > 1"
              :interval="5000"
              arrow="always"
              indicator-position="outside"
              class="image-carousel"
            >
              <el-carousel-item v-for="(img, index) in goodsDetail.images" :key="index">
                <div class="carousel-image-wrapper">
                  <img :src="img" :alt="`${goodsDetail.title} ${index + 1}`" />
                </div>
              </el-carousel-item>
            </el-carousel>
            <!-- 单图展示 -->
            <div v-else class="main-image">
              <img
                :src="getImageUrl(goodsDetail.images)"
                :alt="goodsDetail.title"
              />
              <!-- 状态标签 -->
              <div class="status-badge" :class="getStatusClass(goodsDetail.status)">
                {{ getStatusText(goodsDetail.status) }}
              </div>
            </div>
            <!-- 缩略图列表 -->
            <div class="thumbnail-list" v-if="goodsDetail.images?.length > 1">
              <div
                v-for="(img, index) in goodsDetail.images"
                :key="index"
                class="thumbnail-item"
                @click="currentImageIndex = index"
              >
                <img :src="img" :alt="`${goodsDetail.title} ${index + 1}`" />
              </div>
            </div>
          </div>

          <!-- 右侧信息区域 -->
          <div class="info-section">
            <h1 class="goods-title">{{ goodsDetail.title }}</h1>

            <div class="price-row">
              <span class="price">¥{{ goodsDetail.price }}</span>
              <span class="price-label">价格</span>
            </div>

            <el-descriptions :column="1" border class="info-descriptions">
              <el-descriptions-item label="商品分类">
                <el-tag size="small">{{ goodsDetail.categoryText }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="新旧程度">
                {{ getConditionText(goodsDetail.condition) }}
              </el-descriptions-item>
              <el-descriptions-item label="发布时间">
                {{ formatDateTime(goodsDetail.createTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="浏览次数">
                {{ goodsDetail.viewCount || 0 }} 次
              </el-descriptions-item>
            </el-descriptions>

            <!-- 商品描述 -->
            <div class="description-section">
              <h3>商品描述</h3>
              <p>{{ goodsDetail.description || '暂无描述' }}</p>
            </div>

            <!-- 交易引导提示 -->
            <div class="trade-tips" v-if="canContact">
              <el-icon><InfoFilled /></el-icon>
              <span>建议在中科大校内当面交易，验货后再付款，安全有保障</span>
            </div>

            <!-- 操作按钮 -->
            <div class="action-buttons">
              <!-- 收藏按钮 -->
              <el-button
                :type="isFavorited ? 'warning' : 'default'"
                size="large"
                @click="handleFavorite"
              >
                <el-icon><Star /></el-icon>
                {{ isFavorited ? '已收藏' : '收藏' }}
              </el-button>

              <!-- 联系卖家按钮 -->
              <el-button
                v-if="canContact"
                type="primary"
                size="large"
                @click="contactSeller"
                class="contact-btn"
              >
                <el-icon><ChatDotRound /></el-icon>
                我想要
              </el-button>

              <el-button
                v-if="canContact"
                size="large"
                @click="showMeetupRecommendation"
              >
                <el-icon><Location /></el-icon>
                面交建议
              </el-button>

              <el-button
                v-if="canContact"
                size="large"
                @click="handleBargainTemplate"
              >
                <el-icon><EditPen /></el-icon>
                一键议价
              </el-button>

              <!-- 自己的商品提示 -->
              <el-alert
                v-if="goodsDetail.sellerId === userStore.userInfo?.userId"
                title="这是您发布的商品"
                type="info"
                show-icon
                :closable="false"
                class="status-alert"
              />

              <!-- 状态提示 -->
              <el-alert
                v-if="goodsDetail.status === 'PENDING'"
                title="商品正在审核中，暂不能购买"
                type="warning"
                show-icon
                :closable="false"
                class="status-alert"
              />
              <el-alert
                v-if="goodsDetail.status === 'SOLD'"
                title="该商品已售出"
                type="info"
                show-icon
                :closable="false"
                class="status-alert"
              />
            </div>
          </div>
        </div>

        <!-- 卖家信息 -->
        <div class="seller-section">
          <h3>卖家信息</h3>
          <div class="seller-info">
            <el-avatar :size="56" :src="goodsDetail.sellerAvatar">
              {{ goodsDetail.sellerName?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="seller-detail">
              <div class="seller-name">{{ goodsDetail.sellerName || '未知用户' }}</div>
              <div class="seller-meta">
                <span v-if="goodsDetail.sellerCampus">
                  <el-icon><Location /></el-icon>
                  {{ goodsDetail.sellerCampus }}
                </span>
                <span>发布时间: {{ formatDateTime(goodsDetail.createTime) }}</span>
              </div>
            </div>
          </div>

          <div class="trust-card" v-if="trustScore">
            <div class="trust-header">
              <span class="trust-title">卖家可信度</span>
              <el-tag :type="trustLevelTag(trustScore.level)">
                {{ trustScore.score }}分 · {{ trustLevelText(trustScore.level) }}
              </el-tag>
            </div>
            <div class="trust-meta">
              <span>历史发布 {{ trustScore.totalListings || 0 }}</span>
              <span>历史成交 {{ trustScore.soldCount || 0 }}</span>
              <span v-if="trustScore.avgReplyMinutes != null">
                平均回复 {{ trustScore.avgReplyMinutes }} 分钟
              </span>
            </div>
            <ul class="trust-highlights" v-if="trustScore.highlights?.length">
              <li v-for="(tip, idx) in trustScore.highlights" :key="idx">{{ tip }}</li>
            </ul>
          </div>
        </div>
      </template>
    </div>

    <el-dialog
      v-model="showMeetupDialog"
      title="校区面交建议"
      width="620px"
      :close-on-click-modal="false"
    >
      <div v-loading="meetupLoading">
        <div v-if="meetupData">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="你的校区">{{ meetupData.buyerCampus }}</el-descriptions-item>
            <el-descriptions-item label="卖家校区">{{ meetupData.sellerCampus }}</el-descriptions-item>
            <el-descriptions-item label="推荐会合校区">{{ meetupData.recommendedCampus }}</el-descriptions-item>
            <el-descriptions-item label="推荐理由">{{ meetupData.reason }}</el-descriptions-item>
          </el-descriptions>
          <div class="meetup-section">
            <h4>推荐面交点</h4>
            <div class="spot-list">
              <div v-for="(spot, idx) in meetupData.suggestedSpots || []" :key="`spot-${idx}`" class="spot-row">
                <span>{{ spot }}</span>
                <el-button size="small" type="primary" plain @click="goMeetupNavigation(spot)">
                  <el-icon><Location /></el-icon>
                  去这里
                </el-button>
              </div>
            </div>
          </div>
          <div class="meetup-section">
            <h4>建议时间段</h4>
            <ul>
              <li v-for="(slot, idx) in meetupData.suggestedTimeSlots || []" :key="`slot-${idx}`">{{ slot }}</li>
            </ul>
          </div>
          <div class="meetup-section">
            <h4>安全提醒</h4>
            <ul>
              <li v-for="(tip, idx) in meetupData.safetyTips || []" :key="`tip-${idx}`">{{ tip }}</li>
            </ul>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { secondhandApi } from '@/api'
import { useUserStore } from '@/stores'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Star, ChatDotRound, InfoFilled, Location, EditPen } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 加载状态
const loading = ref(false)

// 商品详情
const goodsDetail = ref(null)

// 当前查看的图片索引
const currentImageIndex = ref(0)

// 收藏状态
const isFavorited = ref(false)

// 卖家可信度
const trustScore = ref(null)

// 面交建议
const showMeetupDialog = ref(false)
const meetupData = ref(null)
const meetupLoading = ref(false)

// 分类映射
const categoryMap = {
  BOOKS: '教材',
  DIGITAL: '数码',
  APPLIANCE: '电器',
  DAILY: '生活',
  OTHER: '其他'
}

// 新旧程度映射
const conditionMap = {
  NEW: '全新',
  LIKE_NEW: '几乎全新',
  GOOD: '有明显使用痕迹'
}

// 是否可以联系卖家
const canContact = computed(() => {
  if (!goodsDetail.value) return false
  if (goodsDetail.value.status === 'SOLD') return false
  if (goodsDetail.value.status === 'PENDING') return false
  if (goodsDetail.value.sellerId === userStore.userInfo?.userId) return false
  return true
})

// 获取商品详情
const fetchDetail = async () => {
  const id = route.params.id
  if (!id) return

  loading.value = true
  try {
    const res = await secondhandApi.getDetail(id)
    // 处理图片数据
    let images = res.data.images
    if (typeof images === 'string') {
      try {
        images = JSON.parse(images)
      } catch (e) {
        images = images ? [images] : []
      }
    }
    goodsDetail.value = {
      ...res.data,
      images: Array.isArray(images) ? images : [],
      categoryText: categoryMap[res.data.category] || res.data.category
    }

    if (goodsDetail.value?.sellerId) {
      fetchSellerTrust(goodsDetail.value.sellerId)
    }

    // 只有登录用户才检查收藏状态
    if (userStore.isLoggedIn) {
      checkFavoriteStatus(id)
    }
  } catch (error) {
    console.error('获取商品详情失败:', error)
    goodsDetail.value = null
  } finally {
    loading.value = false
  }
}

const fetchSellerTrust = async (sellerId) => {
  try {
    const res = await secondhandApi.getSellerTrust(sellerId)
    trustScore.value = res.data || null
  } catch (error) {
    console.error('获取卖家可信度失败:', error)
    trustScore.value = null
  }
}

// 获取图片URL
const getImageUrl = (images) => {
  if (!images) return '/src/assets/placeholder.png'
  if (typeof images === 'string') {
    try {
      const arr = JSON.parse(images)
      if (Array.isArray(arr) && arr.length > 0) return arr[0]
    } catch (e) {
      return images
    }
  }
  if (Array.isArray(images) && images.length > 0) return images[0]
  return '/src/assets/placeholder.png'
}

// 检查收藏状态
const checkFavoriteStatus = async (id) => {
  try {
    const res = await secondhandApi.getFavoriteStatus(id)
    isFavorited.value = res.data || false
  } catch (error) {
    console.error('检查收藏状态失败:', error)
  }
}

// 收藏/取消收藏
const favoriteLoading = ref(false)
const handleFavorite = async () => {
  if (!goodsDetail.value) return
  
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  
  if (favoriteLoading.value) return
  favoriteLoading.value = true

  try {
    if (isFavorited.value) {
      await secondhandApi.unfavorite(goodsDetail.value.id)
      isFavorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await secondhandApi.favorite(goodsDetail.value.id)
      isFavorited.value = true
      ElMessage.success('收藏成功')
    }
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error(error.message || '操作失败，请稍后重试')
  } finally {
    favoriteLoading.value = false
  }
}

// 联系卖家
const contactSeller = () => {
  if (!goodsDetail.value) return

  router.push({
    path: '/messages',
    query: { 
      userId: goodsDetail.value.sellerId, 
      itemId: goodsDetail.value.id,
      itemTitle: goodsDetail.value.title
    }
  })
}

// 校区面交建议
const showMeetupRecommendation = async () => {
  if (!goodsDetail.value) return
  meetupLoading.value = true
  showMeetupDialog.value = true
  try {
    const res = await secondhandApi.getMeetupRecommendation(goodsDetail.value.id)
    meetupData.value = res.data
  } catch (error) {
    console.error('获取面交建议失败:', error)
    ElMessage.error(error.message || '获取面交建议失败')
    showMeetupDialog.value = false
  } finally {
    meetupLoading.value = false
  }
}

const goMeetupNavigation = (spot) => {
  if (!spot) return
  router.push({
    path: '/navigation',
    query: {
      destination: spot,
      campus: meetupData.value?.recommendedCampus || '',
      source: 'secondhand',
      intent: 'meetup'
    }
  })
}

// 一键议价模板
const handleBargainTemplate = async () => {
  if (!goodsDetail.value) return
  try {
    const res = await secondhandApi.getBargainTemplate(goodsDetail.value.id)
    const draft = res.data?.quickCopy || ''
    router.push({
      path: '/messages',
      query: {
        userId: goodsDetail.value.sellerId,
        itemId: goodsDetail.value.id,
        itemTitle: goodsDetail.value.title,
        draft
      }
    })
  } catch (error) {
    console.error('生成议价模板失败:', error)
    ElMessage.error(error.message || '生成议价模板失败')
  }
}

// 返回
const goBack = () => {
  // 返回时刷新列表数据，确保浏览量同步
  router.back()
  // 触发列表刷新事件
  window.dispatchEvent(new CustomEvent('refresh-secondhand-list'))
}

// 辅助函数
const getStatusClass = (status) => {
  const map = {
    PENDING: 'status-pending',
    AUDITING: 'status-pending',
    ACTIVE: 'status-available',
    SOLD: 'status-sold'
  }
  return map[status] || 'status-available'
}

const getStatusText = (status) => {
  const map = {
    PENDING: '审核中',
    APPROVED: '在售',
    REJECTED: '审核拒绝',
    REMOVED: '已下架',
    SOLD: '已售出'
  }
  return map[status] || '在售'
}

const getConditionText = (condition) => {
  return conditionMap[condition] || condition || '未填写'
}

const trustLevelText = (level) => {
  const map = {
    HIGH: '高可信',
    MEDIUM: '中可信',
    LOW: '待验证'
  }
  return map[level] || '待验证'
}

const trustLevelTag = (level) => {
  if (level === 'HIGH') return 'success'
  if (level === 'MEDIUM') return 'warning'
  return 'info'
}

const formatDateTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.back-nav {
  margin-bottom: 20px;
}

.back-nav .el-button {
  color: #606266;
}

.back-nav .el-button:hover {
  color: #004191;
}

.detail-container {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 4px 20px rgba(0, 65, 145, 0.08);
}

.detail-main {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  margin-bottom: 32px;
}

/* 左侧图片 */
.image-section {
  position: sticky;
  top: 20px;
}

/* 图片轮播 */
.image-carousel {
  border-radius: 16px;
  overflow: hidden;
}

.image-carousel :deep(.el-carousel__container) {
  aspect-ratio: 4 / 3;
}

.carousel-image-wrapper {
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.carousel-image-wrapper img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.image-carousel :deep(.el-carousel__arrow) {
  background: rgba(0, 65, 145, 0.8);
}

.image-carousel :deep(.el-carousel__arrow:hover) {
  background: #004191;
}

.image-carousel :deep(.el-carousel__indicators--outside) {
  margin-top: 12px;
}

.image-carousel :deep(.el-carousel__indicator--horizontal .el-carousel__button) {
  background: #dcdfe6;
}

.image-carousel :deep(.el-carousel__indicator--horizontal.is-active .el-carousel__button) {
  background: #004191;
}

.main-image {
  position: relative;
  aspect-ratio: 4 / 3;
  border-radius: 16px;
  overflow: hidden;
  background: #f5f7fa;
}

.main-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.status-badge {
  position: absolute;
  top: 16px;
  right: 16px;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  color: #fff;
}

.status-pending {
  background: #e6a23c;
}

.status-available {
  background: #004191;
}

.status-sold {
  background: #909399;
}

.thumbnail-list {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  overflow-x: auto;
}

.thumbnail-item {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;
  flex-shrink: 0;
}

.thumbnail-item:hover {
  border-color: #004191;
}

.thumbnail-item.active {
  border-color: #004191;
}

.thumbnail-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 右侧信息 */
.info-section {
  min-width: 0;
}

.goods-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px 0;
  line-height: 1.4;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 24px;
  padding: 16px;
  background: linear-gradient(135deg, #E8F4FD 0%, #D4E9F7 100%);
  border-radius: 12px;
}

.price {
  font-size: 36px;
  font-weight: 700;
  color: #004191;
}

.price-label {
  font-size: 14px;
  color: #909399;
}

.info-descriptions {
  margin-bottom: 24px;
}

.info-descriptions :deep(.el-descriptions__label) {
  background: #f8fafc;
  color: #606266;
  font-weight: 500;
}

.info-descriptions :deep(.el-descriptions__content) {
  color: #303133;
}

.description-section {
  margin-bottom: 24px;
}

.description-section h3 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
}

.description-section p {
  color: #606266;
  line-height: 1.8;
  margin: 0;
  white-space: pre-wrap;
}

/* 交易引导提示 */
.trade-tips {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #fff7e6 0%, #fffbe6 100%);
  border: 1px solid #ffd591;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #d48806;
}

.trade-tips .el-icon {
  font-size: 16px;
  color: #fa8c16;
}

.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.contact-btn {
  flex: 1;
  min-width: 200px;
}

.status-alert {
  width: 100%;
  margin-top: 8px;
}

/* 卖家信息 */
.seller-section {
  padding-top: 32px;
  border-top: 1px solid #f0f2f5;
}

.seller-section h3 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
}

.seller-detail {
  flex: 1;
}

.seller-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.seller-meta {
  font-size: 13px;
  color: #909399;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.seller-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.seller-meta .el-icon {
  font-size: 14px;
}

.trust-card {
  margin-top: 16px;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #e4e7ed;
  background: #fafcff;
}

.trust-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.trust-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.trust-meta {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: #606266;
}

.trust-highlights {
  margin: 10px 0 0;
  padding-left: 18px;
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
}

.meetup-section {
  margin-top: 14px;
}

.meetup-section h4 {
  margin: 0 0 8px;
  font-size: 14px;
  color: #303133;
}

.meetup-section ul {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  line-height: 1.7;
}

.spot-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.spot-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f7f9fc;
  color: #606266;
  font-size: 13px;
}

/* 响应式 */
@media (max-width: 992px) {
  .detail-main {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .image-section {
    position: static;
  }
}

@media (max-width: 768px) {
  .detail-container {
    padding: 20px;
  }

  .goods-title {
    font-size: 20px;
  }

  .price {
    font-size: 28px;
  }

  .action-buttons {
    flex-direction: column;
  }

  .contact-btn {
    width: 100%;
  }
}
</style>
