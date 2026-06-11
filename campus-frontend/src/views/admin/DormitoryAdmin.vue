<template>
  <div class="dormitory-admin">
    <div class="page-header">
      <div>
        <h2>宿舍管理</h2>
        <p>{{ managerHint }}</p>
      </div>
      <div class="header-actions">
        <el-button @click="reloadAll">刷新数据</el-button>
        <el-button type="primary" @click="openBuildingDialog">新增楼栋</el-button>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col v-if="userStore.isAdmin" :xs="24" :xl="24">
        <el-card class="page-card">
          <template #header>
            <div class="card-header">
              <span>楼栋与宿管绑定</span>
            </div>
          </template>

          <el-table :data="buildingList" v-loading="buildingLoading" empty-text="暂无楼栋，请先新增楼栋">
            <el-table-column prop="name" label="楼栋名称" min-width="140" />
            <el-table-column prop="campus" label="校区" min-width="120" />
            <el-table-column prop="floors" label="楼层数" width="100" />
            <el-table-column prop="roomsPerFloor" label="每层房间数" width="120" />
            <el-table-column prop="capacityPerRoom" label="每间入住人数" width="130" />
            <el-table-column label="适用性别" width="110">
              <template #default="{ row }">{{ getGenderText(row.gender) }}</template>
            </el-table-column>
            <el-table-column label="当前宿管" min-width="180">
              <template #default="{ row }">{{ row.managerName || '未分配' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="300">
              <template #default="{ row }">
                <el-button size="small" type="primary" plain @click="openEditBuildingDialog(row)">编辑信息</el-button>
                <el-button size="small" @click="openAssignManagerDialog(row)">分配宿管</el-button>
                <el-button size="small" type="warning" @click="clearManager(row)">设为未分配</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="12">
        <el-card class="page-card">
          <template #header>
            <div class="card-header">
              <span>候选用户</span>
              <span class="subtle-text">点击表格可快速带入分配表单</span>
            </div>
          </template>

          <el-form :inline="true" class="toolbar-form">
            <el-form-item>
              <el-input v-model="userQuery.keyword" placeholder="搜索姓名/账号/手机号" clearable @keyup.enter="handleUserSearch" />
            </el-form-item>
            <el-form-item>
              <el-select v-model="userQuery.assigned" placeholder="分配状态" style="width: 140px" @change="handleUserSearch">
                <el-option label="全部" :value="null" />
                <el-option label="未分配" :value="false" />
                <el-option label="已分配" :value="true" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUserSearch">查询</el-button>
            </el-form-item>
          </el-form>

          <el-table
            :data="userList"
            v-loading="userLoading"
            highlight-current-row
            empty-text="暂无可分配用户"
            @current-change="handleUserCurrentChange"
            @row-click="handleUserCurrentChange"
          >
            <el-table-column prop="realName" label="姓名" min-width="120" />
            <el-table-column prop="username" label="账号" min-width="140" />
            <el-table-column prop="role" label="角色" width="100">
              <template #default="{ row }">{{ getRoleText(row.role) }}</template>
            </el-table-column>
            <el-table-column label="当前宿舍" min-width="180">
              <template #default="{ row }">{{ row.dormitoryText || '未分配' }}</template>
            </el-table-column>
          </el-table>

          <div class="pagination" v-if="userPagination.total > 0">
            <el-pagination
              v-model:current-page="userPagination.page"
              v-model:page-size="userPagination.size"
              :page-sizes="[10, 20, 30]"
              :total="userPagination.total"
              layout="total, sizes, prev, pager, next"
              @size-change="loadUsers"
              @current-change="loadUsers"
            />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="12">
        <el-card class="page-card">
          <template #header>
            <div class="card-header">
              <span>宿舍列表</span>
              <span class="subtle-text">可直接选中宿舍查看成员并用于分配</span>
            </div>
          </template>

          <el-form :inline="true" class="toolbar-form">
            <el-form-item label="楼栋">
              <el-input
                v-model="dormitoryQuery.building"
                :disabled="userStore.isDormManager"
                placeholder="楼栋"
                clearable
                @keyup.enter="handleDormitorySearch"
              />
            </el-form-item>
            <el-form-item label="楼层">
              <el-input v-model="dormitoryQuery.floor" placeholder="如 3" clearable @keyup.enter="handleDormitorySearch" />
            </el-form-item>
            <el-form-item label="房间号">
              <el-input v-model="dormitoryQuery.roomNo" placeholder="房间号" clearable @keyup.enter="handleDormitorySearch" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleDormitorySearch">查询</el-button>
            </el-form-item>
          </el-form>

          <el-table
            :data="dormitoryList"
            v-loading="dormitoryLoading"
            highlight-current-row
            empty-text="当前没有查询到宿舍，可先点击页面右上角新增楼栋"
            @current-change="handleDormitoryCurrentChange"
            @row-click="handleDormitoryCurrentChange"
          >
            <el-table-column prop="building" label="楼栋" min-width="110" />
            <el-table-column prop="floor" label="楼层" width="90" />
            <el-table-column prop="roomNo" label="房间号" width="110" />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column label="入住情况" width="120">
              <template #default="{ row }">{{ row.currentCount || 0 }}/{{ row.capacity || '-' }}</template>
            </el-table-column>
          </el-table>

          <div class="pagination" v-if="dormitoryPagination.total > 0">
            <el-pagination
              v-model:current-page="dormitoryPagination.page"
              v-model:page-size="dormitoryPagination.size"
              :page-sizes="[10, 20, 30, 50]"
              :total="dormitoryPagination.total"
              layout="total, sizes, prev, pager, next"
              @size-change="loadDormitories"
              @current-change="loadDormitories"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="bottom-row">
      <el-col :xs="24" :xl="10">
        <el-card class="page-card">
          <template #header>
            <div class="card-header">
              <span>分配宿舍</span>
              <span class="subtle-text">也可以不点表格，直接在下方选择</span>
            </div>
          </template>

          <el-form :model="assignForm" label-width="90px">
            <el-form-item label="用户">
              <el-select v-model="assignForm.userId" filterable clearable placeholder="请选择用户" style="width: 100%" @change="handleUserSelectChange">
                <el-option
                  v-for="item in userList"
                  :key="item.userId"
                  :label="`${item.realName || item.username} (${getRoleText(item.role)})`"
                  :value="item.userId"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="宿舍">
              <el-select v-model="assignForm.dormitoryId" filterable clearable placeholder="请选择宿舍" style="width: 100%" @change="handleDormitorySelectChange">
                <el-option
                  v-for="item in dormitoryList"
                  :key="item.id"
                  :label="`${item.building} ${item.roomNo} (${item.currentCount || 0}/${item.capacity || '-'})`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="校区">
              <el-input :model-value="selectedCampusText" disabled placeholder="将自动绑定楼栋所属校区" />
            </el-form-item>
            <el-form-item label="床位">
              <el-input v-model="assignForm.bed" placeholder="如：1号床" />
            </el-form-item>
            <el-form-item label="入住日期">
              <el-date-picker v-model="assignForm.checkInDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
            <div class="selected-summary">
              <span>当前用户：{{ selectedUserText }}</span>
              <span>当前宿舍：{{ selectedDormitoryText }}</span>
            </div>
            <el-form-item>
              <el-button type="primary" :loading="assignLoading" :disabled="!canAssign" @click="handleAssign">分配宿舍</el-button>
              <el-button @click="resetAssignForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="14">
        <el-card class="page-card">
          <template #header>
            <div class="card-header">
              <span>当前宿舍成员</span>
              <span class="subtle-text">{{ selectedDormitoryText }}</span>
            </div>
          </template>

          <el-table :data="memberList" v-loading="memberLoading" empty-text="请选择宿舍后查看成员">
            <el-table-column prop="realName" label="姓名" min-width="120" />
            <el-table-column prop="username" label="账号" min-width="130" />
            <el-table-column prop="phone" label="手机号" min-width="140" />
            <el-table-column prop="joinTime" label="入住时间" min-width="180" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button link type="danger" @click="handleCheckout(row)">退宿</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="buildingDialogVisible" title="新增楼栋" width="520px">
      <el-form :model="buildingForm" label-width="90px">
        <el-form-item label="楼栋名称">
          <el-input v-model="buildingForm.name" placeholder="如：学生公寓3栋" />
        </el-form-item>
        <el-form-item label="校区">
          <el-input v-model="buildingForm.campus" placeholder="如：东校区" />
        </el-form-item>
        <el-form-item label="楼层数">
          <el-input-number v-model="buildingForm.floors" :min="1" :max="99" style="width: 100%" />
        </el-form-item>
        <el-form-item label="每层房间数">
          <el-input-number v-model="buildingForm.roomsPerFloor" :min="1" :max="99" style="width: 100%" />
        </el-form-item>
        <el-form-item label="每间住几人">
          <el-input-number v-model="buildingForm.capacityPerRoom" :min="1" :max="12" style="width: 100%" />
        </el-form-item>
        <el-form-item label="适用性别">
          <el-select v-model="buildingForm.gender" clearable placeholder="请选择" style="width: 100%">
            <el-option label="男" value="MALE" />
            <el-option label="女" value="FEMALE" />
          </el-select>
        </el-form-item>
        <el-form-item label="宿管">
          <el-select v-model="buildingForm.managerId" clearable filterable placeholder="可先不分配" style="width: 100%">
            <el-option v-for="item in dormManagerOptions" :key="item.userId" :label="`${item.realName || item.username} (${item.managedBuildingName || '未分配'})`" :value="item.userId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="buildingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="buildingSaving" @click="handleCreateBuilding">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignManagerDialogVisible" title="分配宿管" width="420px">
      <el-form label-width="90px">
        <el-form-item label="楼栋">
          <span>{{ currentBuilding?.name || currentBuilding?.code }}</span>
        </el-form-item>
        <el-form-item label="宿管">
          <el-select v-model="selectedManagerId" clearable filterable placeholder="不选择则设为未分配" style="width: 100%">
            <el-option label="未分配" :value="null" />
            <el-option v-for="item in dormManagerOptions" :key="item.userId" :label="`${item.realName || item.username} (${item.managedBuildingName || '未分配'})`" :value="item.userId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignManagerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="managerSaving" @click="handleAssignManager">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="buildingEditDialogVisible" title="编辑楼栋信息" width="520px">
      <el-form :model="buildingEditForm" label-width="110px">
        <el-form-item label="楼栋名称">
          <el-input v-model="buildingEditForm.name" placeholder="如：学生公寓3栋" />
        </el-form-item>
        <el-form-item label="校区">
          <el-input v-model="buildingEditForm.campus" placeholder="如：东校区" />
        </el-form-item>
        <el-form-item label="楼层数">
          <el-input-number v-model="buildingEditForm.floors" :min="1" :max="99" style="width: 100%" disabled />
        </el-form-item>
        <el-form-item label="每层房间数">
          <el-input-number v-model="buildingEditForm.roomsPerFloor" :min="1" :max="99" style="width: 100%" disabled />
        </el-form-item>
        <el-form-item label="每间住几人">
          <el-input-number v-model="buildingEditForm.capacityPerRoom" :min="1" :max="12" style="width: 100%" />
        </el-form-item>
        <el-form-item label="适用性别">
          <el-select v-model="buildingEditForm.gender" clearable placeholder="请选择" style="width: 100%">
            <el-option label="男" value="MALE" />
            <el-option label="女" value="FEMALE" />
          </el-select>
        </el-form-item>
        <el-alert
          title="提示：楼层数和每层房间数是初始化结构，当前版本不支持在此直接修改。"
          type="info"
          :closable="false"
          show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="buildingEditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="buildingEditSaving" @click="handleUpdateBuilding">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { dormitoryApi } from '@/api'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'

