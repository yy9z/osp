<template>
  <div class="campus-map-container">
    <!-- 地图容器 -->
    <div ref="mapContainer" class="map-wrapper"></div>

    <!-- 搜索框（导航页通过 hideSearchBox 隐藏，避免重复功能） -->
    <div v-if="!hideSearchBox" class="map-search-box">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索校园地点"
        clearable
        @keyup.enter="handleSearch"
        @clear="clearSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <el-button @click="handleSearch">搜索</el-button>
        </template>
      </el-input>

      <!-- 搜索结果列表 -->
      <div v-if="searchResults.length > 0" class="search-results">
        <div
          v-for="(result, index) in searchResults"
          :key="index"
          class="search-result-item"
          @click="handleSelectSearchResult(result)"
        >
          <div class="result-name">{{ result.name }}</div>
          <div class="result-address">{{ result.address || '暂无地址' }}</div>
        </div>
      </div>
    </div>

    <!-- 地图控制按钮 -->
    <div class="map-controls">
      <el-button
        circle
        size="small"
        title="定位"
        @click="handleLocate"
      >
        <el-icon><Location /></el-icon>
      </el-button>
      <el-button
        circle
        size="small"
        title="放大"
        @click="handleZoomIn"
      >
        <el-icon><Plus /></el-icon>
      </el-button>
      <el-button
        circle
        size="small"
        title="缩小"
        @click="handleZoomOut"
      >
        <el-icon><Minus /></el-icon>
      </el-button>
      <el-button
        circle
        size="small"
        title="重新加载"
        @click="handleReload"
      >
        <el-icon><RefreshRight /></el-icon>
      </el-button>
    </div>

    <!-- 选点模式提示 -->
    <div v-if="selectPointMode" class="select-point-tip">
      <el-alert
        :title="selectPointTip"
        type="info"
        :closable="false"
        show-icon
      />
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="map-loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>{{ loadingText }}</span>
    </div>

    <!-- 地图底图错误提示 -->
    <div v-if="mapErrorMessage" class="map-error-overlay">
      <img class="map-error-bg" src="/images/campus-map.jpg" alt="校园地图底图" />
      <div class="map-error-card">
        <div class="map-error-title">地图底图加载失败</div>
        <div class="map-error-text">{{ mapErrorMessage }}</div>
        <div class="map-error-tip">请检查高德 Key、安全密钥与 Referer 白名单（localhost / 127.0.0.1）。</div>
        <el-button type="primary" size="small" @click="handleReload">重试</el-button>
      </div>
    </div>

    <!-- 用户位置标记 -->
    <div v-if="showUserLocation && userLocation" class="user-location-info">
      <el-tag type="success" size="small">
        <el-icon><Location /></el-icon>
        已定位到您当前位置
      </el-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search,
  Location,
  Plus,
  Minus,
  RefreshRight,
  Loading
} from '@element-plus/icons-vue'
import { navigationApi } from '@/api/navigation'
import type {
  Poi,
  PoiCategory,
  RouteResponse,
  RouteSegment,
  ResolvedPlace,
  UserLocation
} from '@/types/navigation'
import { USTC_MAP_CONFIG } from '@/types/navigation'
import {
  loadAMap,
  ensureAMapPlugins,
  createPoiMarker,
  createStartMarker,
  createEndMarker,
  createWaypointMarker,
  createUserLocationMarker,
  createRoutePolyline,
  createRouteMask,
  createInfoWindow,
  searchPlace,
  formatDistance,
  formatDuration,
  buildItineraryPoints,
  buildMultiSegmentPath
} from '@/utils/map'

// Props 定义
interface Props {
  height?: string
  showUserLocation?: boolean
  selectPointMode?: boolean
  selectPointType?: 'start' | 'end' | 'all'
  manualPath?: [number, number][] // 手动传入的路径数组，用于对接后端Dijkstra算法
  startName?: string // 起点名称
  endName?: string // 终点名称
  hideSearchBox?: boolean // 导航页使用自己的输入框时隐藏此搜索框
}

const props = withDefaults(defineProps<Props>(), {
  height: '100%',
  showUserLocation: false,
  selectPointMode: false,
  selectPointType: 'all',
  manualPath: () => [],
  startName: '起点',
  endName: '终点',
  hideSearchBox: false
})

// Emits 定义
const emit = defineEmits<{
  (e: 'poiClick', poi: Poi): void
  (e: 'routeCalculated', route: RouteResponse): void
  (e: 'locationChanged', location: UserLocation): void
  (e: 'mapLoaded'): void
  (e: 'pointSelected', data: { type: string; lat: number; lng: number; name: string }): void
}>()

// 地图实例
const map = ref<AMap.Map | null>(null)
const mapContainer = ref<HTMLElement | null>(null)

// 加载状态
const loading = ref(false)
const loadingText = ref('加载地图...')
const mapErrorMessage = ref('')
let mapTileProbeTimer: number | null = null

// 搜索相关
const searchKeyword = ref('')
const searchResults = ref<Array<{ name: string; address: string; location: { lat: number; lng: number } }>>([])

// 数据
const pois = ref<Poi[]>([])
const markers = ref<AMap.Marker[]>([])
const routeLine = ref<AMap.Polyline | null>(null)
const routeLines = ref<AMap.Polyline[]>([])
const startMarker = ref<AMap.Marker | null>(null)
const endMarker = ref<AMap.Marker | null>(null)
const waypointMarkers = ref<AMap.Marker[]>([])
const userLocationMarker = ref<AMap.Marker | null>(null)
const infoWindow = ref<AMap.InfoWindow | null>(null)

// 地图配置
const mapConfig = USTC_MAP_CONFIG
const amapKey = import.meta.env.VITE_AMAP_KEY

// 用户位置
const userLocation = ref<UserLocation | null>(null)

// 选点提示
const selectPointTip = ref('点击地图选择位置')
let mapCompleteHandler: (() => void) | null = null
let mapErrorHandler: ((event: AmapMapErrorEvent) => void) | null = null
let mapClickHandler: ((event: AmapMapClickEvent) => void) | null = null

const AMAP_ERROR_MESSAGES: Record<string, string> = {
  INVALID_USER_KEY: '高德 Key 无效',
  INVALID_USER_SCODE: '高德安全密钥错误',
  USERKEY_PLAT_NOMATCH: 'Key 平台类型不匹配（需 Web 端 JSAPI Key）',
  USER_DAILY_QUERY_OVER_LIMIT: '今日地图调用量超限',
  USER_IP_WHITE_LIST_ERROR: 'IP/Referer 白名单不匹配',
  JSAPI_INVALID_KEY: '高德 Key 无效',
  JSAPI_INVALID_USER_SCODE: '高德安全密钥错误'
}

type UnknownRecord = Record<string, unknown>
type AmapLngLatLike = { getLat?: () => number; getLng?: () => number; lat?: number; lng?: number }
type AmapMapErrorEvent = { info?: string; message?: string }
type AmapMapClickEvent = { lnglat?: { getLat: () => number; getLng: () => number } }
type AmapGeocoderResult = { regeocode?: { formattedAddress?: string }; info?: string; message?: string }
type AmapProbeResult = { info?: string; message?: string }
type AmapWalkingStep = { instruction?: string; distance?: number | string; duration?: number | string; path?: unknown[] }
type AmapWalkingRoute = { distance?: number | string; time?: number | string; steps?: AmapWalkingStep[] }
type AmapWalkingResult = { routes?: AmapWalkingRoute[]; info?: string; message?: string }
type AmapGeolocationResult = { position?: { getLat: () => number; getLng: () => number }; accuracy?: number; info?: string; message?: string }

