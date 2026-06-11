/**
 * Badge 消息角标组件
 * 仿微信红色圆点角标，支持 Pulse 呼吸动画
 */
<template>
  <div class="ustc-badge" :class="{ 'has-count': showCount }">
    <!-- 插槽：角标包裹的内容 -->
    <slot />
    
    <!-- 角标 -->
    <transition name="badge-fade">
      <div 
        v-if="visible" 
        class="badge-dot" 
        :class="[size, { pulse: animate }]"
        :style="badgeStyle"
      >
        <span v-if="showCount && count > 0" class="badge-count">
          {{ formattedCount }}
        </span>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  /** 角标数量 */
  count?: number
  /** 最大显示数量，超过显示 max+ */
  max?: number
  /** 是否显示红点（无数字时） */
  showDot?: boolean
  /** 是否有动画 */
  animate?: boolean
  /** 角标颜色 */
  color?: string
  /** 角标尺寸 */
  size?: 'small' | 'default' | 'large'
}

const props = withDefaults(defineProps<Props>(), {
  count: 0,
  max: 99,
  showDot: true,
  animate: true,
  color: '#F56C6C',
  size: 'default'
})

// 计算是否可见
const visible = computed(() => {
  if (!props.showDot && props.count <= 0) return false
  return props.count > 0 || props.showDot
})

// 是否显示数字
const showCount = computed(() => {
  return props.count > 0 && !props.showDot
})

// 格式化显示数量
const formattedCount = computed(() => {
  if (props.count > props.max) {
    return `${props.max}+`
  }
  return props.count
})

// 角标样式
const badgeStyle = computed(() => {
  if (props.showDot || props.count <= 0) {
    return {
      backgroundColor: props.color,
      minWidth: props.size === 'small' ? '8px' : props.size === 'large' ? '14px' : '10px',
      height: props.size === 'small' ? '8px' : props.size === 'large' ? '14px' : '10px'
    }
  }
  return {
    backgroundColor: props.color
  }
})
</script>

<style scoped>
.ustc-badge {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.badge-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  white-space: nowrap;
  z-index: 10;
  box-shadow: 0 2px 6px rgba(245, 108, 108, 0.4);
}

/* 尺寸变体 */
.badge-dot.small {
  min-width: 8px;
  height: 8px;
  font-size: 9px;
  padding: 0 3px;
}

.badge-dot.default {
  min-width: 10px;
  height: 10px;
  font-size: 10px;
  padding: 0 4px;
}

.badge-dot.large {
  min-width: 14px;
  height: 14px;
  font-size: 11px;
  padding: 0 5px;
}

/* 呼吸动画 */
.badge-dot.pulse {
  animation: badgePulse 2s ease-in-out infinite;
}

@keyframes badgePulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 2px 6px rgba(245, 108, 108, 0.4);
  }
  50% {
    transform: scale(1.15);
    box-shadow: 0 3px 10px rgba(245, 108, 108, 0.6);
  }
}

/* 过渡动画 */
.badge-fade-enter-active {
  animation: badgeIn 0.3s ease-out;
}

.badge-fade-leave-active {
  animation: badgeOut 0.2s ease-in;
}

@keyframes badgeIn {
  from {
    opacity: 0;
    transform: scale(0.5);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes badgeOut {
  from {
    opacity: 1;
    transform: scale(1);
  }
  to {
    opacity: 0;
    transform: scale(0.5);
  }
}

/* 数量显示 */
.badge-count {
  line-height: 1;
  font-weight: 600;
}

/* 有数量时的圆角 */
.badge-dot.has-count {
  border-radius: 10px;
  min-width: auto;
  padding: 0 5px;
  height: 18px;
}

.badge-dot.small.has-count {
  height: 14px;
  padding: 0 4px;
}

.badge-dot.large.has-count {
  height: 22px;
  padding: 0 6px;
}
</style>