const userStore = useUserStore()

const userLoading = ref(false)
const dormitoryLoading = ref(false)
const memberLoading = ref(false)
const assignLoading = ref(false)
const buildingLoading = ref(false)
const buildingSaving = ref(false)
const buildingEditSaving = ref(false)
const managerSaving = ref(false)

const userList = ref([])
const dormitoryList = ref([])
const memberList = ref([])
const buildingList = ref([])
const dormManagerOptions = ref([])
const managedBuilding = ref(null)
const buildingDialogVisible = ref(false)
const buildingEditDialogVisible = ref(false)
const assignManagerDialogVisible = ref(false)
const currentBuilding = ref(null)
const selectedManagerId = ref(null)
const editingBuildingId = ref(null)

const userQuery = reactive({
  keyword: '',
  assigned: null
})

const dormitoryQuery = reactive({
  building: '',
  floor: '',
  roomNo: ''
})

const userPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const dormitoryPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const assignForm = reactive({
  userId: null,
  dormitoryId: null,
  campus: '东校区',
  bed: '',
  checkInDate: ''
})

const buildingForm = reactive({
  name: '',
  campus: '',
  floors: 6,
  roomsPerFloor: 20,
  capacityPerRoom: 4,
  gender: '',
  managerId: null
})

const buildingEditForm = reactive({
  name: '',
  campus: '',
  floors: 1,
  roomsPerFloor: 1,
  capacityPerRoom: 4,
  gender: ''
})

