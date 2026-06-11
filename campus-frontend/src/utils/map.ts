/**
 * 校园地图工具函数 - 高德地图版本
 */
import type { PoiCategory, MapConfig, ResolvedPlace, RouteSegment } from '@/types/navigation'

// 高德地图类型声明
declare global {
  interface Window {
    AMap: typeof AMap
    AMapUI: typeof AMapUI
    _AMapSecurityConfig?: {
      securityJsCode: string
    }
  }
}

// 高德地图 SDK 地址 - 使用 2.0 版本
const AMAP_SDK_URL = 'https://webapi.amap.com/maps?v=2.0&key='
const AMAP_SCRIPT_ID = 'amap-jsapi-script'
let amapLoadPromise: Promise<void> | null = null
const loadedPluginSet = new Set<string>()

type MarkerPoi = { id: number; name: string; latitude: number; longitude: number; category: PoiCategory }
type LngLatLike = { getLat?: () => number; getLng?: () => number; lat?: number; lng?: number }
type GeolocationResult = { position?: LngLatLike; info?: string; message?: string }
type GeocodeResult = { geocodes?: Array<{ location?: LngLatLike }> }
type ReverseGeocodeResult = { regeocode?: { formattedAddress?: string } }
type PlaceSearchPoi = { name?: string; address?: string; location?: LngLatLike }
type PlaceSearchResult = { poiList?: { pois?: PlaceSearchPoi[] } }
type WalkingStepResult = { instruction?: string; distance?: number; duration?: number; path?: unknown[] }
type WalkingRouteResult = { distance?: number; time?: number; steps?: WalkingStepResult[] }
type WalkingSearchResult = { routes?: WalkingRouteResult[]; info?: string; message?: string }

// 初始化安全配置（必须在加载高德地图之前配置）
function initSecurityConfig() {
  const securityCode = import.meta.env.VITE_AMAP_SECURITY_CODE
  if (securityCode && typeof window !== 'undefined') {
    window._AMapSecurityConfig = {
      securityJsCode: securityCode
    }
    console.log('高德地图安全配置已加载')
  }
}

function appendPreconnect(host: string) {
  if (typeof document === 'undefined') return
  if (document.querySelector(`link[data-amap-preconnect="${host}"]`)) return
  const link = document.createElement('link')
  link.rel = 'preconnect'
  link.href = host
  link.crossOrigin = 'anonymous'
  link.setAttribute('data-amap-preconnect', host)
  document.head.appendChild(link)
}

function getPluginChecker(plugin: string): (() => boolean) | null {
  const checkerMap: Record<string, () => boolean> = {
    'AMap.Geocoder': () => Boolean(window.AMap?.Geocoder),
    'AMap.PlaceSearch': () => Boolean(window.AMap?.PlaceSearch),
    'AMap.Geolocation': () => Boolean(window.AMap?.Geolocation),
    'AMap.Walking': () => Boolean(window.AMap?.Walking),
    'AMap.ToolBar': () => Boolean(window.AMap?.ToolBar),
    'AMap.Scale': () => Boolean(window.AMap?.Scale)
  }
  return checkerMap[plugin] || null
}

// 加载高德地图 SDK
export function loadAMap(): Promise<void> {
  // 先初始化安全配置（必须在加载地图之前）
  initSecurityConfig()

  if (window.AMap) {
    return Promise.resolve()
  }

  if (amapLoadPromise) {
    return amapLoadPromise
  }

  const amapKey = import.meta.env.VITE_AMAP_KEY
  if (!amapKey || amapKey === '您的高德地图Web端Key') {
    return Promise.reject(new Error('请配置高德地图 Key'))
  }

  // 优先建立连接，减少 TLS 与 DNS 耗时
  appendPreconnect('https://webapi.amap.com')

  amapLoadPromise = new Promise((resolve, reject) => {
    const finishSuccess = () => {
      if (window.AMap) {
        resolve()
      } else {
        amapLoadPromise = null
        reject(new Error('高德地图加载失败'))
      }
    }

    const finishError = () => {
      amapLoadPromise = null
      reject(new Error('高德地图脚本加载失败'))
    }

    const existingScript = document.getElementById(AMAP_SCRIPT_ID) as HTMLScriptElement | null
    if (existingScript) {
      if (window.AMap) {
        finishSuccess()
      } else {
        existingScript.addEventListener('load', finishSuccess, { once: true })
        existingScript.addEventListener('error', finishError, { once: true })
      }
      return
    }

    // 首屏只加载核心 SDK，插件按需加载，减少初始化时延
    const scriptSrc = `${AMAP_SDK_URL}${amapKey}`
    const script = document.createElement('script')
    script.id = AMAP_SCRIPT_ID
    script.src = scriptSrc
    script.async = true
    script.defer = true
    script.onload = finishSuccess
    script.onerror = finishError
    document.head.appendChild(script)
  })

  return amapLoadPromise
}

