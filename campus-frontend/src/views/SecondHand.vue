<template>
  <div class="page-container" :key="route.fullPath">
    <!-- 逛逛市场 -->
    <div v-show="currentTab === 'market'" class="market-container">
          <!-- 左侧分类侧边栏 -->
          <aside class="category-sidebar">
            <div class="sidebar-title">商品分类</div>
            <ul class="category-list">
              <li
                v-for="cat in categories"
                :key="cat.value"
                :class="['category-item', { active: marketParams.category === cat.value }]"
                @click="handleCategoryChange(cat.value)"
              >
                <el-icon><component :is="cat.icon" /></el-icon>
                <span>{{ cat.label }}</span>
              </li>
            </ul>
          </aside>

          <!-- 右侧商品区域 -->
          <main class="market-main">
            <!-- 搜索和排序 -->
            <div class="market-toolbar">
              <el-input
                v-model="marketParams.keyword"
                placeholder="搜索商品名称"
                clearable
                class="search-input"
                @clear="handleSearch"
                @keyup.enter="handleSearch"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
                <template #append>
                  <el-button @click="handleSearch">搜索</el-button>
                </template>
              </el-input>
              <el-select v-model="marketParams.sort" placeholder="排序" @change="handleSortChange">
                <el-option label="智能推荐" value="smart" />
                <el-option label="最新发布" value="latest" />
                <el-option label="价格从低到高" value="price_asc" />
                <el-option label="价格从高到低" value="price_desc" />
              </el-select>
              <el-button class="subscribe-btn" @click="openSubscriptionDialog">
                <el-icon><Bell /></el-icon>
                求购订阅
              </el-button>
            </div>

            <!-- 商品网格容器 - 固定最小高度防止塌陷 -->
            <div class="goods-grid" :key="marketParams.category">
              <!-- 骨架屏 - 仅在首次加载且无数据时显示 -->
              <Transition name="fade-slide" mode="out-in">
                <div v-if="marketLoading && marketList.length === 0" key="skeleton" class="skeleton-grid">
                  <el-skeleton
                    v-for="i in 8"
                    :key="i"
                    animated
                    class="skeleton-card"
                  >
                    <template #template>
                      <el-skeleton-item variant="image" class="skeleton-image" />
                      <div class="skeleton-content">
                        <el-skeleton-item variant="h3" class="skeleton-title" />
                        <el-skeleton-item variant="text" class="skeleton-price" />
                        <el-skeleton-item variant="text" class="skeleton-meta" />
                      </div>
                    </template>
                  </el-skeleton>
                </div>

                <!-- 暂无商品空状态 - 请求完成后且无数据时显示 -->
                <div v-else-if="!marketLoading && marketList.length === 0" key="empty" class="empty-state-wrapper">
                  <el-empty description="该分类下暂无商品，去看看别的吧">
                    <template #image>
                      <div class="empty-image">
                        <el-icon :size="80" color="#c0c4cc"><Shop /></el-icon>
                      </div>
                    </template>
                    <el-button
                      type="primary"
                      class="publish-btn"
                      @click="openPublishDialog"
                    >
                      去发布商品
                    </el-button>
                  </el-empty>
                </div>

                <!-- 商品列表 - 有数据时显示 -->
                <div v-else key="list" class="goods-list-grid">
                  <div
                    v-for="item in marketList"
                    :key="item.id"
                    class="goods-card"
                    @click="goToDetail(item.id)"
                  >
                    <div class="card-image">
                      <img
                        :src="getImageUrl(item.images)"
                        :alt="item.title"
                        @error="handleImageError"
                      />
                      <!-- 状态标签 -->
                      <div class="status-tag" :class="getStatusClass(item.status)">
                        {{ getStatusText(item.status) }}
                      </div>
                      <!-- 收藏按钮 -->
                      <button
                        class="favorite-btn"
                        :class="{ favorited: item.isFavorited }"
                        @click.stop="handleFavorite(item)"
                      >
                        <el-icon><Star /></el-icon>
                      </button>
                    </div>
                    <div class="card-content">
                      <h3 class="card-title">{{ item.title }}</h3>
                      <p class="card-description" v-if="item.description">{{ item.description }}</p>
                      <div class="card-price">¥{{ item.price }}</div>
                      <div v-if="item.recommendReason" class="recommend-row">
                        <span class="recommend-tag">推荐理由：{{ item.recommendReason }}</span>
                      </div>
                      <div class="card-meta">
                        <span class="category-tag">{{ item.categoryText }}</span>
                        <span class="time-text">{{ formatTime(item.createTime) }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </Transition>
            </div>

          <!-- 分页 -->
            <div class="pagination" v-if="marketTotal > 0">
              <el-pagination
                v-model:current-page="marketParams.page"
                v-model:page-size="marketParams.size"
                :total="marketTotal"
                :page-sizes="[8, 12, 16, 20]"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleMarketSizeChange"
                @current-change="handleMarketPageChange"
              />
            </div>
          </main>
        </div>

        <!-- 我的发布 -->
        <div v-show="currentTab === 'mystore' && route.query.tab !== 'favorites'" class="mystore-container">
          <!-- 工具栏 -->
          <div class="mystore-toolbar">
            <el-button type="primary" @click="openPublishDialog">
              <el-icon><Plus /></el-icon>
              发布商品
            </el-button>
            <!-- 状态筛选 -->
            <div class="status-filters">
              <el-radio-group v-model="myStoreParams.status" @change="fetchMyStoreList">
                <el-radio-button value="">全部</el-radio-button>
                <el-radio-button value="PENDING">审核中</el-radio-button>
                <el-radio-button value="APPROVED">在售</el-radio-button>
                <el-radio-button value="REJECTED">已拒绝</el-radio-button>
                <el-radio-button value="REMOVED">已下架</el-radio-button>
                <el-radio-button value="SOLD">已售出</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <!-- 我的发布列表 -->
          <div class="my-goods-list-wrapper" :key="myStoreParams.status">
            <Transition name="fade-slide" mode="out-in">
              <div v-if="myStoreLoading" key="loading" class="my-goods-list">
                <div v-loading="myStoreLoading" class="loading-skeleton-wrapper">
                  <el-skeleton v-for="i in 5" :key="i" animated :rows="2" />
                </div>
              </div>

              <el-empty
                v-else-if="myStoreList.length === 0"
                key="empty"
                description="暂无商品"
                class="my-goods-empty"
              />

              <div v-else key="list" class="my-goods-list">
                <el-table :data="myStoreList" style="width: 100%" stripe>
                  <el-table-column label="商品信息" min-width="300">
                    <template #default="{ row }">
                      <div class="table-goods-info">
                        <img
                          :src="getImageUrl(row.images)"
                          class="table-goods-image"
                          @error="handleImageError"
                        />
                        <div class="table-goods-detail">
                          <div class="table-goods-title">{{ row.title }}</div>
                          <div class="table-goods-meta">
                            <span>分类: {{ row.categoryText }}</span>
                            <span>价格: ¥{{ row.price }}</span>
                          </div>
                        </div>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column label="状态" width="100">
                    <template #default="{ row }">
                      <el-tag :type="getStatusTagType(row.status)" size="small">
                        {{ getStatusText(row.status) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="浏览" width="80">
                    <template #default="{ row }">
                      {{ row.viewCount || 0 }}
                    </template>
                  </el-table-column>
                  <el-table-column label="发布时间" width="120">
                    <template #default="{ row }">
                      {{ formatTime(row.createTime) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="200" fixed="right">
                    <template #default="{ row }">
                      <!-- 在售状态：标记已售、编辑、下架 -->
                      <template v-if="row.status === 'APPROVED'">
                        <el-button
                          type="primary"
                          size="small"
                          link
                          @click="handleMarkSold(row)"
                        >
                          标记已售
                        </el-button>
                        <el-button
                          size="small"
                          link
                          @click="handleEdit(row)"
                        >
                          编辑
                        </el-button>
                        <el-button
                          type="danger"
                          size="small"
                          link
                          @click="handleRemove(row)"
                        >
                          下架
                        </el-button>
                      </template>
                      <!-- 已售出状态：重新上架、编辑、删除 -->
                      <template v-else-if="row.status === 'SOLD'">
                        <el-button
                          type="success"
                          size="small"
                          link
                          @click="handleRelist(row)"
                        >
                          重新上架
                        </el-button>
                        <el-button
                          size="small"
                          link
                          @click="handleEdit(row)"
                        >
                          编辑
                        </el-button>
                        <el-button
                          type="danger"
                          size="small"
                          link
                          @click="handleDelete(row)"
                        >
                          删除
                        </el-button>
                      </template>
                      <!-- 已下架/待审核/审核拒绝状态：重新上架、删除 -->
                      <template v-else-if="row.status === 'REMOVED'">
                        <el-button
                          type="success"
                          size="small"
                          link
                          @click="handleRelist(row)"
                        >
                          重新上架
                        </el-button>
                        <el-button
                          size="small"
                          link
                          @click="handleEdit(row)"
                        >
                          编辑
                        </el-button>
                        <el-button
                          type="danger"
                          size="small"
                          link
                          @click="handleDelete(row)"
                        >
                          删除
                        </el-button>
                      </template>
                      <!-- 待审核状态：撤销审核、删除 -->
                      <template v-else-if="row.status === 'PENDING'">
                        <el-button
                          type="warning"
                          size="small"
                          link
                          @click="handleRemove(row)"
                        >
                          撤销审核
                        </el-button>
                        <el-button
                          type="danger"
                          size="small"
                          link
                          @click="handleDelete(row)"
                        >
                          删除
                        </el-button>
                      </template>
                      <!-- 审核拒绝状态：重新上架、编辑、删除 -->
                      <template v-else-if="row.status === 'REJECTED'">
                        <el-button
                          type="success"
                          size="small"
                          link
                          @click="handleRelist(row)"
                        >
                          重新上架
                        </el-button>
                        <el-button
                          size="small"
                          link
                          @click="handleEdit(row)"
                        >
                          编辑
                        </el-button>
                        <el-button
                          type="danger"
                          size="small"
                          link
                          @click="handleDelete(row)"
                        >
                          删除
                        </el-button>
                      </template>
                    </template>
                  </el-table-column>
                </el-table>

                <!-- 分页 -->
                <div class="pagination" v-if="myStoreTotal > 0">
                  <el-pagination
                    v-model:current-page="myStoreParams.page"
                    v-model:page-size="myStoreParams.size"
                    :total="myStoreTotal"
                    :page-sizes="[10, 20, 30]"
                    layout="total, sizes, prev, pager, next, jumper"
                    @size-change="handleMyStoreSizeChange"
                    @current-change="handleMyStorePageChange"
                  />
                </div>
              </div>
            </Transition>
          </div>
        </div>

        <!-- 我的收藏 - 独立容器 -->
        <div v-show="route.query.tab === 'favorites'" class="favorites-container">
          <div class="favorites-header">
            <h2>我的收藏</h2>
            <span class="favorites-count">共 {{ favoritesTotal }} 件商品</span>
          </div>

          <div class="favorites-list-wrapper">
            <Transition name="fade-slide" mode="out-in">
              <div v-if="favoritesLoading" key="loading" class="favorites-loading">
                <div v-loading="favoritesLoading" class="loading-skeleton-wrapper">
                  <el-skeleton v-for="i in 8" :key="i" animated :rows="3" />
                </div>
              </div>

              <el-empty
                v-else-if="favoritesList.length === 0"
                key="empty"
                description="暂无收藏"
                class="favorites-empty"
              />

              <div v-else key="list" class="favorites-content">
                <div class="favorites-grid">
                  <div
                    v-for="item in favoritesList"
                    :key="item.id"
                    class="goods-card"
                    @click="goToDetail(item.id)"
                  >
                    <div class="card-image">
                      <img :src="getImageUrl(item.images)" :alt="item.title" @error="handleImageError" />
                      <div class="status-tag" :class="getStatusClass(item.status)">
                        {{ getStatusText(item.status) }}
                      </div>
                      <button
                        class="favorite-btn favorited"
                        @click.stop="handleRemoveFavorite(item)"
                      >
                        <el-icon><Star /></el-icon>
                      </button>
                    </div>
                    <div class="card-content">
                      <h3 class="card-title">{{ item.title }}</h3>
                      <p class="card-description" v-if="item.description">{{ item.description }}</p>
                      <div class="card-price">¥{{ item.price }}</div>
                      <div class="card-meta">
                        <span class="category-tag">{{ getCategoryText(item.category) }}</span>
                        <span class="time-text">{{ formatTime(item.createTime) }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <!-- 分页 -->
              <div v-if="favoritesTotal > favoritesParams.size" class="favorites-pagination">
                <el-pagination
                  v-model:current-page="favoritesParams.page"
                  :page-size="favoritesParams.size"
                  :total="favoritesTotal"
                  layout="prev, pager, next, total"
                  background
                  @current-change="handleFavoritesPageChange"
                />
              </div>
            </div>
          </Transition>
        </div>
    </div>

    <!-- 发布/编辑商品弹窗 -->
    <el-dialog
      v-model="showPublishDialog"
      :title="isEdit ? '编辑商品' : '发布商品'"
      width="680px"
      :close-on-click-modal="false"
      class="publish-dialog"
    >
      <el-form
        :model="publishForm"
        :rules="publishRules"
        ref="publishFormRef"
        label-width="100px"
      >
        <el-form-item label="商品图片" prop="images">
          <div class="image-upload">
            <div class="upload-list" v-if="publishForm.images?.length">
              <div v-for="(img, index) in publishForm.images" :key="index" class="upload-item">
                <img :src="img.ossUrl || img.localUrl" />
                <div v-if="img.uploading" class="upload-loading">
                  <el-icon class="is-loading"><Loading /></el-icon>
                </div>
                <div class="upload-item-actions" @click="removeImage(index)">
                  <el-icon><Close /></el-icon>
                </div>
              </div>
            </div>
            <el-upload
              v-if="!publishForm.images || publishForm.images.length < 5"
              class="upload-trigger"
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleImageChange"
              accept="image/*"
            >
              <el-icon><Plus /></el-icon>
              <div class="upload-text">上传图片</div>
            </el-upload>
          </div>
          <div class="upload-tip">最多上传5张图片，建议尺寸800x800</div>
        </el-form-item>

        <el-form-item label="商品标题" prop="title">
          <el-input
            v-model="publishForm.title"
            placeholder="请输入商品名称"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="商品分类" prop="category">
          <el-select v-model="publishForm.category" placeholder="请选择分类" style="width: 100%;">
            <el-option label="教材" value="BOOKS" />
            <el-option label="数码" value="DIGITAL" />
            <el-option label="电器" value="APPLIANCE" />
            <el-option label="生活" value="DAILY" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>

        <el-form-item label="新旧程度" prop="condition">
          <el-radio-group v-model="publishForm.condition">
            <el-radio value="NEW">全新</el-radio>
            <el-radio value="LIKE_NEW">几乎全新</el-radio>
            <el-radio value="GOOD">有明显使用痕迹</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="商品价格" prop="price">
          <el-input-number
            v-model="publishForm.price"
            :min="0"
            :precision="2"
            :step="1"
            style="width: 100%;"
          />
        </el-form-item>

        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="publishForm.description"
            type="textarea"
            :rows="4"
            placeholder="请详细描述商品信息"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublishDialog = false">取消</el-button>
        <el-button type="primary" @click="submitPublish" :loading="publishLoading">
          {{ isEdit ? '保存修改' : '立即发布' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showSubscriptionDialog"
      title="求购订阅"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form :model="subscriptionForm" label-width="88px" class="subscription-form">
        <el-form-item label="关键词" required>
          <el-input
            v-model="subscriptionForm.keyword"
            placeholder="例如：台灯 / 自行车 / 耳机"
            maxlength="40"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="subscriptionForm.category" placeholder="不限" clearable style="width: 100%">
            <el-option label="教材" value="BOOKS" />
            <el-option label="数码" value="DIGITAL" />
            <el-option label="电器" value="APPLIANCE" />
            <el-option label="生活" value="DAILY" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="预算上限">
          <el-input-number
            v-model="subscriptionForm.maxPrice"
            :min="1"
            :precision="2"
            :step="10"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="校区偏好">
          <el-select v-model="subscriptionForm.campus" placeholder="不限" clearable style="width: 100%">
            <el-option
              v-for="campus in campusOptions"
              :key="campus"
              :label="campus"
              :value="campus"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="subscriptionSubmitting"
            @click="handleCreateSubscription"
          >
            创建订阅
          </el-button>
        </el-form-item>
      </el-form>

      <el-divider>我的订阅</el-divider>
      <div v-loading="subscriptionLoading">
        <el-empty v-if="!subscriptionLoading && subscriptions.length === 0" description="还没有订阅规则" />
        <el-table v-else :data="subscriptions" size="small" stripe>
          <el-table-column prop="keyword" label="关键词" min-width="120" />
          <el-table-column label="条件" min-width="220">
            <template #default="{ row }">
              <span>
                {{ row.category || '不限分类' }}
                · {{ row.maxPrice ? `≤ ¥${row.maxPrice}` : '不限预算' }}
                · {{ row.campus || '不限校区' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button type="danger" link @click="handleDeleteSubscription(row)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, shallowRef, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { secondhandApi } from '@/api'
import { uploadFile } from '@/api/request'
import { useUserStore } from '@/stores'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Plus,
  Close,
  Star,
  Bell,
  Delete,
  Shop,
  Box,
  Grid,
  Present,
  Picture,
  Loading
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 根据 URL 参数获取当前 Tab
const currentTab = computed(() => {
  const tab = route.query.tab
  if (tab === 'mystore' || tab === 'favorites') return 'mystore'
  return 'market'
})

// 分类配置
const categories = [
  { label: '全部', value: '', icon: Grid },
  { label: '教材', value: 'BOOKS', icon: Box },
  { label: '数码', value: 'DIGITAL', icon: Picture },
  { label: '电器', value: 'APPLIANCE', icon: Present },
  { label: '生活', value: 'DAILY', icon: Present },
  { label: '其他', value: 'OTHER', icon: Grid }
]

// 分类映射
const categoryMap = {
  BOOKS: '教材',
  DIGITAL: '数码',
  APPLIANCE: '电器',
  DAILY: '生活',
  OTHER: '其他'
}

// ====== 逛逛市场相关 ======
const marketLoading = ref(false)
const marketList = ref([])
const marketTotal = ref(0)
// 用于跟踪当前请求的分类，确保切换时旧数据保留
const pendingCategory = ref('')
// 加载防抖定时器
let loadingTimer = null
let loadingRequestId = 0
const marketParams = reactive({
  page: 1,
  size: 12,
  keyword: '',
  category: '',
  sort: 'smart'
})

// ====== 求购订阅相关 ======
const showSubscriptionDialog = ref(false)
const subscriptionLoading = ref(false)
const subscriptionSubmitting = ref(false)
const subscriptions = ref([])
const campusOptions = ['东校区', '西校区', '中校区', '南校区', '高新校区']
const subscriptionForm = reactive({
  keyword: '',
  category: '',
  maxPrice: null,
  campus: ''
})

// 防抖显示骨架屏
const showLoading = () => {
  loadingRequestId++
  const currentRequestId = loadingRequestId
  if (loadingTimer) {
    clearTimeout(loadingTimer)
    loadingTimer = null
  }
  loadingTimer = setTimeout(() => {
    if (currentRequestId === loadingRequestId) {
      marketLoading.value = true
    }
  }, 200)
}

// 隐藏骨架屏
const hideLoading = () => {
  loadingRequestId++
  if (loadingTimer) {
    clearTimeout(loadingTimer)
    loadingTimer = null
  }
  marketLoading.value = false
}

// 获取市场商品列表
const fetchMarketList = async () => {
  // 请求开始时记录目标分类，用于判断空状态显示
  pendingCategory.value = marketParams.category
  // 使用防抖显示骨架屏，避免请求过快时闪烁
  showLoading()
  try {
    const params = {
      page: marketParams.page,
      size: marketParams.size,
      keyword: marketParams.keyword,
      category: marketParams.category,
      sort: marketParams.sort
    }
    const res = await secondhandApi.getList(params)
    marketList.value = res.data.records.map(item => ({
      ...item,
      categoryText: categoryMap[item.category] || item.category,
      isFavorited: false
    }))
    marketTotal.value = res.data.total

    // 检查收藏状态
    await checkFavoritesStatus()
  } catch (error) {
    console.error('获取商品列表失败:', error)
  } finally {
    // 使用防抖隐藏骨架屏
    hideLoading()
  }
}

// 检查收藏状态
const checkFavoritesStatus = async () => {
  try {
    const res = await secondhandApi.getFavorites()
    const records = res.data?.records || res.data || []
    const favoritedIds = new Set(records.map(item => item.id))
    marketList.value.forEach(item => {
      item.isFavorited = favoritedIds.has(item.id)
    })
  } catch (error) {
    console.error('获取收藏状态失败:', error)
  }
}

// 分类切换
const handleCategoryChange = (category) => {
  marketParams.category = category
  marketParams.page = 1
  syncToUrl()
  fetchMarketList()
}

// 排序切换
const handleSortChange = () => {
  marketParams.page = 1
  syncToUrl()
  fetchMarketList()
}

// 搜索
const handleSearch = () => {
  marketParams.page = 1
  syncToUrl()
  fetchMarketList()
}

// 同步状态到 URL
const syncToUrl = () => {
  const query = {}
  if (marketParams.keyword) query.keyword = marketParams.keyword
  if (marketParams.category) query.category = marketParams.category
  if (marketParams.sort && marketParams.sort !== 'smart') query.sort = marketParams.sort
  if (marketParams.page > 1) query.page = String(marketParams.page)
  
  router.replace({ query })
}

// 从 URL 恢复状态
const restoreFromUrl = () => {
  const { query } = route
  if (query.keyword) marketParams.keyword = query.keyword
  if (query.category) marketParams.category = query.category
  if (query.sort && ['smart', 'latest', 'price_asc', 'price_desc'].includes(query.sort)) {
    marketParams.sort = query.sort
  }
  if (query.page) marketParams.page = parseInt(query.page)
}

// 市场分页
const handleMarketSizeChange = (size) => {
  marketParams.size = size
  marketParams.page = 1
  syncToUrl()
  fetchMarketList()
}

const handleMarketPageChange = (page) => {
  marketParams.page = page
  syncToUrl()
  fetchMarketList()
}

// 收藏/取消收藏
const favoriteLoading = ref(new Set())
const handleFavorite = async (item) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  
  if (favoriteLoading.value.has(item.id)) return
  favoriteLoading.value.add(item.id)
  
  try {
    if (item.isFavorited) {
      await secondhandApi.unfavorite(item.id)
      item.isFavorited = false
      ElMessage.success('已取消收藏')
    } else {
      await secondhandApi.favorite(item.id)
      item.isFavorited = true
      ElMessage.success('收藏成功')
    }
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error(error.message || '操作失败，请稍后重试')
  } finally {
    favoriteLoading.value.delete(item.id)
  }
}

// 打开订阅弹窗
const openSubscriptionDialog = async () => {
  showSubscriptionDialog.value = true
  await fetchSubscriptions()
}

// 获取订阅列表
const fetchSubscriptions = async () => {
  subscriptionLoading.value = true
  try {
    const res = await secondhandApi.getSubscriptions()
    subscriptions.value = res.data || []
  } catch (error) {
    console.error('获取订阅列表失败:', error)
    ElMessage.error(error.message || '获取订阅列表失败')
  } finally {
    subscriptionLoading.value = false
  }
}

// 创建订阅
const handleCreateSubscription = async () => {
  if (!subscriptionForm.keyword || !subscriptionForm.keyword.trim()) {
    ElMessage.warning('请输入订阅关键词')
    return
  }
  subscriptionSubmitting.value = true
  try {
    await secondhandApi.createSubscription({
      keyword: subscriptionForm.keyword.trim(),
      category: subscriptionForm.category || null,
      maxPrice: subscriptionForm.maxPrice || null,
      campus: subscriptionForm.campus || null
    })
    ElMessage.success('订阅创建成功')
    subscriptionForm.keyword = ''
    subscriptionForm.maxPrice = null
    await fetchSubscriptions()
  } catch (error) {
    console.error('创建订阅失败:', error)
    ElMessage.error(error.message || '创建订阅失败')
  } finally {
    subscriptionSubmitting.value = false
  }
}

// 删除订阅
const handleDeleteSubscription = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该订阅吗？删除后将不再收到到货提醒。', '删除订阅', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await secondhandApi.deleteSubscription(row.id)
    ElMessage.success('删除成功')
    await fetchSubscriptions()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除订阅失败:', error)
      ElMessage.error(error.message || '删除订阅失败')
    }
  }
}

// ====== 我的发布相关 ======
const myStoreLoading = ref(false)
const myStoreList = ref([])
const myStoreTotal = ref(0)
const myStoreParams = reactive({
  page: 1,
  size: 10,
  status: ''
})

// ====== 我的收藏相关 ======
const favoritesLoading = ref(false)
const favoritesList = ref([])
const favoritesTotal = ref(0)
const favoritesParams = reactive({
  page: 1,
  size: 12
})

// 获取我的收藏列表
const fetchFavoritesList = async () => {
  favoritesLoading.value = true
  try {
    const res = await secondhandApi.getFavorites(favoritesParams)
    const records = res.data?.records || res.data || []
    favoritesList.value = records.map(item => ({
      ...item,
      categoryText: categoryMap[item.category] || item.category
    }))
    favoritesTotal.value = res.data?.total || records.length
  } catch (error) {
    console.error('获取我的收藏失败:', error)
  } finally {
    favoritesLoading.value = false
  }
}

// 收藏分页
const handleFavoritesPageChange = (page) => {
  favoritesParams.page = page
  fetchFavoritesList()
}

// 监听路由参数，自动加载收藏数据
watch(() => route.query.tab, (newTab) => {
  if (newTab === 'favorites') {
    favoritesParams.page = 1
    fetchFavoritesList()
  }
}, { immediate: true })

// 移除收藏
const handleRemoveFavorite = async (item) => {
  try {
    await secondhandApi.unfavorite(item.id)
    favoritesTotal.value = Math.max(0, favoritesTotal.value - 1)
    // 如果当前页只剩一条且不是第一页，回到上一页
    if (favoritesList.value.length === 1 && favoritesParams.page > 1) {
      favoritesParams.page -= 1
    }
    await fetchFavoritesList()
    ElMessage.success('已取消收藏')
  } catch (error) {
    console.error('取消收藏失败:', error)
    ElMessage.error(error.message || '操作失败')
  }
}

// 获取分类文本
const getCategoryText = (category) => {
  return categoryMap[category] || category
}

// 获取我的发布列表
const fetchMyStoreList = async () => {
  myStoreLoading.value = true
  try {
    const params = {
      page: myStoreParams.page,
      size: myStoreParams.size,
      status: myStoreParams.status
    }
    const res = await secondhandApi.getMyList(params)
    console.log('我的发布API返回:', res.data.records)
    myStoreList.value = res.data.records.map(item => {
      console.log('商品item:', item.id, 'viewCount:', item.viewCount)
      return {
        ...item,
        categoryText: categoryMap[item.category] || item.category
      }
    })
    myStoreTotal.value = res.data.total
  } catch (error) {
    console.error('获取我的发布失败:', error)
  } finally {
    myStoreLoading.value = false
  }
}

// 我的发布分页
const handleMyStoreSizeChange = (size) => {
  myStoreParams.size = size
  fetchMyStoreList()
}

const handleMyStorePageChange = (page) => {
  myStoreParams.page = page
  fetchMyStoreList()
}

// 标记已售
const handleMarkSold = async (item) => {
  try {
    await ElMessageBox.confirm('确定要标记该商品为已售出吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await secondhandApi.markAsSold(item.id)
    ElMessage.success('标记成功')
    fetchMyStoreList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('标记失败:', error)
      ElMessage.error(error.message || '操作失败')
    }
  }
}

// 重新上架
const handleRelist = async (item) => {
  try {
    await ElMessageBox.confirm('确认重新上架该商品吗？上架后所有同学将再次可见。', '重新上架', {
      confirmButtonText: '确定上架',
      cancelButtonText: '取消',
      type: 'info'
    })
    await secondhandApi.relist(item.id)
    ElMessage.success('重新上架成功')
    fetchMyStoreList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重新上架失败:', error)
      ElMessage.error(error.message || '操作失败')
    }
  }
}

// 编辑
const handleEdit = (item) => {
  // 解析图片数据 - 转换为上传组件期望的格式
  let images = []
  if (item.images) {
    let imageUrls = item.images
    // 如果是字符串，尝试解析
    if (typeof imageUrls === 'string') {
      try {
        imageUrls = JSON.parse(imageUrls)
      } catch (e) {
        // 可能是逗号分隔的字符串
        imageUrls = imageUrls.split(',').filter(url => url.trim())
      }
    }
    // 转换为 { ossUrl: 'url' } 格式
    if (Array.isArray(imageUrls)) {
      images = imageUrls
        .filter(url => url && typeof url === 'string')
        .map(url => ({
          ossUrl: url.trim(),
          localUrl: null,
          uploading: false
        }))
    }
  }

  // 填充表单
  Object.assign(publishForm, {
    id: item.id,
    title: item.title,
    category: item.category,
    price: item.price,
    condition: item.condition || 'NEW',
    description: item.description || '',
    images: images
  })
  isEdit.value = true
  showPublishDialog.value = true
}

// 下架（将状态改为已下架）
const handleRemove = async (item) => {
  try {
    await ElMessageBox.confirm('确定要下架该商品吗？下架后商品将不在列表中显示。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await secondhandApi.remove(item.id, '')
    ElMessage.success('下架成功')
    fetchMyStoreList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('下架失败:', error)
      ElMessage.error(error.message || '操作失败')
    }
  }
}

// 删除（软删除）
const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm('确定要删除该商品吗？此操作不可恢复！', '删除商品', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'error'
    })
    await secondhandApi.delete(item.id)
    ElMessage.success('删除成功')
    fetchMyStoreList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(error.message || '操作失败')
    }
  }
}

// ====== 发布商品弹窗 ======
const showPublishDialog = ref(false)
const publishFormRef = ref()
const publishLoading = ref(false)
const isEdit = ref(false)
const publishForm = reactive({
  id: null,
  title: '',
  category: '',
  price: 0,
  condition: '全新',
  description: '',
  images: []
})

const publishRules = {
  title: [
    { required: true, message: '请输入商品名称', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择商品分类', trigger: 'change' }
  ],
  price: [
    { required: true, message: '请输入商品价格', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入商品描述', trigger: 'blur' }
  ]
}

// 打开发布弹窗
const openPublishDialog = () => {
  // 重置表单
  Object.assign(publishForm, {
    id: null,
    title: '',
    category: '',
    price: 0,
    condition: 'NEW',
    description: '',
    images: []
  })
  isEdit.value = false
  showPublishDialog.value = true
}

// 处理图片选择 - 支持本地预览 + OSS上传
const handleImageChange = async (file) => {
  if (!publishForm.images) {
    publishForm.images = []
  }
  if (publishForm.images.length >= 5) {
    ElMessage.warning('最多上传5张图片')
    return
  }
  
  // 先显示本地预览
  const localPreview = URL.createObjectURL(file.raw)
  const imageItem = {
    localUrl: localPreview,
    ossUrl: '',
    uploading: true
  }
  publishForm.images.push(imageItem)
  
  try {
    const res = await uploadFile(file.raw)
    // 处理各种返回格式
    let url = res.data
    if (typeof url === 'object' && url !== null) {
      url = url.url || url.filename || JSON.stringify(url)
    }
    console.log('上传成功，OSS URL:', url)
    // 确保只存储字符串URL
    imageItem.ossUrl = String(url)
    imageItem.uploading = false
  } catch (error) {
    console.error('图片上传失败:', error)
    ElMessage.error('图片上传失败')
    // 移除上传失败的图片
    const idx = publishForm.images.indexOf(imageItem)
    if (idx > -1) {
      publishForm.images.splice(idx, 1)
    }
  }
}

// 移除图片
const removeImage = (index) => {
  publishForm.images.splice(index, 1)
}

// 提交发布
const submitPublish = async () => {
  if (!publishFormRef.value) return

  await publishFormRef.value.validate(async (valid) => {
    if (!valid) return

    // 检查是否有正在上传的图片
    const uploading = publishForm.images?.find(img => img.uploading)
    if (uploading) {
      ElMessage.warning('图片正在上传中，请稍候')
      return
    }

    console.log('=== 提交前 publishForm.images ===')
    console.log('publishForm.images:', JSON.stringify(publishForm.images))
    console.log('publishForm.images length:', publishForm.images?.length)

    // 处理 images 字段 - 转换为纯字符串数组
    let imageUrls = []
    if (publishForm.images && publishForm.images.length > 0) {
      imageUrls = publishForm.images.map(img => {
        console.log('处理图片项:', img)
        // 情况1: ossUrl 是对象 { url: "..." }
        if (img.ossUrl && typeof img.ossUrl === 'object' && img.ossUrl.url) {
          return img.ossUrl.url
        }
        // 情况2: ossUrl 是字符串
        if (img.ossUrl && typeof img.ossUrl === 'string') {
          return img.ossUrl
        }
        // 情况3: fallback 到 localUrl
        return img.localUrl || ''
      }).filter(url => typeof url === 'string' && (url.startsWith('http') || url.startsWith('/')))
    }

    console.log('处理后的 imageUrls:', imageUrls)

    // 构建提交数据
    const submitData = {
      title: publishForm.title,
      description: publishForm.description,
      price: publishForm.price,
      category: publishForm.category,
      condition: publishForm.condition,
      images: imageUrls
    }

    console.log('提交数据:', JSON.stringify(submitData))

    publishLoading.value = true
    try {
      if (isEdit.value) {
        // 编辑 - 调用更新API
        await secondhandApi.update(publishForm.id, submitData)
        ElMessage.success('修改成功')
      } else {
        // 发布 - 只提交OSS URL字符串数组
        await secondhandApi.publish(submitData)
        ElMessage.success('商品发布成功')
      }
      showPublishDialog.value = false
      fetchMyStoreList()
      if (currentTab.value === 'market') {
        fetchMarketList()
      }
    } catch (error) {
      console.error('操作失败:', error)
      ElMessage.error(error.message || '操作失败，请稍后重试')
    } finally {
      publishLoading.value = false
    }
  })
}

// ====== 辅助函数 ======
// 获取状态样式类
const getStatusClass = (status) => {
  const map = {
    PENDING: 'status-pending',
    AVAILABLE: 'status-available',
    SOLD: 'status-sold'
  }
  return map[status] || 'status-available'
}

// 获取状态文本
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

// 获取状态标签类型
const getStatusTagType = (status) => {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    REMOVED: 'info',
    SOLD: 'info'
  }
  return map[status] || 'info'
}

// 获取图片URL - 处理各种返回格式
const getImageUrl = (images) => {
  // 默认图片路径 - 确保文件存在于 public/images/ 目录下
  const defaultImage = '/images/placeholder.png'

  const normalizeUrl = (value) => {
    if (!value) return ''
    if (typeof value === 'string') {
      return (value.startsWith('http') || value.startsWith('/')) ? value : ''
    }
    if (typeof value === 'object') {
      const candidate = value.url || value.ossUrl || value.localUrl || ''
      if (typeof candidate === 'string' && (candidate.startsWith('http') || candidate.startsWith('/'))) {
        return candidate
      }
    }
    return ''
  }

  if (!images) return defaultImage

  try {
    // 如果是 JSON 数组字符串，尝试解析
    if (typeof images === 'string') {
      // 尝试解析 JSON
      if (images.startsWith('[') && images.endsWith(']')) {
        const arr = JSON.parse(images)
        if (Array.isArray(arr) && arr.length > 0) {
          return normalizeUrl(arr[0]) || defaultImage
        }
      } else if (images.startsWith('http') || images.startsWith('/')) {
        // 直接是 URL
        return images
      }
    }
    
    // 如果是数组
    if (Array.isArray(images) && images.length > 0) {
      return normalizeUrl(images[0]) || defaultImage
    }

    // 如果是对象（兼容历史数据）
    if (typeof images === 'object') {
      return normalizeUrl(images) || defaultImage
    }
  } catch (e) {
    console.error('解析图片出错:', e)
    // 解析失败，可能是普通字符串 URL
    if (typeof images === 'string' && (images.startsWith('http') || images.startsWith('/'))) {
      return images
    }
  }
  
  return defaultImage
}

// 图片加载失败处理
const handleImageError = (e) => {
  e.target.src = '/images/placeholder.png'
  e.target.onerror = null // 防止循环调用
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return `${date.getMonth() + 1}-${date.getDate()}`
}

// 跳转详情
const goToDetail = (id) => {
  router.push(`/secondhand/${id}`)
}

// 初始化数据 - 根据当前tab加载对应数据
const initData = () => {
  if (currentTab.value === 'market') {
    fetchMarketList()
  } else {
    fetchMyStoreList()
  }
}

const handleRefreshSecondhandList = () => {
  if (currentTab.value === 'mystore') {
    fetchMyStoreList()
  }
}

// 路由监听 - 解决切换不加载问题
watch(() => route.query, (newQuery, oldQuery) => {
  if (JSON.stringify(newQuery) !== JSON.stringify(oldQuery)) {
    restoreFromUrl()
    initData()
  }
}, { deep: true })

onMounted(() => {
  restoreFromUrl()
  initData()
  
  // 监听详情页返回事件，刷新列表数据
  window.addEventListener('refresh-secondhand-list', handleRefreshSecondhandList)
})

onUnmounted(() => {
  // 移除事件监听
  window.removeEventListener('refresh-secondhand-list', handleRefreshSecondhandList)
})
</script>

<style scoped>
.page-container {
  max-width: 100%;
  margin: 0 auto;
  padding: 0 0 16px;
}

/* 过渡动画 - 淡入淡出 + 滑动效果 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
  width: 100%;
}

.fade-slide-enter-active > *,
.fade-slide-leave-active > * {
  width: 100%;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 逛逛市场布局 */
.market-container {
  display: flex;
  gap: 16px;
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 20px rgba(0, 65, 145, 0.08);
}

/* 左侧分类侧边栏 */
.category-sidebar {
  width: 220px;
  flex-shrink: 0;
  background: #f8fafc;
  border-radius: 12px;
  padding: 16px;
  height: fit-content;
  position: sticky;
  top: 20px;
  align-self: flex-start;
}

.sidebar-title {
  font-size: 14px;
  font-weight: 600;
  color: #004191;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.category-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
  transition: all 0.2s;
  margin-bottom: 4px;
}

.category-item:hover {
  background: rgba(0, 65, 145, 0.08);
  color: #004191;
}

.category-item.active {
  background: #004191;
  color: #fff;
}

.category-item .el-icon {
  font-size: 16px;
}

/* 右侧商品区域 */
.market-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 70vh;
}

.market-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: stretch;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  min-width: 0;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 8px 0 0 8px;
}

