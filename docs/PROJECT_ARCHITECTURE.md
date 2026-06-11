# 校园一站式服务平台 - 项目架构文档

## 一、项目概述

这是一个**高校校园一站式服务平台**，采用前后端分离架构：

| 层级 | 技术栈 |
|------|--------|
| 后端 | Spring Boot 4.0 + MyBatis + MySQL + Spring Security + JWT |
| 前端 | Vue 3 + Pinia + Vue Router + Element Plus + 高德地图 |
| 存储 | 阿里云 OSS |

---

## 二、后端架构

### 2.1 包结构

```
src/main/java/com/caspar/
├── common/          # 通用类（统一响应、异常处理）
├── config/          # 配置类（安全、CORS、MyBatis）
├── controller/      # 控制器层 (8个)
├── entity/          # 实体类
│   ├── dto/         # 数据传输对象 (16个)
│   └── enums/       # 枚举类
├── mapper/          # MyBatis Mapper接口 (15个)
├── service/         # 服务层接口
│   └── impl/        # 服务实现 (7个)
└── util/            # 工具类 (7个)
```

### 2.2 API 端点清单

#### 用户管理模块 - UserController

| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| POST | `/api/user/register` | 用户注册 | 公开 |
| POST | `/api/user/login` | 用户登录 | 公开 |
| GET | `/api/user/info` | 获取当前用户信息 | 登录 |
| GET | `/api/user/{id}` | 根据ID获取用户信息 | 登录 |
| PUT | `/api/user/password` | 修改密码 | 登录 |
| PUT | `/api/user/profile` | 更新个人资料 | 登录 |
| GET | `/api/user/list` | 用户列表 | 管理员 |

#### 二手交易模块 - SecondhandGoodsController

| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| POST | `/api/secondhand/publish` | 发布商品 | 登录 |
| GET | `/api/secondhand/list` | 商品列表（分页/搜索/排序） | 公开 |
| GET | `/api/secondhand/{id}` | 商品详情 | 公开 |
| GET | `/api/secondhand/my` | 我的发布 | 登录 |
| DELETE | `/api/secondhand/{id}` | 下架商品 | 登录 |
| PUT | `/api/secondhand/{id}` | 更新商品 | 登录 |
| PUT | `/api/secondhand/{id}/sold` | 标记已售 | 登录 |
| PUT | `/api/secondhand/{id}/relist` | 重新上架 | 登录 |
| POST | `/api/secondhand/{id}/favorite` | 收藏商品 | 登录 |
| DELETE | `/api/secondhand/{id}/favorite` | 取消收藏 | 登录 |
| GET | `/api/secondhand/favorites` | 我的收藏列表 | 登录 |
| GET | `/api/secondhand/{id}/favorite/status` | 检查收藏状态 | 登录 |

#### 失物招领模块 - LostFoundController

| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| POST | `/api/lostfound/publish` | 发布失物/招领信息 | 登录 |
| GET | `/api/lostfound/list` | 列表（分页/搜索） | 公开 |
| GET | `/api/lostfound/{id}` | 详情 | 公开 |
| POST | `/api/lostfound/{id}/claim` | 认领申请 | 登录 |
| GET | `/api/lostfound/{id}/claims` | 获取认领记录 | 登录 |
| GET | `/api/lostfound/my` | 我的发布 | 登录 |
| DELETE | `/api/lostfound/{id}` | 删除 | 登录 |
| PUT | `/api/lostfound/{id}/resolve` | 标记已解决 | 登录 |

#### 消息中心模块 - MessageController

| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| POST | `/api/message/send` | 发送消息 | 登录 |
| GET | `/api/message/conversations` | 获取会话列表 | 登录 |
| GET | `/api/message/history/{userId}` | 获取聊天记录 | 登录 |
| PUT | `/api/message/read/{userId}` | 标记已读 | 登录 |
| GET | `/api/message/unread-count` | 获取未读消息数 | 登录 |
| GET | `/api/message/conversation/{otherUserId}` | 获取或创建会话 | 登录 |
| PUT | `/api/message/conversation/{otherUserId}/pin` | 置顶/取消置顶会话 | 登录 |
| DELETE | `/api/message/conversation/{otherUserId}` | 删除会话 | 登录 |

