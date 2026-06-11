<template>
  <div class="secondhand-admin">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>二手交易审核管理</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="待审核" name="pending">
          <el-table :data="pendingList" v-loading="pendingLoading">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="title" label="商品标题" min-width="150" />
            <el-table-column prop="price" label="价格" width="100">
              <template #default="{ row }">
                ¥{{ row.price }}
              </template>
            </el-table-column>
            <el-table-column prop="category" label="分类" width="100" />
            <el-table-column prop="sellerName" label="卖家" width="100" />
            <el-table-column prop="createTime" label="发布时间" width="160" />
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="showDetail(row)">详情</el-button>
                <el-button type="success" size="small" @click="handleApprove(row)">通过</el-button>
                <el-button type="danger" size="small" @click="handleReject(row)">拒绝</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="pendingPage"
            :page-size="10"
            :total="pendingTotal"
            layout="total, prev, pager, next"
            @current-change="loadPendingList"
            style="margin-top: 20px; justify-content: center"
          />
        </el-tab-pane>

        <el-tab-pane label="所有商品" name="all">
          <el-form inline>
            <el-form-item>
              <span slot="label" class="filter-label">状态筛选</span>
              <el-select v-model="statusFilter" placeholder="全部" clearable @change="loadAllList">
                <el-option label="全部" value="" />
                <el-option label="待审核" value="PENDING" />
                <el-option label="已上架" value="APPROVED" />
                <el-option label="已拒绝" value="REJECTED" />
                <el-option label="已下架" value="REMOVED" />
                <el-option label="已售出" value="SOLD" />
              </el-select>
            </el-form-item>
          </el-form>
          <el-table :data="allList" v-loading="allLoading">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="title" label="商品标题" min-width="150" />
            <el-table-column prop="price" label="价格" width="100">
              <template #default="{ row }">
                ¥{{ row.price }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="sellerName" label="卖家" width="100" />
            <el-table-column prop="createTime" label="发布时间" width="160" />
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="showDetail(row)">详情</el-button>
                <el-button v-if="row.status === 'APPROVED' || row.status === 'PENDING'" type="danger" size="small" @click="handleRemove(row)">下架</el-button>
                <el-button type="info" size="small" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="allPage"
            :page-size="10"
            :total="allTotal"
            layout="total, prev, pager, next"
            @current-change="loadAllList"
            style="margin-top: 20px; justify-content: center"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="rejectDialogVisible" title="拒绝原因" width="400px">
      <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="请输入拒绝原因" />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="removeDialogVisible" title="下架原因" width="400px">
      <el-input v-model="removeReason" type="textarea" :rows="4" placeholder="请输入下架原因" />
      <template #footer>
        <el-button @click="removeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRemove">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deleteDialogVisible" title="删除商品" width="400px">
      <p style="color: #f56c6c; margin-bottom: 15px;">确定要删除该商品吗？此操作不可恢复！</p>
      <el-input v-model="deleteReason" type="textarea" :rows="3" placeholder="请输入删除原因（可选）" />
      <template #footer>
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmDelete">确定删除</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="商品详情" width="700px">
      <el-descriptions :column="2" border v-if="currentDetail">
        <el-descriptions-item label="商品ID">{{ currentDetail.id }}</el-descriptions-item>
        <el-descriptions-item label="商品标题">{{ currentDetail.title }}</el-descriptions-item>
        <el-descriptions-item label="价格">¥{{ currentDetail.price }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ getCategoryText(currentDetail.category) }}</el-descriptions-item>
        <el-descriptions-item label="新旧程度">{{ getConditionText(currentDetail.condition) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentDetail.status)">{{ getStatusText(currentDetail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="卖家">{{ currentDetail.sellerName }}</el-descriptions-item>
        <el-descriptions-item label="卖家电话">{{ currentDetail.sellerPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="浏览次数">{{ currentDetail.viewCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ currentDetail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="商品描述" :span="2">{{ currentDetail.description || '暂无描述' }}</el-descriptions-item>
        <el-descriptions-item label="商品图片" :span="2" v-if="currentDetail.imageList && currentDetail.imageList.length > 0">
          <div class="image-list">
            <el-image
              v-for="(img, index) in currentDetail.imageList"
              :key="index"
              :src="img"
              :preview-src-list="currentDetail.imageList"
              :initial-index="index"
              fit="cover"
              class="goods-image"
            />
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="拒绝原因" :span="2" v-if="currentDetail.rejectReason">
          <span style="color: #f56c6c;">{{ currentDetail.rejectReason }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="下架原因" :span="2" v-if="currentDetail.removeReason">
          <span style="color: #e6a23c;">{{ currentDetail.removeReason }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const activeTab = ref('pending')
const pendingList = ref([])
const pendingLoading = ref(false)
const pendingPage = ref(1)
const pendingTotal = ref(0)

const allList = ref([])
const allLoading = ref(false)
const allPage = ref(1)
const allTotal = ref(0)
const statusFilter = ref('')

const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const currentRejectItem = ref(null)

const removeDialogVisible = ref(false)
const removeReason = ref('')
const currentRemoveItem = ref(null)

const deleteDialogVisible = ref(false)
const deleteReason = ref('')
const currentDeleteItem = ref(null)

const detailDialogVisible = ref(false)
const currentDetail = ref(null)

const getStatusType = (status) => {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    REMOVED: 'info',
    SOLD: ''
  }
  return map[status] || ''
}

const getStatusText = (status) => {
  const map = {
    PENDING: '待审核',
    APPROVED: '已上架',
    REJECTED: '已拒绝',
    REMOVED: '已下架',
    SOLD: '已售出'
  }
  return map[status] || status
}

const getCategoryText = (category) => {
  const map = {
    ELECTRONICS: '电子产品',
    BOOKS: '图书教材',
    CLOTHING: '服装鞋帽',
    SPORTS: '运动户外',
    DAILY: '生活用品',
    OTHER: '其他'
  }
  return map[category] || category
}

const getConditionText = (condition) => {
  const map = {
    NEW: '全新',
    LIKE_NEW: '几乎全新',
    GOOD: '良好',
    FAIR: '一般'
  }
  return map[condition] || condition
}

const showDetail = async (row) => {
  try {
    const res = await request.get(`/secondhand/${row.id}`)
    if (res.code === 200) {
      currentDetail.value = res.data
      detailDialogVisible.value = true
    }
  } catch (error) {
    console.error('获取详情失败:', error)
  }
}

const loadPendingList = async () => {
  pendingLoading.value = true
  try {
    const res = await request.get('/admin/secondhand/pending', {
      params: { page: pendingPage.value, size: 10 }
    })
    if (res.code === 200) {
      pendingList.value = res.data.records || []
      pendingTotal.value = res.data.total || 0
    }
  } catch (error) {
    console.error('加载待审核列表失败:', error)
  } finally {
    pendingLoading.value = false
  }
}

const loadAllList = async () => {
  allLoading.value = true
  try {
    const params = { page: allPage.value, size: 10 }
    if (statusFilter.value) {
      params.status = statusFilter.value
    }
    const res = await request.get('/admin/secondhand/all', { params })
    if (res.code === 200) {
      allList.value = res.data.records || []
      allTotal.value = res.data.total || 0
    }
  } catch (error) {
    console.error('加载列表失败:', error)
  } finally {
    allLoading.value = false
  }
}

const handleApprove = async (row) => {
  try {
    const res = await request.put(`/admin/secondhand/${row.id}/approve`)
    if (res.code === 200) {
      ElMessage.success('审核通过')
      loadPendingList()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    console.error('审核通过失败:', error)
  }
}

const handleReject = (row) => {
  currentRejectItem.value = row
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  try {
    const res = await request.put(`/admin/secondhand/${currentRejectItem.value.id}/reject`, {
      reason: rejectReason.value
    })
    if (res.code === 200) {
      ElMessage.success('已拒绝')
      rejectDialogVisible.value = false
      loadPendingList()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleRemove = (row) => {
  currentRemoveItem.value = row
  removeReason.value = ''
  removeDialogVisible.value = true
}

const confirmRemove = async () => {
  if (!removeReason.value.trim()) {
    ElMessage.warning('请输入下架原因')
    return
  }
  try {
    const res = await request.put(`/admin/secondhand/${currentRemoveItem.value.id}/remove`, {
      reason: removeReason.value
    })
    if (res.code === 200) {
      ElMessage.success('已下架')
      removeDialogVisible.value = false
      loadAllList()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = (row) => {
  currentDeleteItem.value = row
  deleteReason.value = ''
  deleteDialogVisible.value = true
}

const confirmDelete = async () => {
  try {
    const res = await request.delete(`/admin/secondhand/${currentDeleteItem.value.id}`, {
      data: { reason: deleteReason.value }
    })
    if (res.code === 200) {
      ElMessage.success('删除成功')
      deleteDialogVisible.value = false
      loadAllList()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const handleTabChange = (tabName) => {
  if (tabName === 'pending') {
    loadPendingList()
  } else if (tabName === 'all') {
    loadAllList()
  }
}

onMounted(() => {
  loadPendingList()
})
</script>

<style scoped>
.secondhand-admin {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-label {
  font-size: 14px;
  color: #606266;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.goods-image {
  width: 100px;
  height: 100px;
  border-radius: 4px;
  cursor: pointer;
}
</style>
