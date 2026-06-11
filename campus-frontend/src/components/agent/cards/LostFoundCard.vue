<template>
  <div class="lostfound-card">
    <div class="lf-header">
      <el-icon><Search /></el-icon>
      <span>{{ data.type === 'LOST' ? '失物登记' : '拾物登记' }}</span>
      <el-tag size="small" :type="data.type === 'LOST' ? 'danger' : 'success'">
        {{ data.type === 'LOST' ? '寻物' : '招领' }}
      </el-tag>
    </div>
    <div class="lf-body">
      <div class="lf-item">
        <span class="label">物品</span>
        <span class="value">{{ data.title }}</span>
      </div>
      <div class="lf-item" v-if="data.recordId">
        <span class="label">记录ID</span>
        <span class="value">#{{ data.recordId }}</span>
      </div>
    </div>
    <div class="lf-candidates" v-if="data.candidates && data.candidates.length > 0">
      <div class="candidates-title">
        <el-icon><Warning /></el-icon>
        找到 {{ data.candidates.length }} 条可能匹配记录：
      </div>
      <div
        v-for="c in data.candidates"
        :key="c.id"
        class="candidate-row"
        @click="goLostFound"
      >
        <span class="c-title">{{ c.title }}</span>
        <span class="c-loc" v-if="c.location">{{ c.location }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { Search, Warning } from '@element-plus/icons-vue'

const router = useRouter()
const props = defineProps({
  data: { type: Object, required: true }
})
function goLostFound() { router.push('/lostfound') }
</script>

<style scoped>
.lostfound-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  overflow: hidden;
  margin-top: 8px;
}
.lf-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #f6d365 0%, #fda085 100%);
  color: #fff;
  font-weight: 600;
  font-size: 14px;
}
.lf-header .el-tag { margin-left: auto; }
.lf-body { padding: 10px 14px; }
.lf-item { display: flex; justify-content: space-between; padding: 3px 0; font-size: 13px; }
.lf-item .label { color: #909399; }
.lf-item .value { color: #303133; font-weight: 500; }
.lf-candidates { padding: 10px 14px; border-top: 1px solid #f0f0f0; }
.candidates-title { display: flex; align-items: center; gap: 4px; font-size: 13px; color: #e6a23c; margin-bottom: 6px; }
.candidate-row {
  display: flex;
  justify-content: space-between;
  padding: 5px 8px;
  background: #fafafa;
  border-radius: 6px;
  margin-bottom: 4px;
  cursor: pointer;
  font-size: 13px;
}
.candidate-row:hover { background: #f5f7fa; }
.c-title { color: #303133; }
.c-loc { color: #909399; font-size: 12px; }
</style>