const selectedUser = computed(() => userList.value.find(item => item.userId === assignForm.userId) || null)
const selectedDormitory = computed(() => dormitoryList.value.find(item => item.id === assignForm.dormitoryId) || null)

const selectedUserText = computed(() => {
  if (!selectedUser.value) {
    return '未选择用户'
  }
  return `${selectedUser.value.realName || selectedUser.value.username} (${selectedUser.value.username})`
})

const selectedDormitoryText = computed(() => {
  if (!selectedDormitory.value) {
    return '未选择宿舍'
  }
  return `${selectedDormitory.value.building} ${selectedDormitory.value.roomNo}`
})

const selectedCampusText = computed(() => {
  if (selectedDormitory.value) {
    const matchedBuilding = buildingList.value.find(item => item.name === selectedDormitory.value.building || item.code === selectedDormitory.value.building)
    if (matchedBuilding?.campus) {
      return matchedBuilding.campus
    }
  }

  if (userStore.isDormManager) {
    return managedBuilding.value?.campus || '未绑定校区'
  }

  return '请先选择宿舍'
})

const managerHint = computed(() => {
  if (userStore.isDormManager) {
    const buildingText = managedBuilding.value?.name || managedBuilding.value?.code || '未绑定楼栋'
    return `当前账号为宿管员，仅管理 ${buildingText}，并且可以给自己分配宿舍。`
  }
  return '管理员和宿管员都可在此分配宿舍、退宿，以及维护宿舍列表。'
})