const asRecord = (raw: unknown): UnknownRecord =>
  (raw && typeof raw === 'object') ? raw as UnknownRecord : {}

const resolveAmapErrorMessage = (raw: unknown): string => {
  const text = String(raw || '').toUpperCase()
  for (const [code, message] of Object.entries(AMAP_ERROR_MESSAGES)) {
    if (text.includes(code)) {
      return `${message}（${code}）`
    }
  }
  return String(raw || '未知错误')
}

const isAmapAuthError = (raw: unknown) => {
  const text = String(raw || '').toUpperCase()
  return /(INVALID_USER_|USERKEY_|SCODE|REFERER|JSAPI|DAILY_QUERY_OVER_LIMIT)/.test(text)
}

const ensurePluginReady = async (pluginName: string, checker: () => boolean) => {
  if (checker()) return true
  try {
    await ensureAMapPlugins(pluginName)
  } catch (error) {
    console.warn(`加载插件失败: ${pluginName}`, error)
  }
  return checker()
}

const clearMapTileProbeTimer = () => {
  if (mapTileProbeTimer !== null) {
    window.clearTimeout(mapTileProbeTimer)
    mapTileProbeTimer = null
  }
}

const reportAmapRuntimeError = (raw: unknown) => {
  if (mapErrorMessage.value) return
  const text = String(raw || '')
  const upper = text.toUpperCase()
  if (!/(AMAP|INVALID_USER_|USERKEY_|SCODE|REFERER|JSAPI|高德)/.test(`${upper}${text}`)) return
  mapErrorMessage.value = resolveAmapErrorMessage(text)
  loading.value = false
  ElMessage.error(`地图加载异常：${mapErrorMessage.value}`)
}

const cleanupPoiMarkers = () => {
  markers.value.forEach((marker) => marker.setMap(null))
  markers.value = []
}

const cleanupUserLocationMarker = () => {
  if (userLocationMarker.value) {
    userLocationMarker.value.setMap(null)
    userLocationMarker.value = null
  }
}

const detachMapListeners = () => {
  if (!map.value) return
  if (mapCompleteHandler) {
    map.value.off('complete', mapCompleteHandler)
    mapCompleteHandler = null
  }
  if (mapErrorHandler) {
    map.value.off('error', mapErrorHandler)
    mapErrorHandler = null
  }
  if (mapClickHandler) {
    map.value.off('click', mapClickHandler)
    mapClickHandler = null
  }
}

const handleWindowError = (event: ErrorEvent) => {
  const rawError = event?.error?.message || event?.message || event
  reportAmapRuntimeError(rawError)
}

const handleUnhandledRejection = (event: PromiseRejectionEvent) => {
  const reason = event?.reason as unknown
  const reasonRecord = asRecord(reason)
  const rawError = reasonRecord.message || reasonRecord.info || reason
  reportAmapRuntimeError(rawError)
}

const hasAmapTileTraffic = () => {
  try {
    const entries = performance.getEntriesByType('resource') as PerformanceResourceTiming[]
    return entries.some((entry) => {
      const name = (entry.name || '').toLowerCase()
      if (!/(autonavi|amap)\.com/.test(name)) return false
      return /(tile|maptile|vector|style|vt\?|sprite|roadnet|overlay)/.test(name)
    })
  } catch {
    return false
  }
}

const startMapTileProbe = () => {
  clearMapTileProbeTimer()
  mapTileProbeTimer = window.setTimeout(() => {
    if (mapErrorMessage.value || !map.value) return
    if (hasAmapTileTraffic()) return
    mapErrorMessage.value = '地图底图未成功加载（可能是 Key/SCode/Referer 或网络限制）'
    loading.value = false
    ElMessage.warning('地图底图未成功加载，请检查高德配置')
  }, 12000)
}

const probeAmapAvailability = async () => {
  if (mapErrorMessage.value) return
  const geocoderReady = await ensurePluginReady('AMap.Geocoder', () => Boolean(window.AMap?.Geocoder))
  if (!geocoderReady) return

  await new Promise<void>((resolve) => {
    let settled = false
    const done = () => {
      if (settled) return
      settled = true
      resolve()
    }

    const timeout = window.setTimeout(() => {
      // Geocoder 超时不一定意味着底图不可用，避免误判
      if (!mapErrorMessage.value && !hasAmapTileTraffic()) {
        mapErrorMessage.value = '地图服务响应超时（可能是网络或鉴权限制）'
      }
      done()
    }, 10000)

    try {
      const geocoder = new window.AMap.Geocoder()
      geocoder.getAddress(
        new window.AMap.LngLat(mapConfig.center[1], mapConfig.center[0]),
        (status: string, result: AmapProbeResult) => {
          window.clearTimeout(timeout)
          if (status !== 'complete') {
            const rawError = result?.info || result?.message || status
            // 仅对明确鉴权错误设置全屏错误提示
            if (isAmapAuthError(rawError)) {
              const friendly = resolveAmapErrorMessage(rawError)
              if (!mapErrorMessage.value) {
                mapErrorMessage.value = friendly
              }
            }
          }
          done()
        }
      )
    } catch (error) {
      window.clearTimeout(timeout)
      if (!mapErrorMessage.value) {
        mapErrorMessage.value = resolveAmapErrorMessage(error)
      }
      done()
    }
  })
}

/**
 * 初始化地图
 */
const initMap = async () => {
  if (!mapContainer.value) return

  try {
    clearMapTileProbeTimer()
    mapErrorMessage.value = ''
    loading.value = true
    loadingText.value = '加载高德地图...'

    // 加载高德地图 SDK
    await loadAMap()

    loadingText.value = '初始化地图...'

    // 创建地图实例
    map.value = new window.AMap.Map(mapContainer.value, {
      center: new window.AMap.LngLat(mapConfig.center[1], mapConfig.center[0]),
      zoom: mapConfig.zoom,
      minZoom: mapConfig.minZoom,
      maxZoom: mapConfig.maxZoom,
      viewMode: '2D',
      mapStyle: 'amap://styles/normal',
      showIndoorMap: false,
      zoomEnable: true,
      dragEnable: true,
      rotateEnable: false,
      pitchEnable: false,
      animateEnable: true,
      keyboardEnable: false
    })

    // 地图加载完成事件
    mapCompleteHandler = () => {
      emit('mapLoaded')
      loading.value = false
      // 兜底探测：有些黑屏场景不会触发 map error 事件
      probeAmapAvailability()
      // 二次兜底：检测是否有底图资源流量
      startMapTileProbe()
    }
    map.value.on('complete', mapCompleteHandler)

    // 关键：底图黑网格常见于 Key/SCode/白名单问题，直接给出可读错误
    mapErrorHandler = (e: AmapMapErrorEvent) => {
      const rawError = e?.info || e?.message || e
      mapErrorMessage.value = resolveAmapErrorMessage(rawError)
      clearMapTileProbeTimer()
      loading.value = false
      ElMessage.error(`地图加载异常：${mapErrorMessage.value}`)
      console.error('高德地图错误:', rawError)
    }
    map.value.on('error', mapErrorHandler)

    // 点击地图事件
    mapClickHandler = (e: AmapMapClickEvent) => {
      if (props.selectPointMode) {
        const lat = e?.lnglat?.getLat?.()
        const lng = e?.lnglat?.getLng?.()
        if (lat != null && lng != null) {
          handleMapClick(lat, lng)
        }
      }
    }
    map.value.on('click', mapClickHandler)

    // 仅加载轻量 UI 插件，重插件按需加载
    window.AMap.plugin(['AMap.ToolBar', 'AMap.Scale', 'AMap.Geolocation'], () => {
      if (map.value) {
        map.value.addControl(new window.AMap.ToolBar({
          position: 'RT',
          visible: false
        }))
        map.value.addControl(new window.AMap.Scale({
          position: 'LB'
        }))
      }
    })
  } catch (error) {
    console.error('初始化地图失败:', error)
    clearMapTileProbeTimer()
    mapErrorMessage.value = resolveAmapErrorMessage(error)
    ElMessage.error('地图加载失败，请检查配置')
    loading.value = false
  }
}

