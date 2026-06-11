<template>
  <div class="lostfound-admin">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>失物招领管理</span>
        </div>
      </template>

      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable @change="loadList">
            <el-option label="有效" value="ACTIVE" />
            <el-option label="已认领" value="CLAIMED" />
            <el-option label="已下架" value="REMOVED" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 'LOST' ? 'danger' : 'success'">
              {{ row.type === 'LOST' ? '失物' : '招领' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="150" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publisherName" label="发布者" width="100" />
        <el-table-column prop="createTime" label="发布时间" width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'ACTIVE'" type="danger" size="small" @click="handleRemove(row)">下架</el-button>
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

    <el-dialog v-model="removeDialogVisible" title="下架原因" width="400px">
      <el-input v-model="removeReason" type="textarea" :rows="4" placeholder="请输入下架原因" />
      <template #footer>
        <el-button @click="removeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRemove">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const list = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const statusFilter = ref('')

const removeDialogVisible = ref(false)
const removeReason = ref('')
const currentItem = ref(null)

const getStatusType = (status) => {
  const map = { ACTIVE: 'success', CLAIMED: 'warning', REMOVED: 'info' }
  return map[status] || ''
}

const getStatusText = (status) => {
  const map = { ACTIVE: '有效', CLAIMED: '已认领', REMOVED: '已下架' }
  return map[status] || status
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/lostfound/all', {
      params: { page: page.value, size: 10, status: statusFilter.value }
    })
    if (res.code === 200) {
      list.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('加载列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleRemove = (row) => {
  currentItem.value = row
  removeReason.value = ''
  removeDialogVisible.value = true
}

const confirmRemove = async () => {
  if (!removeReason.value.trim()) {
    ElMessage.warning('请输入下架原因')
    return
  }
  try {
    const res = await request.put(`/admin/lostfound/${currentItem.value.id}/remove`, {
      reason: removeReason.value
    })
    if (res.code === 200) {
      ElMessage.success('已下架')
      removeDialogVisible.value = false
      loadList()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.lostfound-admin {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