#### 宿舍管理模块 - DormitoryController

| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| GET | `/api/dormitory/list` | 宿舍列表（分页/搜索） | 登录 |
| GET | `/api/dormitory/{id}` | 宿舍详情 | 登录 |
| GET | `/api/dormitory/my` | 我的宿舍 | 登录 |
| GET | `/api/dormitory/{id}/members` | 宿舍成员列表 | 登录 |
| POST | `/api/dormitory/repair` | 报修申请 | 登录 |
| GET | `/api/dormitory/repair/my` | 我的报修列表 | 登录 |
| GET | `/api/dormitory/repair/list` | 报修列表 | 管理员 |
| PUT | `/api/dormitory/repair/{id}` | 处理报修 | 管理员 |

#### 校园导航模块 - NavigationController

| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| GET | `/api/navigation/places` | POI列表（分页/搜索） | 公开 |
| GET | `/api/navigation/places/{id}` | POI详情 | 公开 |
| GET | `/api/navigation/map` | 获取校园地图信息 | 公开 |
| GET | `/api/navigation/pois` | 获取所有POI | 公开 |
| GET | `/api/navigation/pois/category/{category}` | 按类别获取POI | 公开 |
| GET | `/api/navigation/nearby` | 附近搜索 | 公开 |
| POST | `/api/navigation/route` | 路径规划 | 公开 |
| POST | `/api/navigation/route/amap` | 高德路径规划代理 | 公开 |
| GET | `/api/navigation/regions` | 获取区域GeoJSON | 公开 |
| GET | `/api/navigation/convert` | 坐标转换(WGS-84→GCJ-02) | 公开 |
| GET | `/api/navigation/geofence/check` | 围栏检查 | 公开 |

#### 首页模块 - HomeController

| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| GET | `/api/v1/home/data` | 获取首页仪表盘数据 | 登录 |

#### 文件上传模块 - FileController

| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| POST | `/api/file/upload` | 单文件上传（阿里云OSS） | 登录 |
| POST | `/api/file/upload/multiple` | 多文件上传 | 登录 |

---

## 三、前端架构

### 3.1 目录结构

```
campus-frontend/src/
├── api/           # API接口定义 (8个)
├── assets/        # 静态资源（图标、样式）
├── components/    # 公共组件 (4个)
├── layouts/       # 布局组件 (MainLayout)
├── router/        # 路由配置
├── stores/        # Pinia状态管理 (2个)
├── types/         # TypeScript类型定义
├── utils/         # 工具函数
└── views/         # 页面组件 (10个)
```

### 3.2 页面组件清单

| 页面 | 路由 | 功能描述 |
|------|------|---------|
| Login.vue | `/login` | 用户登录页面 |
| Register.vue | `/register` | 用户注册页面 |
| Dashboard.vue | `/` | 首页仪表盘 |
| SecondHand.vue | `/secondhand` | 二手交易列表 |
| SecondHandDetail.vue | `/secondhand/:id` | 商品详情页 |
| LostFound.vue | `/lostfound` | 失物招领页面 |
| Messages.vue | `/messages` | 消息中心 |
| Dormitory.vue | `/dormitory` | 宿舍管理 |
| Navigation.vue | `/navigation` | 校园导航 |
| Profile.vue | `/profile` | 个人中心 |

### 3.3 状态管理

| Store | 状态 | 方法 |
|-------|------|------|
| **useUserStore** | token, userInfo | login, register, fetchUserInfo, changePassword, logout |
| **useMessageStore** | unreadCount, conversations | refreshUnreadCount, refreshConversations, incrementUnread, clearConversationUnread |

### 3.4 路由配置

```
/login          - 登录页（无需认证）
/register       - 注册页（无需认证）
/               - MainLayout（需认证）
  ├── /               - Dashboard（首页）
  ├── /secondhand     - SecondHand（二手交易）
  ├── /secondhand/:id - SecondHandDetail（商品详情）
  ├── /lostfound      - LostFound（失物招领）
  ├── /messages       - Messages（消息中心）
  ├── /dormitory      - Dormitory（宿舍管理）
  ├── /navigation     - Navigation（校园导航）
  └── /profile        - Profile（个人中心）
```

