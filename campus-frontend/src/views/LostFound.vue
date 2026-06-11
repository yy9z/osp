<template>
  <div class="page-container">
    <div class="page-header">
      <h2>失物招领</h2>
      <el-button type="primary" @click="handlePublish">
        <el-icon><Plus /></el-icon>
        发布信息
      </el-button>
    </div>

    <!-- 4 Tab 核心结构 -->
    <el-tabs v-model="activeTab" class="lostfound-tabs" @tab-change="onTabChange">
      <!-- 寻物公告：全校所有失物帖 -->
      <el-tab-pane label="寻物公告" name="lost-board">
        <div class="tab-toolbar">
          <el-input v-model="boardSearch.keyword" placeholder="搜索物品描述" clearable style="width:200px"
            @clear="fetchLostBoard" @keyup.enter="fetchLostBoard"><template #prefix><el-icon><Search /></el-icon></template></el-input>
          <el-select v-model="boardSearch.category" placeholder="物品分类" clearable style="width:130px" @change="fetchLostBoard">
            <el-option v-for="(text,val) in categoryMap" :key="val" :label="text" :value="val" />
          </el-select>
          <el-button type="primary" plain @click="fetchLostBoard">搜索</el-button>
        </div>
        <div class="list-container" v-loading="lostBoardLoading">
          <el-empty v-if="!lostBoardLoading && lostBoardList.length===0" description="暂无寻物公告" />
          <div v-else class="lostfound-list">
            <el-card v-for="item in lostBoardList" :key="item.id" class="lostfound-card" shadow="hover">
              <div class="card-inner">
                <div class="card-left">
                  <div class="card-meta">
                    <el-tag type="danger" size="small">寻物</el-tag>
                    <el-tag v-if="item.source==='AGENT'" type="info" size="small" effect="plain">AI发布</el-tag>
                    <el-tag v-if="item.status!=='OPEN'" type="success" size="small">已找回</el-tag>
                  </div>
                  <div v-if="getCoverImage(item)" class="card-cover">
                    <img :src="getCoverImage(item)" :alt="item.title" @error="handleImageError" />
                  </div>
                  <h3 class="card-title">{{ item.title }}</h3>
                  <p class="card-desc">{{ item.description }}</p>
                  <div class="card-footer">
                    <span class="info-item"><el-icon><Location /></el-icon>{{ item.location }}</span>
                    <span class="info-item"><el-icon><User /></el-icon>{{ item.publisherName }}</span>
                    <span class="info-item"><el-icon><Clock /></el-icon>{{ item.createTime?.substring(0,10) }}</span>
                  </div>
                </div>
                <div class="card-actions">
                  <el-button v-if="item.status==='OPEN'" size="small" type="success" plain @click="handleContact(item)">我捡到了</el-button>
                </div>
              </div>
            </el-card>
          </div>
        </div>
        <el-pagination v-if="lostBoardTotal>0" v-model:current-page="lostBoardPage" :page-size="pageSize"
          :total="lostBoardTotal" layout="total,prev,pager,next" class="pagination" @current-change="fetchLostBoard" />
      </el-tab-pane>

      <!-- 招领公告：全校所有拾物帖 -->
      <el-tab-pane label="招领公告" name="found-board">
        <div class="tab-toolbar">
          <el-input v-model="foundSearch.keyword" placeholder="搜索物品描述" clearable style="width:200px"
            @clear="fetchFoundBoard" @keyup.enter="fetchFoundBoard"><template #prefix><el-icon><Search /></el-icon></template></el-input>
          <el-select v-model="foundSearch.category" placeholder="物品分类" clearable style="width:130px" @change="fetchFoundBoard">
            <el-option v-for="(text,val) in categoryMap" :key="val" :label="text" :value="val" />
          </el-select>
          <el-button type="primary" plain @click="fetchFoundBoard">搜索</el-button>
        </div>
        <div class="list-container" v-loading="foundBoardLoading">
          <el-empty v-if="!foundBoardLoading && foundBoardList.length===0" description="暂无招领公告" />
          <div v-else class="lostfound-list">
            <el-card v-for="item in foundBoardList" :key="item.id" class="lostfound-card" shadow="hover">
              <div class="card-inner">
                <div class="card-left">
                  <div class="card-meta">
                    <el-tag type="success" size="small">招领</el-tag>
                    <el-tag v-if="item.source==='AGENT'" type="info" size="small" effect="plain">AI发布</el-tag>
                    <el-tag v-if="item.status!=='OPEN'" type="info" size="small">已归还</el-tag>
                  </div>
                  <div v-if="getCoverImage(item)" class="card-cover">
                    <img :src="getCoverImage(item)" :alt="item.title" @error="handleImageError" />
                  </div>
                  <h3 class="card-title">{{ item.title }}</h3>
                  <p class="card-desc">{{ item.description }}</p>
                  <div class="card-footer">
                    <span class="info-item"><el-icon><Location /></el-icon>{{ item.location }}</span>
                    <span class="info-item"><el-icon><User /></el-icon>{{ item.publisherName }}</span>
                    <span class="info-item"><el-icon><Clock /></el-icon>{{ item.createTime?.substring(0,10) }}</span>
                  </div>
                </div>
                <div class="card-actions">
                  <el-button v-if="item.status==='OPEN'" size="small" type="primary" plain @click="handleContact(item)">这是我的</el-button>
                </div>
              </div>
            </el-card>
          </div>
        </div>
        <el-pagination v-if="foundBoardTotal>0" v-model:current-page="foundBoardPage" :page-size="pageSize"
          :total="foundBoardTotal" layout="total,prev,pager,next" class="pagination" @current-change="fetchFoundBoard" />
      </el-tab-pane>

      <!-- 我的失物：仅当前用户发布的 LOST 记录 -->
      <el-tab-pane label="我的失物" name="my-lost">
        <div class="list-container" v-loading="myLostLoading">
          <el-empty v-if="!myLostLoading && myLostList.length===0" description="你还没有发布寻物公告">
            <el-button type="primary" @click="openPublishDialog('LOST')">立即发布</el-button>
          </el-empty>
          <div v-else class="lostfound-list">
            <el-card v-for="item in myLostList" :key="item.id" class="lostfound-card" shadow="hover">
              <div class="card-inner">
                <div class="card-left">
                  <div class="card-meta">
                    <el-tag type="danger" size="small">失物</el-tag>
                    <el-tag v-if="item.status!=='OPEN'" type="success" size="small">已找回</el-tag>
                    <el-tag v-if="item.source==='AGENT'" type="info" size="small" effect="plain">AI发布</el-tag>
                  </div>
                  <div v-if="getCoverImage(item)" class="card-cover">
                    <img :src="getCoverImage(item)" :alt="item.title" @error="handleImageError" />
                  </div>
                  <h3 class="card-title">{{ item.title }}</h3>
                  <p class="card-desc">{{ item.description }}</p>
                  <div class="card-footer">
                    <span class="info-item"><el-icon><Location /></el-icon>{{ item.location }}</span>
                    <span class="info-item"><el-icon><Clock /></el-icon>{{ item.createTime?.substring(0,10) }}</span>
                  </div>
                </div>
                <div class="card-actions">
                  <el-button size="small" type="primary" plain @click="handleSmartMatch(item)">智能匹配</el-button>
                  <el-button v-if="item.status==='OPEN'" size="small" type="success" plain @click="handleMarkResolved(item)">标记已找回</el-button>
                  <el-button size="small" type="danger" plain @click="handleDelete(item)">删除</el-button>
                </div>
              </div>
            </el-card>
          </div>
        </div>
      </el-tab-pane>

      <!-- 我的招领：仅当前用户发布的 FOUND 记录 -->
      <el-tab-pane label="我的招领" name="my-found">
        <div class="list-container" v-loading="myFoundLoading">
          <el-empty v-if="!myFoundLoading && myFoundList.length===0" description="你还没有发布招领公告">
            <el-button type="primary" @click="openPublishDialog('FOUND')">立即发布</el-button>
          </el-empty>
          <div v-else class="lostfound-list">
            <el-card v-for="item in myFoundList" :key="item.id" class="lostfound-card" shadow="hover">
              <div class="card-inner">
                <div class="card-left">
                  <div class="card-meta">
                    <el-tag type="success" size="small">招领</el-tag>
                    <el-tag v-if="item.status!=='OPEN'" type="info" size="small">已归还</el-tag>
                    <el-tag v-if="item.source==='AGENT'" type="info" size="small" effect="plain">AI发布</el-tag>
                  </div>
                  <div v-if="getCoverImage(item)" class="card-cover">
                    <img :src="getCoverImage(item)" :alt="item.title" @error="handleImageError" />
                  </div>
                  <h3 class="card-title">{{ item.title }}</h3>
                  <p class="card-desc">{{ item.description }}</p>
                  <div class="card-footer">
                    <span class="info-item"><el-icon><Location /></el-icon>{{ item.location }}</span>
                    <span class="info-item"><el-icon><Clock /></el-icon>{{ item.createTime?.substring(0,10) }}</span>
                  </div>
                </div>
                <div class="card-actions">
                  <el-button size="small" type="primary" plain @click="handleSmartMatch(item)">智能匹配</el-button>
                  <el-button v-if="item.status==='OPEN'" size="small" type="success" plain @click="handleMarkResolved(item)">标记已归还</el-button>
                  <el-button size="small" type="danger" plain @click="handleDelete(item)">删除</el-button>
                </div>
              </div>
            </el-card>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 发布信息对话框 -->
    <el-dialog
      v-model="showPublishDialog"
      title="发布信息"
      width="580px"
      :close-on-click-modal="false"
    >
      <el-form :model="publishForm" :rules="publishRules" ref="publishFormRef" label-width="80px">
        <el-form-item label="信息类型" prop="type">
          <el-radio-group v-model="publishForm.type">
            <el-radio value="LOST">寻物公告（我丢了）</el-radio>
            <el-radio value="FOUND">招领公告（我捡到了）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="publishForm.title" placeholder="请输入标题" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="物品分类" prop="category">
          <el-select v-model="publishForm.category" placeholder="请选择分类" style="width: 100%;">
            <el-option label="电子产品" value="ELECTRONICS" />
            <el-option label="证件钱包" value="WALLET" />
            <el-option label="书籍资料" value="BOOKS" />
            <el-option label="衣物饰品" value="CLOTHING" />
            <el-option label="钥匙雨伞" value="KEYS" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item :label="publishForm.type==='LOST'?'丢失地点':'拾取地点'" prop="location">
          <el-input v-model="publishForm.location" placeholder="请输入丢失或拾取地点" />
        </el-form-item>
        <el-form-item label="丢失时间">
          <el-date-picker
            v-model="publishForm.lostTime"
            type="datetime"
            placeholder="选择日期时间"
            style="width: 100%;"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="详细描述" prop="description">
          <el-input
            v-model="publishForm.description"
            type="textarea"
            :rows="4"
            placeholder="请详细描述物品特征等信息"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="物品图片">
          <div class="image-upload">
            <div class="upload-list" v-if="publishForm.images?.length">
              <div v-for="(img, index) in publishForm.images" :key="index" class="upload-item">
                <img :src="img" />
                <div class="upload-item-actions">
                  <el-icon @click.stop="removeImage(index)"><Close /></el-icon>
                </div>
              </div>
            </div>
            <el-upload
              v-if="!publishForm.images || publishForm.images.length < 3"
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
          <div class="upload-tip">最多上传3张图片</div>
        </el-form-item>
        <el-form-item label="联系方式" prop="contact">
          <el-input v-model="publishForm.contact" placeholder="请输入联系方式（手机号或微信）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublishDialog = false">取消</el-button>
        <el-button type="primary" @click="submitPublish" :loading="publishLoading">发布</el-button>
      </template>
    </el-dialog>

    <!-- 联系对话框 -->
    <el-dialog v-model="showMatchDialog" title="智能匹配结果" width="560px">
      <div v-loading="matchLoading">
        <div v-if="matchSource" class="match-source">
          <el-tag size="small" type="info" effect="plain">
            当前{{ matchSource.type === 'LOST' ? '失物' : '招领' }}：{{ matchSource.title }}
          </el-tag>
        </div>

        <el-empty v-if="!matchLoading && matchList.length === 0" description="暂未发现高置信匹配，建议补充更多特征描述后重试" />

        <div v-else class="match-list">
          <div v-for="item in matchList" :key="item.id" class="match-item">
            <div class="match-head">
              <div class="match-title">{{ item.title }}</div>
              <el-tag size="small" :type="getMatchScoreType(item.matchScore)">
                匹配度 {{ item.matchScore }}%
              </el-tag>
            </div>
            <div class="match-meta">
              <span>{{ item.type === 'FOUND' ? '招领公告' : '寻物公告' }}</span>
              <span>·</span>
              <span>{{ getCategoryText(item.category) }}</span>
              <span v-if="item.location">· {{ item.location }}</span>
            </div>
            <div v-if="item.matchReasons?.length" class="match-reasons">
              匹配依据：{{ item.matchReasons.join('、') }}
            </div>
            <div class="match-actions">
              <el-button size="small" type="primary" plain @click="handleContact(item)">联系发布者</el-button>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showMatchDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showContactDialog" title="联系发布者" width="400px">
      <div v-if="contactTarget" style="font-size:14px;line-height:2">
        <p>物品：<strong>{{ contactTarget.title }}</strong></p>
        <p>联系方式：<strong>{{ contactTarget.contact || contactTarget.publisherPhone || '对方未留联系方式' }}</strong></p>
      </div>
      <template #footer>
        <el-button @click="showContactDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { lostFoundApi } from '@/api'
