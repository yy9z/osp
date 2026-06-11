<template>
  <div class="secondhand-my">
    <el-card>
      <template #header>
        <span>我的发布</span>
      </template>

      <el-table :data="list" v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="商品标题" min-width="150" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">
            ¥{{ row.price }}
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'REJECTED'" type="primary" size="small" @click="handleResubmit(row)">重新提交</el-button>
            <el-button type="info" size="small" @click="viewDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        :page-size="10"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadList"
        style="margin-top: 20px; justify-content: center"
      />
    </el-card>

    <el-dialog v-model="resubmitDialogVisible" title="重新提交商品" width="600px">
      <el-form :model="resubmitForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="resubmitForm.title" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="resubmitForm.category">
            <el-option label="电子产品" value="ELECTRONICS" />
            <el-option label="图书" value="BOOKS" />
            <el-option label="服装" value="CLOTHING" />
            <el-option label="运动" value="SPORTS" />
            <el-option label="日用品" value="DAILY" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="resubmitForm.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="新旧程度">
          <el-select v-model="resubmitForm.condition">
            <el-option label="全新" value="NEW" />
            <el-option label="几乎全新" value="LIKE_NEW" />
            <el-option label="较好" value="GOOD" />
            <el-option label="一般" value="FAIR" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="resubmitForm.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resubmitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmResubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { secondhandApi } from '@/api/secondhand'

const router = useRouter()

const list = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)

const resubmitDialogVisible = ref(false)
const resubmitForm = ref({ id: null, title: '', category: '', price: 0, condition: '', description: '' })

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

const loadList = async () => {
  loading.value = true
  try {
    const res = await secondhandApi.getMyList({
      page: page.value,
      size: 10
    })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('加载列表失败:', error)
    ElMessage.error(error.message || '加载列表失败')
  } finally {
    loading.value = false
  }
}

const viewDetail = (row) => {
  router.push(`/secondhand/${row.id}`)
}

const handleResubmit = (row) => {
  resubmitForm.value = {
    id: row.id,
    title: row.title,
    category: row.category,
    price: row.price,
    condition: row.condition,
    description: row.description
  }
  resubmitDialogVisible.value = true
}

const confirmResubmit = async () => {
  try {
    await secondhandApi.update(resubmitForm.value.id, resubmitForm.value)
    ElMessage.success('提交成功，等待审核')
    resubmitDialogVisible.value = false
    loadList()
  } catch (error) {
    ElMessage.error(error.message || '提交失败')
  }
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.secondhand-my {
  padding: 20px;
}
</style>
