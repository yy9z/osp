<template>
  <div class="page-container">
    <div class="page-header">
      <h2>报修管理</h2>
    </div>

    <div class="stats-cards">
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon pending">
            <el-icon><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.pending }}</div>
            <div class="stat-label">待处理</div>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon processing">
            <el-icon><Loading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.processing }}</div>
            <div class="stat-label">处理中</div>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon completed">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.completed }}</div>
            <div class="stat-label">已完成</div>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon total">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.total }}</div>
            <div class="stat-label">总计</div>
          </div>
        </div>
      </el-card>
    </div>

    <el-card>
      <div class="filter-bar">
        <el-select v-model="filterParams.building" placeholder="选择楼栋" clearable style="width: 150px" @change="handleFilterChange">
          <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.name" />
        </el-select>
        <el-select v-model="filterParams.status" placeholder="报修状态" clearable style="width: 120px" @change="handleFilterChange">
          <el-option label="待处理" value="PENDING" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已完成" value="COMPLETED" />
        </el-select>
        <el-select v-model="filterParams.urgency" placeholder="紧急程度" clearable style="width: 120px" @change="handleFilterChange">
          <el-option label="高" value="high" />
          <el-option label="中" value="medium" />
          <el-option label="低" value="low" />
        </el-select>
        <el-button type="primary" @click="fetchRepairList">筛选</el-button>
        <el-button @click="resetFilter">重置</el-button>
      </div>

      <el-table :data="repairList" v-loading="loading" style="width: 100%; margin-top: 20px">
        <el-table-column type="index" label="编号" width="80" :index="getRowIndex" />
        <el-table-column prop="building" label="楼栋" width="100" />
        <el-table-column prop="roomNo" label="房间号" width="100" />
        <el-table-column prop="description" label="报修描述" show-overflow-tooltip />
        <el-table-column prop="urgency" label="紧急程度" width="100">
          <template #default="{ row }">
            <el-tag :type="getUrgencyType(row.urgency)" size="small">
              {{ getUrgencyText(row.urgency) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="申请人" width="100" />
        <el-table-column prop="phone" label="联系电话" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getRepairStatusType(row.status)">
              {{ getRepairStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="viewDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              size="small"
              link
              type="warning"
              @click="startRepair(row)"
            >
              开始处理
            </el-button>
            <el-button
              v-if="row.status === 'PROCESSING'"
              size="small"
              link
              type="success"
              @click="openCompleteDialog(row)"
            >
              处理完成
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" v-if="pagination.total > 0">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchRepairList"
          @current-change="fetchRepairList"
        />
      </div>
    </el-card>

    <el-dialog v-model="showDetailDialog" title="报修详情" width="700px">
      <el-descriptions :column="2" border v-if="currentRepair">
        <el-descriptions-item label="编号">{{ currentRepair.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getRepairStatusType(currentRepair.status)">
            {{ getRepairStatusText(currentRepair.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="楼栋">{{ currentRepair.building }}</el-descriptions-item>
        <el-descriptions-item label="房间号">{{ currentRepair.roomNo }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ currentRepair.realName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentRepair.phone }}</el-descriptions-item>
        <el-descriptions-item label="紧急程度">
          <el-tag :type="getUrgencyType(currentRepair.urgency)" size="small">
            {{ getUrgencyText(currentRepair.urgency) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ currentRepair.createTime }}</el-descriptions-item>
        <el-descriptions-item label="问题描述" :span="2">{{ currentRepair.description }}</el-descriptions-item>
        <el-descriptions-item label="报修图片" :span="2" v-if="repairImages.length > 0">
          <div class="image-preview">
            <el-image
              v-for="(img, index) in repairImages"
              :key="index"
              :src="img"
              :preview-src-list="repairImages"
              fit="cover"
              class="preview-image"
            />
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="处理人" v-if="currentRepair.handlerName">{{ currentRepair.handlerName }}</el-descriptions-item>
        <el-descriptions-item label="开始处理时间" v-if="currentRepair.processingStartedAt">{{ currentRepair.processingStartedAt }}</el-descriptions-item>
        <el-descriptions-item label="完成时间" v-if="currentRepair.handleTime && currentRepair.status === 'COMPLETED'">{{ currentRepair.handleTime }}</el-descriptions-item>
        <el-descriptions-item label="处理备注" :span="2" v-if="currentRepair.remark">{{ currentRepair.remark }}</el-descriptions-item>
        <el-descriptions-item label="处理照片" :span="2" v-if="handleImages.length > 0">
          <div class="image-preview">
            <el-image
              v-for="(img, index) in handleImages"
              :key="index"
              :src="img"
              :preview-src-list="handleImages"
              fit="cover"
              class="preview-image"
            />
          </div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="showCompleteDialog" title="处理完成" width="550px" :close-on-click-modal="false">
      <el-form :model="completeForm" :rules="completeRules" ref="completeFormRef" label-width="90px">
        <el-form-item label="报修信息">
          <div class="repair-info">
            <p><strong>房间：</strong>{{ currentRepair?.building }} - {{ currentRepair?.roomNo }}</p>
            <p><strong>问题：</strong>{{ currentRepair?.description }}</p>
          </div>
        </el-form-item>
        <el-form-item label="处理备注" prop="remark">
          <el-input
            v-model="completeForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入处理备注"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="处理照片" prop="handleImages">
          <el-upload
            v-model:file-list="completeFileList"
            :http-request="customUpload"
            list-type="picture-card"
            :on-success="handleUploadSuccess"
            :on-remove="handleUploadRemove"
            :before-upload="beforeUpload"
            :limit="3"
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
            <template #tip>
              <div class="upload-tip">最多上传3张图片</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCompleteDialog = false">取消</el-button>
        <el-button type="primary" @click="submitComplete" :loading="completeLoading">确认完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { dormitoryApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Loading, CircleCheck, Document, Plus } from '@element-plus/icons-vue'

const loading = ref(false)
const completeLoading = ref(false)

const buildings = ref([])
const repairList = ref([])
const stats = ref({ pending: 0, processing: 0, completed: 0, total: 0 })

const showDetailDialog = ref(false)
const showCompleteDialog = ref(false)
const currentRepair = ref(null)

const completeFormRef = ref()
const completeFileList = ref([])

const filterParams = reactive({
  building: '',
  status: '',
  urgency: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const completeForm = reactive({
  remark: '',
  handleImages: []
})

const completeRules = {
  remark: [{ required: true, message: '请填写处理备注', trigger: 'blur' }]
}

const repairImages = computed(() => {
  if (!currentRepair.value?.images) return []
  try {
    return JSON.parse(currentRepair.value.images)
  } catch {
    return []
  }
})

const handleImages = computed(() => {
  if (!currentRepair.value?.handleImages) return []
  try {
    return JSON.parse(currentRepair.value.handleImages)
  } catch {
    return []
  }
})

const getUrgencyType = (urgency) => {
  const map = { high: 'danger', medium: 'warning', low: 'info' }
  return map[urgency] || 'info'
}

const getUrgencyText = (urgency) => {
  const map = { high: '高', medium: '中', low: '低' }
  return map[urgency] || '低'
}

const getRepairStatusType = (status) => {
  const map = { PENDING: 'info', PROCESSING: 'primary', COMPLETED: 'success' }
  return map[status] || 'info'
}

const getRepairStatusText = (status) => {
  const map = { PENDING: '待处理', PROCESSING: '处理中', COMPLETED: '已完成' }
  return map[status] || status
}

const fetchBuildings = async () => {
  try {
    const res = await dormitoryApi.getBuildings()
    buildings.value = res.data || []
  } catch (error) {
    console.error('获取楼栋列表失败:', error)
  }
}

const fetchStats = async () => {
  try {
    const res = await dormitoryApi.getRepairStats(filterParams.building)
    stats.value = res.data || { pending: 0, processing: 0, completed: 0, total: 0 }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const fetchRepairList = async () => {
  loading.value = true
  try {
    const res = await dormitoryApi.getRepairList({
      page: pagination.page,
      size: pagination.size,
      ...filterParams
    })
    repairList.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    console.error('获取报修列表失败:', error)
  } finally {
    loading.value = false
  }
}

const getRowIndex = (index) => (pagination.page - 1) * pagination.size + index + 1

const handleFilterChange = () => {
  pagination.page = 1
  fetchStats()
}

const resetFilter = () => {
  filterParams.building = ''
  filterParams.status = ''
  filterParams.urgency = ''
  pagination.page = 1
  fetchStats()
  fetchRepairList()
}

const viewDetail = (row) => {
  currentRepair.value = row
  showDetailDialog.value = true
}

const startRepair = async (row) => {
  try {
    await ElMessageBox.confirm('确定开始处理该报修吗？', '开始处理', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await dormitoryApi.startRepair(row.id)
    if (res.code === 200) {
      ElMessage.success('已开始处理')
      fetchRepairList()
      fetchStats()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

const openCompleteDialog = (row) => {
  currentRepair.value = row
  completeForm.remark = ''
  completeForm.handleImages = []
  completeFileList.value = []
  showCompleteDialog.value = true
}

const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }
  return true
}

const customUpload = async (options) => {
  const formData = new FormData()
  formData.append('file', options.file)
  
  try {
    const token = sessionStorage.getItem('token')
    const response = await fetch('/api/file/upload', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      },
      body: formData
    })
    
    const result = await response.json()
    if (result.code === 200) {
      options.onSuccess(result, options.file)
    } else {
      options.onError(new Error(result.message || '上传失败'))
      ElMessage.error(result.message || '上传失败')
    }
  } catch (error) {
    options.onError(error)
    ElMessage.error('上传失败')
  }
}

const handleUploadSuccess = (response) => {
  if (response.code === 200) {
    completeForm.handleImages.push(response.data.url)
  }
}

const handleUploadRemove = (file) => {
  const url = file.response?.data?.url || file.url
  const index = completeForm.handleImages.indexOf(url)
  if (index > -1) {
    completeForm.handleImages.splice(index, 1)
  }
}

const submitComplete = async () => {
  if (!completeFormRef.value) return

  await completeFormRef.value.validate(async (valid) => {
    if (!valid) return

    if (!completeForm.remark.trim() && completeForm.handleImages.length === 0) {
      ElMessage.warning('请填写处理备注或上传处理照片')
      return
    }

    completeLoading.value = true
    try {
      const res = await dormitoryApi.completeRepair(currentRepair.value.id, {
        remark: completeForm.remark,
        handleImages: completeForm.handleImages.length > 0 ? JSON.stringify(completeForm.handleImages) : null
      })
      if (res.code === 200) {
        ElMessage.success('处理完成')
        showCompleteDialog.value = false
        fetchRepairList()
        fetchStats()
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    } catch (error) {
      ElMessage.error(error.message || '操作失败')
    } finally {
      completeLoading.value = false
    }
  })
}

onMounted(() => {
  fetchBuildings()
  fetchStats()
  fetchRepairList()
})
</script>

<style scoped>
.page-container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-icon.pending {
  background: #fef0f0;
  color: #f56c6c;
}

.stat-icon.processing {
  background: #ecf5ff;
  color: #409eff;
}

.stat-icon.completed {
  background: #f0f9eb;
  color: #67c23a;
}

.stat-icon.total {
  background: #f4f4f5;
  color: #909399;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.image-preview {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.preview-image {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  cursor: pointer;
}

.repair-info {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  font-size: 14px;
}

.repair-info p {
  margin: 4px 0;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