---

## 四、数据库设计

### 4.1 核心业务表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| **user** | 用户表 | user_id, username, password, real_name, student_id, phone, email, role, avatar, bio |
| **secondhand** | 二手商品表 | id, title, description, price, category, images, condition, status, seller_id, view_count |
| **goods_favorite** | 商品收藏表 | id, user_id, goods_id |
| **lostfound** | 失物招领表 | id, type, title, description, category, images, location, reward, status, publisher_id |
| **lostfound_claim** | 认领记录表 | id, lostfound_id, claimer_id, message, status |
| **message** | 消息表 | id, sender_id, receiver_id, content, related_type, related_id, is_read |
| **conversation_settings** | 会话设置表 | id, user_id, other_user_id, is_pinned, is_deleted |
| **notification** | 通知表 | id, type, title, content, related_id, related_type |

### 4.2 宿舍管理表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| **dormitory** | 宿舍表 | id, building, floor, room_no, type, capacity, current_count, gender, head_id |
| **dormitory_member** | 宿舍成员表 | id, dormitory_id, user_id, join_time |
| **dormitory_repair** | 报修表 | id, dormitory_id, user_id, description, images, status, remark |

### 4.3 校园导航表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| **campus_map** | 校园地图表 | id, name, center_lat, center_lng, zoom, image_url |
| **campus_poi** | POI地点表 | id, name, category, description, latitude, longitude, location(Point), floor, image_url |
| **campus_region** | 区域表 | id, name, type, geojson |
| **campus_path_node** | 路径节点表 | id, name, latitude, longitude, node_type, poi_id |
| **campus_path_edge** | 路径边表 | id, from_node_id, to_node_id, distance, way_type |

---

## 五、功能模块详解

### 模块一：用户管理模块

**功能点清单：**

| 功能 | 描述 |
|------|------|
| 用户注册 | 支持学生/教师/管理员角色，学号/工号唯一性校验 |
| 用户登录 | JWT Token认证，Token有效期管理 |
| 个人信息 | 查看、修改头像、昵称、个人简介 |
| 密码修改 | 原密码验证，新密码加密存储 |
| 用户列表 | 管理员查看所有用户，支持搜索分页 |

### 模块二：二手交易模块

**功能点清单：**

| 功能 | 描述 |
|------|------|
| 商品发布 | 支持多图片上传（阿里云OSS），商品分类、成色、价格 |
| 商品列表 | 分类筛选、关键词搜索、价格排序、分页加载 |
| 商品详情 | 浏览计数、收藏状态、卖家信息、即时通讯入口 |
| 我的发布 | 编辑商品、下架、重新上架、标记已售 |
| 商品收藏 | 收藏/取消收藏，我的收藏列表 |
| URL状态同步 | 排序、分类、搜索参数同步到URL，支持返回恢复 |

### 模块三：失物招领模块

**功能点清单：**

| 功能 | 描述 |
|------|------|
| 发布信息 | 失物/招领类型，物品分类，图片上传，悬赏金额 |
| 列表浏览 | 类型筛选、分类筛选、关键词搜索 |
| 认领申请 | 填写认领说明，提交申请 |
| 认领管理 | 发布者查看认领记录，批准/拒绝 |
| 状态管理 | 标记已解决，删除发布 |

### 模块四：消息中心模块

**功能点清单：**

| 功能 | 描述 |
|------|------|
| 发送私信 | 文本消息，关联商品/失物招领 |
| 会话列表 | 最新消息预览、未读数统计、置顶管理 |
| 聊天记录 | 分页加载，自动滚动到底部 |
| 未读统计 | 侧边栏角标、会话列表角标同步 |
| 会话管理 | 置顶、删除（物理删除消息记录） |
| 轮询机制 | 5秒轮询获取新消息 |

### 模块五：宿舍管理模块

**功能点清单：**