import { useUserStore } from '@/stores'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Location, User, Clock, Close } from '@element-plus/icons-vue'
import placeholderImg from '@/assets/placeholder.png'

const route = useRoute()
const userStore = useUserStore()
const validTabs = ['lost-board', 'found-board', 'my-lost', 'my-found']

// ─── Tab 状态 ───
const activeTab = ref('lost-board')
const pageSize = ref(10)

// ─── 寻物公告（公共池） ───
const lostBoardList = ref([])
const lostBoardLoading = ref(false)
const lostBoardTotal = ref(0)
const lostBoardPage = ref(1)
const boardSearch = reactive({ keyword: '', category: '' })

// ─── 招领公告（公共池） ───
const foundBoardList = ref([])
const foundBoardLoading = ref(false)
const foundBoardTotal = ref(0)
const foundBoardPage = ref(1)
const foundSearch = reactive({ keyword: '', category: '' })

// ─── 我的失物 ───
const myLostList = ref([])
const myLostLoading = ref(false)

// ─── 我的招领 ───
const myFoundList = ref([])
const myFoundLoading = ref(false)

const categoryMap = {
  ELECTRONICS: '电子产品', WALLET: '证件钱包', BOOKS: '书籍资料',
  CLOTHING: '衣物饰品', KEYS: '钥匙雨伞', OTHER: '其他'
}