export async function ensureAMapPlugins(plugins: string | string[]): Promise<void> {
  await loadAMap()
  const pluginList = (Array.isArray(plugins) ? plugins : [plugins]).filter(Boolean)
  if (!pluginList.length) return

  const missing = pluginList.filter((plugin) => {
    const checker = getPluginChecker(plugin)
    if (checker?.()) {
      loadedPluginSet.add(plugin)
      return false
    }
    return !loadedPluginSet.has(plugin)
  })
  if (!missing.length) return

  await new Promise<void>((resolve, reject) => {
    try {
      window.AMap.plugin(missing, () => {
        missing.forEach((plugin) => loadedPluginSet.add(plugin))
        resolve()
      })
    } catch (error) {
      reject(error)
    }
  })
}

// 创建高德地图实例
export function createAMap(container: HTMLElement, config: MapConfig): AMap.Map {
  const map = new window.AMap.Map(container, {
    center: new window.AMap.LngLat(config.center[1], config.center[0]),
    zoom: config.zoom,
    minZoom: config.minZoom,
    maxZoom: config.maxZoom,
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
  return map
}

// 创建POI标记
export function createPoiMarker(
  poi: MarkerPoi,
  onClick?: (poi: MarkerPoi) => void
): AMap.Marker {
  const marker = new window.AMap.Marker({
    position: new window.AMap.LngLat(poi.longitude, poi.latitude),
    title: poi.name,
    offset: new window.AMap.Pixel(0, 0),
    label: {
      content: poi.name,
      direction: 'top',
      style: {
        fontSize: '12px',
        fontWeight: 'bold',
        color: '#333333',
        backgroundColor: 'rgba(255, 255, 255, 0.9)',
        border: '1px solid #ddd',
        borderRadius: '4px',
        padding: '2px 6px',
        boxShadow: '0 2px 4px rgba(0,0,0,0.15)',
        whiteSpace: 'nowrap'
      }
    }
  })

  if (onClick) {
    marker.on('click', () => onClick(poi))
  }

  return marker
}

// 创建起点标记
export function createStartMarker(position: [number, number], name: string): AMap.Marker {
  return new window.AMap.Marker({
    position: new window.AMap.LngLat(position[1], position[0]),
    title: name || '起点',
    content: '<div style="background:#3366FF;width:16px;height:16px;border-radius:50%;border:2px solid #fff;box-shadow:0 2px 4px rgba(0,0,0,0.3);"></div>',
    offset: new window.AMap.Pixel(-8, -8)
  })
}

// 创建终点标记
export function createEndMarker(position: [number, number], name: string): AMap.Marker {
  return new window.AMap.Marker({
    position: new window.AMap.LngLat(position[1], position[0]),
    title: name || '终点',
    content: '<div style="background:#FF6633;width:16px;height:16px;border-radius:50%;border:2px solid #fff;box-shadow:0 2px 4px rgba(0,0,0,0.3);"></div>',
    offset: new window.AMap.Pixel(-8, -8)
  })
}

// 创建用户位置标记
export function createUserLocationMarker(position: [number, number]): AMap.Marker {
  return new window.AMap.Marker({
    position: new window.AMap.LngLat(position[1], position[0]),
    content: '<div style="background:#00CA50;width:20px;height:20px;border-radius:50%;border:3px solid #fff;box-shadow:0 2px 4px rgba(0,0,0,0.3);"></div>',
    offset: new window.AMap.Pixel(-10, -10)
  })
}

export function createWaypointMarker(
  position: [number, number],
  name: string,
  index: number
): AMap.Marker {
  return new window.AMap.Marker({
    position: new window.AMap.LngLat(position[1], position[0]),
    title: name || `途经 ${index + 1}`,
    content: `<div style="background:#FFB020;width:22px;height:22px;border-radius:50%;border:2px solid #fff;box-shadow:0 2px 4px rgba(0,0,0,0.3);display:flex;align-items:center;justify-content:center;color:#fff;font-size:12px;font-weight:700;">${index + 1}</div>`,
    offset: new window.AMap.Pixel(-11, -11)
  })
}

export function buildMultiSegmentPath(segments: RouteSegment[]): [number, number][] {
  const path: [number, number][] = []
  segments.forEach((segment) => {
    segment.path?.forEach((point) => {
      const lat = Number(point[0])
      const lng = Number(point[1])
      // 过滤无效坐标（0,0 或超出范围的坐标）
      if (lat === 0 && lng === 0) return
      if (lat < -90 || lat > 90 || lng < -180 || lng > 180) return
      const last = path[path.length - 1]
      if (last && last[0] === lat && last[1] === lng) {
        return
      }
      path.push([lat, lng])
    })
  })
  return path
}

export function buildItineraryPoints(
  origin: ResolvedPlace,
  waypoints: ResolvedPlace[] = [],
  destination: ResolvedPlace
): Array<ResolvedPlace & { pointType: 'origin' | 'waypoint' | 'destination'; waypointIndex?: number }> {
  return [
    { ...origin, pointType: 'origin' as const },
    ...waypoints.map((waypoint, index) => ({ ...waypoint, pointType: 'waypoint' as const, waypointIndex: index })),
    { ...destination, pointType: 'destination' as const }
  ]
}

// 创建路径折线
export function createRoutePolyline(
  path: [number, number][],
  options?: { strokeColor?: string; strokeWeight?: number; strokeOpacity?: number }
): AMap.Polyline {
  const lnglatPath = path.map((p) => new window.AMap.LngLat(p[1], p[0]))

  return new window.AMap.Polyline({
    path: lnglatPath,
    strokeColor: options?.strokeColor || '#3366FF',
    strokeWeight: options?.strokeWeight || 5,
    strokeOpacity: options?.strokeOpacity || 0.8,
    strokeStyle: 'solid',
    lineJoin: 'round',
    lineCap: 'round',
    showDir: true,
    zIndex: 50,
    isOutline: true,
    outlineColor: '#ffffff',
    borderWeight: 1
  })
}

// 创建路径遮罩层
export function createRouteMask(path: [number, number][]): AMap.Polyline {
  const lnglatPath = path.map((p) => new window.AMap.LngLat(p[1], p[0]))

  return new window.AMap.Polyline({
    path: lnglatPath,
    strokeColor: '#ffffff',
    strokeWeight: 10,
    strokeOpacity: 1,
    strokeStyle: 'solid',
    zIndex: 49
  })
}

// 创建信息窗口
export function createInfoWindow(title: string, content: string, closeCallBack?: () => void): AMap.InfoWindow {
  const infoWindow = new window.AMap.InfoWindow({
    isCustom: false,
    content: createInfoWindowContent(title, content),
    offset: new window.AMap.Pixel(0, -30)
  })

  if (closeCallBack) {
    infoWindow.on('close', closeCallBack)
  }

  return infoWindow
}

// 创建信息窗口内容
function createInfoWindowContent(title: string, content: string): HTMLElement {
  const div = document.createElement('div')
  div.className = 'amap-info-content'
  const titleNode = document.createElement('div')
  titleNode.className = 'info-title'
  titleNode.textContent = title || ''
  const contentNode = document.createElement('div')
  contentNode.className = 'info-content'
  contentNode.textContent = content || ''
  div.appendChild(titleNode)
  div.appendChild(contentNode)
  return div
}

// 格式化距离
export function formatDistance(meters: number): string {
  if (meters < 1000) {
    return `${Math.round(meters)} 米`
  }
  return `${(meters / 1000).toFixed(2)} 公里`
}

// 格式化时间
export function formatDuration(seconds: number): string {
  if (seconds < 60) {
    return `${Math.round(seconds)} 秒`
  }
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) {
    return `${minutes} 分钟`
  }
  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60
  return `${hours} 小时 ${remainingMinutes} 分钟`
}

// 获取当前位置
export async function getCurrentPosition(): Promise<{ latitude: number; longitude: number }> {
  await ensureAMapPlugins('AMap.Geolocation')
  return new Promise((resolve, reject) => {
    if (!window.AMap?.Geolocation) {
      reject(new Error('定位插件未加载'))
      return
    }

    const geolocation = new window.AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 0,
      convert: true,
      showButton: false,
      showMarker: false,
      showCircle: false,
      panToLocation: false,
      zoomToAccuracy: false
    })

    geolocation.getCurrentPosition((status: string, result: GeolocationResult) => {
      if (status === 'complete' && result?.position) {
        resolve({
          latitude: result.position.getLat?.() ?? Number.NaN,
          longitude: result.position.getLng?.() ?? Number.NaN
        })
      } else {
        reject(new Error(`定位失败: ${result?.info || result?.message || '未知错误'}`))
      }
    })
  })
}