/**
 * 处理地图点击（选点模式）
 */
const handleMapClick = async (lat: number, lng: number) => {
  // 显示点击位置的信息窗口
  const address = await getAddressFromCoord(lng, lat)
  const markerContent = document.createElement('div')
  markerContent.className = 'click-marker-info'

  const clickInfo = document.createElement('div')
  clickInfo.className = 'click-info'

  const coordNode = document.createElement('div')
  coordNode.className = 'click-coord'
  coordNode.textContent = `坐标: ${lat.toFixed(6)}, ${lng.toFixed(6)}`

  const addressNode = document.createElement('div')
  addressNode.className = 'click-address'
  addressNode.textContent = address || '未知位置'

  const actionsNode = document.createElement('div')
  actionsNode.className = 'click-actions'

  const startButton = document.createElement('button')
  startButton.className = 'btn-set-start'
  startButton.type = 'button'
  startButton.textContent = '设为起点'
  startButton.addEventListener('click', () => {
    emit('pointSelected', { type: 'start', lat, lng, name: address || '选点位置' })
  })

  const endButton = document.createElement('button')
  endButton.className = 'btn-set-end'
  endButton.type = 'button'
  endButton.textContent = '设为终点'
  endButton.addEventListener('click', () => {
    emit('pointSelected', { type: 'end', lat, lng, name: address || '选点位置' })
  })

  actionsNode.appendChild(startButton)
  actionsNode.appendChild(endButton)
  clickInfo.appendChild(coordNode)
  clickInfo.appendChild(addressNode)
  clickInfo.appendChild(actionsNode)
  markerContent.appendChild(clickInfo)

  // 创建信息窗口
  if (infoWindow.value) {
    infoWindow.value.close()
  }

  infoWindow.value = new window.AMap.InfoWindow({
    isCustom: false,
    content: markerContent,
    offset: new window.AMap.Pixel(0, -30),
    closeWhenClickMap: true
  })

  infoWindow.value.open(map.value!, new window.AMap.LngLat(lng, lat))
}

/**
 * 从坐标获取地址
 */
const getAddressFromCoord = async (lng: number, lat: number): Promise<string> => {
  const geocoderReady = await ensurePluginReady('AMap.Geocoder', () => Boolean(window.AMap?.Geocoder))
  if (!geocoderReady) return ''

  return new Promise((resolve) => {
    if (!window.AMap.Geocoder) {
      resolve('')
      return
    }

    const geocoder = new window.AMap.Geocoder()
    geocoder.getAddress(new window.AMap.LngLat(lng, lat), (status: string, result: AmapGeocoderResult) => {
      if (status === 'complete' && result?.regeocode) {
        resolve(result.regeocode.formattedAddress || '')
      } else {
        resolve('')
      }
    })
  })
}

/**
 * 加载 POI 数据
 */
const loadPois = async () => {
  try {
    const res = await navigationApi.getPois()
    pois.value = res.data || []
    renderPoiMarkers()
  } catch (error) {
    console.error('加载POI失败:', error)
  }
}

/**
 * 渲染 POI 标记点
 */
const renderPoiMarkers = () => {
  if (!map.value) return

  // 清除旧标记
  markers.value.forEach((marker) => marker.setMap(null))
  markers.value = []

  // 创建新标记
  pois.value.forEach((poi) => {
    const marker = createPoiMarker(
      {
        latitude: poi.latitude,
        longitude: poi.longitude,
        name: poi.name,
        category: poi.category as PoiCategory
      },
      () => {
        emit('poiClick', poi)
        showPoiInfoWindow(poi)
      }
    )

    marker.setMap(map.value!)
    markers.value.push(marker)
  })
}

/**
 * 显示 POI 信息窗口
 */
const showPoiInfoWindow = (poi: Poi) => {
  if (!map.value) return

  const contentParts = [
    poi.description || '暂无描述',
    [poi.floor, poi.openTime].filter(Boolean).join(' · ')
  ].filter(Boolean)
  const content = contentParts.join(' ｜ ')

  if (infoWindow.value) {
    infoWindow.value.close()
  }

  infoWindow.value = createInfoWindow(poi.name, content)
  infoWindow.value.open(
    map.value,
    new window.AMap.LngLat(poi.longitude, poi.latitude)
  )
}