.market-toolbar :deep(.el-select) {
  width: 140px;
  flex-shrink: 0;
}

.subscribe-btn {
  flex-shrink: 0;
}

.subscription-form {
  margin-bottom: 8px;
}

/* 商品网格容器 - 外层包裹器 */
.goods-grid {
  /* 不使用grid，因为它包含的是骨架屏/空状态/商品列表三个区块 */
  width: 100%;
  box-sizing: border-box;
  min-height: 70vh;
  /* 关键：防止子元素被拉伸 */
  display: block;
}

/* 商品列表网格 - 真正的商品卡片容器 */
.goods-list-grid {
  display: grid;
  /* 响应式多列布局 */
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
  width: 100%;
  box-sizing: border-box;
  /* 关键：防止卡片被拉伸填满高度 */
  align-items: start;
}

/* 暂无商品空状态容器 */
.empty-state-wrapper {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
  padding: 40px 0;
  background: transparent;
}

/* 空状态图标 */
.empty-image {
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.6;
}

/* 去发布商品按钮 */
.publish-btn {
  background-color: #004191;
  border-color: #004191;
  border-radius: 12px;
  padding: 12px 24px;
  font-size: 15px;
  font-weight: 500;
  margin-top: 16px;
  transition: all 0.3s ease;
}