// 判断坐标是否在校园范围内
export function isInCampusBounds(lat: number, lng: number): boolean {
  const bounds = [
    31.820, 117.250,
    31.850, 117.285
  ]
  return lat >= bounds[0] && lat <= bounds[2] && lng >= bounds[1] && lng <= bounds[3]
}

// 地理编码
export async function geocode(address: string): Promise<{ lat: number; lng: number }> {
  await ensureAMapPlugins('AMap.Geocoder')
  return new Promise((resolve, reject) => {
    if (!window.AMap.Geocoder) {
      reject(new Error('地理编码插件未加载'))
      return
    }

    const geocoder = new window.AMap.Geocoder({
      city: '合肥',
      citylimit: true
    })

    geocoder.getLocation(address, (status: string, result: GeocodeResult) => {
      if (status === 'complete' && result?.geocodes && result.geocodes.length > 0) {
        const location = result.geocodes[0]?.location
        resolve({
          lat: location?.getLat?.() ?? Number.NaN,
          lng: location?.getLng?.() ?? Number.NaN
        })
      } else {
        reject(new Error('未找到该地址'))
      }
    })
  })
}

// 逆地理编码
export async function reverseGeocode(lng: number, lat: number): Promise<string> {
  await ensureAMapPlugins('AMap.Geocoder')
  return new Promise((resolve, reject) => {
    if (!window.AMap.Geocoder) {
      reject(new Error('地理编码插件未加载'))
      return
    }

    const geocoder = new window.AMap.Geocoder()
    geocoder.getAddress(new window.AMap.LngLat(lng, lat), (status: string, result: ReverseGeocodeResult) => {
      if (status === 'complete' && result?.regeocode) {
        resolve(result.regeocode.formattedAddress || '未知地址')
      } else {
        resolve('未知地址')
      }
    })
  })
}

