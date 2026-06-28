<template>
  <div class="profile-container">
    <div class="profile-layout">
      <div class="profile-card user-card">
        <div class="avatar-section">
          <div class="avatar-wrapper" @click="triggerAvatarUpload">
            <el-avatar :size="100" :src="userStore.userInfo?.avatar" class="user-avatar">
              {{ userStore.userInfo?.realName?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="avatar-overlay">
              <el-icon><Camera /></el-icon>
              <span>更换头像</span>
            </div>
          </div>
          <input
            ref="avatarInput"
            type="file"
            accept="image/*"
            style="display: none"
            @change="handleAvatarChange"
          />
        </div>
        <div class="user-info">
          <h2 class="user-name">{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</h2>
          <p class="user-role">{{ getRoleText(userStore.userInfo?.role) }}</p>
          <div class="user-meta">
            <span class="meta-item">
              <el-icon><School /></el-icon>
              中国科学技术大学
            </span>
          </div>
          <div class="user-status">
            <el-tag type="success" size="small">正常</el-tag>
          </div>
        </div>
      </div>

      <div class="profile-card main-card">
        <el-tabs v-model="activeTab" class="profile-tabs">
          <el-tab-pane label="基本资料" name="profile">
            <el-form
              ref="profileFormRef"
              :model="profileForm"
              :rules="profileRules"
              label-width="80px"
              class="profile-form"
            >
              <el-form-item label="用户名">
                <el-input :model-value="userStore.userInfo?.username" disabled />
              </el-form-item>
              <el-form-item label="真实姓名" prop="realName">
                <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
              </el-form-item>
              <el-form-item label="学号" prop="studentId">
                <el-input v-model="profileForm.studentId" placeholder="请输入学号" />
              </el-form-item>
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
              </el-form-item>
              <el-form-item label="个人简介" prop="bio">
                <el-input
                  v-model="profileForm.bio"
                  type="textarea"
                  :rows="4"
                  placeholder="介绍一下自己吧"
                  maxlength="200"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="profileLoading" @click="handleUpdateProfile">
                  保存修改
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="安全设置" name="security">
            <el-form
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-width="100px"
              class="password-form"
            >
              <el-form-item label="当前密码" prop="oldPassword">
                <el-input
                  v-model="passwordForm.oldPassword"
                  type="password"
                  placeholder="请输入当前密码"
                  show-password
                />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  placeholder="请输入新密码（6-20位）"
                  show-password
                />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入新密码"
                  show-password
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">
                  修改密码
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="宿舍信息" name="dormitory">
            <div class="dormitory-readonly">
              <el-alert
                title="宿舍信息已改为由宿管统一分配和退宿，个人中心不能再自行修改。"
                type="info"
                :closable="false"
                show-icon
              />

              <el-descriptions v-if="hasDormitoryInfo" :column="2" border class="dormitory-descriptions">
                <el-descriptions-item label="校区">{{ dormitoryForm.campus || '-' }}</el-descriptions-item>
                <el-descriptions-item label="楼栋号">{{ dormitoryForm.building || '-' }}</el-descriptions-item>
                <el-descriptions-item label="房间号">{{ dormitoryForm.room || '-' }}</el-descriptions-item>
                <el-descriptions-item label="床位号">{{ dormitoryForm.bed || '-' }}</el-descriptions-item>
                <el-descriptions-item label="入住日期" :span="2">{{ dormitoryForm.checkInDate || '-' }}</el-descriptions-item>
              </el-descriptions>

              <el-empty v-else description="当前暂无已分配宿舍，请联系宿管分配。" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores'
import { dormitoryApi, userApi } from '@/api'
import { ElMessage } from 'element-plus'
import { Camera, School } from '@element-plus/icons-vue'
import { uploadToOSS } from '@/utils/oss'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('profile')
const avatarInput = ref(null)
const profileFormRef = ref(null)
const passwordFormRef = ref(null)
const dormitoryFormRef = ref(null)
const profileLoading = ref(false)
const passwordLoading = ref(false)

const profileForm = reactive({
  realName: '',
  studentId: '',
  phone: '',
  email: '',
  bio: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const dormitoryForm = reactive({
  campus: '',
  building: '',
  room: '',
  bed: '',
  checkInDate: ''
})

const hasDormitoryInfo = computed(() => {
  return !!(dormitoryForm.building || dormitoryForm.room || dormitoryForm.bed)
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const profileRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d).+$/, message: '密码需包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const getRoleText = (role) => {
  const roleMap = {
    STUDENT: '学生',
    TEACHER: '教师',
    ADMIN: '管理员',
    DORM_MANAGER: '宿管员'
  }
  return roleMap[role] || '用户'
}

const initProfileForm = async () => {
  try {
    await userStore.fetchUserInfo()
    if (userStore.userInfo) {
      profileForm.realName = userStore.userInfo.realName || ''
      profileForm.studentId = userStore.userInfo.studentId || ''
      profileForm.phone = userStore.userInfo.phone || ''
      profileForm.email = userStore.userInfo.email || ''
      profileForm.bio = userStore.userInfo.bio || ''
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

const initDormitoryForm = async () => {
  try {
    const [dormitoryRes, profileDormitoryRes] = await Promise.all([
      dormitoryApi.getMyDormitory(),
      userApi.getDormitory().catch(() => null)
    ])
    const assignedDormitory = dormitoryRes.code === 200 ? dormitoryRes.data || null : null
    const profileDormitory = profileDormitoryRes?.data || null

    dormitoryForm.campus = profileDormitory?.campus || ''
    dormitoryForm.building = assignedDormitory?.building || profileDormitory?.building || ''
    dormitoryForm.room = assignedDormitory?.roomNo || profileDormitory?.room || ''
    dormitoryForm.bed = profileDormitory?.bed || ''
    dormitoryForm.checkInDate = profileDormitory?.checkInDate || ''
  } catch (error) {
    console.error('获取宿舍信息失败:', error)
    dormitoryForm.campus = ''
    dormitoryForm.building = ''
    dormitoryForm.room = ''
    dormitoryForm.bed = ''
    dormitoryForm.checkInDate = ''
  }
}

const triggerAvatarUpload = () => {
  if (avatarInput.value) {
    avatarInput.value.click()
  }
}

const handleAvatarChange = async (event) => {
  const file = event.target.files && event.target.files[0]
  if (!file) return

  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过2MB')
    return
  }

  try {
    ElMessage.info('正在上传头像...')
    const avatarUrl = await uploadToOSS(file)
    await userApi.updateProfile({ avatar: avatarUrl })
    await userStore.fetchUserInfo()
    ElMessage.success('头像更新成功')
  } catch (error) {
    console.error('头像上传失败:', error)
    ElMessage.error(error.message || '头像上传失败')
  }

  event.target.value = ''
}

const handleUpdateProfile = async () => {
  if (!profileFormRef.value) return

  try {
    const valid = await profileFormRef.value.validate()
    if (!valid) return

    profileLoading.value = true
    await userApi.updateProfile(profileForm)
    await userStore.fetchUserInfo()
    ElMessage.success('资料更新成功')
  } catch (error) {
    console.error('更新资料失败:', error)
    ElMessage.error(error.message || '更新失败')
  } finally {
    profileLoading.value = false
  }
}

const handleChangePassword = async () => {
  if (!passwordFormRef.value) return

  try {
    const valid = await passwordFormRef.value.validate()
    if (!valid) return

    passwordLoading.value = true
    await userApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.logout()
    router.push('/login')
  } catch (error) {
    console.error('修改密码失败:', error)
    ElMessage.error(error.message || '修改失败')
  } finally {
    passwordLoading.value = false
  }
}

watch(
  () => route.query.tab,
  (tab) => {
    if (tab === 'security') {
      activeTab.value = 'security'
    }
  },
  { immediate: true }
)

watch(
  () => route.hash,
  (hash) => {
    if (hash === '#dormitory') {
      activeTab.value = 'dormitory'
    }
  },
  { immediate: true }
)

watch(activeTab, (tab) => {
  if (tab === 'dormitory') {
    initDormitoryForm()
  }
})

onMounted(() => {
  initProfileForm()
  initDormitoryForm()
})
</script>

<style scoped>
.profile-container {
  padding: 24px;
  min-height: calc(100vh - 60px);
  background: var(--ustc-bg-light);
}

.profile-layout {
  display: flex;
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.profile-card {
  background: var(--ustc-card);
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.user-card {
  width: 280px;
  flex-shrink: 0;
  padding: 32px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.avatar-section {
  margin-bottom: 20px;
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
}

.user-avatar {
  border: 4px solid var(--ustc-primary);
  box-shadow: 0 4px 12px rgba(0, 65, 145, 0.2);
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 65, 145, 0.7);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s ease;
  border-radius: 50%;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.avatar-overlay .el-icon {
  font-size: 24px;
  margin-bottom: 4px;
}

.avatar-overlay span {
  font-size: 12px;
}

.user-info {
  width: 100%;
}

.user-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--ustc-text-primary);
  margin: 0 0 4px 0;
}

.user-role {
  font-size: 14px;
  color: var(--ustc-text-secondary);
  margin: 0 0 16px 0;
}

.user-meta {
  margin-bottom: 12px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--ustc-text-secondary);
}

.user-status {
  margin-top: 8px;
}

.main-card {
  flex: 1;
  min-width: 0;
  padding: 24px;
}

.profile-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
}

.profile-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 500;
}

.profile-tabs :deep(.el-tabs__item.is-active) {
  color: var(--ustc-primary);
}

.profile-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--ustc-primary);
}

.profile-form,
.password-form,
.dormitory-readonly {
  max-width: 500px;
}

.profile-form :deep(.el-input__wrapper),
.password-form :deep(.el-input__wrapper),
.dormitory-form :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.profile-form :deep(.el-textarea__inner) {
  border-radius: 8px;
}

.dormitory-descriptions {
  margin-top: 16px;
}

@media (max-width: 768px) {
  .profile-layout {
    flex-direction: column;
  }

  .user-card {
    width: 100%;
  }
}
</style>
