<template>
  <div class="nav-page">
    <div ref="topBarRef" class="top-bar" :class="{ 'with-panel': showRightPanel }">
      <div class="input-row">
        <el-input
          v-model="naturalInput"
          class="nl-input"
          placeholder="输入地点或需求，例如：图书馆、东区学生食堂、从我的位置去西校区图书馆"
          clearable
          @keydown.enter="handleNaturalSubmit"
        >
          <template #prefix>
            <el-icon style="color:#409EFF"><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" :loading="submitting" :disabled="!naturalInput.trim()" @click="handleNaturalSubmit">规划</el-button>
        <el-button v-if="navState !== 'idle'" @click="resetToIdle"><el-icon><RefreshLeft /></el-icon></el-button>
      </div>

      <div v-if="showFollowUpStatus" class="follow-up-status">
        <el-tag size="small" type="warning" effect="plain" class="follow-up-status-tag">本次导航追问</el-tag>
        <el-icon class="follow-up-status-icon"><InfoFilled /></el-icon>
        <span class="follow-up-status-text">{{ followUpStatusText }}</span>
      </div>

      <!-- 场景和偏好标签显示 -->
      <div v-if="currentTaskType || activePreferences.length" class="task-info-row">
        <el-tag v-if="currentTaskType" size="small" type="info" effect="light">{{ getSceneLabel(currentTaskType) }}</el-tag>
        <el-tag v-for="pref in activePreferences" :key="pref" size="small" type="success" effect="light">{{ getPreferenceLabel(pref) }}</el-tag>
      </div>

      <div class="mode-row" v-if="resolvedDestination || navState === 'ready'">
        <el-radio-group v-model="travelMode" size="small" @change="handleModeChange">
          <el-radio-button value="walking">步行</el-radio-button>
          <el-radio-button value="cycling">骑行</el-radio-button>
        </el-radio-group>
        <el-tag v-if="activeCampus" type="primary" size="small" effect="light">{{ activeCampus }}</el-tag>
        <el-tag v-if="isFromAgent" size="small" effect="light">来自智能助手</el-tag>
      </div>
    </div>

    <div class="map-area">
      <CampusMap
        ref="mapRef"
        height="100%"
        :show-user-location="true"
        :hide-search-box="true"
        :select-point-mode="isSelectingPoint"
        :select-point-type="selectingType"
        @route-calculated="handleRouteCalculated"
        @location-changed="handleLocationChanged"
        @map-loaded="handleMapLoaded"
        @point-selected="handlePointSelected"
      />

      <div v-if="isSelectingPoint" class="select-tip-bar">
        <el-icon><Aim /></el-icon>
        {{ selectingType === 'start' ? '在地图上点击选择起点' : '在地图上点击选择终点' }}
        <el-button link @click="cancelSelect" style="color:#fff">取消</el-button>
      </div>

      <div v-if="navState === 'loading' || isAgentThinking" class="loading-overlay">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        <p>{{ isAgentThinking ? '智能助手正在继续解析导航任务…' : '正在规划路线…' }}</p>
      </div>
    </div>

    <transition name="slide-right">
      <div v-if="showRightPanel" ref="routePanelRef" class="route-panel" :style="routePanelStyle">
        <div class="route-summary">
          <div class="summary-title">
            Agent 导航任务
            <el-tag v-if="currentTaskType" size="small" type="info" effect="dark" style="margin-left: 8px">
              {{ getSceneLabel(currentTaskType) }}
            </el-tag>
          </div>
          <div v-if="agentSummary" class="agent-summary-text">{{ agentSummary }}</div>
          <div v-if="reasoning" class="reasoning-text">{{ reasoning }}</div>
          <div v-if="activePreferences.length" class="preferences-row">
            <el-tag v-for="pref in activePreferences" :key="pref" size="small" effect="dark" style="margin-right: 4px">
              {{ getPreferenceLabel(pref) }}
            </el-tag>
          </div>
          <div class="od-row" v-if="resolvedDestination">
            <div class="od-point">
              <span class="dot dot-start"></span>
              <span class="od-name">{{ startPointName || '当前位置' }}</span>
            </div>
            <div class="od-line"></div>
            <div class="od-point">
              <span class="dot dot-end"></span>
              <span class="od-name">{{ endPointName }}</span>
            </div>
          </div>
          <div v-if="routeType === 'multi_waypoint' && routeChainText" class="route-chain-text">
            {{ routeChainText }}
          </div>
          <el-alert
            v-if="routeRenderNotice"
            class="route-render-alert"
            :title="routeRenderNotice.title"
            :description="routeRenderNotice.message"
            :type="routeRenderNotice.type"
            :closable="false"
            show-icon
          />
          <div v-if="currentRoute" class="stats-row">
            <span class="stat-item"><el-icon><Timer /></el-icon>{{ formatDuration(currentRoute.duration) }}</span>
            <span class="stat-sep">·</span>
            <span class="stat-item"><el-icon><MapLocation /></el-icon>{{ formatDistance(currentRoute.distance) }}</span>
          </div>

          <div v-if="currentSelectedAlternative" class="selected-route-brief">
            <div class="selected-route-brief-header">
              <span class="selected-route-brief-title">当前方案：路线{{ currentSelectedAlternative.index + 1 }}</span>
              <el-tag
                v-if="currentSelectedAlternative.score != null"
                :type="getScoreType(currentSelectedAlternative.score)"
                size="small"
                effect="light"
              >
                {{ currentSelectedAlternative.score }}/10
              </el-tag>
            </div>
            <div v-if="currentSelectedAlternative.summary" class="selected-route-brief-text">
              {{ currentSelectedAlternative.summary }}
            </div>
            <div v-if="currentSelectedAlternative.reason" class="selected-route-brief-note">
              {{ currentSelectedAlternative.reason }}
            </div>
          </div>

          <!-- 安全分析展示 -->
          <div v-if="safetyAnalysis" class="safety-section">
            <div class="safety-header">
              <span class="safety-title">路线安全分析</span>
              <el-tag :type="safetyAnalysis.safetyScore >= 7 ? 'success' : safetyAnalysis.safetyScore >= 5 ? 'warning' : 'danger'" size="small" effect="dark">
                安全评分 {{ safetyAnalysis.safetyScore }}/10
              </el-tag>
            </div>
            <div v-if="safetyAnalysis.warnings?.length" class="safety-warnings">
              <div v-for="(warning, idx) in safetyAnalysis.warnings" :key="idx" class="safety-warning-item">
                <el-icon class="warning-icon"><Warning /></el-icon>
                <span>{{ warning }}</span>
              </div>
            </div>
            <div v-if="safetyAnalysis.suggestions?.length" class="safety-suggestions">
              <div v-for="(suggestion, idx) in safetyAnalysis.suggestions" :key="idx" class="safety-suggestion-item">
                <el-icon class="suggestion-icon"><InfoFilled /></el-icon>
                <span>{{ suggestion }}</span>
              </div>
            </div>
            <div v-if="safetyAnalysis.hasAlternative && safetyAnalysis.alternativeSummary" class="safety-alternative">
              <el-icon><Guide /></el-icon>
              <span>{{ safetyAnalysis.alternativeSummary }}</span>
            </div>
          </div>

          <!-- 多路线选择展示 -->
          <div v-if="totalRoutes > 1" class="route-selection-section">
            <div class="route-selection-header">
              <span class="route-selection-title">选择路线方案</span>
              <el-tag type="primary" size="small" effect="plain">共{{ totalRoutes }}条可选</el-tag>
            </div>
            <div v-if="selectionReason" class="route-selection-reason">
              <el-icon><InfoFilled /></el-icon>
              <span>{{ selectionReason }}</span>
            </div>
            <div class="alternative-routes-list">
              <div v-for="route in alternativeRoutes" :key="route.index"
                   class="alternative-route-item"
                   :class="{ 'selected-route': route.selected }"
                   role="button"
                   tabindex="0"
                   @click="selectAlternativeRoute(route.index)"
                   @keydown.enter.prevent="selectAlternativeRoute(route.index)"
                   @keydown.space.prevent="selectAlternativeRoute(route.index)">
                <div class="route-label">路线{{ route.index + 1 }}</div>
                <div class="route-stats">
                  <span>{{ formatDistance(route.distance) }}</span>
                  <span class="route-stat-sep">·</span>
                  <span>{{ formatDuration(route.duration) }}</span>
                </div>
                <div v-if="route.score != null" class="route-score">
                  <el-tag :type="getScoreType(route.score)" size="small" effect="plain">{{ route.score }}/10</el-tag>
                </div>
                <div v-if="route.summary" class="route-summary-text">{{ route.summary }}</div>
                <div v-if="route.reason" class="route-reason-text">{{ route.reason }}</div>
                <el-tag v-if="route.selected" type="success" size="small">当前已选</el-tag>
                <el-tag v-else type="info" size="small" effect="plain">点击选择</el-tag>
              </div>
            </div>
          </div>
        </div>

        <div class="route-panel-body">
          <div v-if="clarificationRequired" class="clarify-section">
            <div class="section-title">需要确认</div>
            <div class="clarify-text">{{ clarificationMessage }}</div>
            <div class="clarify-options">
              <el-button v-for="option in clarificationOptions" :key="option" size="small" @click="selectClarification(option)">
                {{ option }}
              </el-button>
            </div>
          </div>

          <div v-else-if="candidatePanelExpanded && destinationCandidates.length > 1" class="candidate-section">
            <div class="section-title">
              <span>候选地点</span>
              <el-button link type="primary" @click="candidatePanelExpanded = false">收起</el-button>
            </div>
            <div class="candidate-list">
              <div
                v-for="place in destinationCandidates"
                :key="`${place.name}-${place.lat}-${place.lng}`"
                class="candidate-item"
                :class="{ 'candidate-item-selected': isCurrentCandidate(place) }"
                @click="selectCandidate(place)"
              >
                <div class="candidate-name">{{ place.name }}</div>
                <div class="candidate-meta">
                  <span v-if="place.campus">{{ place.campus }}</span>
                  <span v-if="place.address">{{ place.address }}</span>
                  <span v-if="place.distanceToUser != null">距您约 {{ formatDistance(place.distanceToUser) }}</span>
                </div>
                <el-tag v-if="isCurrentCandidate(place)" size="small" type="success" effect="plain">当前已选</el-tag>
              </div>
            </div>
          </div>

          <div v-else-if="resolvedDestination" class="actions-section">
            <div class="section-title">快捷操作</div>
            <div class="action-buttons">
              <el-button size="small" @click="useCurrentLocationAsStart({ preferAgentReplan: true })">使用当前位置</el-button>
              <el-button size="small" @click="startSelectStart">地图选起点</el-button>
              <el-button v-if="destinationCandidates.length > 1" size="small" @click="candidatePanelExpanded = true">切换候选地点</el-button>
            </div>
          </div>

          <div v-if="routeType === 'multi_waypoint' && itineraryPoints.length" class="itinerary-section">
            <div class="section-title">行程链路</div>
            <div class="itinerary-list">
              <div v-for="(point, index) in itineraryPoints" :key="`${point.label}-${point.name}-${index}`" class="itinerary-item">
                <div class="itinerary-header">
                  <div class="itinerary-label">{{ point.label }}</div>
                  <div class="itinerary-name">{{ point.name }}</div>
                </div>
                <div v-if="index > 0 && point.distance != null && point.duration != null" class="itinerary-meta">
                  本段 {{ formatDistance(point.distance) }} · {{ formatDuration(point.duration) }}
                </div>
                <div v-if="point.slotProfile" class="segment-slot-profile">
                  <div v-if="point.slotProfile.segmentGoal" class="slot-goal">{{ point.slotProfile.segmentGoal }}</div>
                  <div class="slot-tags">
                    <el-tag v-if="point.slotProfile.taskScene" size="small" type="info" effect="plain">
                      {{ getSceneLabel(point.slotProfile.taskScene) }}
                    </el-tag>
                    <el-tag v-if="point.slotProfile.timeSlot" size="small" type="warning" effect="plain">
                      {{ getTimeSlotLabel(point.slotProfile.timeSlot) }}
                    </el-tag>
                    <el-tag v-if="point.slotProfile.urgencyMinutes != null" size="small" type="danger" effect="plain">
                      {{ point.slotProfile.urgencyMinutes }} 分钟内
                    </el-tag>
                    <el-tag v-for="pref in point.slotProfile.preferences" :key="`${point.name}-${pref}`" size="small" type="success" effect="plain">
                      {{ getPreferenceLabel(pref) }}
                    </el-tag>
                    <el-tag v-for="constraint in point.slotProfile.constraints" :key="`${point.name}-${constraint}`" size="small" type="primary" effect="plain">
                      {{ getConstraintLabel(constraint) }}
                    </el-tag>
                  </div>
                  <div v-if="point.slotProfile.notes" class="slot-notes">{{ point.slotProfile.notes }}</div>
                </div>
                <!-- 分段评估显示 -->
                <div v-if="point.analysis" class="segment-analysis">
                  <el-tag :type="getScoreType(point.analysis.score)" size="small" effect="plain">
                    {{ point.analysis.score }}/10
                  </el-tag>
                  <span class="analysis-reason">{{ point.analysis.reason }}</span>
                </div>
              </div>
            </div>
          </div>

          <div v-if="overallAnalysis" class="overall-analysis-section">
            <div class="overall-header">
              <span class="overall-title">{{ routeType === 'multi_waypoint' ? '全路径综合评估' : '全路径评估' }}</span>
              <el-tag :type="getScoreType(overallAnalysis.overallScore)" size="small" effect="dark">
                {{ overallAnalysis.overallScore }}/10
              </el-tag>
            </div>
            <div class="overall-summary">{{ overallAnalysis.summary }}</div>
            <div v-if="overallAnalysis.keyPoints?.length" class="overall-keypoints">
              <div v-for="(point, idx) in overallAnalysis.keyPoints" :key="idx" class="keypoint-item">
                <el-icon class="keypoint-icon"><InfoFilled /></el-icon>
                <span>{{ point }}</span>
              </div>
            </div>
            <div v-if="overallAnalysis.recommendation" class="overall-recommendation">
              <el-icon><Guide /></el-icon>
              <span>{{ overallAnalysis.recommendation }}</span>
            </div>
          </div>

          <div class="steps-section" v-if="currentRoute?.steps?.length">
            <div class="steps-header" @click="stepsExpanded = !stepsExpanded">
              <span>路线步骤</span>
              <el-icon :style="{ transform: stepsExpanded ? 'rotate(180deg)' : '' }"><ArrowDown /></el-icon>
            </div>
            <div v-if="stepsExpanded" class="steps-list">
              <div v-for="(step, i) in currentRoute.steps" :key="i" class="step-item">
                <div class="step-num">{{ i + 1 }}</div>
                <div class="step-content">
                  <div class="step-instr">{{ step.instruction }}</div>
                  <div class="step-meta">{{ formatDistance(step.distance) }} · {{ formatDuration(step.duration) }}</div>
                </div>
              </div>
            </div>
          </div>

          <div class="suggestions-section" v-if="followUpSuggestions.length">
            <p class="suggestions-label">还可以继续：</p>
            <div class="suggestions-list">
              <el-tag v-for="s in followUpSuggestions" :key="s" class="suggestion-tag" effect="plain" round @click="applySuggestion(s)">
                {{ s }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { Search, Timer, MapLocation, ArrowDown, Loading, Aim, RefreshLeft, Warning, InfoFilled, Guide } from '@element-plus/icons-vue'
import CampusMap from '@/components/CampusMap.vue'
import { navigationApi } from '@/api/navigation'
import { chatWithAgent, clearAgentSession } from '@/api/agent'
import { useNavigationStore } from '@/stores/navigation'
import type {
  AgentNavigationPayload,
  AlternativeRoute,
  NavigationResolveResponse,
  OverallAnalysis,
  ResolvedPlace,
  RouteResponse,
  RouteSegment,
  SafetyAnalysis,
  SegmentSlotProfile,
  UserLocation
} from '@/types/navigation'
import { formatDistance, formatDuration } from '@/utils/map'

const mapRef = ref<InstanceType<typeof CampusMap> | null>(null)
const topBarRef = ref<HTMLElement | null>(null)
const routePanelRef = ref<HTMLElement | null>(null)
const navStore = useNavigationStore()
const route = useRoute()
const { latestAgentRouteTask } = storeToRefs(navStore)

const PAGE_INSET = 12
const PANEL_GAP = 16
const DEFAULT_ROUTE_PANEL_TOP = 146
let topBarResizeObserver: ResizeObserver | null = null

const navState = ref<'idle' | 'loading' | 'ready'>('idle')
const submitting = ref(false)
const naturalInput = ref('')
const travelMode = ref<'walking' | 'cycling'>('walking')
const isFromAgent = ref(false)
const agentSummary = ref('')
const reasoning = ref('')
const followUpSuggestions = ref<string[]>([])
const activeCampus = ref('')
const currentQuery = ref('')
const pendingAgentQuery = ref('')
const currentTaskType = ref('')
const activePreferences = ref<string[]>([])

const startPointName = ref('')
const endPointName = ref('')
const startPoint = ref<{ lat: number; lng: number; name: string } | null>(null)
const endPoint = ref<{ lat: number; lng: number; name: string } | null>(null)
const currentRoute = ref<RouteResponse | null>(null)
const resolvedDestination = ref<ResolvedPlace | null>(null)
const destinationCandidates = ref<ResolvedPlace[]>([])
const currentWaypoints = ref<ResolvedPlace[]>([])
const routeSegments = ref<RouteSegment[]>([])
const routeType = ref<'single' | 'multi_waypoint'>('single')
const routeRenderNotice = ref<null | {
  type: 'info' | 'warning'
  title: string
  message: string
}>(null)

const safetyAnalysis = ref<SafetyAnalysis | null>(null)
const overallAnalysis = ref<OverallAnalysis | null>(null)
const alternativeRoutes = ref<AlternativeRoute[]>([])
const selectedRouteIndex = ref<number>(0)
const totalRoutes = ref<number>(1)
const selectionReason = ref<string>('')

const clarificationRequired = ref(false)
const clarificationMessage = ref('')
const clarificationOptions = ref<string[]>([])
const candidatePanelExpanded = ref(false)

const isSelectingPoint = ref(false)
const selectingType = ref<'start' | 'end'>('start')
const userLocation = ref<UserLocation | null>(navStore.userLocation || null)
const stepsExpanded = ref(true)
const isMapReady = ref(false)
const pendingAutoRoute = ref<null | 'current-location' | 'route' | 'resolved-route'>(null)
const isAgentThinking = ref(false)
const routePanelTop = ref(DEFAULT_ROUTE_PANEL_TOP)
const navigationSessionId = ref<string | null>(null)

const SCENE_LABELS: Record<string, string> = {
  class_commute: '上课赶路',
  dining: '就餐',
  express: '取快递',
  study: '学习',
  medical: '就医',
  return_dorm: '回宿舍',
  campus_tour: '校园导览',
  daily_convenience: '日常出行'
}

const PREFERENCE_LABELS: Record<string, string> = {
  fastest: '最快路线',
  shortest: '最短路线',
  safeNight: '夜间安全',
  avoidCrowd: '避开人群',
  avoidSun: '避开暴晒',
  luggageFriendly: '适合搬行李',
  avoidMainRoad: '避开主干道',
  viaCanteen: '途经食堂',
  viaPrintShop: '途经打印店'
}

const TIME_SLOT_LABELS: Record<string, string> = {
  early_morning: '清晨',
  morning: '上午',
  noon_heat: '正午暴晒',
  afternoon_heat: '下午暴晒',
  evening_peak: '晚高峰',
  night: '夜晚',
  late_night: '深夜'
}

const CONSTRAINT_LABELS: Record<string, string> = {
  avoid_main_road: '避开主干道',
  prefer_main_road: '主干道优先'
}

function getSceneLabel(scene: string) {
  return SCENE_LABELS[scene] || scene
}

function getPreferenceLabel(pref: string) {
  return PREFERENCE_LABELS[pref] || pref
}

function getTimeSlotLabel(timeSlot?: string) {
  if (!timeSlot) return ''
  return TIME_SLOT_LABELS[timeSlot] || timeSlot
}

function getConstraintLabel(constraint?: string) {
  if (!constraint) return ''
  return CONSTRAINT_LABELS[constraint] || constraint
}

function getScoreType(score: number): 'success' | 'warning' | 'danger' {
  if (score >= 7) return 'success'
  if (score >= 5) return 'warning'
  return 'danger'
}

const showRightPanel = computed(() => clarificationRequired.value || !!resolvedDestination.value || !!currentRoute.value)
const showFollowUpStatus = computed(() => clarificationRequired.value || (!!navigationSessionId.value && !!pendingAgentQuery.value))
const followUpStatusText = computed(() => {
  if (clarificationRequired.value) {
    const message = clarificationMessage.value || agentSummary.value || '请继续补充当前导航信息'
    const optionsText = clarificationOptions.value.length ? ` 可选：${clarificationOptions.value.join(' / ')}` : ''
    return `${message}${optionsText}`
  }

  if (navigationSessionId.value && pendingAgentQuery.value) {
    return `正在继续处理当前导航需求：${pendingAgentQuery.value}`
  }

  return ''
})

const isSamePlace = (first?: Partial<ResolvedPlace> | null, second?: Partial<ResolvedPlace> | null) => {
  if (!first || !second) return false

  const firstLat = first.lat != null ? Number(first.lat) : null
  const firstLng = first.lng != null ? Number(first.lng) : null
  const secondLat = second.lat != null ? Number(second.lat) : null
  const secondLng = second.lng != null ? Number(second.lng) : null

  if (firstLat != null && firstLng != null && secondLat != null && secondLng != null) {
    return Math.abs(firstLat - secondLat) < 0.000001 && Math.abs(firstLng - secondLng) < 0.000001
  }

  return Boolean(first.name && second.name && first.name === second.name)
}

const isCurrentCandidate = (place: ResolvedPlace) => isSamePlace(place, resolvedDestination.value)

const hasAgentRouteContext = () => Boolean(
  currentQuery.value ||
  naturalInput.value.trim() ||
  pendingAgentQuery.value ||
  isFromAgent.value ||
  currentTaskType.value ||
  activePreferences.value.length ||
  safetyAnalysis.value ||
  overallAnalysis.value ||
  totalRoutes.value > 1 ||
  routeSegments.value.length ||
  reasoning.value ||
  agentSummary.value
)

const resetRoutePanelScroll = async () => {
  await nextTick()
  if (routePanelRef.value) {
    routePanelRef.value.scrollTop = 0
  }
}

const buildCurrentNavigationQuery = () => (
  naturalInput.value.trim() ||
  currentQuery.value ||
  pendingAgentQuery.value ||
  (resolvedDestination.value ? `帮我导航到${resolvedDestination.value.name}` : '')
)

const replanNavigationWithAgent = async (instruction: string) => {
  const baseQuery = buildCurrentNavigationQuery()
  if (!baseQuery) {
    return false
  }

  submitting.value = true
  try {
    await clearNavigationAgentSession()
    const result = await submitAgentQuery(`${baseQuery}，${instruction}`)
    if (!result?.handled) {
      navState.value = 'idle'
      ElMessage.info('已记录您的调整，请继续补充需求')
      return false
    }
    return true
  } catch {
    isAgentThinking.value = false
    navState.value = 'idle'
    ElMessage.error('重新规划失败，请重试')
    return false
  } finally {
    submitting.value = false
  }
}

const routePanelStyle = computed(() => ({
  top: `${routePanelTop.value}px`
}))
const itineraryPoints = computed(() => {
  const points: Array<{ name: string; label: string; distance?: number; duration?: number; analysis?: { score: number; reason: string }, slotProfile?: SegmentSlotProfile }> = []
  if (startPointName.value) {
    points.push({ name: startPointName.value, label: '起点' })
  }
  currentWaypoints.value.forEach((waypoint, index) => {
    const segment = routeSegments.value[index]
    points.push({
      name: waypoint.name,
      label: `途经点 ${index + 1}`,
      distance: segment?.distance,
      duration: segment?.duration,
      analysis: segment?.analysis,
      slotProfile: segment?.slotProfile
    })
  })
  if (endPointName.value) {
    const lastSegment = routeSegments.value[routeSegments.value.length - 1]
    points.push({
      name: endPointName.value,
      label: currentWaypoints.value.length ? '终点' : '终点',
      distance: lastSegment?.distance,
      duration: lastSegment?.duration,
      analysis: lastSegment?.analysis,
      slotProfile: lastSegment?.slotProfile
    })
  }
  return points
})

const routeChainText = computed(() => {
  const chain = [
    startPointName.value || '当前位置',
    ...currentWaypoints.value.map((waypoint) => waypoint.name).filter(Boolean),
    endPointName.value
  ].filter(Boolean)
  return chain.join(' → ')
})

const currentSelectedAlternative = computed(() => (
  alternativeRoutes.value.find(route => route.index === selectedRouteIndex.value)
  || alternativeRoutes.value.find(route => route.selected)
  || null
))

const buildContextualSuggestions = (payload: Partial<AgentNavigationPayload>) => {
  const dest = payload.destination || payload.destinationText || endPointName.value || '目的地'
  return [
    travelMode.value === 'walking' ? '换骑行重新规划' : '换步行重新规划',
    `从${dest}返回怎么走`,
    `${dest}附近有什么设施`
  ]
}

const syncResolvedDestination = (place: ResolvedPlace | null, rawName?: string) => {
  resolvedDestination.value = place
  activeCampus.value = place?.campus || ''
  if (!place) return
  endPoint.value = { lat: Number(place.lat), lng: Number(place.lng), name: place.name }
  endPointName.value = place.name || rawName || ''
  mapRef.value?.setTemporaryPlace({
    lat: Number(place.lat),
    lng: Number(place.lng),
    name: place.name,
    address: place.address
  })
}

const normalizePlace = (place: Partial<ResolvedPlace> | null | undefined): ResolvedPlace | null => {
  if (!place || place.lat == null || place.lng == null || !place.name) {
    return null
  }
  return {
    ...place,
    name: String(place.name),
    lat: Number(place.lat),
    lng: Number(place.lng)
  }
}

const asRecord = (value: unknown): Record<string, unknown> =>
  (value && typeof value === 'object') ? value as Record<string, unknown> : {}

const normalizePathPoints = (path: unknown): [number, number][] => {
  if (!Array.isArray(path)) {
    return []
  }

  return path
    .map((point: unknown) => {
      if (Array.isArray(point) && point.length >= 2) {
        const first = Number(point[0])
        const second = Number(point[1])
        if (Number.isFinite(first) && Number.isFinite(second)) {
          if (first >= -90 && first <= 90 && second >= -180 && second <= 180) {
            return [first, second] as [number, number]
          }
          if (second >= -90 && second <= 90 && first >= -180 && first <= 180) {
            return [second, first] as [number, number]
          }
        }
      }

      if (point && typeof point === 'object') {
        const objectPoint = point as Record<string, unknown>
        if (objectPoint.lat != null && objectPoint.lng != null) {
          return [Number(objectPoint.lat), Number(objectPoint.lng)] as [number, number]
        }
        if (objectPoint.latitude != null && objectPoint.longitude != null) {
          return [Number(objectPoint.latitude), Number(objectPoint.longitude)] as [number, number]
        }
      }

      return null
    })
    .filter((point): point is [number, number] => Array.isArray(point) && Number.isFinite(point[0]) && Number.isFinite(point[1]))
}

const normalizeSegment = (segment: unknown): RouteSegment => {
  const raw = asRecord(segment)
  const slotProfile = asRecord(raw.slotProfile)
  const analysis = asRecord(raw.analysis)
  const alternativeRoutes = Array.isArray(raw.alternativeRoutes)
    ? raw.alternativeRoutes.map((route) => {
        const routeRaw = asRecord(route)
        return {
          index: Number(routeRaw.index || 0),
          distance: Number(routeRaw.distance || 0),
          duration: Number(routeRaw.duration || 0),
          summary: routeRaw.summary ? String(routeRaw.summary) : undefined,
          reason: routeRaw.reason ? String(routeRaw.reason) : undefined,
          score: routeRaw.score != null ? Number(routeRaw.score) : undefined,
          selected: Boolean(routeRaw.selected)
        }
      })
    : undefined

  return {
    index: Number(raw.index || 0),
    from: normalizePlace(raw.from as Partial<ResolvedPlace>),
    to: normalizePlace(raw.to as Partial<ResolvedPlace>),
    distance: Number(raw.distance || 0),
    duration: Number(raw.duration || 0),
    steps: Array.isArray(raw.steps) ? raw.steps : [],
    path: normalizePathPoints(raw.path),
    geometrySource: raw.geometrySource ? String(raw.geometrySource) : undefined,
    selectedRouteIndex: raw.selectedRouteIndex != null ? Number(raw.selectedRouteIndex) : undefined,
    totalRoutes: raw.totalRoutes != null ? Number(raw.totalRoutes) : undefined,
    selectionReason: raw.selectionReason ? String(raw.selectionReason) : undefined,
    alternativeRoutes,
    slotProfile: raw.slotProfile
      ? {
          index: Number(slotProfile.index || raw.index || 0),
          destination: slotProfile.destination ? String(slotProfile.destination) : undefined,
          segmentGoal: slotProfile.segmentGoal ? String(slotProfile.segmentGoal) : undefined,
          taskScene: slotProfile.taskScene ? String(slotProfile.taskScene) : undefined,
          preferences: Array.isArray(slotProfile.preferences) ? slotProfile.preferences.map(String) : [],
          constraints: Array.isArray(slotProfile.constraints) ? slotProfile.constraints.map(String) : [],
          timeContext: slotProfile.timeContext ? String(slotProfile.timeContext) : undefined,
          timeSlot: slotProfile.timeSlot ? String(slotProfile.timeSlot) : undefined,
          urgencyMinutes: slotProfile.urgencyMinutes != null ? Number(slotProfile.urgencyMinutes) : null,
          notes: slotProfile.notes ? String(slotProfile.notes) : undefined
        }
      : undefined,
    analysis: raw.analysis
      ? {
          score: Number(analysis.score || 0),
          reason: analysis.reason ? String(analysis.reason) : '',
          warnings: Array.isArray(analysis.warnings) ? analysis.warnings.map(String) : [],
          suggestions: Array.isArray(analysis.suggestions) ? analysis.suggestions.map(String) : []
        }
      : undefined
  }
}

const normalizeSafetyAnalysis = (analysis: unknown): SafetyAnalysis | null => {
  if (!analysis) {
    return null
  }
  const raw = asRecord(analysis)

  return {
    safetyScore: Number(raw.safetyScore || 0),
    warnings: Array.isArray(raw.warnings) ? raw.warnings.map(String) : [],
    suggestions: Array.isArray(raw.suggestions) ? raw.suggestions.map(String) : [],
    hasAlternative: Boolean(raw.hasAlternative),
    alternativeSummary: raw.alternativeSummary ? String(raw.alternativeSummary) : undefined
  }
}

const normalizeOverallAnalysis = (analysis: unknown): OverallAnalysis | null => {
  if (!analysis) {
    return null
  }
  const raw = asRecord(analysis)

  return {
    overallScore: Number(raw.overallScore || 0),
    summary: raw.summary ? String(raw.summary) : '',
    keyPoints: Array.isArray(raw.keyPoints) ? raw.keyPoints.map(String) : [],
    recommendation: raw.recommendation ? String(raw.recommendation) : ''
  }
}

const buildFallbackOverallAnalysisFromRoute = (route: Partial<AlternativeRoute>): OverallAnalysis | null => {
  const score = route.score != null ? Number(route.score) : 7
  const summary = route.reason || route.summary

  if (!summary && route.score == null) {
    return null
  }

  const keyPoints = [route.summary, route.reason].filter((item): item is string => Boolean(item))

  return {
    overallScore: score,
    summary: summary || '路线已准备就绪',
    keyPoints,
    recommendation: route.reason || '可按当前路线前往'
  }
}

const normalizeAlternativeRoute = (route: unknown): AlternativeRoute => {
  const raw = asRecord(route)
  return {
    index: Number(raw.index || 0),
    distance: Number(raw.distance || 0),
    duration: Number(raw.duration || 0),
    summary: raw.summary ? String(raw.summary) : undefined,
    reason: raw.reason ? String(raw.reason) : undefined,
    score: raw.score != null ? Number(raw.score) : undefined,
    steps: Array.isArray(raw.steps) ? raw.steps : [],
    path: normalizePathPoints(raw.path),
    safetyAnalysis: normalizeSafetyAnalysis(raw.safetyAnalysis),
    overallAnalysis: normalizeOverallAnalysis(raw.overallAnalysis),
    selected: Boolean(raw.selected)
  }
}

const syncAlternativeRouteSelectionState = (routeIndex: number) => {
  alternativeRoutes.value = alternativeRoutes.value.map(route => ({
    ...route,
    selected: route.index === routeIndex
  }))
}

const buildRouteResponseFromAlternative = (route: AlternativeRoute): RouteResponse => ({
  distance: Number(route.distance || 0),
  duration: Number(route.duration || 0),
  steps: Array.isArray(route.steps) ? route.steps : [],
  path: normalizePathPoints(route.path),
  selectedRouteIndex: selectedRouteIndex.value,
  totalRoutes: totalRoutes.value,
  selectionReason: selectionReason.value,
  alternativeRoutes: alternativeRoutes.value.map(routeItem => ({
    ...routeItem,
    path: normalizePathPoints(routeItem.path),
    steps: Array.isArray(routeItem.steps) ? routeItem.steps : []
  }))
})

const syncLatestAgentRouteTaskSelection = () => {
  if (!isFromAgent.value || !latestAgentRouteTask.value) {
    return
  }

  navStore.setLatestAgentRouteTask({
    ...latestAgentRouteTask.value,
    distance: currentRoute.value?.distance,
    duration: currentRoute.value?.duration,
    path: currentRoute.value?.path,
    steps: currentRoute.value?.steps,
    selectedRouteIndex: selectedRouteIndex.value,
    totalRoutes: totalRoutes.value,
    selectionReason: selectionReason.value,
    alternativeRoutes: alternativeRoutes.value.map(route => ({
      ...route,
      path: normalizePathPoints(route.path),
      steps: Array.isArray(route.steps) ? route.steps : []
    })),
    safetyAnalysis: safetyAnalysis.value ? { ...safetyAnalysis.value } : null,
    overallAnalysis: overallAnalysis.value ? { ...overallAnalysis.value } : null
  })
}

const syncRouteInsightsFromAlternative = async (
  route: AlternativeRoute,
  options: {
    syncMap?: boolean
    syncStore?: boolean
    updateSelectionReason?: boolean
  } = {}
) => {
  const nextOverall = normalizeOverallAnalysis(route.overallAnalysis) || buildFallbackOverallAnalysisFromRoute(route)
  const nextSafety = normalizeSafetyAnalysis(route.safetyAnalysis)

  if (options.updateSelectionReason !== false) {
    selectionReason.value = route.reason
      ? `已切换为路线${route.index + 1}：${route.reason}`
      : `已切换为路线${route.index + 1}`
  }

  safetyAnalysis.value = nextSafety
  overallAnalysis.value = nextOverall
  currentRoute.value = buildRouteResponseFromAlternative(route)

  if (options.syncStore !== false) {
    syncLatestAgentRouteTaskSelection()
  }

  if (options.syncMap !== false && startPoint.value && endPoint.value) {
    clearRouteRenderNotice()
    await nextTick()
    mapRef.value?.renderResolvedRoute(currentRoute.value, startPoint.value, endPoint.value)
  }
}

const mergeSegmentPaths = (segments: RouteSegment[]): [number, number][] => {
  const merged: [number, number][] = []
  segments.forEach((segment) => {
    segment.path.forEach((point) => {
      const last = merged[merged.length - 1]
      if (last && last[0] === point[0] && last[1] === point[1]) {
        return
      }
      merged.push(point)
    })
  })
  return merged
}

const syncFloatingLayout = () => {
  const topBarHeight = topBarRef.value?.offsetHeight || 0
  routePanelTop.value = Math.max(DEFAULT_ROUTE_PANEL_TOP, PAGE_INSET + topBarHeight + PANEL_GAP)
}

const clearClarification = () => {
  clarificationRequired.value = false
  clarificationMessage.value = ''
  clarificationOptions.value = []
  navStore.clearClarification()
}

const clearRouteInsights = () => {
  safetyAnalysis.value = null
  overallAnalysis.value = null
  alternativeRoutes.value = []
  selectedRouteIndex.value = 0
  totalRoutes.value = 1
  selectionReason.value = ''
}

const clearRouteRenderNotice = () => {
  routeRenderNotice.value = null
}

const applyMultiSegmentRenderResult = (
  renderResult?: { mode?: 'segment' | 'fallback_total' | 'failed'; reason?: string } | null,
  notify = false
) => {
  if (!renderResult || renderResult.mode === 'segment') {
    clearRouteRenderNotice()
    return
  }

  if (renderResult.mode === 'fallback_total') {
    const message = renderResult.reason || '分段路径数据不完整，已退化为总路径规划展示'
    routeRenderNotice.value = {
      type: 'warning',
      title: '已退化为总路径规划',
      message: `${message}。当前地图展示的是高德返回的整体路径，分段槽位分析仍保留在右侧说明中。`
    }
    if (notify) {
      ElMessage.warning(routeRenderNotice.value.message)
    }
    return
  }

  const message = renderResult.reason || '当前未拿到可绘制的分段路径和总路径'
  routeRenderNotice.value = {
    type: 'info',
    title: '路线绘制受限',
    message
  }
  if (notify) {
    ElMessage.warning(message)
  }
}

const queueAutoRoute = (mode: 'current-location' | 'route' | 'resolved-route') => {
  pendingAutoRoute.value = mode
}

const flushPendingAutoRoute = async () => {
  if (!isMapReady.value || !pendingAutoRoute.value) return

  const mode = pendingAutoRoute.value
  pendingAutoRoute.value = null

  if (mode === 'current-location') {
    await useCurrentLocationAsStart()
    return
  }

  if (mode === 'resolved-route') {
    if (startPoint.value && endPoint.value && currentRoute.value?.path?.length) {
      mapRef.value?.renderResolvedRoute(currentRoute.value, startPoint.value, endPoint.value)
    }
    return
  }

  if (startPoint.value && endPoint.value) {
    await handleCalculateRoute()
  }
}

const applyResolveResult = async (result: NavigationResolveResponse, meta: { query: string; summary?: string; suggestions?: string[]; source?: string } = { query: '' }) => {
  currentQuery.value = meta.query
  isFromAgent.value = meta.source === 'agent'
  if (result.clarificationRequired) {
    candidatePanelExpanded.value = false
    clarificationRequired.value = true
    clarificationMessage.value = result.message || '请先确认校区'
    clarificationOptions.value = result.clarificationOptions || []
    destinationCandidates.value = []
    agentSummary.value = meta.summary || clarificationMessage.value
    reasoning.value = ''
    followUpSuggestions.value = []
    pendingAgentQuery.value = meta.query
    navStore.setClarification({ query: meta.query, options: clarificationOptions.value, message: clarificationMessage.value })
    navState.value = 'idle'
    await resetRoutePanelScroll()
    return
  }

  clearClarification()
  if (!result.success || !result.place) {
    navState.value = 'idle'
    ElMessage.warning(result.message || '未找到相关地点')
    return
  }

  syncResolvedDestination(result.place, meta.query)
  destinationCandidates.value = (result.places || result.candidates || []).map(place => ({
    ...place,
    lat: Number(place.lat),
    lng: Number(place.lng)
  }))
  candidatePanelExpanded.value = false
  currentWaypoints.value = []
  routeSegments.value = []
  routeType.value = 'single'
  clearRouteInsights()
  agentSummary.value = meta.summary || result.message || `已为您识别目的地【${result.place.name}】`
  reasoning.value = result.message || ''
  followUpSuggestions.value = meta.suggestions?.length ? meta.suggestions : buildContextualSuggestions({ destination: result.place.name })
  await resetRoutePanelScroll()

  if (userLocation.value) {
    await useCurrentLocationAsStart()
  } else {
    startPoint.value = null
    startPointName.value = ''
    currentRoute.value = null
    navState.value = 'idle'
    ElMessage.info('已定位目的地，请使用当前位置或在地图上选择起点')
  }
}

const resolvePlace = async (query: string, campus?: string, meta: { summary?: string; suggestions?: string[]; source?: string } = {}) => {
  navState.value = 'loading'
  const res = await navigationApi.resolvePlace({
    keyword: query,
    campus,
    userLat: userLocation.value?.lat,
    userLng: userLocation.value?.lng
  })
  await applyResolveResult(res.data, { query, ...meta })
}

const useCurrentLocationAsStart = async (options: { preferAgentReplan?: boolean } = {}) => {
  if (!resolvedDestination.value) return
  if (options.preferAgentReplan && hasAgentRouteContext()) {
    const handled = await replanNavigationWithAgent('起点改为当前位置')
    if (handled) {
      return
    }
  }
  if (!isMapReady.value) {
    queueAutoRoute('current-location')
    return
  }
  if (!userLocation.value) {
    try {
      await mapRef.value?.handleLocate()
    } catch {
      // handleLocate 已给出提示，这里只中断自动规划
    }
    if (!userLocation.value) {
      navState.value = 'idle'
      ElMessage.warning('请先授权定位，或在地图上手动选择起点')
      return
    }
  }
  startPoint.value = { lat: userLocation.value.lat, lng: userLocation.value.lng, name: '当前位置' }
  startPointName.value = '当前位置'
  await handleCalculateRoute()
}

const clearNavigationAgentSession = async () => {
  const sessionId = navigationSessionId.value
  navigationSessionId.value = null

  if (!sessionId) return

  try {
    await clearAgentSession(sessionId)
  } catch {
    // 导航页的短会话清理失败不影响后续规划
  }
}

const submitAgentQuery = async (text: string, options: { continueSession?: boolean } = {}) => {
  isAgentThinking.value = true

  // 如果没有用户位置，先尝试获取
  if (!userLocation.value && mapRef.value) {
    try {
      console.log('尝试获取用户位置...')
      await mapRef.value.handleLocate()
    } catch (e) {
      console.warn('获取位置失败，继续使用空位置:', e)
    }
  }

  const context = userLocation.value
    ? {
        userLat: userLocation.value.lat,
        userLng: userLocation.value.lng,
        currentLat: userLocation.value.lat,
        currentLng: userLocation.value.lng
      }
    : null
  const res = await chatWithAgent({
    sessionId: options.continueSession ? navigationSessionId.value : null,
    message: text,
    context
  })
  const data = res.data
  if (!data) {
    isAgentThinking.value = false
    throw new Error('无响应')
  }
  navigationSessionId.value = data.sessionId || null

  if (data.followUpType || data.askFor === 'campus') {
    clarificationRequired.value = true
    clarificationMessage.value = data.reply || '请先确认校区'
    clarificationOptions.value = data.followUpOptions || []
    agentSummary.value = data.reply || ''
    currentQuery.value = text
    pendingAgentQuery.value = text
    navState.value = 'idle'
    isAgentThinking.value = false
    return { handled: true }
  }

  const routeCard = (data.cards || []).find((c: { type?: string }) => c.type === 'ROUTE')
  if (routeCard?.data) {
    navStore.setLatestAgentRouteTask({ source: 'agent', ...routeCard.data })
    await applyTaskPayload(routeCard.data, data.followUpSuggestions || [], 'agent')
    pendingAgentQuery.value = ''
    await clearNavigationAgentSession()
    isAgentThinking.value = false
    return { handled: true }
  }

  await clearNavigationAgentSession()
  isAgentThinking.value = false
  return { handled: false, data }
}

const handleNaturalSubmit = async () => {
  const text = naturalInput.value.trim()
  if (!text) return

  submitting.value = true
  try {
    const continueSession = clarificationRequired.value || !!pendingAgentQuery.value
    if (!continueSession) {
      await clearNavigationAgentSession()
    }

    const result = await submitAgentQuery(text, { continueSession })
    if (result.handled) {
      return
    }

    ElMessage.info(result.data?.reply ? result.data.reply.substring(0, 80) : '请继续补充信息')
    navState.value = 'idle'
  } catch (e) {
    isAgentThinking.value = false
    ElMessage.error('路线规划失败，请重试')
    navState.value = 'idle'
  } finally {
    submitting.value = false
  }
}

const applyTaskPayload = async (payload: AgentNavigationPayload, suggestions: string[] = [], source = 'agent') => {
  isFromAgent.value = source === 'agent'
  travelMode.value = (payload.mode as 'walking' | 'cycling') || 'walking'
  agentSummary.value = payload.agentSummary || ''
  reasoning.value = payload.reasoning || ''
  followUpSuggestions.value = suggestions.length ? suggestions : buildContextualSuggestions(payload)
  activeCampus.value = payload.campus || ''
  routeType.value = payload.routeType || 'single'

  // 提取场景和偏好信息
  currentTaskType.value = payload.taskType || ''
  activePreferences.value = Array.isArray(payload.preferences) ? payload.preferences : []

  // 每次应用新任务时先清空旧起点，避免复用上一次显式起点。
  startPoint.value = null
  startPointName.value = ''
  currentRoute.value = null

  if (payload.clarificationRequired) {
    candidatePanelExpanded.value = false
    clarificationRequired.value = true
    clarificationMessage.value = payload.agentSummary || '请先确认校区'
    clarificationOptions.value = payload.clarificationOptions || []
    destinationCandidates.value = []
    currentQuery.value = payload.destinationText || payload.destination || naturalInput.value
    pendingAgentQuery.value = currentQuery.value
    currentWaypoints.value = (payload.waypoints || []).map(place => normalizePlace(place)).filter(Boolean) as ResolvedPlace[]
    routeSegments.value = []
    currentRoute.value = null
    clearRouteInsights()
    navState.value = 'idle'
    await resetRoutePanelScroll()
    return
  }

  clearClarification()

  const place = normalizePlace(payload.destLat != null && payload.destLng != null
    ? {
        name: payload.destination || payload.destinationText || '',
        address: payload.destinationAddress,
        lat: payload.destLat,
        lng: payload.destLng,
        campus: payload.campus,
        source: payload.resolutionSource
      }
    : null)

  syncResolvedDestination(place, payload.destinationText || payload.destination)
  pendingAgentQuery.value = ''
  destinationCandidates.value = (payload.destinationOptions || payload.candidates || []).map(place => ({
    ...place,
    lat: Number(place.lat),
    lng: Number(place.lng)
  }))
  candidatePanelExpanded.value = false
  currentWaypoints.value = (payload.waypoints || []).map(place => normalizePlace(place)).filter(Boolean) as ResolvedPlace[]
  routeSegments.value = (payload.segments || []).map(segment => normalizeSegment(segment))
  const normalizedTopLevelPath = normalizePathPoints(payload.path)
  const mergedSegmentPath = mergeSegmentPaths(routeSegments.value)
  await resetRoutePanelScroll()

  console.info('[navigation] raw route payload', {
    routeType: payload.routeType,
    routeReady: payload.routeReady,
    geometrySource: payload.geometrySource,
    geometryFallbackReason: payload.geometryFallbackReason,
    topLevelPathPoints: Array.isArray(payload.path) ? payload.path.length : 0,
    topLevelPathSample: Array.isArray(payload.path) ? payload.path.slice(0, 2) : payload.path,
    segmentCount: Array.isArray(payload.segments) ? payload.segments.length : 0,
    segmentPathPoints: Array.isArray(payload.segments)
      ? payload.segments.map((segment: { path?: unknown[] }) => Array.isArray(segment?.path) ? segment.path.length : 0)
      : [],
    segmentPathSample: Array.isArray(payload.segments)
      ? payload.segments.slice(0, 2).map((segment: { path?: unknown[] }, index: number) => ({
          index,
          sample: Array.isArray(segment?.path) ? segment.path.slice(0, 2) : segment?.path
        }))
      : [],
    normalizedTopLevelPathPoints: normalizedTopLevelPath.length,
    mergedSegmentPathPoints: mergedSegmentPath.length
  })
  console.info('[navigation] route payload diagnostics', JSON.stringify({
    routeType: payload.routeType,
    routeReady: payload.routeReady,
    amapApiVersion: payload.amapApiVersion,
    routingStrategy: payload.routingStrategy,
    geometrySource: payload.geometrySource,
    geometryFallbackReason: payload.geometryFallbackReason,
    topLevelPathPoints: Array.isArray(payload.path) ? payload.path.length : 0,
    topLevelPathSample: Array.isArray(payload.path) ? payload.path.slice(0, 2) : payload.path,
    segmentCount: Array.isArray(payload.segments) ? payload.segments.length : 0,
    segmentPathPoints: Array.isArray(payload.segments)
      ? payload.segments.map((segment: { path?: unknown[] }) => Array.isArray(segment?.path) ? segment.path.length : 0)
      : [],
    segmentPathSample: Array.isArray(payload.segments)
      ? payload.segments.slice(0, 2).map((segment: { path?: unknown[] }, index: number) => ({
          index,
          sample: Array.isArray(segment?.path) ? segment.path.slice(0, 2) : segment?.path
        }))
      : [],
    normalizedTopLevelPathPoints: normalizedTopLevelPath.length,
    mergedSegmentPathPoints: mergedSegmentPath.length
  }))

  if (payload.originLat != null && payload.originLng != null) {
    startPoint.value = { lat: Number(payload.originLat), lng: Number(payload.originLng), name: payload.origin || '当前位置' }
    startPointName.value = payload.origin || '当前位置'
  }

  if (payload.routeReady && (normalizedTopLevelPath.length || mergedSegmentPath.length || Array.isArray(payload.steps))) {
    const normalizedAlternativeRoutes = (payload.alternativeRoutes || []).map(normalizeAlternativeRoute)

    currentRoute.value = {
      distance: payload.distance || 0,
      duration: payload.duration || 0,
      steps: payload.steps || [],
      path: normalizedTopLevelPath.length ? normalizedTopLevelPath : mergedSegmentPath,
      selectedRouteIndex: payload.selectedRouteIndex || 0,
      totalRoutes: payload.totalRoutes || 1,
      selectionReason: payload.selectionReason || '',
      alternativeRoutes: normalizedAlternativeRoutes
    }
    navState.value = 'ready'

    safetyAnalysis.value = normalizeSafetyAnalysis(payload.safetyAnalysis)
    overallAnalysis.value = normalizeOverallAnalysis(payload.overallAnalysis)

    // 提取多路线选择信息
    if (payload.totalRoutes && payload.totalRoutes > 1) {
      totalRoutes.value = payload.totalRoutes
      selectedRouteIndex.value = payload.selectedRouteIndex || 0
      selectionReason.value = payload.selectionReason || ''
      alternativeRoutes.value = normalizedAlternativeRoutes
      const selectedAlternative = alternativeRoutes.value.find(route => route.index === selectedRouteIndex.value)
        || alternativeRoutes.value.find(route => route.selected)
      if (selectedAlternative) {
        await syncRouteInsightsFromAlternative(selectedAlternative, {
          syncMap: false,
          syncStore: false,
          updateSelectionReason: false
        })
      }
    } else {
      totalRoutes.value = 1
      selectedRouteIndex.value = 0
      selectionReason.value = ''
      alternativeRoutes.value = []
    }

    await nextTick()
    if (routeType.value === 'multi_waypoint' && startPoint.value && endPoint.value && routeSegments.value.length) {
      const renderResult = mapRef.value?.renderMultiSegmentRoute(routeSegments.value, [
        { name: startPoint.value.name, lat: startPoint.value.lat, lng: startPoint.value.lng },
        ...currentWaypoints.value,
        { name: endPoint.value.name, lat: endPoint.value.lat, lng: endPoint.value.lng }
      ], currentRoute.value?.path || [])
      applyMultiSegmentRenderResult(renderResult, true)
      return
    }
    if (startPoint.value && endPoint.value && currentRoute.value?.path?.length) {
      clearRouteRenderNotice()
      queueAutoRoute('resolved-route')
      await flushPendingAutoRoute()
    }
    return
  }

  if (!place) {
    navState.value = 'idle'
    return
  }

  if (startPoint.value) {
    await resetRoutePanelScroll()
    queueAutoRoute('route')
    await flushPendingAutoRoute()
    return
  }

  await resetRoutePanelScroll()
  queueAutoRoute('current-location')
  await flushPendingAutoRoute()
}

const handleModeChange = async () => {
  navStore.setPreferredMode(travelMode.value)
  if (startPoint.value && endPoint.value) {
    queueAutoRoute('route')
    await flushPendingAutoRoute()
  }
}

const handleRouteCalculated = (route: RouteResponse) => {
  currentRoute.value = route
  navState.value = 'ready'
  clearRouteRenderNotice()
}

const handleLocationChanged = async (loc: UserLocation) => {
  userLocation.value = loc
  navStore.setUserLocation(loc)
  await flushPendingAutoRoute()
}

const handleMapLoaded = async () => {
  isMapReady.value = true
  await flushPendingAutoRoute()
}

const handlePointSelected = async (data: { type: string; lat: number; lng: number; name: string }) => {
  pendingAutoRoute.value = null
  if (data.type === 'start') {
    startPoint.value = { lat: data.lat, lng: data.lng, name: data.name }
    startPointName.value = data.name
    isSelectingPoint.value = false
    if (endPoint.value) {
      if (hasAgentRouteContext()) {
        await replanNavigationWithAgent(`起点改为${data.name}`)
      } else {
        await handleCalculateRoute()
      }
    }
    return
  }
  endPoint.value = { lat: data.lat, lng: data.lng, name: data.name }
  endPointName.value = data.name
  syncResolvedDestination({ name: data.name, lat: data.lat, lng: data.lng }, data.name)
  isSelectingPoint.value = false
  if (startPoint.value) handleCalculateRoute()
}

const handleCalculateRoute = async () => {
  if (!endPoint.value) {
    ElMessage.warning('请先确认目的地')
    return
  }
  if (!startPoint.value) {
    ElMessage.warning('请先选择起点')
    return
  }

  navState.value = 'loading'
  try {
    if (routeType.value === 'multi_waypoint' && currentWaypoints.value.length && routeSegments.value.length) {
      const renderResult = mapRef.value?.renderMultiSegmentRoute(routeSegments.value, [
        { name: startPoint.value.name, lat: startPoint.value.lat, lng: startPoint.value.lng },
        ...currentWaypoints.value,
        { name: endPoint.value.name, lat: endPoint.value.lat, lng: endPoint.value.lng }
      ], currentRoute.value?.path || [])
      applyMultiSegmentRenderResult(renderResult, true)
      navState.value = 'ready'
      return
    }
    clearRouteInsights()
    clearRouteRenderNotice()
    await mapRef.value?.calculateRoute(startPoint.value.lat, startPoint.value.lng, endPoint.value.lat, endPoint.value.lng)
  } catch (e) {
    navState.value = 'idle'
    ElMessage.error('路线规划失败，请重试')
  }
}

const startSelectStart = () => {
  selectingType.value = 'start'
  isSelectingPoint.value = true
}

const cancelSelect = () => {
  isSelectingPoint.value = false
}

const applySuggestion = (text: string) => {
  naturalInput.value = text
  handleNaturalSubmit()
}

const selectAlternativeRoute = async (routeIndex: number) => {
  const targetRoute = alternativeRoutes.value.find(route => route.index === routeIndex)
  if (!targetRoute) {
    return
  }

  if (selectedRouteIndex.value === routeIndex && targetRoute.selected) {
    return
  }

  selectedRouteIndex.value = routeIndex
  syncAlternativeRouteSelectionState(routeIndex)
  const normalizedTargetRoute = alternativeRoutes.value.find(route => route.index === routeIndex) || targetRoute
  await syncRouteInsightsFromAlternative(normalizedTargetRoute)
}

const selectCandidate = async (place: ResolvedPlace) => {
  candidatePanelExpanded.value = false

  if (isCurrentCandidate(place)) {
    ElMessage.info(`当前路线已选择【${place.name}】`)
    return
  }

  clearClarification()
  const instruction = place.campus
    ? `目的地改为${place.name}，校区是${place.campus}`
    : `目的地改为${place.name}`
  await replanNavigationWithAgent(instruction)
}

const selectClarification = async (campus: string) => {
  clearClarification()

  if (pendingAgentQuery.value) {
    naturalInput.value = pendingAgentQuery.value
    await submitAgentQuery(`${pendingAgentQuery.value}，校区是${campus}`, { continueSession: true })
    return
  }

  if (currentQuery.value) {
    await resolvePlace(currentQuery.value, campus, {
      summary: `已按您选择的${campus}继续解析导航需求`,
      source: isFromAgent.value ? 'agent' : 'page'
    })
  }
}

const resetToIdle = () => {
  navState.value = 'idle'
  naturalInput.value = ''
  agentSummary.value = ''
  reasoning.value = ''
  followUpSuggestions.value = []
  startPoint.value = null
  endPoint.value = null
  currentRoute.value = null
  resolvedDestination.value = null
  destinationCandidates.value = []
  currentWaypoints.value = []
  routeSegments.value = []
  routeType.value = 'single'
  currentQuery.value = ''
  pendingAgentQuery.value = ''
  startPointName.value = ''
  endPointName.value = ''
  activeCampus.value = ''
  currentTaskType.value = ''
  activePreferences.value = []
  candidatePanelExpanded.value = false
  pendingAutoRoute.value = null
  safetyAnalysis.value = null
  overallAnalysis.value = null
  alternativeRoutes.value = []
  selectedRouteIndex.value = 0
  totalRoutes.value = 1
  selectionReason.value = ''
  clearRouteRenderNotice()
  clearClarification()
  isSelectingPoint.value = false
  mapRef.value?.clearRoute()
  void clearNavigationAgentSession()
}

watch(
  latestAgentRouteTask,
  async (task) => {
    if (!task) return
    if (navState.value !== 'idle' || clarificationRequired.value || resolvedDestination.value || currentRoute.value) {
      return
    }
    naturalInput.value = task.destinationText || task.destination || naturalInput.value
    await applyTaskPayload(task, [], task.source || 'agent')
  },
  { deep: true }
)

const firstQueryValue = (value: unknown) => {
  if (Array.isArray(value)) return value[0] || ''
  return typeof value === 'string' ? value : ''
}

onMounted(async () => {
  await nextTick()
  syncFloatingLayout()

  if (typeof ResizeObserver !== 'undefined' && topBarRef.value) {
    topBarResizeObserver = new ResizeObserver(() => {
      syncFloatingLayout()
    })
    topBarResizeObserver.observe(topBarRef.value)
  }

  if (navStore.userLocation) {
    userLocation.value = navStore.userLocation
  }
  if (navStore.preferredMode) {
    travelMode.value = navStore.preferredMode
  }

  const pending = navStore.consumePendingTask()
  if (pending) {
    naturalInput.value = pending.destinationText || pending.destination || ''
    await applyTaskPayload(pending, [], pending.source || 'agent')
  } else {
    const destinationFromQuery = firstQueryValue(route.query.destination).trim()
    if (destinationFromQuery) {
      const campusFromQuery = firstQueryValue(route.query.campus).trim()
      const sourceFromQuery = firstQueryValue(route.query.source).trim() || 'page'
      naturalInput.value = destinationFromQuery
      await resolvePlace(destinationFromQuery, campusFromQuery || undefined, {
        source: sourceFromQuery,
        summary: firstQueryValue(route.query.intent) === 'meetup'
          ? `已从二手交易面交建议带入目的地【${destinationFromQuery}】`
          : `已带入目的地【${destinationFromQuery}】`
      })
    }
  }
})

onBeforeUnmount(() => {
  topBarResizeObserver?.disconnect()
  topBarResizeObserver = null
  void clearNavigationAgentSession()
})
</script>

<style scoped>
.nav-page {
  position: relative;
  height: calc(100vh - 60px);
  overflow: hidden;
  background: #f0f2f5;
}
.top-bar {
  position: absolute;
  top: 12px;
  left: 12px;
  right: 12px;
  z-index: 100;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 12px;
  padding: 12px 14px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: right 0.24s ease;
}
.input-row { display: flex; gap: 8px; align-items: center; }
.follow-up-status {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #d9ecff;
  border-radius: 10px;
  background: linear-gradient(135deg, #f5f9ff 0%, #edf4ff 100%);
  color: #1f4f96;
  font-size: 12px;
  line-height: 1.5;
}
.follow-up-status-tag,
.follow-up-status-icon {
  flex-shrink: 0;
}
.follow-up-status-icon {
  margin-top: 2px;
  color: #409eff;
}
.follow-up-status-text {
  min-width: 0;
}
.nl-input { flex: 1; }
.task-info-row { display: flex; gap: 6px; flex-wrap: wrap; align-items: center; }
.mode-row { display: flex; align-items: center; gap: 10px; }
.map-area { position: absolute; inset: 0; z-index: 1; }
.select-tip-bar {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 50;
  background: rgba(0, 65, 145, 0.92);
  color: #fff;
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.loading-overlay {
  position: absolute;
  inset: 0;
  z-index: 60;
  background: rgba(255, 255, 255, 0.7);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-size: 14px;
  color: #606266;
}
.route-panel {
  position: absolute;
  right: 12px;
  bottom: 12px;
  width: 330px;
  z-index: 90;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  overflow-x: hidden;
  overflow-y: auto;
}
.route-summary {
  background: linear-gradient(135deg, #004191 0%, #1a5cb8 100%);
  padding: 16px;
  color: #fff;
  height: fit-content;
}
.route-panel-body {
  background: #fff;
  padding-bottom: 12px;
}
.summary-title { font-size: 14px; font-weight: 600; margin-bottom: 8px; display: flex; align-items: center; }
.agent-summary-text { font-size: 13px; line-height: 1.6; margin-bottom: 10px; }
.reasoning-text { font-size: 12px; color: rgba(255,255,255,0.85); line-height: 1.5; margin-bottom: 12px; }
.preferences-row { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 4px; }
.od-row { display: flex; flex-direction: column; gap: 8px; }
.route-chain-text { margin-top: 8px; font-size: 12px; color: rgba(255,255,255,0.88); line-height: 1.6; }
.route-render-alert { margin-top: 10px; }
.od-point { display: flex; align-items: center; gap: 8px; }
.dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.dot-start { background: #67c23a; }
.dot-end { background: #ffd04b; }
.od-line { margin-left: 3px; width: 2px; height: 18px; background: rgba(255,255,255,0.35); }
.od-name { font-size: 14px; }
.stats-row { margin-top: 12px; display: flex; align-items: center; gap: 8px; }
.stat-item { display: inline-flex; align-items: center; gap: 4px; font-size: 13px; }
.stat-sep { opacity: 0.7; }
.selected-route-brief {
  margin-top: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.14);
  border-radius: 8px;
}
.selected-route-brief-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.selected-route-brief-title {
  font-size: 13px;
  font-weight: 600;
}
.selected-route-brief-text {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.5;
  color: rgba(255, 255, 255, 0.95);
}
.selected-route-brief-note {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: rgba(255, 255, 255, 0.82);
}
.clarify-section, .candidate-section, .actions-section, .itinerary-section, .steps-section, .suggestions-section { padding: 14px 16px; border-top: 1px solid #eef2f6; flex-shrink: 0; }
.section-title, .steps-header { font-size: 14px; font-weight: 600; color: #303133; display: flex; align-items: center; justify-content: space-between; }
.clarify-text { margin-top: 8px; color: #606266; line-height: 1.6; font-size: 13px; }
.clarify-options, .action-buttons, .suggestions-list { margin-top: 10px; display: flex; flex-wrap: wrap; gap: 8px; }
.candidate-list { margin-top: 10px; display: flex; flex-direction: column; gap: 8px; }
.itinerary-list { margin-top: 10px; display: flex; flex-direction: column; gap: 10px; }
.itinerary-item { border: 1px solid #e8eef5; border-radius: 8px; padding: 10px 12px; background: #fafcff; }
.itinerary-label { font-size: 12px; color: #909399; }
.itinerary-name { margin-top: 4px; font-size: 13px; font-weight: 600; color: #303133; }
.itinerary-meta { margin-top: 4px; font-size: 12px; color: #606266; }
.segment-slot-profile { margin-top: 8px; padding-top: 8px; border-top: 1px dashed #e4e7ed; }
.slot-goal { font-size: 12px; color: #303133; font-weight: 600; line-height: 1.5; }
.slot-tags { margin-top: 6px; display: flex; flex-wrap: wrap; gap: 6px; }
.slot-notes { margin-top: 6px; font-size: 12px; color: #606266; line-height: 1.5; }
.candidate-item { border: 1px solid #e8eef5; border-radius: 8px; padding: 10px 12px; cursor: pointer; transition: all 0.2s ease; }
.candidate-item:hover { border-color: #409eff; background: #f5f9ff; }
.candidate-item-selected { border-color: #67c23a; background: #f3fbf3; }
.candidate-name { font-size: 13px; font-weight: 600; color: #303133; }
.candidate-meta { margin-top: 4px; display: flex; flex-direction: column; gap: 2px; font-size: 12px; color: #909399; }
.steps-section { min-height: 0; display: flex; flex-direction: column; }
.steps-list { margin-top: 12px; display: flex; flex-direction: column; gap: 10px; padding-right: 4px; }
.step-item { display: flex; gap: 10px; }
.step-num { width: 22px; height: 22px; border-radius: 50%; background: #ecf5ff; color: #409eff; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600; flex-shrink: 0; }
.step-content { flex: 1; min-width: 0; }
.step-instr { color: #303133; font-size: 13px; line-height: 1.5; }
.step-meta { color: #909399; font-size: 12px; margin-top: 2px; }
.suggestions-label { margin: 0; color: #606266; font-size: 13px; }
.suggestion-tag { cursor: pointer; }
.safety-section { margin-top: 12px; padding: 12px; background: rgba(255, 255, 255, 0.15); border-radius: 8px; }
.safety-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.safety-title { font-size: 13px; font-weight: 600; }
.safety-warnings { margin-bottom: 8px; }
.safety-warning-item { display: flex; align-items: flex-start; gap: 6px; font-size: 12px; margin-bottom: 4px; color: #fef0f0; }
.warning-icon { color: #f56c6c; margin-top: 2px; flex-shrink: 0; }
.safety-suggestions { margin-bottom: 8px; }
.safety-suggestion-item { display: flex; align-items: flex-start; gap: 6px; font-size: 12px; margin-bottom: 4px; color: rgba(255, 255, 255, 0.9); }
.suggestion-icon { color: #e6a23c; margin-top: 2px; flex-shrink: 0; }
.safety-alternative { display: flex; align-items: flex-start; gap: 6px; font-size: 12px; color: #67c23a; padding-top: 8px; border-top: 1px solid rgba(255, 255, 255, 0.2); }
/* 多路线选择样式 */
.route-selection-section { margin-top: 12px; padding: 12px; background: rgba(255, 255, 255, 0.15); border-radius: 8px; }
.route-selection-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.route-selection-title { font-size: 13px; font-weight: 600; }
.route-selection-reason { display: flex; align-items: flex-start; gap: 6px; font-size: 12px; color: rgba(255, 255, 255, 0.9); margin-bottom: 10px; }
.alternative-routes-list { display: flex; flex-direction: column; gap: 6px; }
.alternative-route-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid transparent;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease, transform 0.2s ease;
}
.alternative-route-item:hover,
.alternative-route-item:focus-visible {
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(255, 255, 255, 0.36);
  outline: none;
  transform: translateY(-1px);
}
.alternative-route-item.selected-route { background: rgba(103, 194, 58, 0.2); border: 1px solid rgba(103, 194, 58, 0.4); }
.route-label { font-weight: 600; min-width: 50px; }
.route-stats { flex: 1; display: flex; align-items: center; gap: 4px; color: rgba(255, 255, 255, 0.85); }
.route-stat-sep { opacity: 0.5; }
.route-score { flex-shrink: 0; }
.route-summary-text { width: 100%; color: rgba(255, 255, 255, 0.95); line-height: 1.5; }
.route-reason-text { width: 100%; color: rgba(255, 255, 255, 0.82); line-height: 1.5; }
/* 行程链路样式更新 */
.itinerary-header { display: flex; align-items: center; gap: 8px; }
.itinerary-item { border: 1px solid #e8eef5; border-radius: 8px; padding: 10px 12px; background: #fafcff; }
.segment-analysis { margin-top: 8px; display: flex; align-items: flex-start; gap: 8px; padding-top: 8px; border-top: 1px dashed #e4e7ed; }
.analysis-reason { font-size: 12px; color: #606266; line-height: 1.5; flex: 1; }
/* 综合评估样式 */
.overall-analysis-section { padding: 14px 16px; border-top: 1px solid #eef2f6; background: linear-gradient(135deg, #f0f7ff 0%, #e8f4ff 100%); }
.overall-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.overall-title { font-size: 14px; font-weight: 600; color: #303133; }
.overall-summary { font-size: 13px; color: #303133; line-height: 1.6; margin-bottom: 10px; }
.overall-keypoints { margin-bottom: 10px; }
.keypoint-item { display: flex; align-items: flex-start; gap: 6px; font-size: 12px; color: #606266; margin-bottom: 4px; }
.keypoint-icon { color: #409eff; margin-top: 2px; flex-shrink: 0; }
.overall-recommendation { display: flex; align-items: flex-start; gap: 6px; font-size: 12px; color: #67c23a; padding-top: 8px; border-top: 1px solid #d9ecff; }
.slide-right-enter-active, .slide-right-leave-active { transition: all 0.3s ease; }
.slide-right-enter-from, .slide-right-leave-to { opacity: 0; transform: translateX(20px); }

@media (max-width: 960px) {
  .top-bar.with-panel {
    right: 12px;
  }

  .route-panel {
    left: 12px;
    width: auto;
  }
}
</style>