const canAssign = computed(() => {
  return !!assignForm.userId && !!assignForm.dormitoryId && !!assignForm.bed
})

const getRoleText = (role) => {
  const map = {
    STUDENT: '学生',
    TEACHER: '教师',
    ADMIN: '管理员',
    DORM_MANAGER: '宿管员'
  }
  return map[role] || role
}

const getGenderText = (gender) => {
  const map = {
    MALE: '男',
    FEMALE: '女'
  }
  return map[gender] || '不限'
}

const matchesManagedBuilding = (dormitory) => {
  if (!userStore.isDormManager || !managedBuilding.value) {
    return true
  }
  const building = dormitory?.building || ''
  return building === managedBuilding.value.code || building === managedBuilding.value.name
}

const loadManagedBuilding = async () => {
  if (!userStore.isDormManager) {
    return
  }
  try {
    const res = await dormitoryApi.getMyBuilding()
    managedBuilding.value = res.data || null
    dormitoryQuery.building = managedBuilding.value?.code || managedBuilding.value?.name || ''
  } catch (error) {
    console.error('获取管理楼栋失败:', error)
  }
}

const loadBuildings = async () => {
  if (!userStore.isAdmin) {
    return
  }
  buildingLoading.value = true
  try {
    const res = await dormitoryApi.getBuildings()
    buildingList.value = res.data || []
  } catch (error) {
    console.error('获取楼栋列表失败:', error)
    ElMessage.error('获取楼栋列表失败')
  } finally {
    buildingLoading.value = false
  }
}

const loadDormManagers = async () => {
  if (!userStore.isAdmin) {
    return
  }
  try {
    const res = await request.get('/admin/user/list', {
      params: { page: 1, size: 200, role: 'DORM_MANAGER' }
    })
    dormManagerOptions.value = res.data?.records || []
  } catch (error) {
    console.error('获取宿管列表失败:', error)
    ElMessage.error('获取宿管列表失败')
  }
}

