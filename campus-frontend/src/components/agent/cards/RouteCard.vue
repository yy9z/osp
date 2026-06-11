<template>
  <div class="route-card">
    <div class="route-header">
      <el-icon><MapLocation /></el-icon>
      <span>导航结果</span>
    </div>
    <div class="route-body">
      <div class="route-item" v-if="displayOrigin">
        <el-icon class="dot start-dot"><Location /></el-icon>
        <span>{{ displayOrigin }}</span>
      </div>
      <div class="route-arrow">↓</div>
      <div class="route-item">
        <el-icon class="dot end-dot"><MapLocation /></el-icon>
        <span class="dest">{{ displayDestination }}</span>
      </div>
      <div v-if="data.campus" class="route-campus">{{ data.campus }}</div>
      <div class="route-summary" v-if="routeStatsText">{{ routeStatsText }}</div>
      <div v-if="waypointNames.length" class="route-desc route-itinerary">{{ itineraryPreview }}</div>
      <div class="route-desc" v-if="data.agentSummary || data.description">{{ data.agentSummary || data.description }}</div>
      <div v-if="routeChoiceText" class="route-desc route-choice">{{ routeChoiceText }}</div>
      <div v-if="data.selectionReason" class="route-desc route-choice-reason">{{ data.selectionReason }}</div>

      <div v-if="segmentSummaries.length" class="route-analysis-list">
        <div v-for="segment in segmentSummaries" :key="segment.key" class="route-analysis-item">
          <el-tag :type="getScoreType(segment.score)" size="small" effect="plain">{{ segment.score }}/10</el-tag>
          <span>{{ segment.text }}</span>
        </div>
      </div>

      <div v-if="overallSummary" class="route-desc route-overall">{{ overallSummary }}</div>

      <!-- 场景标签 -->
      <div v-if="data.taskType" class="route-scene">
        <el-tag size="small" type="info" effect="plain">{{ getSceneLabel(data.taskType) }}</el-tag>
      </div>

      <!-- 偏好标签 -->
      <div v-if="data.preferences?.length" class="route-preferences">
        <el-tag v-for="pref in data.preferences" :key="pref" size="small" effect="light">{{ getPreferenceLabel(pref) }}</el-tag>
      </div>

      <div v-if="data.clarificationRequired && data.clarificationOptions?.length" class="route-options">
        <el-tag v-for="option in data.clarificationOptions" :key="option" size="small" effect="plain">{{ option }}</el-tag>
      </div>
    </div>
    <div class="route-footer">
      <el-button type="primary" size="small" @click="goNav">在地图中查看</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { MapLocation, Location } from '@element-plus/icons-vue'
import { useNavigationStore } from '@/stores/navigation'
import { formatDistance, formatDuration } from '@/utils/map'

const router = useRouter()
const navStore = useNavigationStore()
const props = defineProps({
  data: { type: Object, required: true }
})

const displayOrigin = computed(() => props.data.origin || props.data.originText)
const displayDestination = computed(() => props.data.destination || props.data.destinationText || '未识别目的地')
const waypointNames = computed(() => Array.isArray(props.data.waypoints)
  ? props.data.waypoints.map((waypoint) => waypoint?.name || waypoint).filter(Boolean)
  : [])
const routeStatsText = computed(() => {
  const parts = []
  if (props.data.duration != null) {
    const duration = Number(props.data.duration)
    parts.push(Number.isFinite(duration) ? formatDuration(duration) : String(props.data.duration))
  }
  if (props.data.distance != null) {
    const distance = Number(props.data.distance)
    parts.push(Number.isFinite(distance) ? formatDistance(distance) : String(props.data.distance))
  }
  return parts.join(' · ')
})
const itineraryPreview = computed(() => {
  const points = [displayOrigin.value || '当前位置', ...waypointNames.value, displayDestination.value]
  return points.filter(Boolean).join(' → ')
})
const routeChoiceText = computed(() => {
  if (!props.data.totalRoutes || props.data.totalRoutes <= 1) return ''
  return `共 ${props.data.totalRoutes} 条路线，已选第 ${(Number(props.data.selectedRouteIndex || 0) + 1)} 条`
})
const segmentSummaries = computed(() => Array.isArray(props.data.segments)
  ? props.data.segments
      .map((segment, index) => {
        const reason = segment?.analysis?.reason
        if (!reason) return null
        const fromName = segment?.from?.name || (index === 0 ? displayOrigin.value || '当前位置' : `第${index}段`)
        const toName = segment?.to?.name || segment?.name || `第${index + 1}段`
        return {
          key: `${fromName}-${toName}-${index}`,
          score: Number(segment?.analysis?.score || 7),
          text: `${fromName} → ${toName}：${reason}`
        }
      })
      .filter(Boolean)
  : [])
const overallSummary = computed(() => props.data.overallAnalysis?.summary || '')

function goNav() {
  navStore.setPendingTask({
    source: 'agent',
    ...props.data
  })
  router.push('/navigation')
}

const SCENE_LABELS = {
  class_commute: '上课赶路',
  dining: '就餐',
  express: '取快递',
  study: '学习',
  medical: '就医',
  return_dorm: '回宿舍',
  campus_tour: '校园导览',
  daily_convenience: '日常出行'
}

const PREFERENCE_LABELS = {
  fastest: '最快路线',
  shortest: '最短路线',
  safeNight: '夜间安全',
  avoidCrowd: '避开人群',
  luggageFriendly: '适合搬行李',
  viaCanteen: '途经食堂',
  viaPrintShop: '途经打印店'
}

function getScoreType(score) {
  if (score >= 7) return 'success'
  if (score >= 5) return 'warning'
  return 'danger'
}

function getSceneLabel(scene) {
  return SCENE_LABELS[scene] || scene
}

function getPreferenceLabel(pref) {
  return PREFERENCE_LABELS[pref] || pref
}
</script>

<style scoped>
.route-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  overflow: hidden;
  margin-top: 8px;
}
.route-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #d9ecff 0%, #ecf5ff 100%);
  color: #303133;
  font-weight: 600;
  font-size: 14px;
}
.route-body { padding: 12px 14px; }
.route-item { display: flex; align-items: center; gap: 8px; font-size: 14px; color: #303133; }
.route-arrow { text-align: center; color: #c0c4cc; margin: 4px 20px; font-size: 16px; }
.dest { font-weight: 600; color: #409eff; }
.route-campus { margin-top: 6px; color: #1d61c2; font-size: 12px; font-weight: 500; }
.route-summary { margin-top: 6px; font-size: 13px; color: #606266; font-weight: 500; }
.route-desc { margin-top: 4px; font-size: 13px; color: #909399; line-height: 1.5; }
.route-itinerary { color: #606266; font-weight: 500; }
.route-choice { color: #1d61c2; font-weight: 500; }
.route-choice-reason { color: #606266; }
.route-scene { margin-top: 6px; }
.route-preferences { margin-top: 6px; display: flex; gap: 4px; flex-wrap: wrap; }
.route-options { margin-top: 8px; display: flex; gap: 6px; flex-wrap: wrap; }
.route-analysis-list { margin-top: 8px; display: flex; flex-direction: column; gap: 6px; }
.route-analysis-item { display: flex; align-items: flex-start; gap: 8px; font-size: 12px; color: #606266; line-height: 1.5; }
.route-overall { color: #303133; font-weight: 500; }
.start-dot { color: #67c23a; }
.end-dot { color: #409eff; }
.route-footer { padding: 8px 14px 12px; }
</style>
