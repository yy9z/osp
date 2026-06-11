/**
 * 校园导航模块 TypeScript 类型定义
 */

// POI 分类枚举
export enum PoiCategory {
  TEACHING = 'TEACHING',     // 教学楼
  DINING = 'DINING',         // 食堂
  DORMITORY = 'DORMITORY',   // 宿舍
  LIBRARY = 'LIBRARY',       // 图书馆
  SPORTS = 'SPORTS',         // 运动场
  ADMIN = 'ADMIN',           // 行政楼
  SCENIC = 'SCENIC',         // 景点
  GATE = 'GATE',             // 校门
  ENTRANCE = 'ENTRANCE'      // 校门（兼容）
}

// POI 分类配置
export interface PoiCategoryConfig {
  text: string
  color: string
  icon: string
}

// POI 数据接口
export interface Poi {
  id: number
  name: string
  category: PoiCategory
  description?: string
  latitude: number
  longitude: number
  floor?: string
  openTime?: string
  image?: string
}

// 区域数据接口 (GeoJSON)
export interface Region {
  id: number
  name: string
  type: string
  geoJson: GeoJSON.Feature<GeoJSON.Polygon | GeoJSON.MultiPolygon>
}

// 路径规划请求
export interface RouteRequest {
  startLat: number
  startLng: number
  endLat: number
  endLng: number
}

// 路径规划响应
export interface RouteResponse {
  distance: number
  duration: number
  path: [number, number][]
  steps: RouteStep[]
  selectedRouteIndex?: number
  totalRoutes?: number
  selectionReason?: string
  alternativeRoutes?: AlternativeRoute[]
}

export interface ResolvedPlace {
  id?: string
  name: string
  address?: string
  lat: number
  lng: number
  campus?: string
  source?: string
  distanceToUser?: number | null
}

export interface RouteSegment {
  index: number
  from: ResolvedPlace | null
  to: ResolvedPlace | null
  distance: number
  duration: number
  steps: RouteStep[]
  path: [number, number][]
  geometrySource?: string
  selectedRouteIndex?: number
  totalRoutes?: number
  selectionReason?: string
  alternativeRoutes?: AlternativeRoute[]
  slotProfile?: SegmentSlotProfile
  analysis?: SegmentAnalysis      // 该段分析结果
}

export interface NavigationClarification {
  clarificationRequired: boolean
  clarificationType?: string
  clarificationOptions?: string[]
  message?: string
}

export interface AgentNavigationPayload extends NavigationClarification {
  source?: string
  destination?: string
  destinationText?: string
  destinationAddress?: string
  destLat?: number
  destLng?: number
  origin?: string
  originText?: string
  originAddress?: string
  originLat?: number
  originLng?: number
  campus?: string
  mode?: string
  routeReady?: boolean
  routeType?: 'single' | 'multi_waypoint'
  path?: [number, number][]
  steps?: RouteStep[]
  distance?: number
  duration?: number
  waypoints?: ResolvedPlace[]
  segments?: RouteSegment[]
  reasoning?: string
  resolutionSource?: string
  agentSummary?: string
  candidates?: ResolvedPlace[]
  destinationOptions?: ResolvedPlace[]
  preferences?: string[]
  taskType?: string
  timeSlot?: string
  safetyAnalysis?: SafetyAnalysis
  overallAnalysis?: OverallAnalysis    // 综合分析结果（多段路线）
  // 多路线选择
  selectedRouteIndex?: number          // 选中的路线索引
  totalRoutes?: number                // 总路线数
  selectionReason?: string            // 选择理由
  alternativeRoutes?: AlternativeRoute[] // 备选路线列表
  amapApiVersion?: string
  routingStrategy?: string
  geometrySource?: string
  geometryFallbackReason?: string
}

// 路线安全分析结果
export interface SafetyAnalysis {
  safetyScore: number           // 安全评分 1-10
  warnings: string[]            // 警告信息列表
  suggestions: string[]         // 建议列表
  hasAlternative: boolean       // 是否有替代路线
  alternativeSummary?: string   // 替代路线摘要
}

// 单段路径分析结果
export interface SegmentAnalysis {
  score: number                 // 该段评分 1-10
  reason: string                // 评估理由
  warnings: string[]            // 该段警告
  suggestions: string[]         // 该段建议
}