const getCategoryText = (cat) => categoryMap[cat] || cat
const normalizeImageList = (images) => {
  if (!images) return []
  if (Array.isArray(images)) return images.filter(Boolean)
  if (typeof images === 'string') {
    try {
      const parsed = JSON.parse(images)
      if (Array.isArray(parsed)) return parsed.filter(Boolean)
    } catch (_) {
      return images ? [images] : []
    }
  }
  return []
}
const normalizeRecords = (records = []) => records.map(item => ({
  ...item,
  imageList: normalizeImageList(item.images)
}))
const getCoverImage = (item) => item?.imageList?.[0] || ''
const handleImageError = (event) => {
  event.target.src = placeholderImg
}

// ─── 数据获取 ───
const fetchLostBoard = async () => {
  lostBoardLoading.value = true
  try {
    const res = await lostFoundApi.getList({
      type: 'LOST', keyword: boardSearch.keyword,
      category: boardSearch.category, page: lostBoardPage.value, size: pageSize.value
    })
    lostBoardList.value = normalizeRecords(res.data.records || [])
    lostBoardTotal.value = res.data.total || 0
  } finally { lostBoardLoading.value = false }
}

const fetchFoundBoard = async () => {
  foundBoardLoading.value = true
  try {
    const res = await lostFoundApi.getList({
      type: 'FOUND', keyword: foundSearch.keyword,
      category: foundSearch.category, page: foundBoardPage.value, size: pageSize.value
    })
    foundBoardList.value = normalizeRecords(res.data.records || [])
    foundBoardTotal.value = res.data.total || 0
  } finally { foundBoardLoading.value = false }
}