/**
 * 搜索地点
 */
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) return

  try {
    loading.value = true
    loadingText.value = '搜索中...'

    const results = await searchPlace(searchKeyword.value)
    searchResults.value = results
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

/**
 * 清除搜索
 */
const clearSearch = () => {
  searchResults.value = []
  searchKeyword.value = ''
}

/**
 * 选择搜索结果
 */
const handleSelectSearchResult = (result: { name: string; address: string; location: { lat: number; lng: number } }) => {
  // 在地图上标记
  if (map.value) {
    map.value.setCenter(new window.AMap.LngLat(result.location.lng, result.location.lat), true)
    map.value.setZoom(17)
  }

  // 清除之前的搜索结果
  searchResults.value = []

  // 显示信息窗口
  if (infoWindow.value) {
    infoWindow.value.close()
  }

  infoWindow.value = createInfoWindow(result.name, result.address || '暂无地址')
  infoWindow.value.open(map.value!, new window.AMap.LngLat(result.location.lng, result.location.lat))

  ElMessage.success(`已定位到: ${result.name}`)
}

/**
 * 路径规划选项
 */
interface RouteOptions {
  startName?: string
  endName?: string
  path?: [number, number][] // 手动传入的路径数组，用于对接后端Dijkstra算法
}

interface MultiSegmentRenderResult {
  mode: 'segment' | 'fallback_total' | 'failed'
  reason?: string
}

const ROUTE_MIN_ZOOM = mapConfig.minZoom
const MULTI_SEGMENT_COLORS = ['#3366FF', '#FF8C42', '#00A870', '#8E44AD']

const isValidRouteCoordinate = (lat: number, lng: number) => (
  Number.isFinite(lat)
  && Number.isFinite(lng)
  && lat >= -90
  && lat <= 90
  && lng >= -180
  && lng <= 180
  && !(lat === 0 && lng === 0)
)

const normalizeRoutePoint = (point: unknown): [number, number] | null => {
  let rawLat: number | undefined
  let rawLng: number | undefined

  if (Array.isArray(point) && point.length >= 2) {
    rawLat = Number(point[0])
    rawLng = Number(point[1])
  } else if (point && typeof point === 'object') {
    const objectPoint = point as UnknownRecord
    if (objectPoint.lat != null && objectPoint.lng != null) {
      rawLat = Number(objectPoint.lat)
      rawLng = Number(objectPoint.lng)
    } else if (objectPoint.latitude != null && objectPoint.longitude != null) {
      rawLat = Number(objectPoint.latitude)
      rawLng = Number(objectPoint.longitude)
    }
  }

  if (rawLat == null || rawLng == null) {
    return null
  }

  if (isValidRouteCoordinate(rawLat, rawLng)) {
    return [rawLat, rawLng]
  }

  if (isValidRouteCoordinate(rawLng, rawLat)) {
    console.warn('检测到疑似经纬度顺序颠倒，已自动纠正路线点', { rawLat, rawLng })
    return [rawLng, rawLat]
  }

  return null
}

const normalizeRoutePath = (path: [number, number][]) => {
  const normalized: [number, number][] = []
  path.forEach((point) => {
    const normalizedPoint = normalizeRoutePoint(point)
    if (!normalizedPoint) return
    const [lat, lng] = normalizedPoint
    const last = normalized[normalized.length - 1]
    if (last && last[0] === lat && last[1] === lng) return
    normalized.push([lat, lng])
  })
  return normalized
}

interface WalkingProbeLeg {
  label: string
  start: { lat: number; lng: number; name: string }
  end: { lat: number; lng: number; name: string }
  backendCandidateRoutes?: number
  backendSelectedRouteIndex?: number
  backendSelectionReason?: string
}

const lastWalkingProbeKey = ref('')

const ensureWalkingPluginReady = async () => {
  if (!window.AMap) return false
  if (window.AMap.Walking) return true

  try {
    await new Promise<void>((resolve) => {
      window.AMap.plugin('AMap.Walking', () => resolve())
    })
  } catch (error) {
    console.warn('加载 AMap.Walking probe 插件失败', error)
  }

  return Boolean(window.AMap.Walking)
}

const extractWalkingProbePath = (route: unknown): [number, number][] => {
  const routeRecord = asRecord(route)
  const fullPath: unknown[] = []
  if (Array.isArray(routeRecord.steps)) {
    routeRecord.steps.forEach((step) => {
      const stepRecord = asRecord(step)
      if (Array.isArray(stepRecord.path)) {
        stepRecord.path.forEach((point) => fullPath.push(point))
      }
    })
  }

  return fullPath
    .map((point) => {
      const pointRecord = asRecord(point) as AmapLngLatLike
      if (typeof pointRecord?.getLat === 'function' && typeof pointRecord?.getLng === 'function') {
        return [pointRecord.getLat(), pointRecord.getLng()] as [number, number]
      }
      if (pointRecord?.lat != null && pointRecord?.lng != null) {
        return [Number(pointRecord.lat), Number(pointRecord.lng)] as [number, number]
      }
      return null
    })
    .filter((point): point is [number, number] => Array.isArray(point) && Number.isFinite(point[0]) && Number.isFinite(point[1]))
}

const summarizeWalkingProbeRoutes = (routes: AmapWalkingRoute[] = []) => routes.map((route, index: number) => {
  const path = extractWalkingProbePath(route)
  return {
    index,
    distance: typeof route?.distance === 'number' ? route.distance : Number(route?.distance || 0),
    duration: typeof route?.time === 'number' ? route.time : Number(route?.time || 0),
    steps: Array.isArray(route?.steps) ? route.steps.length : 0,
    pathPoints: path.length,
    firstInstruction: Array.isArray(route?.steps) && route.steps[0]?.instruction ? String(route.steps[0].instruction) : ''
  }
})

const probeWalkingCandidates = async (
  context: 'single' | 'multi_waypoint',
  legs: WalkingProbeLeg[]
) => {
  if (!legs.length) return

  const probeKey = JSON.stringify({
    context,
    legs: legs.map((leg) => ({
      startLat: Number(leg.start.lat).toFixed(6),
      startLng: Number(leg.start.lng).toFixed(6),
      endLat: Number(leg.end.lat).toFixed(6),
      endLng: Number(leg.end.lng).toFixed(6),
      backendCandidateRoutes: leg.backendCandidateRoutes ?? null,
      backendSelectedRouteIndex: leg.backendSelectedRouteIndex ?? null
    }))
  })

  if (probeKey === lastWalkingProbeKey.value) {
    return
  }
  lastWalkingProbeKey.value = probeKey

  const ready = await ensureWalkingPluginReady()
  if (!ready) {
    console.warn('[amap-walking-probe] AMap.Walking not available')
    return
  }

  for (let i = 0; i < legs.length; i += 1) {
    const leg = legs[i]
    await new Promise<void>((resolve) => {
      const walking = new window.AMap.Walking({
        panel: '',
        map: null,
        hideMarkers: true,
        isOutline: false,
        outlineColor: '#ffffff',
        extensions: 'all'
      })

      walking.search(
        new window.AMap.LngLat(leg.start.lng, leg.start.lat),
        new window.AMap.LngLat(leg.end.lng, leg.end.lat),
        (status: string, result: AmapWalkingResult) => {
          const routes = Array.isArray(result?.routes) ? result.routes : []
          console.info('[amap-walking-probe]', JSON.stringify({
            context,
            legIndex: i,
            label: leg.label,
            from: leg.start.name,
            to: leg.end.name,
            status,
            backendCandidateRoutes: leg.backendCandidateRoutes ?? null,
            backendSelectedRouteIndex: leg.backendSelectedRouteIndex ?? null,
            backendSelectionReason: leg.backendSelectionReason ?? null,
            jsapiRoutes: routes.length,
            jsapiRouteSummaries: summarizeWalkingProbeRoutes(routes)
          }))
          if (typeof walking.clear === 'function') {
            walking.clear()
          }
          resolve()
        }
      )
    })
  }
}

const fitRouteToViewport = (
  path: [number, number][] = [],
  fallbackPoints: Array<{ lat: number; lng: number }> = []
) => {
  if (!map.value) return

  const normalizedPath = normalizeRoutePath(path)
  const normalizedFallback = fallbackPoints
    .map((point) => ({ lat: Number(point.lat), lng: Number(point.lng) }))
    .filter((point) => isValidRouteCoordinate(point.lat, point.lng))

  const points = normalizedPath.length
    ? normalizedPath.map(([lat, lng]) => ({ lat, lng }))
    : normalizedFallback

  if (!points.length) {
    map.value.setZoomAndCenter(mapConfig.zoom, new window.AMap.LngLat(mapConfig.center[1], mapConfig.center[0]))
    return
  }

  if (points.length === 1) {
    map.value.setZoomAndCenter(17, new window.AMap.LngLat(points[0].lng, points[0].lat))
    return
  }

  let minLat = points[0].lat
  let maxLat = points[0].lat
  let minLng = points[0].lng
  let maxLng = points[0].lng

  points.forEach(({ lat, lng }) => {
    minLat = Math.min(minLat, lat)
    maxLat = Math.max(maxLat, lat)
    minLng = Math.min(minLng, lng)
    maxLng = Math.max(maxLng, lng)
  })

  const centerLat = (minLat + maxLat) / 2
  const centerLng = (minLng + maxLng) / 2
  const maxDelta = Math.max(maxLat - minLat, maxLng - minLng)

  let zoom = 18
  if (maxDelta > 8) {
    zoom = 4
  } else if (maxDelta > 4) {
    zoom = 5
  } else if (maxDelta > 2) {
    zoom = 6
  } else if (maxDelta > 1) {
    zoom = 7
  } else if (maxDelta > 0.5) {
    zoom = 8
  } else if (maxDelta > 0.2) {
    zoom = 9
  } else if (maxDelta > 0.1) {
    zoom = 11
  } else if (maxDelta > 0.06) {
    zoom = 14
  } else if (maxDelta > 0.02) {
    zoom = 15
  } else if (maxDelta > 0.008) {
    zoom = 16
  } else if (maxDelta > 0.003) {
    zoom = 17
  }

  zoom = Math.max(ROUTE_MIN_ZOOM, Math.min(zoom, mapConfig.maxZoom))
  map.value.setZoomAndCenter(zoom, new window.AMap.LngLat(centerLng, centerLat))
}

/**
 * 路径规划
 * @param startLat 起点纬度
 * @param startLng 起点经度
 * @param endLat 终点纬度
 * @param endLng 终点经度
 * @param options 路径规划选项
 */
const calculateRoute = async (
  startLat: number,
  startLng: number,
  endLat: number,
  endLng: number,
  options: RouteOptions = {}
) => {
  try {
    loading.value = true
    loadingText.value = '规划路线...'

    // 清除旧路径
    clearRoute()

    let routeData: {
      distance: number
      duration: number
      path: [number, number][]
      steps: Array<{
        instruction: string
        distance: number
        duration: number
      }>
    }

    // 判断是使用手动路径还是调用后端API
    if (options.path && options.path.length > 0) {
      // 使用手动传入的路径（对接后端Dijkstra算法）
      console.log('使用手动路径进行路径渲染')
      routeData = await calculateManualRoute(
        [startLat, startLng],
        [endLat, endLng],
        options.path
      )
    } else {
      // 优先使用后端代理的高德地图路径规划API
      try {
        console.log('调用后端高德路径规划代理API...')
        const backendResponse = await navigationApi.calculateRouteAmap({
          fromLat: startLat,
          fromLng: startLng,
          toLat: endLat,
          toLng: endLng,
          wayType: 'WALK'
        })
        
        if (backendResponse.code === 200 && backendResponse.data) {
          const data = asRecord(backendResponse.data)
          console.log('后端高德路径规划成功，点数:', data.path?.length, '距离:', data.distance)
          
          routeData = {
            distance: Number(data.distance || 0),
            duration: Number(data.duration || 0),
            path: Array.isArray(data.path)
              ? data.path.map((p) => {
                  if (Array.isArray(p) && p.length >= 2) {
                    return [Number(p[0]), Number(p[1])] as [number, number]
                  }
                  return null
                }).filter((p): p is [number, number] => p !== null)
              : [],
            steps: Array.isArray(data.steps)
              ? data.steps.map((s, idx: number) => {
                  const step = asRecord(s)
                  return {
                    instruction: String(step.instruction || `第 ${idx + 1} 步`),
                    distance: Number(step.distance || 0),
                    duration: Number(step.duration || 0)
                  }
                })
              : []
          }
          
          if (!routeData.path || routeData.path.length === 0) {
            throw new Error('后端返回的路径数据为空')
          }
        } else {
          throw new Error(backendResponse.message || '后端路径规划失败')
        }
      } catch (backendError: unknown) {
        const errorRecord = asRecord(backendError)
        console.error('后端高德路径规划失败:', errorRecord.message || backendError)
        ElMessage.error('路线规划失败，请稍后重试')
        loading.value = false
        return
      }
    }

    const normalizedPath = normalizeRoutePath(routeData.path || [])
    const pathPoints = normalizedPath.length
      ? normalizedPath
      : [[startLat, startLng], [endLat, endLng]] as [number, number][]

    // 绘制路径
    drawRoute(normalizedPath)

    // 添加起点/终点标记
    addStartEndMarkers(
      [startLat, startLng],
      [endLat, endLng],
      options.startName || '起点',
      options.endName || '终点'
    )

    fitRouteToViewport(normalizedPath, [
      { lat: startLat, lng: startLng },
      { lat: endLat, lng: endLng }
    ])

    const stepsWithPoints = routeData.steps.map((step, index) => {
      const startIdx = Math.min(
        Math.floor((index / routeData.steps.length) * pathPoints.length),
        pathPoints.length - 1
      )
      const endIdx = Math.min(
        Math.floor(((index + 1) / routeData.steps.length) * pathPoints.length),
        pathPoints.length - 1
      )
      return {
        instruction: step.instruction,
        distance: step.distance,
        duration: step.duration,
        startPoint: pathPoints[startIdx] || [startLat, startLng],
        endPoint: pathPoints[endIdx] || [endLat, endLng]
      }
    })

    // 格式化返回数据
    const routeResponse: RouteResponse = {
      distance: routeData.distance,
      duration: routeData.duration,
      path: normalizedPath,
      steps: stepsWithPoints
    }

    // 显示路径信息
    showRouteInfo(routeResponse)

    emit('routeCalculated', routeResponse)
    
    ElMessage.success(`路线规划成功，总距离${formatDistance(routeData.distance)}，预计步行${formatDuration(routeData.duration)}`)
  } catch (error: unknown) {
    const errorRecord = asRecord(error)
    console.error('路径规划失败:', error)
    ElMessage.error(String(errorRecord?.message || '路径规划失败，请稍后重试'))
  } finally {
    loading.value = false
  }
}

/**
 * 计算手动路径的距离和耗时
 */
const calculateManualRoute = async (
  start: [number, number],
  end: [number, number],
  path: [number, number][]
): Promise<{
  distance: number
  duration: number
  path: [number, number][]
  steps: Array<{
    instruction: string
    distance: number
    duration: number
  }>
}> => {
  // 计算路径总距离
  let totalDistance = 0
  const distances: number[] = []
  
  for (let i = 0; i < path.length - 1; i++) {
    const d = calculateDistance(path[i], path[i + 1])
    distances.push(d)
    totalDistance += d
  }

  // 添加起点到第一个点的距离
  const firstLeg = calculateDistance(start, path[0])
  totalDistance += firstLeg

  // 添加最后一个点到终点的距离
  const lastLeg = calculateDistance(path[path.length - 1], end)
  totalDistance += lastLeg

  // 平均步行速度 1.2m/s
  const walkingSpeed = 1.2
  const duration = Math.round(totalDistance / walkingSpeed)

  // 生成导航步骤
  const steps: Array<{ instruction: string; distance: number; duration: number }> = []
  
  if (path.length > 2) {
    steps.push({
      instruction: '出发，沿路径步行',
      distance: Math.round(totalDistance * 0.3),
      duration: Math.round((totalDistance * 0.3) / walkingSpeed)
    })
    steps.push({
      instruction: '继续沿路径前行',
      distance: Math.round(totalDistance * 0.4),
      duration: Math.round((totalDistance * 0.4) / walkingSpeed)
    })
    steps.push({
      instruction: '到达目的地',
      distance: Math.round(totalDistance * 0.3),
      duration: Math.round((totalDistance * 0.3) / walkingSpeed)
    })
  } else {
    steps.push({
      instruction: '出发，向目标位置步行',
      distance: totalDistance,
      duration
    })
  }

  return {
    distance: totalDistance,
    duration,
    path,
    steps
  }
}

/**
 * 计算两点之间的距离（米）- Haversine公式
 */
const calculateDistance = (
  point1: [number, number],
  point2: [number, number]
): number => {
  const R = 6371000 // 地球半径（米）
  const lat1 = (point1[0] * Math.PI) / 180
  const lat2 = (point2[0] * Math.PI) / 180
  const deltaLat = ((point2[0] - point1[0]) * Math.PI) / 180
  const deltaLng = ((point2[1] - point1[1]) * Math.PI) / 180

  const a =
    Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
    Math.cos(lat1) *
      Math.cos(lat2) *
      Math.sin(deltaLng / 2) *
      Math.sin(deltaLng / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c
}

/**
 * 显示路径信息
 */
const showRouteInfo = (route: RouteResponse) => {
  console.log('路径规划结果:', {
    distance: formatDistance(route.distance),
    duration: formatDuration(route.duration),
    steps: route.steps.length,
    pathPoints: route.path.length
  })
}

// 路径遮罩层
const routeMask = ref<AMap.Polyline | null>(null)

/**
 * 绘制路径
 */
const drawRoute = (path: [number, number][]) => {
  if (!map.value) return

  const normalizedPath = normalizeRoutePath(path)

  // 清除旧的路径
  routeLines.value.forEach((line) => line.setMap(null))
  routeLines.value = []
  if (routeLine.value) {
    routeLine.value.setMap(null)
    routeLine.value = null
  }

  // 清除旧的遮罩层
  if (routeMask.value) {
    routeMask.value.setMap(null)
    routeMask.value = null
  }

  if (normalizedPath.length < 2) {
    return
  }

  // 先绘制白色遮罩层（提高可见度）
  routeMask.value = createRouteMask(normalizedPath)
  routeMask.value.setMap(map.value)

  // 再绘制主路径
  routeLine.value = createRoutePolyline(normalizedPath)
  routeLine.value.setMap(map.value)
  routeLines.value = [routeLine.value]
}

const renderMultiSegmentRoute = (
  segments: RouteSegment[],
  itineraryPoints: ResolvedPlace[],
  fallbackPath: [number, number][] = []
): MultiSegmentRenderResult => {
  if (!map.value) {
    return {
      mode: 'failed',
      reason: '地图尚未初始化，暂时无法绘制路线'
    }
  }

  clearRoute()

  const segmentPaths = segments
    .map((segment, index) => ({
      path: normalizeRoutePath(segment.path || []),
      color: MULTI_SEGMENT_COLORS[index % MULTI_SEGMENT_COLORS.length]
    }))
    .filter((segment) => segment.path.length > 1)

  const rawSegmentPointCount = segments.reduce((sum, segment) => (
    sum + (Array.isArray(segment.path) ? segment.path.length : 0)
  ), 0)
  const fullPath = normalizeRoutePath(buildMultiSegmentPath(segments))
  const mergedPath = normalizeRoutePath(fallbackPath)
  const rawTotalPathCount = Array.isArray(fallbackPath) ? fallbackPath.length : 0
  const hasSegmentGeometry = segmentPaths.length > 0 && fullPath.length > 1
  const shouldFallbackToTotalPath = !hasSegmentGeometry && mergedPath.length > 1
  const effectivePath = hasSegmentGeometry ? fullPath : mergedPath
  let renderResult: MultiSegmentRenderResult = { mode: 'segment' }

  if (shouldFallbackToTotalPath) {
    const diagnostics = {
      segmentCount: segments.length,
      rawSegmentPointCount,
      validSegmentCount: segmentPaths.length,
      fullPathPoints: fullPath.length,
      rawTotalPathCount,
      totalPathPoints: mergedPath.length
    }
    renderResult = {
      mode: 'fallback_total',
      reason: '分段路径数据不完整，已退化为总路径规划展示'
    }
    console.warn('分段路径为空，回退使用总路径绘制', diagnostics)
    console.warn('[map] route fallback diagnostics', JSON.stringify(diagnostics))
  } else if (!hasSegmentGeometry) {
    const diagnostics = {
      segmentCount: segments.length,
      rawSegmentPointCount,
      validSegmentCount: segmentPaths.length,
      fullPathPoints: fullPath.length,
      rawTotalPathCount,
      totalPathPoints: mergedPath.length,
      rawSegmentSample: segments.slice(0, 2).map((segment, index) => ({
        index,
        sample: Array.isArray(segment.path) ? segment.path.slice(0, 2) : segment.path
      })),
      rawTotalSample: Array.isArray(fallbackPath) ? fallbackPath.slice(0, 2) : fallbackPath
    }
    const formatMismatchHint = rawSegmentPointCount > 1 || rawTotalPathCount > 1
      ? '后端虽然返回了路径，但坐标在前端标准化后全部失效，通常是经纬度顺序或坐标结构不符合约定'
      : '分段路径和总路径都不可用，仅展示起终点与途经点标记'
    renderResult = {
      mode: 'failed',
      reason: formatMismatchHint
    }
    console.warn('分段路径和总路径都为空，仅展示起终点标记', diagnostics)
    console.warn('[map] route failure diagnostics', JSON.stringify(diagnostics))
  }

  if (effectivePath.length > 1) {
    routeMask.value = createRouteMask(effectivePath)
    routeMask.value.setMap(map.value)
  }

  routeLines.value = segmentPaths
    .map((segment) => createRoutePolyline(segment.path, {
      strokeColor: segment.color,
      strokeWeight: 6,
      strokeOpacity: 0.9
    }))
    .filter((line) => !!line)

  if (!routeLines.value.length && effectivePath.length > 1) {
    routeLines.value = [createRoutePolyline(effectivePath, {
      strokeColor: '#3366FF',
      strokeWeight: 6,
      strokeOpacity: 0.9
    })]
  }

  routeLines.value.forEach((line) => line.setMap(map.value!))
  routeLine.value = routeLines.value[0] || null

  const [origin, ...rest] = itineraryPoints
  const destination = rest[rest.length - 1]
  const waypoints = rest.slice(0, -1)

  if (origin) {
    startMarker.value = createStartMarker([origin.lat, origin.lng], origin.name)
    startMarker.value.setMap(map.value)
  }

  if (destination) {
    endMarker.value = createEndMarker([destination.lat, destination.lng], destination.name)
    endMarker.value.setMap(map.value)
  }

  waypointMarkers.value = waypoints.map((waypoint, index) => {
    const marker = createWaypointMarker([waypoint.lat, waypoint.lng], waypoint.name, index)
    marker.setMap(map.value!)
    return marker
  })

  const probeLegs = itineraryPoints.slice(0, -1).map((point, index) => {
    const nextPoint = itineraryPoints[index + 1]
    const segment = segments[index]
    return nextPoint ? {
      label: `leg=${index}`,
      start: { lat: point.lat, lng: point.lng, name: point.name },
      end: { lat: nextPoint.lat, lng: nextPoint.lng, name: nextPoint.name },
      backendCandidateRoutes: segment?.totalRoutes,
      backendSelectedRouteIndex: segment?.selectedRouteIndex,
      backendSelectionReason: segment?.selectionReason
    } satisfies WalkingProbeLeg : null
  }).filter((leg): leg is WalkingProbeLeg => leg !== null)

  void probeWalkingCandidates('multi_waypoint', probeLegs)
  fitRouteToViewport(effectivePath, itineraryPoints)
  return renderResult
}

const renderResolvedRoute = (
  route: RouteResponse,
  start: { lat: number; lng: number; name: string },
  end: { lat: number; lng: number; name: string }
) => {
  if (!map.value) return

  const normalizedPath = normalizeRoutePath(route.path || [])
  clearRoute()
  drawRoute(normalizedPath)
  addStartEndMarkers([start.lat, start.lng], [end.lat, end.lng], start.name, end.name)
  fitRouteToViewport(normalizedPath, [
    { lat: start.lat, lng: start.lng },
    { lat: end.lat, lng: end.lng }
  ])
  void probeWalkingCandidates('single', [{
    label: 'single_leg',
    start,
    end,
    backendCandidateRoutes: route.totalRoutes,
    backendSelectedRouteIndex: route.selectedRouteIndex,
    backendSelectionReason: route.selectionReason
  }])
  showRouteInfo({
    ...route,
    path: normalizedPath
  })
  emit('routeCalculated', {
    ...route,
    path: normalizedPath
  })
}

/**
 * 添加起点/终点标记
 */
const addStartEndMarkers = (
  start: [number, number],
  end: [number, number],
  startName: string,
  endName: string
) => {
  if (!map.value) return

  // 起点
  startMarker.value = createStartMarker(start, startName)
  startMarker.value.setMap(map.value)

  // 终点
  endMarker.value = createEndMarker(end, endName)
  endMarker.value.setMap(map.value)
}

/**
 * 清除路径
 */
const clearRoute = () => {
  if (routeMask.value) {
    routeMask.value.setMap(null)
    routeMask.value = null
  }

  routeLines.value.forEach((line) => line.setMap(null))
  routeLines.value = []

  if (routeLine.value) {
    routeLine.value.setMap(null)
    routeLine.value = null
  }

  if (startMarker.value) {
    startMarker.value.setMap(null)
    startMarker.value = null
  }

  if (endMarker.value) {
    endMarker.value.setMap(null)
    endMarker.value = null
  }

  waypointMarkers.value.forEach((marker) => marker.setMap(null))
  waypointMarkers.value = []
}

/**
 * 定位用户当前位置
 */
const applyUserLocation = (lat: number, lng: number, accuracy?: number) => {
  if (map.value) {
    if (userLocationMarker.value) {
      userLocationMarker.value.setMap(null)
    }

    userLocationMarker.value = createUserLocationMarker([lat, lng])
    userLocationMarker.value.setMap(map.value)
    map.value.setCenter(new window.AMap.LngLat(lng, lat), true)
    map.value.setZoom(17)
  }

  userLocation.value = {
    lat,
    lng,
    accuracy,
    timestamp: Date.now()
  }

  emit('locationChanged', userLocation.value)
}

const locateByAMap = async (): Promise<{ lat: number; lng: number; accuracy?: number }> => {
  const geolocationReady = await ensurePluginReady('AMap.Geolocation', () => Boolean(window.AMap?.Geolocation))
  if (!geolocationReady || !window.AMap.Geolocation) {
    throw new Error('定位插件未加载')
  }

  return new Promise((resolve, reject) => {
    const geolocation = new window.AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 0,
      convert: true,
      showButton: false,
      showMarker: false,
      showCircle: false,
      panToLocation: true,
      zoomToAccuracy: false
    })

    geolocation.getCurrentPosition((status: string, result: AmapGeolocationResult) => {
      if (status === 'complete' && result?.position) {
        resolve({
          lat: result.position.getLat(),
          lng: result.position.getLng(),
          accuracy: result.accuracy
        })
        return
      }

      reject(new Error(result?.info || result?.message || '高德定位失败'))
    })
  })
}

