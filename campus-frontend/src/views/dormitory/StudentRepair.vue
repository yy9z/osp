<template>
  <div class="page-container">
    <div class="page-header">
      <h2>我的报修</h2>
      <el-button type="primary" @click="handleApplyRepair">
        <el-icon><Plus /></el-icon>
        申请报修
      </el-button>
    </div>

    <el-card>
      <el-table :data="repairs" v-loading="repairLoading" style="width: 100%">
        <el-table-column type="index" label="编号" width="80" :index="getRepairRowIndex" />
        <el-table-column prop="description" label="报修描述" show-overflow-tooltip />
        <el-table-column prop="urgency" label="紧急程度" width="100">
          <template #default="{ row }">
            <el-tag :type="getUrgencyType(row.urgency)" size="small">
              {{ getUrgencyText(row.urgency) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getRepairStatusType(row.status)">
              {{ getRepairStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="180" />
        <el-table-column prop="remark" label="处理备注" show-overflow-tooltip />
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="viewRepairDetail(row)">
              查看详情
            </el-button>
            <el-button 
              v-if="row.status === 'PENDING'" 
              size="small" 
              link 
              type="danger" 
              @click="handleCancelRepair(row)"
            >
              撤销
            </el-button>
            <template v-else-if="row.status === 'CANCELLED'">
              <el-button 
                size="small" 
                link 
                type="primary" 
                @click="handleEditRepair(row)"
              >
                编辑
              </el-button>
              <el-button 
                size="small" 
                link 
                type="success" 
                @click="handleReapplyRepair(row)"
              >
                重新申请
              </el-button>
            </template>
            <el-button
              v-if="row.status === 'COMPLETED' || row.status === 'CANCELLED'"
              size="small"
              link
              type="danger"
              @click="handleDeleteRepair(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" v-if="repairTotal > 0">
        <el-pagination
          v-model:current-page="repairParams.page"
          v-model:page-size="repairParams.size"
          :total="repairTotal"
          :page-sizes="[10, 20, 30]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchRepairs"
          @current-change="fetchRepairs"
        />
      </div>
    </el-card>

    <el-dialog v-model="showRepairDialog" :title="isEditing ? '编辑报修' : '申请报修'" width="550px" :close-on-click-modal="false">
      <el-form :model="repairForm" :rules="repairRules" ref="repairFormRef" label-width="90px">
        <el-form-item label="问题描述" prop="description">
          <el-input
            v-model="repairForm.description"
            type="textarea"
            :rows="4"
            placeholder="请详细描述需要维修的问题"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="紧急程度" prop="urgency">
          <el-select v-model="repairForm.urgency" placeholder="请选择紧急程度" style="width: 100%">
            <el-option label="低" value="low" />
            <el-option label="中" value="medium" />
            <el-option label="高" value="high" />
          </el-select>
        </el-form-item>
        <el-form-item label="上传图片">
          <div class="image-upload">
            <div class="upload-list" v-if="repairForm.images?.length">
              <div v-for="(img, index) in repairForm.images" :key="index" class="upload-item">
                <img :src="img" />
                <div class="upload-item-actions" @click="removeImage(index)">
                  <el-icon><Close /></el-icon>
                </div>
              </div>
            </div>
            <el-upload
              v-if="!repairForm.images || repairForm.images.length < 3"
              class="upload-trigger"
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleImageChange"
              :limit="3"
              accept="image/*"
            >
              <el-icon><Plus /></el-icon>
              <div class="upload-text">上传图片</div>
            </el-upload>
          </div>
          <div class="upload-tip">最多上传3张图片，单张不超过5MB</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRepairDialog = false">取消</el-button>
        <el-button type="primary" @click="submitRepair" :loading="submitLoading">
          {{ isEditing ? '保存修改' : '提交' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showDetailDialog" title="报修详情" width="600px">
      <el-descriptions :column="2" border v-if="currentRepair">
        <el-descriptions-item label="编号">{{ currentRepair.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getRepairStatusType(currentRepair.status)">
            {{ getRepairStatusText(currentRepair.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="楼栋">{{ currentRepair.building }}</el-descriptions-item>
        <el-descriptions-item label="房间号">{{ currentRepair.roomNo }}</el-descriptions-item>
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
        <el-descriptions-item label="处理完成时间" v-if="currentRepair.handleTime && currentRepair.status === 'COMPLETED'">{{ currentRepair.handleTime }}</el-descriptions-item>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { dormitoryApi, userApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Close } from '@element-plus/icons-vue'
import { uploadToOSS } from '@/utils/oss'

const repairLoading = ref(false)
const submitLoading = ref(false)

const repairs = ref([])
const repairTotal = ref(0)
const hasDormitory = ref(false)
const myDormitory = ref(null)

const showRepairDialog = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const showDetailDialog = ref(false)
const currentRepair = ref(null)

const repairFormRef = ref()

const repairForm = reactive({
  description: '',
  urgency: 'low',
  images: []
})

const repairParams = reactive({
  page: 1,
  size: 10
})

const repairRules = {
  description: [
    { required: true, message: '请输入问题描述', trigger: 'blur' },
    { min: 5, message: '问题描述至少5个字符', trigger: 'blur' }
  ],
  urgency: [
    { required: true, message: '请选择紧急程度', trigger: 'change' }
  ]
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
  const map = { PENDING: 'info', PROCESSING: 'primary', COMPLETED: 'success', CANCELLED: 'info' }
  return map[status] || 'info'
}

const getRepairStatusText = (status) => {
  const map = { PENDING: '待处理', PROCESSING: '处理中', COMPLETED: '已完成', CANCELLED: '已撤销' }
  return map[status] || status
}

const checkDormitory = async () => {
  try {
    const res = await dormitoryApi.getMyDormitory()
    myDormitory.value = res.data || null
    hasDormitory.value = !!res.data
  } catch (error) {
    myDormitory.value = null
    try {
      const profileRes = await userApi.getDormitory()
      hasDormitory.value = !!profileRes.data
    } catch {
      hasDormitory.value = false
    }
  }
}

const fetchRepairs = async () => {
  repairLoading.value = true
  try {
    const res = await dormitoryApi.getMyRepairs(repairParams)
    repairs.value = res.data?.records || []
    repairTotal.value = res.data?.total || 0
  } catch (error) {
    console.error('获取报修列表失败:', error)
  } finally {
    repairLoading.value = false
  }
}

const getRepairRowIndex = (index) => (repairParams.page - 1) * repairParams.size + index + 1

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

const handleImageChange = async (file) => {
  if (!beforeUpload(file.raw)) return
  
  if (repairForm.images.length >= 3) {
    ElMessage.warning('最多上传3张图片')
    return
  }

  try {
    ElMessage.info('正在上传图片...')
    const url = await uploadToOSS(file.raw)
    repairForm.images.push(url)
    ElMessage.success('图片上传成功')
  } catch (error) {
    console.error('图片上传失败:', error)
  }
}

const removeImage = (index) => {
  repairForm.images.splice(index, 1)
}

const submitRepair = async () => {
  if (!repairFormRef.value) return

  await repairFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      if (isEditing.value && editingId.value) {
        const res = await dormitoryApi.updateRepair(editingId.value, {
          description: repairForm.description,
          urgency: repairForm.urgency,
          images: repairForm.images.length > 0 ? JSON.stringify(repairForm.images) : null
        })
        if (res.code === 200) {
          ElMessage.success('修改成功')
          showRepairDialog.value = false
          resetRepairForm()
          fetchRepairs()
        } else {
          ElMessage.error(res.message || '修改失败')
        }
      } else {
        let requestPayload = null

        const dormRes = await dormitoryApi.getMyDormitory()
        if (dormRes.data?.id) {
          requestPayload = {
            dormitoryId: dormRes.data.id,
            description: repairForm.description,
            urgency: repairForm.urgency,
            images: repairForm.images.length > 0 ? JSON.stringify(repairForm.images) : null
          }
        } else {
          const profileRes = await userApi.getDormitory()
          if (!profileRes.data) {
            ElMessage.error('您还没有填写或分配宿舍信息')
            return
          }

          requestPayload = {
            campus: profileRes.data.campus,
            building: profileRes.data.building,
            roomNo: profileRes.data.room,
            description: repairForm.description,
            urgency: repairForm.urgency,
            images: repairForm.images.length > 0 ? JSON.stringify(repairForm.images) : null
          }
        }

        await dormitoryApi.createRepair(requestPayload)
        ElMessage.success('报修申请已提交')
        showRepairDialog.value = false
        resetRepairForm()
        fetchRepairs()
        checkDormitory()
      }
    } catch (error) {
      ElMessage.error(error.message || '提交失败，请稍后重试')
    } finally {
      submitLoading.value = false
    }
  })
}

const resetRepairForm = () => {
  repairForm.description = ''
  repairForm.urgency = 'low'
  repairForm.images = []
}

const viewRepairDetail = (row) => {
  currentRepair.value = row
  showDetailDialog.value = true
}

const handleCancelRepair = async (row) => {
  try {
    await ElMessageBox.confirm('确定要撤销该报修申请吗？', '撤销确认', {
      confirmButtonText: '确定撤销',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await dormitoryApi.cancelRepair(row.id)
    if (res.code === 200) {
      ElMessage.success('撤销成功')
      fetchRepairs()
    } else {
      ElMessage.error(res.message || '撤销失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '撤销失败')
    }
  }
}

const handleApplyRepair = async () => {
  if (!hasDormitory.value) {
    ElMessage.warning('您还没有分配宿舍，请先联系管理员分配宿舍')
    return
  }
  isEditing.value = false
  editingId.value = null
  resetRepairForm()
  showRepairDialog.value = true
}

const handleEditRepair = async (row) => {
  isEditing.value = true
  editingId.value = row.id
  repairForm.description = row.description
  repairForm.urgency = row.urgency || 'low'
  repairForm.images = row.images ? (typeof row.images === 'string' ? JSON.parse(row.images) : row.images) : []
  showRepairDialog.value = true
}

const handleReapplyRepair = async (row) => {
  try {
    await ElMessageBox.confirm('确认重新申请？此操作将把申请时间更新为当前时间并将状态设置为待处理', '重新申请确认', {
      confirmButtonText: '确定重新申请',
      cancelButtonText: '取消',
      type: 'info'
    })
    
    const res = await dormitoryApi.reapplyRepair(row.id)
    
    if (res.code === 200) {
      ElMessage.success('重新申请成功')
      fetchRepairs()
    } else {
      ElMessage.error(res.message || '重新申请失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

const handleDeleteRepair = async (row) => {
  try {
    await ElMessageBox.confirm('删除后无法恢复，确定删除这条报修记录吗？', '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await dormitoryApi.deleteRepair(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      if (repairs.value.length === 1 && repairParams.page > 1) {
        repairParams.page -= 1
      }
      fetchRepairs()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(() => {
  checkDormitory()
  fetchRepairs()
})
</script>

<style scoped>
.page-container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
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
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  cursor: pointer;
  color: white;
  opacity: 0;
  transition: opacity 0.2s;
}

.upload-item:hover .upload-item-actions {
  opacity: 1;
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
</style>