.publish-btn:hover {
  background-color: #003376;
  border-color: #003376;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 65, 145, 0.3);
}

/* 骨架屏网格 */
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
  width: 100%;
  box-sizing: border-box;
  align-items: start;
}

.skeleton-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
}

.skeleton-image {
  aspect-ratio: 16 / 10;
  background-color: #f5f5f5;
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

.skeleton-content {
  padding: 14px;
}

.skeleton-title {
  height: 16px;
  width: 80%;
  background-color: #f5f5f5;
  animation: skeleton-pulse 1.5s ease-in-out infinite;
  border-radius: 4px;
  margin-bottom: 12px;
}

.skeleton-price {
  height: 20px;
  width: 40%;
  background-color: #f5f5f5;
  animation: skeleton-pulse 1.5s ease-in-out infinite;
  border-radius: 4px;
  margin-bottom: 12px;
}

.skeleton-meta {
  height: 12px;
  width: 60%;
  background-color: #f5f5f5;
  animation: skeleton-pulse 1.5s ease-in-out infinite;
  border-radius: 4px;
}

/* 简化骨架屏脉冲动画 */
@keyframes skeleton-pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

/* 商品卡片 */
.goods-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #f0f2f5;
  position: relative;
  display: flex;
  flex-direction: column;
  width: 100%;
  box-sizing: border-box;
  min-width: 0;
  /* 关键：让卡片高度由内容决定，不被拉伸 */
  height: auto;
  align-self: start;
}