| 功能 | 描述 |
|------|------|
| 宿舍列表 | 楼栋、楼层、房间号搜索，容量/入住人数显示 |
| 宿舍详情 | 房间信息、成员列表、宿舍长标识 |
| 我的宿舍 | 学生查看自己所在宿舍信息 |
| 报修申请 | 问题描述、图片上传、状态跟踪 |
| 报修处理 | 管理员处理报修、添加备注、更新状态 |

### 模块六：校园导航模块

**功能点清单：**

| 功能 | 描述 |
|------|------|
| 校园地图 | 高德地图展示，自定义POI标记 |
| POI浏览 | 分类筛选（食堂、图书馆、教学楼等）、关键词搜索 |
| POI详情 | 名称、描述、图片、开放时间、楼层 |
| 附近搜索 | 基于当前坐标和半径搜索附近地点 |
| 路径规划 | Dijkstra最短路径算法 + 高德地图API降级策略 |
| 围栏检测 | 判断坐标是否在校园范围内 |
| 坐标转换 | WGS-84转GCJ-02（火星坐标系） |

### 模块七：首页仪表盘模块

**功能点清单：**

| 功能 | 描述 |
|------|------|
| 统计数据 | 二手商品数、失物招领数、报修数、未读消息数 |
| 最新动态 | 最新二手商品、最新失物招领 |
| 快捷入口 | 各功能模块入口 |

### 模块八：文件上传模块

**功能点清单：**

| 功能 | 描述 |
|------|------|
| 单文件上传 | 阿里云OSS存储，返回访问URL |
| 多文件上传 | 批量上传，返回URL列表 |
| 文件验证 | 类型验证（图片）、大小限制（最大10MB） |
| 路径规范 | 按日期分目录存储 |

---

## 六、技术亮点

### 6.1 安全认证

- Spring Security + JWT Token认证
- 密码BCrypt加密存储
- 路由守卫自动校验登录状态
- Token过期自动刷新机制

### 6.2 空间数据处理

- MySQL空间索引（POINT类型）
- JTS库处理地理信息
- Dijkstra最短路径算法
- 高德地图API集成

### 6.3 文件存储

- 阿里云OSS对象存储
- 按日期分目录管理
- 支持多图片批量上传

### 6.4 前端优化

- Pinia响应式状态管理
- 骨架屏加载状态
- 防抖节流优化
- URL状态同步
- 路由懒加载

---

## 七、项目启动

### 后端启动

```bash
cd /Users/caspar/Documents/IdeaProjects/NewOSPFU
./mvnw spring-boot:run
```

后端服务地址：http://localhost:8080

### 前端启动

```bash
cd /Users/caspar/Documents/IdeaProjects/NewOSPFU/campus-frontend
npm install
npm run dev
```

前端服务地址：http://localhost:5173

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

---

## 八、更新日志

| 日期 | 更新内容 |
|------|---------|
| 2026-03-13 | 新增二手交易智能排序（smart）：融合收藏热度、浏览热度、成色、新鲜度与关键词相关性，前后端与Agent默认排序同步升级 |
| 2026-03-13 | 优化智能助手 UNKNOWN 兜底回复：新增“你是谁/你好/谢谢”等通用问答与统一可执行引导，避免固定“无法理解”提示 |
| 2026-03-13 | 修复智能助手意图识别异常：调整 Spring AI 的 SiliconFlow base-url，解决 /api/agent/chat 返回 UNKNOWN 的 404 问题 |
| 2026-03-13 | 修复失物招领图片不显示：前端解析 images 字段并渲染首图，后端发布接口补充 images 入库 |
| 2026-02-26 | 数据库优化：删除未使用的 campus_place 表及相关代码 |
| 2026-02-26 | 修复消息未读数统计问题（SQL查询优化） |
| 2026-02-26 | 修复删除会话后列表不更新问题 |
| 2026-02-26 | 修复空分类商品列表永久加载问题 |
| 2026-02-26 | 添加二手交易URL状态同步功能 |
| 2026-02-26 | 优化收藏功能防重复点击 |
| 2026-02-26 | 添加我的收藏分页功能 |