const locateByBrowser = async (): Promise<{ lat: number; lng: number; accuracy?: number }> => {
  if (!navigator.geolocation) {
    throw new Error('浏览器不支持定位')
  }

  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        resolve({
          lat: position.coords.latitude,
          lng: position.coords.longitude,
          accuracy: position.coords.accuracy
        })
      },
      (error) => {
        const messageMap: Record<number, string> = {
          1: '定位权限被拒绝，请在浏览器中允许定位',
          2: '无法获取当前位置（定位服务不可用）',
          3: '定位超时，请稍后重试'
        }
        reject(new Error(messageMap[error.code] || '浏览器定位失败'))
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      }
    )
  })
}

const handleLocate = async () => {
  try {
    loading.value = true
    loadingText.value = '定位中...'

    try {
      const location = await locateByAMap()
      applyUserLocation(location.lat, location.lng, location.accuracy)
      ElMessage.success('定位成功')
      return
    } catch (amapError) {
      console.warn('高德定位失败，尝试浏览器定位:', amapError)
    }

    const browserLocation = await locateByBrowser()
    applyUserLocation(browserLocation.lat, browserLocation.lng, browserLocation.accuracy)
    ElMessage.success('已使用浏览器定位')
  } catch (error) {
    console.error('定位失败:', error)
    ElMessage.error((error as Error).message || '定位失败，请检查定位权限')
  } finally {
    loading.value = false
  }
}