.goods-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 32px rgba(0, 65, 145, 0.15);
}

.card-image {
  position: relative;
  /* 保持正方形比例 */
  aspect-ratio: 1 / 1;
  width: 100%;
  height: auto;
  overflow: hidden;
  background: #f5f7fa;
  flex-shrink: 0;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
  display: block;
}

.goods-card:hover .card-image img {
  transform: scale(1.05);
}

/* 状态标签 */
.status-tag {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
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

/* 收藏按钮 */
.favorite-btn {
  position: absolute;
  bottom: 12px;
  right: 12px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.favorite-btn:hover {
  background: #fff;
  transform: scale(1.1);
}

.favorite-btn .el-icon {
  font-size: 18px;
  color: #909399;
}

.favorite-btn.favorited .el-icon {
  color: #f7ba2a;
}

/* 卡片内容 */
.card-content {
  padding: 14px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 6px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-description {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin: 0 0 8px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-price {
  font-size: 18px;
  font-weight: 700;
  color: #004191;
  margin-bottom: 8px;
  margin-top: auto;
}

.recommend-row {
  margin-bottom: 8px;
}

.recommend-tag {
  display: inline-block;
  font-size: 12px;
  color: #8a4b00;
  background: #fff3e0;
  border: 1px solid #ffe0b2;
  border-radius: 10px;
  padding: 3px 8px;
  line-height: 1.3;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.category-tag {
  font-size: 12px;
  color: #909399;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
}

.time-text {
  font-size: 12px;
  color: #c0c4cc;
}

/* 分页 */
.pagination {
  margin-top: auto;
  padding: 20px 0;
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0;
}

/* 分页组件样式修复 - 科大蓝主题 */
:deep(.el-pagination) {
  --el-pagination-button-bg-color: #f5f7fa;
  --el-pagination-hover-color: #004191;
  --el-pagination-button-disabled-bg-color: #f5f7fa;
}

/* 修复激活态数字可见性 - 背景蓝色时文字必须为白色 */
:deep(.el-pagination.is-background .el-pager li.is-active) {
  background-color: #004191 !important;
  color: #ffffff !important;
  /* 确保数字居中 */
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
}

/* 分页按钮圆角和悬浮效果 */
:deep(.el-pagination.is-background .el-pager li) {
  border-radius: 8px;
  margin: 0 4px;
  transition: all 0.2s ease;
  /* 确保按钮正常显示 */
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  min-width: 32px;
  height: 32px;
}

:deep(.el-pagination.is-background .el-pager li:hover) {
  background-color: rgba(0, 65, 145, 0.1);
  color: #004191;
}

/* 上一页/下一页按钮圆角 */
:deep(.el-pagination.is-background .btn-prev),
:deep(.el-pagination.is-background .btn-next) {
  border-radius: 8px;
  /* 确保按钮正常显示 */
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
}

:deep(.el-pagination.is-background .btn-prev:hover),
:deep(.el-pagination.is-background .btn-next:hover) {
  background-color: rgba(0, 65, 145, 0.1);
  color: #004191;
}

/* 修复高度塌陷 - 分页固定定位 */
.my-goods-list-wrapper {
  min-height: 500px;
  display: flex;
  flex-direction: column;
}

.my-goods-list-wrapper .my-goods-list {
  flex: 1;
}

.my-goods-list-wrapper .pagination {
  flex-shrink: 0;
  padding-top: 20px;
  margin-top: auto;
}

/* 我的发布 */
.mystore-container {
  min-height: 600px;
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 20px rgba(0, 65, 145, 0.08);
}

.mystore-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f2f5;
  flex-wrap: wrap;
  gap: 12px;
}

/* 我的收藏容器 */
.favorites-container {
  min-height: 600px;
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 20px rgba(0, 65, 145, 0.08);
}

.favorites-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f2f5;
}

.favorites-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.favorites-count {
  font-size: 14px;
  color: #909399;
}

.favorites-list-wrapper {
  min-height: 500px;
}

.favorites-loading,
.favorites-empty {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 收藏网格布局 */
.favorites-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
  width: 100%;
  box-sizing: border-box;
  align-items: start;
}

.favorites-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.favorites-pagination {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.favorites-pagination :deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: var(--ustc-primary);
}

.favorites-pagination :deep(.el-pagination.is-background .el-pager li:not(.is-disabled):hover) {
  color: var(--ustc-primary);
}

.status-filters :deep(.el-radio-button__inner) {
  border-radius: 8px !important;
}

.status-filters :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: 8px !important;
}

.status-filters :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 8px !important;
}

/* 标签切换激活态 - 科大蓝背景白色文字 */
.status-filters :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: #004191 !important;
  border-color: #004191 !important;
  color: #ffffff !important;
  box-shadow: none !important;
}

