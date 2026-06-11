<template>
  <div class="chat-window" ref="windowRef">
    <!-- 欢迎引导（无消息时显示） -->
    <div v-if="messages.length === 0" class="chat-welcome">
      <div class="welcome-icon">🤖</div>
      <h3 class="welcome-title">校园智能助手</h3>
      <p class="welcome-sub">我可以帮你处理：报修宿舍、搜索二手、失物招领、校园导航</p>
      <div class="quick-btns">
        <el-button
          v-for="tip in quickTips"
          :key="tip"
          size="small"
          round
          @click="$emit('send', tip)"
        >{{ tip }}</el-button>
      </div>
    </div>

    <!-- 消息列表 -->
    <div
      v-for="(msg, idx) in messages"
      :key="idx"
      class="msg-row"
      :class="msg.role === 'user' ? 'msg-right' : 'msg-left'"
    >
      <!-- Agent 头像 -->
      <el-avatar v-if="msg.role === 'agent'" class="msg-avatar agent-avatar" :size="36">🤖</el-avatar>

      <div class="msg-bubble-wrap">
        <div
          class="msg-bubble"
          :class="msg.role === 'user' ? 'bubble-user' : 'bubble-agent'"
        >
          {{ msg.content }}
        </div>

        <!-- 结果卡片 -->
        <template v-if="msg.cards && msg.cards.length > 0">
          <component
            v-for="(card, ci) in msg.cards"
            :key="ci"
            :is="resolveCardComponent(card.type)"
            :data="card.data"
            class="msg-card"
          />
        </template>
      </div>

      <!-- 用户头像（使用个人中心真实头像） -->
      <el-avatar
        v-if="msg.role === 'user'"
        class="msg-avatar user-avatar"
        :size="36"
        :src="userStore.userInfo?.avatar"
      >{{ userStore.userInfo?.username?.charAt(0)?.toUpperCase() || '我' }}</el-avatar>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="msg-row msg-left">
      <el-avatar class="msg-avatar agent-avatar" :size="36">🤖</el-avatar>
      <div class="msg-bubble bubble-agent typing">
        <span></span><span></span><span></span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import RepairOrderCard from './cards/RepairOrderCard.vue'
import ProductCard from './cards/ProductCard.vue'
import LostFoundCard from './cards/LostFoundCard.vue'
import RouteCard from './cards/RouteCard.vue'
import MessageCard from './cards/MessageCard.vue'
import ProactiveTipsCard from './cards/ProactiveTipsCard.vue'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  messages: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})
defineEmits(['send'])

const userStore = useUserStore()

const windowRef = ref(null)

const quickTips = [
  '宿舍空调坏了帮我报修',
  '找个50元以内的台灯',
  '我丢了一个黑色保温杯',
  '图书馆怎么走',
  '查看我的待办提醒',
  '查看我的消息通知'
]

function resolveCardComponent(type) {
  return {
    REPAIR_ORDER: RepairOrderCard,
    PRODUCT: ProductCard,
    LOST_FOUND: LostFoundCard,
    ROUTE: RouteCard,
    MESSAGE: MessageCard,
    PROACTIVE_TIPS: ProactiveTipsCard
  }[type] || 'div'
}

// 新消息后自动滚动到底部
watch(() => [props.messages.length, props.loading], () => {
  nextTick(() => {
    if (windowRef.value) {
      windowRef.value.scrollTop = windowRef.value.scrollHeight
    }
  })
})
</script>

<style scoped>
.chat-window {
  flex: 1;
  overflow-y: auto;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 欢迎区域 */
.chat-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: #606266;
}
.welcome-icon { font-size: 56px; margin-bottom: 12px; }
.welcome-title { margin: 0 0 8px; color: #303133; font-size: 20px; }
.welcome-sub { margin: 0 0 20px; font-size: 14px; color: #909399; }
.quick-btns { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }

/* 消息行 */
.msg-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.msg-right { flex-direction: row-reverse; }

/* 头像 */
.msg-avatar { flex-shrink: 0; }
.agent-avatar { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; font-size: 18px; }
.user-avatar { background: var(--el-color-primary); color: #fff; font-size: 12px; font-weight: 600; }

/* 气泡 */
.msg-bubble-wrap { display: flex; flex-direction: column; max-width: 70%; }
.msg-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}
.bubble-user {
  background: var(--el-color-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.bubble-agent {
  background: #f4f4f5;
  color: #303133;
  border-bottom-left-radius: 4px;
}

/* 打字动画 */
.typing {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 14px;
}
.typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #909399;
  animation: blink 1.2s infinite;
}
.typing span:nth-child(2) { animation-delay: .2s; }
.typing span:nth-child(3) { animation-delay: .4s; }
@keyframes blink {
  0%, 80%, 100% { opacity: 0.2; transform: scale(.8); }
  40% { opacity: 1; transform: scale(1); }
}

.msg-card { margin-top: 6px; }
</style>