// 地点搜索
export async function searchPlace(
  keyword: string,
  city: string = '合肥'
): Promise<Array<{ name: string; address: string; location: { lat: number; lng: number } }>> {
  await ensureAMapPlugins('AMap.PlaceSearch')
  return new Promise((resolve, reject) => {
    if (!window.AMap.PlaceSearch) {
      reject(new Error('地点搜索插件未加载'))
      return
    }

    const placeSearch = new window.AMap.PlaceSearch({
      city: city,
      citylimit: true,
      pageSize: 10,
      pageIndex: 1,
      type: '',
      panel: '',
      showCover: false,
      autoFitView: false
    })

    placeSearch.search(keyword, (status: string, result: PlaceSearchResult) => {
      if (status === 'complete' && result?.poiList) {
        const pois = (result.poiList.pois || []).map((poi) => ({
          name: poi.name || '',
          address: poi.address || '',
          location: {
            lat: poi.location?.getLat?.() ?? Number.NaN,
            lng: poi.location?.getLng?.() ?? Number.NaN
          }
        })).filter((poi) => Number.isFinite(poi.location.lat) && Number.isFinite(poi.location.lng))
        resolve(pois)
      } else {
        resolve([])
      }
    })
  })
}

// 错误消息映射
const ERROR_MESSAGES: Record<string, string> = {
  REQUEST_FAILED: '网络请求失败，请检查网络连接',
  INVALID_USER_KEY: '地图密钥无效，请联系管理员',
  INVALID_USER_SCODE: '安全密钥配置错误，请联系管理员',
  USERKEY_PLAT_NOMATCH: '地图密钥平台不匹配，请联系管理员',
  USER_DAILY_QUERY_OVER_LIMIT: '今日地图服务使用量已达上限，请明天再试',
  USER_IP_WHITE_LIST_ERROR: 'IP白名单配置错误，请联系管理员',
  USER_KEY_DAILY_IP_OVER_LIMIT: '密钥IP日用量超限',
  USER_KEY_OVER_IPQLIMIT: '密钥并发量超限',
  SERVICE_RESPONSE_ERROR: '服务响应错误，请稍后重试',
  ENGINE_RESPONSE_ERROR: '路径规划引擎错误',
  NO_DATA: '当前区域暂不支持路径规划'
}