/**
 * 放大地图
 */
const handleZoomIn = () => {
  if (map.value) {
    map.value.setZoom(map.value.getZoom() + 1)
  }
}

/**
 * 缩小地图
 */
const handleZoomOut = () => {
  if (map.value) {
    map.value.setZoom(map.value.getZoom() - 1)
  }
}

/**
 * 重新加载地图
 */
const handleReload = async () => {
  mapErrorMessage.value = ''
  loading.value = true
  loadingText.value = '刷新地图...'
  clearMapTileProbeTimer()

  clearRoute()
  cleanupPoiMarkers()
  cleanupUserLocationMarker()

  if (map.value) {
    detachMapListeners()
    map.value.destroy()
    map.value = null
  }

  try {
    await initMap()
    if (!mapErrorMessage.value) {
      ElMessage.success('地图已刷新')
    }
  } finally {
    loading.value = false
  }
}

/**
 * 附近搜索
 */
const searchNearby = async (lat: number, lng: number, radius: number = 500) => {
  try {
    loading.value = true
    const res = await navigationApi.getNearby({ lat, lng, radius })
    return res.data || []
  } catch (error) {
    console.error('附近搜索失败:', error)
    return []
  } finally {
    loading.value = false
  }
}

/**
 * 飞转到指定位置
 */