/* 标签切换悬浮效果 */
.status-filters :deep(.el-radio-button__inner:hover) {
  color: #004191;
}

/* 加载骨架屏 */
.loading-skeleton-wrapper {
  padding: 20px 0;
  min-height: 200px;
}

/* 空状态 */
.my-goods-empty {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 表格商品信息 */
.table-goods-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.table-goods-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
}

.table-goods-detail {
  flex: 1;
}

.table-goods-title {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.table-goods-meta {
  font-size: 12px;
  color: #909399;
}

/* 发布弹窗 */
:deep(.publish-dialog) {
  border-radius: 16px;
}

:deep(.publish-dialog .el-dialog__header) {
  border-bottom: 1px solid #f0f2f5;
  padding-bottom: 16px;
}

:deep(.publish-dialog .el-dialog__title) {
  font-weight: 600;
  color: #004191;
}

.image-upload {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.upload-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.upload-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #f0f2f5;
}

.upload-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-item-actions {
  position: absolute;
  top: 4px;
  right: 4px;
  cursor: pointer;
  color: #fff;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  transition: background 0.2s;
}

.upload-item-actions:hover {
  background: rgba(0, 0, 0, 0.7);
}

.upload-trigger {
  width: 100px;
  height: 100px;
  border: 2px dashed #dcdfe6;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  background: #fafafa;
}

.upload-trigger:hover {
  border-color: #004191;
  color: #004191;
  background: rgba(0, 65, 145, 0.05);
}

.upload-trigger .el-icon {
  font-size: 24px;
  color: #909399;
}

.upload-text {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

/* 响应式 */
@media (max-width: 1400px) {
  .goods-list-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  }

  .skeleton-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  }
}

@media (max-width: 1200px) {
  .goods-list-grid {
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  }

  .skeleton-grid {
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  }
}

@media (max-width: 992px) {
  .goods-list-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }

  .skeleton-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }

  .market-container {
    flex-direction: column;
  }

  .category-sidebar {
    width: 100%;
    position: static;
  }

  .category-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .category-item {
    margin-bottom: 0;
  }
}

@media (max-width: 768px) {
  .goods-list-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 12px;
  }

  .skeleton-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 12px;
  }

  .market-toolbar {
    flex-direction: column;
  }

  .search-input {
    width: 100%;
  }

  .subscribe-btn {
    width: 100%;
  }

  .mystore-toolbar {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
