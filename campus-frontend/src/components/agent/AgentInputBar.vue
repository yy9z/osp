<template>
  <div class="agent-input-bar">
    <!-- 输入行 -->
    <div class="input-row">
      <el-input
        v-model="inputText"
        placeholder="输入您的需求，例如：宿舍空调坏了帮我报修..."
        class="input-field"
        :disabled="loading"
        @keydown.enter.exact.prevent="handleSend"
        clearable
      />
      <el-button
        type="primary"
        :loading="loading"
        :disabled="!inputText.trim()"
        @click="handleSend"
        class="send-btn"
      >
        <el-icon v-if="!loading"><Promotion /></el-icon>
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Promotion } from '@element-plus/icons-vue'

const props = defineProps({
  loading: { type: Boolean, default: false },
  prefill: { type: String, default: '' }
})
const emit = defineEmits(['send'])

const inputText = ref('')

// 当外部传入 prefill 时自动填充输入框
watch(() => props.prefill, (val) => {
  if (val) inputText.value = val
})

function handleSend() {
  const text = inputText.value.trim()
  if (!text || props.loading) return
  emit('send', text)
  inputText.value = ''
}
</script>

<style scoped>
.agent-input-bar {
  display: flex;
  flex-direction: column;
  border-top: 1px solid #e4e7ed;
  background: #fff;
}

/* 输入行 */
.input-row {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
}
.input-field { flex: 1; }
.send-btn {
  min-width: 80px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
}
.send-btn:hover {
  background: linear-gradient(135deg, #5a6fd8, #6a3f91);
}
</style>
