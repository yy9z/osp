<template>
  <div class="lostfound-my">
    <el-card>
      <template #header>
        <span>我的发布</span>
      </template>

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
        <el-table-column prop="createTime" label="发布时间" width="160" />
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const list = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)

const getStatusType = (status) => {
  const map = { OPEN: 'success', RESOLVED: 'warning', REMOVED: 'info' }
  return map[status] || ''
}

const getStatusText = (status) => {
  const map = { OPEN: '进行中', RESOLVED: '已解决', REMOVED: '已下架' }
  return map[status] || status
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await request.get('/lostfound/my', {
      params: { page: page.value, size: 10 }
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

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.lostfound-my {
  padding: 20px;
}
</style>