const fetchMyLost = async () => {
  myLostLoading.value = true
  try {
    const res = await lostFoundApi.getMyLostList()
    myLostList.value = normalizeRecords(res.data.records || res.data || [])
  } finally { myLostLoading.value = false }
}

const fetchMyFound = async () => {
  myFoundLoading.value = true
  try {
    const res = await lostFoundApi.getMyFoundList()
    myFoundList.value = normalizeRecords(res.data.records || res.data || [])
  } finally { myFoundLoading.value = false }
}

// Tab 切换懒加载
const onTabChange = (tab) => {
  if (tab === 'lost-board' && lostBoardList.value.length === 0) fetchLostBoard()
  if (tab === 'found-board' && foundBoardList.value.length === 0) fetchFoundBoard()
  if (tab === 'my-lost') fetchMyLost()
  if (tab === 'my-found') fetchMyFound()
}

const resolveTabFromRoute = () => {
  const queryTab = typeof route.query.tab === 'string' ? route.query.tab : ''
  if (validTabs.includes(queryTab)) return queryTab
  const segment = route.path.split('/').filter(Boolean).pop() || ''
  if (validTabs.includes(segment)) return segment
  return 'lost-board'
}

const syncTabFromRoute = () => {
  const targetTab = resolveTabFromRoute()
  if (activeTab.value !== targetTab) {
    activeTab.value = targetTab
  }
  onTabChange(targetTab)
}

