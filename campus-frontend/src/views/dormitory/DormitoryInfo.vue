<template>
  <div class="page-container">
    <div class="page-header">
      <h2>宿舍信息</h2>
      <p>集中查看我的宿舍、宿舍列表、宿舍详情和宿舍成员信息。</p>
    </div>

    <el-row :gutter="20" class="page-grid">
      <el-col :xs="24" :lg="canViewDormitoryList ? 8 : 24">
        <el-card v-loading="myDormitoryLoading" class="section-card my-dormitory-card">
          <template #header>
            <div class="card-header">
              <span>我的宿舍</span>
              <el-button v-if="myDormitory" link type="primary" @click="openDormitoryDetail(myDormitory.id)">
                查看详情
              </el-button>
            </div>
          </template>

          <div v-if="myDormitory" class="my-dormitory-content">
            <div class="room-badge">{{ formatRoom(myDormitory) }}</div>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="校区">{{ myDormitoryMeta.campus || '-' }}</el-descriptions-item>
              <el-descriptions-item label="楼栋">{{ myDormitory.building }}</el-descriptions-item>
              <el-descriptions-item label="楼层">{{ myDormitory.floor }} 层</el-descriptions-item>
              <el-descriptions-item label="房间号">{{ myDormitory.roomNo }}</el-descriptions-item>
              <el-descriptions-item label="床位号">{{ myDormitoryMeta.bed || '-' }}</el-descriptions-item>
              <el-descriptions-item label="入住日期">{{ myDormitoryMeta.checkInDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="宿舍类型">{{ myDormitory.type || '-' }}</el-descriptions-item>
              <el-descriptions-item label="当前人数">
                {{ myDormitory.currentCount || 0 }}/{{ myDormitory.capacity || '-' }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <el-empty v-else description="当前账号暂无已分配宿舍" />
        </el-card>
      </el-col>

      <el-col v-if="canViewDormitoryList" :xs="24" :lg="16">
        <el-card v-if="canViewDormitoryList" class="section-card">
          <template #header>
            <div class="card-header">
              <span>{{ listTitle }}</span>
              <el-button link type="primary" @click="fetchDormitoryList">刷新</el-button>
            </div>
          </template>

          <el-form :inline="true" class="filter-form">
            <el-form-item v-if="isAdmin" label="楼栋">
              <el-input v-model="queryForm.building" placeholder="如 1号楼" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item label="楼层">
              <el-input-number v-model="queryForm.floor" :min="1" :max="50" placeholder="楼层" />
            </el-form-item>
            <el-form-item label="房间号">
              <el-input v-model="queryForm.roomNo" placeholder="如 101" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="dormitoryList" v-loading="listLoading" stripe>
            <el-table-column prop="building" label="楼栋" min-width="100" />
            <el-table-column prop="floor" label="楼层" width="90" />
            <el-table-column prop="roomNo" label="房间号" width="110" />
            <el-table-column prop="type" label="类型" width="120" />
            <el-table-column label="入住情况" width="120">
              <template #default="{ row }">
                {{ row.currentCount || 0 }}/{{ row.capacity || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="性别" width="100">
              <template #default="{ row }">{{ formatGender(row.gender) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDormitoryDetail(row.id)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination" v-if="pagination.total > 0">
            <el-pagination
              v-model:current-page="pagination.page"
              v-model:page-size="pagination.size"
              :page-sizes="[10, 20, 30]"
              :total="pagination.total"
              layout="total, sizes, prev, pager, next"
              @size-change="fetchDormitoryList"
              @current-change="fetchDormitoryList"
            />
          </div>
        </el-card>

      </el-col>
    </el-row>

    <el-drawer v-model="detailVisible" title="宿舍详情" size="700px">
      <div v-loading="detailLoading" class="detail-wrapper" v-if="currentDormitory">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="楼栋">{{ currentDormitory.building }}</el-descriptions-item>
          <el-descriptions-item label="楼层">{{ currentDormitory.floor }} 层</el-descriptions-item>
          <el-descriptions-item label="房间号">{{ currentDormitory.roomNo }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ currentDormitory.type || '-' }}</el-descriptions-item>
          <el-descriptions-item label="容量">
            {{ currentDormitory.currentCount || 0 }}/{{ currentDormitory.capacity || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="性别">{{ formatGender(currentDormitory.gender) }}</el-descriptions-item>
          <el-descriptions-item label="宿舍长" :span="2">
            {{ currentDormitory.head?.realName || currentDormitory.head?.username || '暂无' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="members-section">
          <div class="members-header">宿舍成员</div>
          <el-table :data="memberList" empty-text="暂无成员信息" stripe>
            <el-table-column prop="realName" label="姓名" min-width="120" />
            <el-table-column prop="username" label="账号" min-width="140" />
            <el-table-column prop="phone" label="手机号" min-width="140" />
            <el-table-column prop="joinTime" label="加入时间" min-width="180" />
          </el-table>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { dormitoryApi, userApi } from '@/api'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores'

const userStore = useUserStore()

const myDormitoryLoading = ref(false)
const listLoading = ref(false)
const detailLoading = ref(false)

const myDormitory = ref(null)
const myDormitoryMeta = ref({
  campus: '',
  bed: '',
  checkInDate: ''
})
const dormitoryList = ref([])
const currentDormitory = ref(null)
const memberList = ref([])
const detailVisible = ref(false)
const managedBuildingName = ref('')

const queryForm = reactive({
  building: '',
  floor: null,
  roomNo: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const isAdmin = computed(() => userStore.isAdmin)
const isDormManager = computed(() => userStore.isDormManager)
const canViewDormitoryList = computed(() => isAdmin.value || isDormManager.value)
const listTitle = computed(() => (isDormManager.value ? '负责楼栋房间情况' : '宿舍列表'))

const loadManagedBuilding = async () => {
  if (!isDormManager.value) {
    return
  }
  try {
    const res = await dormitoryApi.getMyBuilding()
    managedBuildingName.value = res.data?.name || res.data?.code || ''
    queryForm.building = managedBuildingName.value
  } catch (error) {
    console.error('获取管理楼栋失败:', error)
  }
}

const fetchMyDormitory = async () => {
  myDormitoryLoading.value = true
  try {
    const [dormitoryRes, profileDormitoryRes] = await Promise.all([
      dormitoryApi.getMyDormitory(),
      userApi.getDormitory().catch(() => null)
    ])

    myDormitory.value = dormitoryRes.code === 200 ? dormitoryRes.data || null : null
    myDormitoryMeta.value = {
      campus: profileDormitoryRes?.data?.campus || '',
      bed: profileDormitoryRes?.data?.bed || '',
      checkInDate: profileDormitoryRes?.data?.checkInDate || ''
    }
  } catch (error) {
    console.error('获取我的宿舍失败:', error)
    myDormitory.value = null
    myDormitoryMeta.value = {
      campus: '',
      bed: '',
      checkInDate: ''
    }
  } finally {
    myDormitoryLoading.value = false
  }
}

const fetchDormitoryList = async () => {
  if (!canViewDormitoryList.value) {
    dormitoryList.value = []
    pagination.total = 0
    return
  }
  listLoading.value = true
  try {
    const res = await dormitoryApi.getList({
      page: pagination.page,
      size: pagination.size,
      building: isAdmin.value ? (queryForm.building || undefined) : undefined,
      floor: queryForm.floor || undefined,
      roomNo: queryForm.roomNo || undefined
    })
    dormitoryList.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    console.error('获取宿舍列表失败:', error)
    ElMessage.error('获取宿舍列表失败')
  } finally {
    listLoading.value = false
  }
}

const openDormitoryDetail = async (id) => {
  if (!id) return
  detailVisible.value = true
  detailLoading.value = true
  try {
    const detailRes = await dormitoryApi.getDetail(id)
    currentDormitory.value = detailRes.data || null
    memberList.value = detailRes.data?.members || []

    try {
      const memberRes = await dormitoryApi.getMembers(id)
      memberList.value = memberRes.data || memberList.value
    } catch (error) {
      console.warn('获取宿舍成员失败，已回退到详情接口中的成员信息:', error)
    }
  } catch (error) {
    console.error('获取宿舍详情失败:', error)
    ElMessage.error('获取宿舍详情失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchDormitoryList()
}

const handleReset = () => {
  queryForm.building = isDormManager.value ? managedBuildingName.value : ''
  queryForm.floor = null
  queryForm.roomNo = ''
  pagination.page = 1
  fetchDormitoryList()
}

const formatRoom = (dormitory) => {
  if (!dormitory) return '-'
  return `${dormitory.building || '-'} ${dormitory.roomNo || '-'}`
}

const formatGender = (gender) => {
  const map = {
    MALE: '男',
    FEMALE: '女'
  }
  return map[gender] || gender || '-'
}

onMounted(async () => {
  fetchMyDormitory()
  await loadManagedBuilding()
  fetchDormitoryList()
})
</script>

<style scoped>
.page-container {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 8px;
  font-size: 24px;
  color: #1f2937;
}

.page-header p {
  margin: 0;
  color: #6b7280;
}

.page-grid {
  align-items: stretch;
}

.section-card {
  margin-bottom: 20px;
}

.my-dormitory-card {
  height: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.my-dormitory-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.room-badge {
  display: inline-flex;
  width: fit-content;
  padding: 8px 14px;
  border-radius: 999px;
  background: linear-gradient(135deg, #0f766e, #14b8a6);
  color: #fff;
  font-weight: 600;
}

.filter-form {
  margin-bottom: 16px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.detail-wrapper {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.members-section {
  margin-top: 8px;
}

.members-header {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
}

.permission-card {
  display: none;
}

@media (max-width: 768px) {
  .page-header h2 {
    font-size: 20px;
  }

  .pagination {
    justify-content: center;
  }
}
</style>