const loadUsers = async () => {
  userLoading.value = true
  try {
    const res = await dormitoryApi.getAssignableUsers({
      page: userPagination.page,
      size: userPagination.size,
      keyword: userQuery.keyword || undefined,
      assigned: userQuery.assigned
    })
    userList.value = res.data?.records || []
    userPagination.total = res.data?.total || 0
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
  } finally {
    userLoading.value = false
  }
}

const loadDormitories = async () => {
  dormitoryLoading.value = true
  try {
    const res = await dormitoryApi.getList({
      page: dormitoryPagination.page,
      size: dormitoryPagination.size,
      building: userStore.isDormManager ? undefined : (dormitoryQuery.building || undefined),
      floor: dormitoryQuery.floor ? Number(dormitoryQuery.floor) : undefined,
      roomNo: dormitoryQuery.roomNo || undefined
    })

    dormitoryList.value = res.data?.records || []
    dormitoryPagination.total = res.data?.total || 0
  } catch (error) {
    console.error('获取宿舍列表失败:', error)
    ElMessage.error('获取宿舍列表失败')
  } finally {
    dormitoryLoading.value = false
  }
}

const loadMembers = async (dormitoryId) => {
  if (!dormitoryId) {
    memberList.value = []
    return
  }
  memberLoading.value = true
  try {
    const res = await dormitoryApi.getMembers(dormitoryId)
    memberList.value = res.data || []
  } catch (error) {
    console.error('获取宿舍成员失败:', error)
    ElMessage.error('获取宿舍成员失败')
  } finally {
    memberLoading.value = false
  }
}

const handleUserCurrentChange = (row) => {
  assignForm.userId = row?.userId || null
}

const handleDormitoryCurrentChange = (row) => {
  assignForm.dormitoryId = row?.id || null
  loadMembers(assignForm.dormitoryId)
}

const handleUserSelectChange = (value) => {
  assignForm.userId = value || null
}

const handleDormitorySelectChange = (value) => {
  assignForm.dormitoryId = value || null
  loadMembers(assignForm.dormitoryId)
}

const handleUserSearch = () => {
  userPagination.page = 1
  loadUsers()
}

const handleDormitorySearch = () => {
  dormitoryPagination.page = 1
  loadDormitories()
}

const resetAssignForm = () => {
  assignForm.userId = null
  assignForm.dormitoryId = null
  assignForm.bed = ''
  assignForm.checkInDate = ''
  memberList.value = []
}

const reloadAll = async () => {
  await Promise.all([
    loadUsers(),
    loadDormitories(),
    loadBuildings(),
    loadDormManagers()
  ])
  if (assignForm.dormitoryId) {
    loadMembers(assignForm.dormitoryId)
  }
}

const openBuildingDialog = () => {
  buildingForm.name = ''
  buildingForm.campus = ''
  buildingForm.floors = 6
  buildingForm.roomsPerFloor = 20
  buildingForm.capacityPerRoom = 4
  buildingForm.gender = ''
  buildingForm.managerId = null
  buildingDialogVisible.value = true
}

const openAssignManagerDialog = (building) => {
  currentBuilding.value = building
  selectedManagerId.value = building?.managerId ?? null
  assignManagerDialogVisible.value = true
}

const openEditBuildingDialog = (building) => {
  if (!building) {
    return
  }
  editingBuildingId.value = building.id
  buildingEditForm.name = building.name || ''
  buildingEditForm.campus = building.campus || ''
  buildingEditForm.floors = building.floors || 1
  buildingEditForm.roomsPerFloor = building.roomsPerFloor || 1
  buildingEditForm.capacityPerRoom = building.capacityPerRoom || 4
  buildingEditForm.gender = building.gender || ''
  buildingEditDialogVisible.value = true
}

const handleCreateBuilding = async () => {
  if (!buildingForm.name || !buildingForm.campus) {
    ElMessage.warning('请先填写楼栋名称和校区')
    return
  }

  try {
    buildingSaving.value = true
    const res = await dormitoryApi.createBuilding({
      name: buildingForm.name,
      campus: buildingForm.campus,
      floors: buildingForm.floors,
      roomsPerFloor: buildingForm.roomsPerFloor,
      capacityPerRoom: buildingForm.capacityPerRoom,
      gender: buildingForm.gender || undefined,
      managerId: buildingForm.managerId
    })
    ElMessage.success(res.message || '楼栋创建成功')
    buildingDialogVisible.value = false
    await Promise.all([loadBuildings(), loadDormManagers()])
  } catch (error) {
    console.error('创建楼栋失败:', error)
    ElMessage.error(error.message || '创建楼栋失败')
  } finally {
    buildingSaving.value = false
  }
}