export interface SegmentSlotProfile {
  index: number
  destination?: string
  segmentGoal?: string
  taskScene?: string
  preferences: string[]
  constraints: string[]
  timeContext?: string
  timeSlot?: string
  urgencyMinutes?: number | null
  notes?: string
}

// 综合分析结果
export interface OverallAnalysis {
  overallScore: number          // 总体评分
  summary: string               // 综合评估摘要
  keyPoints: string[]           // 关键提示
  recommendation: string        // 总体建议
}

// 备选路线
export interface AlternativeRoute {
  index: number                 // 路线索引
  distance: number              // 距离（米）
  duration: number              // 时长（秒）
  summary?: string              // 路线摘要
  reason?: string               // 路线评分理由
  score?: number                // 路线评分
  steps?: RouteStep[]           // 该候选路线步骤
  path?: [number, number][]     // 该候选路线轨迹
  safetyAnalysis?: SafetyAnalysis | null
  overallAnalysis?: OverallAnalysis | null
  selected: boolean             // 是否被选中
}

export interface NavigationResolveResponse extends NavigationClarification {
  success: boolean
  place?: ResolvedPlace
  candidates?: ResolvedPlace[]
  places?: ResolvedPlace[]
}

// 导航步骤
export interface RouteStep {
  instruction: string   // 导航指令
  distance: number      // 本段距离(米)
  duration: number      // 本段时间(秒)
  startPoint: [number, number]
  endPoint: [number, number]
}

// 附近搜索请求
export interface NearbyRequest {
  lat: number
  lng: number
  radius?: number  // 半径(米), 默认500
}

// 地图配置
export interface MapConfig {
  center: [number, number]  // [纬度, 经度]
  zoom: number
  minZoom: number
  maxZoom: number
  bounds?: [number, number, number, number] // [南, 西, 北, 东]
}

// 用户位置
export interface UserLocation {
  lat: number
  lng: number
  accuracy?: number
  timestamp?: number
}

// 导航状态
export interface NavigationState {
  isNavigating: boolean
  startPoint: Poi | UserLocation | null
  endPoint: Poi | null
  route: RouteResponse | null
}

// 分类映射表
export const CATEGORY_MAP: Partial<Record<PoiCategory, PoiCategoryConfig>> = {
  [PoiCategory.TEACHING]: {
    text: '教学楼',
    color: '#409EFF', // 蓝色
    icon: 'building'
  },
  [PoiCategory.DINING]: {
    text: '食堂',
    color: '#E6A23C', // 橙色
    icon: 'utensils'
  },
  [PoiCategory.DORMITORY]: {
    text: '宿舍',
    color: '#67C23A', // 绿色
    icon: 'bed'
  },
  [PoiCategory.LIBRARY]: {
    text: '图书馆',
    color: '#9C27B0', // 紫色
    icon: 'book'
  },
  [PoiCategory.SPORTS]: {
    text: '运动场',
    color: '#F56C6C', // 红色
    icon: 'dumbbell'
  },
  [PoiCategory.ADMIN]: {
    text: '行政楼',
    color: '#909399', // 灰色
    icon: 'landmark'
  },
  [PoiCategory.SCENIC]: {
    text: '景点',
    color: '#E91E63', // 粉色
    icon: 'camera'
  },
  [PoiCategory.GATE]: {
    text: '校门',
    color: '#FFD700', // 金色
    icon: 'door-open'
  },
  [PoiCategory.ENTRANCE]: {
    text: '校门',
    color: '#FFD700', // 金色
    icon: 'door-open'
  }
}

// 中国科学技术大学默认配置
// 东校区（主校区）- 行政与底蕴中心
// 纬度 31.8375, 经度 117.2670
export const USTC_MAP_CONFIG: MapConfig = {
  center: [31.8375, 117.2670], // [纬度, 经度] - 东校区中心位置
  zoom: 16,
  minZoom: 4,
  maxZoom: 20,
  bounds: [
    31.820, 117.250, // 西南角 [纬度, 经度]
    31.850, 117.285  // 东北角
  ]
}

// API 响应类型
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// 分页响应
export interface PaginatedResponse<T> {
  records: T[]
  total: number
  page: number
  size: number
}