// ─── 发布 ───

// 发布对话框
const showPublishDialog = ref(false)
const publishFormRef = ref()
const publishLoading = ref(false)
const publishForm = reactive({
  type: 'LOST', title: '', category: '', location: '', lostTime: '', description: '', images: [], contact: ''
})
const publishRules = {
  type: [{ required: true, message: '请选择信息类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择物品分类', trigger: 'change' }],
  location: [{ required: true, message: '请输入地点', trigger: 'blur' }],
  description: [{ required: true, message: '请输入详细描述', trigger: 'blur' }],
  contact: [{ required: true, message: '请输入联系方式', trigger: 'blur' }]
}

// 联系对话框
const showContactDialog = ref(false)
const contactTarget = ref(null)
const showMatchDialog = ref(false)
const matchLoading = ref(false)
const matchSource = ref(null)
const matchList = ref([])

function openPublishDialog(type = 'LOST') {
  Object.assign(publishForm, { type, title: '', category: '', location: '', lostTime: '', description: '', images: [], contact: '' })
  showPublishDialog.value = true
}

// 处理图片选择
const handleImageChange = (file) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    if (!publishForm.images) publishForm.images = []
    if (publishForm.images.length < 3) publishForm.images.push(e.target.result)
  }
  reader.readAsDataURL(file.raw)
}

const removeImage = (index) => { publishForm.images.splice(index, 1) }

const getMatchScoreType = (score) => {
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'info'
}

// 提交发布
const submitPublish = async () => {
  if (!publishFormRef.value) return
  await publishFormRef.value.validate(async (valid) => {
    if (!valid) return
    publishLoading.value = true
    try {
      await lostFoundApi.publish(publishForm)
      ElMessage.success('发布成功')
      showPublishDialog.value = false
      // 刷新对应列表
      if (publishForm.type === 'LOST') { fetchLostBoard(); fetchMyLost() }
      else { fetchFoundBoard(); fetchMyFound() }
    } catch (error) {
      ElMessage.error(error.message || '发布失败，请稍后重试')
    } finally {
      publishLoading.value = false
    }
  })
}