// 获取友好错误消息
function getFriendlyErrorMessage(error: unknown): string {
  if (!error) return '未知错误'
  const payload = (typeof error === 'object' && error !== null) ? error as Record<string, unknown> : {}
  const errorInfo = String(payload.info || payload.message || error)
  for (const [key, message] of Object.entries(ERROR_MESSAGES)) {
    if (errorInfo.includes(key)) {
      return message
    }
  }
  return errorInfo
}

// 验证坐标是否有效
function isValidCoordinate(coord: [number, number]): boolean {
  const [lat, lng] = coord
  return (
    typeof lat === 'number' &&
    typeof lng === 'number' &&
    !isNaN(lat) &&
    !isNaN(lng) &&
    lat >= -90 &&
    lat <= 90 &&
    lng >= -180 &&
    lng <= 180
  )
}

// 计算两点之间的直线距离（米）
function calculateDirectDistance(start: [number, number], end: [number, number]): number {
  const R = 6371000
  const lat1 = (start[0] * Math.PI) / 180
  const lat2 = (end[0] * Math.PI) / 180
  const deltaLat = ((end[0] - start[0]) * Math.PI) / 180
  const deltaLng = ((end[1] - start[1]) * Math.PI) / 180

  const a =
    Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
    Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c
}

// 生成模拟校园路径
function generateMockCampusPath(start: [number, number], end: [number, number]): [number, number][] {
  const distance = calculateDirectDistance(start, end)
  const numPoints = Math.max(5, Math.min(20, Math.round(distance / 30)))
  const path: [number, number][] = []
  path.push(start)

  for (let i = 1; i < numPoints; i++) {
    const ratio = i / numPoints
    const offsetLat = Math.sin(ratio * Math.PI * 2) * 0.0002
    const offsetLng = Math.cos(ratio * Math.PI * 3) * 0.0002
    const lat = start[0] + (end[0] - start[0]) * ratio + offsetLat
    const lng = start[1] + (end[1] - start[1]) * ratio + offsetLng
    path.push([lat, lng])
  }

  path.push(end)
  return path
}

// 使用降级方案生成路径
function generateFallbackPath(
  start: [number, number],
  end: [number, number],
  error?: unknown
): {
  distance: number
  duration: number
  path: [number, number][]
  steps: Array<{ instruction: string; distance: number; duration: number }>
  isFallback: boolean
  fallbackReason?: string
} {
  const distance = calculateDirectDistance(start, end)
  const walkingSpeed = 1.2
  const duration = Math.round(distance / walkingSpeed)
  const path = generateMockCampusPath(start, end)
  const fallbackReason = error ? getFriendlyErrorMessage(error) : '使用离线路径数据'

  console.warn('使用降级方案:', fallbackReason)

  const steps = [
    { instruction: '出发，沿校园道路步行', distance: Math.round(distance * 0.4), duration: Math.round((distance * 0.4) / walkingSpeed) },
    { instruction: '继续沿道路前行', distance: Math.round(distance * 0.3), duration: Math.round((distance * 0.3) / walkingSpeed) },
    { instruction: '到达目的地', distance: Math.round(distance * 0.3), duration: Math.round((distance * 0.3) / walkingSpeed) }
  ]

  return { distance, duration, path, steps, isFallback: true, fallbackReason }
}