const flyTo = (lat: number, lng: number, zoom?: number) => {
  if (map.value) {
    map.value.setCenter(new window.AMap.LngLat(lng, lat))
    if (zoom) {
      map.value.setZoom(zoom)
    }
  }
}

/**
 * 聚焦到指定 POI
 */
const focusOnPoi = (poi: Poi) => {
  flyTo(poi.latitude, poi.longitude, 18)
  showPoiInfoWindow(poi)
}

// 暴露方法给父组件
defineExpose({
  calculateRoute,
  renderResolvedRoute,
  renderMultiSegmentRoute,
  clearRoute,
  handleLocate,
  searchNearby,
  flyTo,
  focusOnPoi,
  handleReload,
  getMap: () => map.value,
  getPois: () => pois.value,
  setTemporaryPlace: (place: { lat: number; lng: number; name: string; address?: string }) => {
    if (!map.value) return
    markers.value.forEach((marker) => marker.setMap(null))
    markers.value = []
    const marker = new window.AMap.Marker({
      position: new window.AMap.LngLat(place.lng, place.lat),
      title: place.name,
      anchor: 'bottom-center'
    })
    marker.setMap(map.value)
    markers.value.push(marker)
    if (infoWindow.value) infoWindow.value.close()
    infoWindow.value = createInfoWindow(place.name, place.address || '中国科学技术大学校园地点')
    infoWindow.value.open(map.value, new window.AMap.LngLat(place.lng, place.lat))
    flyTo(place.lat, place.lng, 18)
  }
})