// 标记已解决
const handleMarkResolved = async (item) => {
  const label = item.type === 'LOST' ? '标记为"已找回"' : '标记为"已归还"'
  try {
    await ElMessageBox.confirm(`确认${label}？`, '确认操作', { type: 'info' })
    await lostFoundApi.markResolved(item.id)
    ElMessage.success('状态已更新')
    item.type === 'LOST' ? fetchMyLost() : fetchMyFound()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

// 删除
const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm('确认删除？', '删除确认', { type: 'warning' })
    await lostFoundApi.deleteItem(item.id)
    ElMessage.success('已删除')
    item.type === 'LOST' ? fetchMyLost() : fetchMyFound()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

// 智能匹配
const handleSmartMatch = async (item) => {
  showMatchDialog.value = true
  matchSource.value = item
  matchLoading.value = true
  try {
    const res = await lostFoundApi.getSmartMatches(item.id, 6)
    matchList.value = res.data || []
  } catch (error) {
    matchList.value = []
    ElMessage.error(error.message || '获取智能匹配失败')
  } finally {
    matchLoading.value = false
  }
}

// 联系
const handleContact = (item) => {
  contactTarget.value = item
  showContactDialog.value = true
}

onMounted(() => {
  syncTabFromRoute()
})

watch(
  () => [route.path, route.query.tab],
  () => {
    syncTabFromRoute()
  }
)
</script>

<style scoped>
.page-container { max-width: 1200px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 20px; color: #303133; }

.lostfound-tabs { background: #fff; border-radius: 8px; padding: 0 16px 16px; }

.tab-toolbar { display: flex; gap: 10px; align-items: center; padding: 12px 0 8px; flex-wrap: wrap; }

.list-container { min-height: 300px; }

.lostfound-list { display: flex; flex-direction: column; gap: 10px; padding: 8px 0; }

.lostfound-card { border-radius: 8px; }

.card-inner { display: flex; justify-content: space-between; align-items: flex-start; }
.card-left { flex: 1; min-width: 0; }
.card-meta { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; flex-wrap: wrap; }
.card-cover { width: 180px; height: 120px; margin-bottom: 8px; border-radius: 8px; overflow: hidden; background: #f5f7fa; }
.card-cover img { width: 100%; height: 100%; object-fit: cover; display: block; }
.card-title { margin: 0 0 4px; font-size: 15px; font-weight: 600; color: #303133; }
.card-desc {
  margin: 0 0 8px; font-size: 13px; color: #606266; line-height: 1.5;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.card-footer { display: flex; gap: 14px; font-size: 12px; color: #909399; flex-wrap: wrap; }
.info-item { display: flex; align-items: center; gap: 3px; }
.card-actions { display: flex; flex-direction: column; gap: 6px; margin-left: 14px; flex-shrink: 0; }

.match-source { margin-bottom: 10px; }
.match-list { display: flex; flex-direction: column; gap: 10px; max-height: 420px; overflow: auto; }
.match-item { border: 1px solid #ebeef5; border-radius: 8px; padding: 10px; background: #fff; }
.match-head { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.match-title { font-size: 14px; font-weight: 600; color: #303133; }
.match-meta { margin-top: 6px; font-size: 12px; color: #909399; display: flex; gap: 6px; flex-wrap: wrap; }
.match-reasons { margin-top: 6px; font-size: 12px; color: #606266; line-height: 1.5; }
.match-actions { margin-top: 8px; }

.pagination { display: flex; justify-content: flex-end; padding: 10px 0; }

/* 发布对话框样式 */
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
  border-radius: 8px;
  overflow: hidden;
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
}

.upload-trigger {
  width: 100px;
  height: 100px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-trigger:hover {
  border-color: #409EFF;
  color: #409EFF;
}

.upload-text {
  font-size: 12px;
  margin-top: 4px;
}

.upload-tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

@media (max-width: 768px) {
  .lostfound-list {
    grid-template-columns: 1fr;
  }

  .pagination {
    flex-direction: column;
    align-items: center;
  }
}
</style>