// 步行路线规划
export async function walkingRoute(
  start: [number, number],
  end: [number, number]
): Promise<{
  distance: number
  duration: number
  path: [number, number][]
  steps: Array<{ instruction: string; distance: number; duration: number }>
  isFallback?: boolean
  fallbackReason?: string
}> {
  if (!Array.isArray(start) || start.length !== 2 || !Array.isArray(end) || end.length !== 2) {
    throw new Error('坐标格式无效')
  }

  if (!isValidCoordinate(start)) {
    throw new Error(`起点坐标无效: lat=${start[0]}, lng=${start[1]}`)
  }

  if (!isValidCoordinate(end)) {
    throw new Error(`终点坐标无效: lat=${end[0]}, lng=${end[1]}`)
  }

  const directDistance = calculateDirectDistance(start, end)
  if (directDistance < 1) {
    return { distance: 0, duration: 0, path: [start, end], steps: [{ instruction: '起点和终点相同', distance: 0, duration: 0 }] }
  }

  if (!window.AMap) {
    console.warn('高德地图未加载，使用降级方案')
    return generateFallbackPath(start, end, '高德地图未加载')
  }

  if (!window.AMap.Walking) {
    console.warn('步行路线规划插件未加载，尝试动态加载')
    try {
      await new Promise<void>((resolve) => {
        window.AMap.plugin('AMap.Walking', () => resolve())
      })
    } catch (e) {
      console.warn('动态加载步行插件失败:', e)
    }

    if (!window.AMap.Walking) {
      console.warn('无法加载步行路线规划插件，使用降级方案')
      return generateFallbackPath(start, end, '步行插件未加载')
    }
  }

  return new Promise((resolve) => {
    const walking = new window.AMap.Walking({
      panel: '',
      map: null,
      hideMarkers: true,
      isOutline: false,
      outlineColor: '#ffffff',
      extensions: 'all'
    })

    const timeout = setTimeout(() => {
      console.warn('路线规划超时，使用降级方案')
      resolve(generateFallbackPath(start, end, '请求超时'))
    }, 8000)

    walking.search(
      new window.AMap.LngLat(start[1], start[0]),
      new window.AMap.LngLat(end[1], end[0]),
      (status: string, result: WalkingSearchResult) => {
        clearTimeout(timeout)
        console.log('高德地图路径规划状态:', status)

        if (status === 'complete' && result && result.routes && result.routes.length > 0) {
          try {
            const route = result.routes[0]
            
            // 必须定义一个空数组，用于存储所有路径点
            const fullPath: unknown[] = []
            
            // 遍历 steps 数组，对每一个 step，必须将其内部的 path 属性（点数组）全部 push 到 fullPath 中
            if (route.steps && Array.isArray(route.steps)) {
              for (const step of route.steps) {
                if (step.path && Array.isArray(step.path)) {
                  step.path.forEach((p) => fullPath.push(p))
                }
              }
            }

            // 将 fullPath 转换为 [lat, lng] 数组格式
            const path: [number, number][] = fullPath.map((p) => {
              const obj = (typeof p === 'object' && p !== null) ? p as LngLatLike : null
              if (obj && typeof obj.getLat === 'function' && typeof obj.getLng === 'function') {
                return [obj.getLat(), obj.getLng()] as [number, number]
              } else if (obj && obj.lat !== undefined && obj.lng !== undefined) {
                return [Number(obj.lat), Number(obj.lng)] as [number, number]
              }
              return null
            }).filter((p): p is [number, number] => p !== null)

            if (path.length === 0) {
              console.warn('高德返回路径为空，使用降级方案')
              resolve(generateFallbackPath(start, end, '路径数据为空'))
              return
            }

            const steps = route.steps?.map((step) => ({
              instruction: step.instruction || '继续前行',
              distance: typeof step.distance === 'number' ? step.distance : 0,
              duration: typeof step.duration === 'number' ? step.duration : 0
            })) || []

            console.log('成功获取步行路线，路径点数:', path.length)
            resolve({
              distance: typeof route.distance === 'number' ? route.distance : directDistance,
              duration: typeof route.time === 'number' ? route.time : Math.round(directDistance / 1.2),
              path,
              steps
            })
          } catch (parseError) {
            console.error('解析路线数据失败:', parseError)
            resolve(generateFallbackPath(start, end, parseError))
          }
        } else if (status === 'no_data') {
          console.warn('高德地图未找到步行路线，使用降级方案')
          resolve(generateFallbackPath(start, end, 'NO_DATA'))
        } else if (status === 'error') {
          console.error('高德地图路径规划错误:', result)
          resolve(generateFallbackPath(start, end, result))
        } else {
          resolve(generateFallbackPath(start, end, status))
        }
      }
    )
  })
}

// 获取地址（从坐标）
export async function getAddressFromCoord(lng: number, lat: number): Promise<string> {
  try {
    return await reverseGeocode(lng, lat)
  } catch {
    return ''
  }
}

export default {
  loadAMap,
  createAMap,
  createPoiMarker,
  createStartMarker,
  createEndMarker,
  createUserLocationMarker,
  createRoutePolyline,
  createRouteMask,
  createInfoWindow,
  formatDistance,
  formatDuration,
  getCurrentPosition,
  isInCampusBounds,
  geocode,
  reverseGeocode,
  searchPlace,
  walkingRoute,
  getAddressFromCoord
}