const handleUpdateBuilding = async () => {
  if (!editingBuildingId.value) {
    ElMessage.warning('未选择楼栋')
    return
  }
  if (!buildingEditForm.name || !buildingEditForm.campus) {
    ElMessage.warning('请先填写楼栋名称和校区')
    return
  }

  try {
    buildingEditSaving.value = true
    const res = await dormitoryApi.updateBuilding(editingBuildingId.value, {
      name: buildingEditForm.name,
      campus: buildingEditForm.campus,
      capacityPerRoom: buildingEditForm.capacityPerRoom,
      gender: buildingEditForm.gender || null
    })
    ElMessage.success(res.message || '楼栋信息更新成功')
    buildingEditDialogVisible.value = false
    await Promise.all([loadBuildings(), loadDormitories(), loadUsers(), loadDormManagers()])
  } catch (error) {
    console.error('更新楼栋失败:', error)
    ElMessage.error(error.message || '更新楼栋失败')
  } finally {
    buildingEditSaving.value = false
  }
}

const handleAssignManager = async () => {
  if (!currentBuilding.value) {
    return
  }

  try {
    managerSaving.value = true
    const res = await dormitoryApi.assignBuildingManager(currentBuilding.value.id, {
      managerId: selectedManagerId.value
    })
    ElMessage.success(res.message || '楼栋负责人已更新')
    assignManagerDialogVisible.value = false
    await Promise.all([loadBuildings(), loadDormManagers()])
  } catch (error) {
    console.error('更新楼栋负责人失败:', error)
    ElMessage.error(error.message || '更新楼栋负责人失败')
  } finally {
    managerSaving.value = false
  }
}

const clearManager = async (building) => {
  try {
    await ElMessageBox.confirm(`确认将 ${building.name || building.code} 设为未分配吗？`, '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await dormitoryApi.assignBuildingManager(building.id, { managerId: null })
    ElMessage.success('已设为未分配')
    await Promise.all([loadBuildings(), loadDormManagers()])
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消楼栋绑定失败:', error)
      ElMessage.error(error.message || '取消楼栋绑定失败')
    }
  }
}

const handleAssign = async () => {
  if (!canAssign.value) {
    ElMessage.warning('请完整选择用户、宿舍并填写床位')
    return
  }

  try {
    assignLoading.value = true
    const res = await dormitoryApi.assignDormitory({
      userId: assignForm.userId,
      dormitoryId: assignForm.dormitoryId,
      bed: assignForm.bed,
      checkInDate: assignForm.checkInDate || null
    })
    ElMessage.success(res.message || '宿舍分配成功')
    assignForm.bed = ''
    assignForm.checkInDate = ''
    await reloadAll()
  } catch (error) {
    console.error('宿舍分配失败:', error)
    ElMessage.error(error.message || '宿舍分配失败')
  } finally {
    assignLoading.value = false
  }
}

const handleCheckout = async (row) => {
  try {
    await ElMessageBox.confirm(`确认让 ${row.realName || row.username} 退宿吗？`, '提示', {
      confirmButtonText: '确认退宿',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await dormitoryApi.checkoutDormitory(row.userId)
    ElMessage.success(res.message || '退宿成功')
    await reloadAll()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('退宿失败:', error)
      ElMessage.error(error.message || '退宿失败')
    }
  }
}

onMounted(async () => {
  await loadManagedBuilding()
  await reloadAll()
})
</script>

<style scoped>
.dormitory-admin {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 6px;
}

.page-header p {
  margin: 0;
  color: #6b7280;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.page-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.toolbar-form {
  margin-bottom: 16px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.bottom-row {
  margin-top: 4px;
}

.subtle-text {
  color: #6b7280;
  font-size: 13px;
}

.selected-summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 18px;
  color: #6b7280;
  font-size: 13px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions .el-button {
    flex: 1;
  }
}
</style>
