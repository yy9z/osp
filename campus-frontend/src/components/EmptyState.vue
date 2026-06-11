<template>
  <div class="empty-state" :class="type">
    <!-- 开口纸箱图标 - 体积感较强的实心风格 -->
    <svg class="empty-icon" viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
      <!-- 纸箱底座 -->
      <path
        d="M10 28H32V60C32 63.3137 34.6863 66 38 66H66C69.3137 66 72 63.3137 72 60V28"
        fill="#E4E7ED"
      />
      <!-- 纸箱底座边框 -->
      <path
        d="M10 28H32V60C32 63.3137 34.6863 66 38 66H66C69.3137 66 72 63.3137 72 60V28"
        stroke="#E4E7ED"
        stroke-width="3"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
      <!-- 纸箱左侧翻盖 - 立体感 -->
      <path
        d="M10 28L36 12H68L72 28"
        fill="#E4E7ED"
      />
      <!-- 纸箱左侧翻盖边框 -->
      <path
        d="M10 28L36 12H68L72 28"
        stroke="#E4E7ED"
        stroke-width="3"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
      <!-- 纸箱中间折痕 -->
      <path
        d="M36 12V28"
        stroke="#E4E7ED"
        stroke-width="3"
        stroke-linecap="round"
      />
      <!-- 纸箱底部横线 -->
      <path
        d="M10 46H72"
        stroke="#E4E7ED"
        stroke-width="3"
        stroke-linecap="round"
      />
    </svg>
    <!-- 文字 -->
    <p class="empty-text">{{ displayText }}</p>
    <slot name="action" />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'default',
    validator: (value) => ['market', 'myStore', 'message', 'default'].includes(value)
  },
  title: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  }
})

// 根据类型生成默认文案
const displayText = computed(() => {
  if (props.title) return props.title
  const titles = {
    market: '暂无商品',
    myStore: '暂无商品',
    message: '暂无消息',
    default: '暂无数据'
  }
  return titles[props.type] || '暂无数据'
})
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  padding: 20px;
}

.empty-icon {
  width: 80px;
  height: 80px;
  margin-bottom: 8px;
}

.empty-text {
  font-size: 14px;
  color: #909399;
  margin: 0;
  text-align: center;
}

/* 响应式 */
@media (max-width: 768px) {
  .empty-icon {
    width: 64px;
    height: 64px;
  }

  .empty-text {
    font-size: 13px;
  }
}
</style>
