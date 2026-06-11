<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.userId)

const loading = ref(false)
const list = ref([])
const page = ref(1)
const total = ref(0)

const roleFilter = ref('')
const keywordFilter = ref('')

const createDialogVisible = ref(false)
const createFormRef = ref()
const creating = ref(false)
const createForm = ref({ username: '', password: '', realName: '', phone: '', email: '', role: 'STUDENT' })

const createRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

const roleOptions = [
  { value: 'STUDENT', label: '学生' },
  { value: 'TEACHER', label: '教师' },
  { value: 'ADMIN', label: '管理员' },
  { value: 'DORM_MANAGER', label: '宿舍管理员' }
]

const getRoleText = (role) => {
  const map = {
    STUDENT: '学生',
    TEACHER: '教师',
    ADMIN: '管理员',
    DORM_MANAGER: '宿管'
  }
  return map[role] || role
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/user/list', {
      params: { page: page.value, size: 10, role: roleFilter.value, keyword: keywordFilter.value }
    })
    if (res.code === 200) {
      list.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('加载列表失败:', error)
    ElMessage.error(error?.message || '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const showCreateDialog = () => {
  createForm.value = { username: '', password: '', realName: '', phone: '', email: '', role: 'STUDENT' }
  createDialogVisible.value = true
}

const handleCreate = async () => {
  if (!createFormRef.value) return
  await createFormRef.value.validate(async (valid) => {
    if (valid) {
      creating.value = true
      try {
        const res = await request.post('/admin/user/create', createForm.value)
        if (res.code === 200) {
          ElMessage.success('创建成功')
          createDialogVisible.value = false
          loadList()
        }
      } catch (error) {
        console.error('创建失败:', error)
        ElMessage.error(error?.message || '创建用户失败')
      } finally {
        creating.value = false
      }
    }
  })
}

const handleDisable = async (row) => {
  try {
    const res = await request.put(`/admin/user/${row.userId}/disable`)
    if (res.code === 200) {
      ElMessage.success('已禁用')
      loadList()
    }
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error(error?.message || '禁用用户失败')
  }
}

const handleEnable = async (row) => {
  try {
    const res = await request.put(`/admin/user/${row.userId}/enable`)
    if (res.code === 200) {
      ElMessage.success('已启用')
      loadList()
    }
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error(error?.message || '启用用户失败')
  }
}

const handleResetPassword = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新密码（至少8位）', `重置 ${row.username} 的密码`, {
      confirmButtonText: '重置',
      cancelButtonText: '取消',
      inputType: 'password',
      inputPattern: /^.{8,64}$/,
      inputErrorMessage: '密码长度需为8-64位'
    })
    const res = await request.put(`/admin/user/${row.userId}/reset-password`, {
      password: value
    })
    if (res.code === 200) {
      ElMessage.success('密码重置成功')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
      ElMessage.error(error?.message || '重置密码失败')
    }
  }
}

const roleDialogVisible = ref(false)
const currentEditRoleUser = ref(null)
const selectedRole = ref('')

const handleEditRole = (row) => {
  currentEditRoleUser.value = row
  selectedRole.value = row.role
  roleDialogVisible.value = true
}

const confirmEditRole = async () => {
  try {
    const res = await request.put(`/admin/user/${currentEditRoleUser.value.userId}/roles`, {
      role: selectedRole.value
    })
    if (res.code === 200) {
      ElMessage.success('权限已更新')
      roleDialogVisible.value = false
      loadList()
    }
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error(error?.message || '更新角色失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要注销该用户吗？此操作不可恢复！', '警告', {
      confirmButtonText: '确定注销',
      cancelButtonText: '取消',
      type: 'error'
    })
    const res = await request.delete(`/admin/user/${row.userId}`)
    if (res.code === 200) {
      ElMessage.success('用户已注销')
      loadList()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

onMounted(() => {
  loadList()
})
</script>

<template>
  <div class="user-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>账号管理</span>
          <el-button type="primary" @click="showCreateDialog">创建用户</el-button>
        </div>
      </template>

      <div class="filter-bar">
        <el-select v-model="roleFilter" placeholder="选择角色" clearable @change="loadList" style="width: 120px">
          <el-option v-for="r in roleOptions" :key="r.value" :label="r.label" :value="r.value" />
        </el-select>
        <el-input v-model="keywordFilter" placeholder="搜索用户名/姓名" clearable @change="loadList" style="width: 200px" />
        <el-button type="primary" @click="loadList">搜索</el-button>
      </div>

      <el-table :data="list" v-loading="loading" style="width: 100%; margin-top: 20px">
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag>{{ getRoleText(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="负责楼栋" width="140">
          <template #default="{ row }">
            <span v-if="row.role === 'DORM_MANAGER'">{{ row.managedBuildingName || '未分配' }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
              {{ row.status === 'ACTIVE' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320">
          <template #default="{ row }">
            <el-button v-if="row.status === 'ACTIVE'" size="small" type="danger" @click="handleDisable(row)">禁用</el-button>
            <el-button v-else size="small" type="success" @click="handleEnable(row)">启用</el-button>
            <el-button size="small" @click="handleEditRole(row)">修改权限</el-button>
            <el-button size="small" @click="handleResetPassword(row)">重置密码</el-button>
            <el-button v-if="row.userId !== currentUserId" size="small" type="danger" @click="handleDelete(row)">注销</el-button>
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

    <el-dialog v-model="createDialogVisible" title="创建用户" width="500px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="createForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="createForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="createForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="createForm.role" style="width: 100%" placeholder="请选择角色">
            <el-option v-for="r in roleOptions" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" title="修改权限" width="400px">
      <el-form label-width="80px">
        <el-form-item label="当前用户">
          <span>{{ currentEditRoleUser?.username }}</span>
        </el-form-item>
        <el-form-item label="用户权限">
          <el-select v-model="selectedRole" placeholder="请选择权限">
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="管理员" value="ADMIN" />
            <el-option label="宿管" value="DORM_MANAGER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmEditRole">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-manage {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.filter-bar {
  display: flex;
  gap: 10px;
}
</style>
