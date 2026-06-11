<template>
  <div class="message-broadcast">
    <el-card>
      <template #header>
        <span>发布系统公告</span>
      </template>
      <el-form :model="form" label-width="80px">
        <el-form-item label="公告标题">
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="公告内容">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">发布公告</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const form = ref({ title: '', content: '' })
const submitting = ref(false)

const handleSubmit = async () => {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入公告标题')
    return
  }
  if (!form.value.content.trim()) {
    ElMessage.warning('请输入公告内容')
    return
  }
  submitting.value = true
  try {
    const res = await request.post('/admin/message/broadcast', form.value)
    if (res.code === 200) {
      ElMessage.success('公告发布成功')
      form.value = { title: '', content: '' }
    } else {
      ElMessage.error(res.message || '发布失败')
    }
  } catch (error) {
    ElMessage.error('发布失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.message-broadcast {
  padding: 20px;
  max-width: 600px;
}
</style>