// 监听选点模式
watch(() => props.selectPointMode, (newVal) => {
  if (newVal) {
    selectPointTip.value = props.selectPointType === 'start'
      ? '请在地图上点击选择起点'
      : props.selectPointType === 'end'
        ? '请在地图上点击选择终点'
        : '请在地图上点击选择位置'
  }
})

// 生命周期
onMounted(() => {
  window.addEventListener('error', handleWindowError, true)
  window.addEventListener('unhandledrejection', handleUnhandledRejection)
  initMap()
})

// 监听手动路径变化，自动触发路径规划
watch(
  () => props.manualPath,
  (newPath) => {
    if (newPath && newPath.length > 0) {
      console.log('检测到手动路径变化，自动触发路径规划')
    }
  },
  { deep: true }
)

// 监听起点/终点名称变化
watch(
  [() => props.startName, () => props.endName],
  ([newStartName, newEndName]) => {
    console.log('起点/终点名称变化:', newStartName, newEndName)
  }
)

onUnmounted(() => {
  clearMapTileProbeTimer()
  window.removeEventListener('error', handleWindowError, true)
  window.removeEventListener('unhandledrejection', handleUnhandledRejection)
  clearRoute()
  cleanupPoiMarkers()
  cleanupUserLocationMarker()
  if (infoWindow.value) {
    infoWindow.value.close()
    infoWindow.value = null
  }

  if (map.value) {
    detachMapListeners()
    map.value.destroy()
    map.value = null
  }
})
</script>

<style scoped>
.campus-map-container {
  position: relative;
  width: 100%;
  height: v-bind(height);
  border-radius: 8px;
  overflow: hidden;
}

.map-wrapper {
  width: 100%;
  height: 100%;
}

/* 搜索框 */
.map-search-box {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 1000;
  width: 300px;
}

.map-search-box :deep(.el-input-group) {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.search-results {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  max-height: 300px;
  overflow-y: auto;
  background: white;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  margin-top: 4px;
}

.search-result-item {
  padding: 10px 12px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-item:hover {
  background: #f5f7fa;
}

.result-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.result-address {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

/* 地图控制按钮 */
.map-controls {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.map-controls .el-button {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.map-controls .el-button:hover {
  background: #f5f7fa;
}

/* 选点提示 */
.select-point-tip {
  position: absolute;
  top: 60px;
  left: 10px;
  z-index: 1000;
  max-width: 280px;
}

/* 加载状态 */
.map-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 1001;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
}

.map-loading .el-icon {
  font-size: 20px;
  color: #409eff;
}

.map-error-overlay {
  position: absolute;
  inset: 0;
  z-index: 1002;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(10, 20, 40, 0.55);
}

.map-error-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.2;
}

.map-error-card {
  position: relative;
  max-width: 540px;
  margin: 0 20px;
  padding: 18px 20px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.2);
}

.map-error-title {
  margin-bottom: 6px;
  font-size: 16px;
  font-weight: 700;
  color: #1f2d3d;
}

.map-error-text {
  margin-bottom: 8px;
  font-size: 14px;
  color: #c45656;
}

.map-error-tip {
  margin-bottom: 12px;
  font-size: 12px;
  color: #606266;
}

/* 用户位置信息 */
.user-location-info {
  position: absolute;
  bottom: 20px;
  left: 20px;
  z-index: 1000;
}

.user-location-info .el-tag {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>

<style>
/* 全局样式 - Marker 自定义 */

/* POI 标记 */
.amap-marker-custom {
  position: relative;
}

.amap-marker-custom .marker-pin {
  width: 30px;
  height: 30px;
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  position: absolute;
  bottom: 0;
  left: 50%;
  margin-left: -15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
}

.amap-marker-custom .marker-center {
  width: 10px;
  height: 10px;
  background: white;
  border-radius: 50%;
  transform: rotate(45deg);
}

/* 路径标记 */
.amap-route-marker {
  position: relative;
}

.amap-route-marker .marker-label {
  position: absolute;
  top: -24px;
  left: 50%;
  transform: translateX(-50%);
  background: white;
  color: #333;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
}

.amap-route-marker .marker-pin {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 3px solid white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

/* 用户位置标记 */
.amap-user-location {
  position: relative;
  width: 24px;
  height: 24px;
}

.amap-user-location .user-pulse {
  position: absolute;
  width: 24px;
  height: 24px;
  background: rgba(103, 194, 58, 0.3);
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.amap-user-location .user-dot {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 12px;
  height: 12px;
  background: #67c23a;
  border: 2px solid white;
  border-radius: 50%;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  100% {
    transform: scale(2.5);
    opacity: 0;
  }
}

/* 信息窗口样式 */
.amap-info-window {
  padding: 12px;
  min-width: 180px;
}

.amap-info-window .info-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.amap-info-window .info-content {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
}

/* 点击选点信息 */
.click-marker-info {
  padding: 8px;
  min-width: 200px;
}

.click-marker-info .click-coord {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.click-marker-info .click-address {
  font-size: 13px;
  color: #333;
  margin-bottom: 8px;
}

.click-marker-info .click-actions {
  display: flex;
  gap: 8px;
}

.click-marker-info .click-actions button {
  flex: 1;
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.2s;
}

.click-marker-info .btn-set-start {
  background: #67c23a;
  color: white;
}

.click-marker-info .btn-set-start:hover {
  background: #85ce61;
}

.click-marker-info .btn-set-end {
  background: #f56c6c;
  color: white;
}

.click-marker-info .btn-set-end:hover {
  background: #f78989;
}

/* POI 信息窗口 */
.poi-info-window h4 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #333;
}

.poi-info-window p {
  margin: 0 0 8px;
  font-size: 13px;
  color: #666;
}

.poi-info-window .poi-meta {
  font-size: 12px;
  color: #999;
}

.poi-info-window .poi-meta span {
  margin-right: 12px;
}
</style>
