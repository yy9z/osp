# 高校校园一站式平台 - API接口文档

## 1. 项目概述

- **项目名称**：高校校园一站式平台
- **技术栈**：Spring Boot (Java 17) + Vue.js + MySQL
- **Base URL**：`http://localhost:8080/api`
- **认证方式**：JWT Token

---

## 2. 用户模块 (User)

### 2.1 注册
- **POST** `/user/register`
- **请求体**：
```json
{
  "username": "string",
  "password": "string",
  "realName": "string",
  "phone": "string",
  "email": "string",
  "role": "STUDENT"  // STUDENT, TEACHER, ADMIN
}
```
- **响应**：
```json
{
  "code": 200,
  "message": "注册成功",
  "data": { "userId": 1 }
}
```

### 2.2 登录
- **POST** `/user/login`
- **请求体**：
```json
{
  "username": "string",
  "password": "string"
}
```
- **响应**：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "username": "string",
    "realName": "string",
    "role": "STUDENT"
  }
}
```

### 2.3 获取当前用户信息
- **GET** `/user/info`
- **Headers**：`Authorization: Bearer <token>`
- **响应**：
```json
{
  "code": 200,
  "data": {
    "userId": 1,
    "username": "string",
    "realName": "string",
    "phone": "string",
    "email": "string",
    "role": "STUDENT",
    "avatar": "string"
  }
}
```

### 2.4 修改密码
- **PUT** `/user/password`
- **Headers**：`Authorization: Bearer <token>`
- **请求体**：
```json
{
  "oldPassword": "string",
  "newPassword": "string"
}
```

### 2.5 用户列表（管理员）
- **GET** `/user/list`
- **Headers**：`Authorization: Bearer <token>`
- **查询参数**：`page=1&size=10&role=STUDENT&keyword=`
- **响应**：
```json
{
  "code": 200,
  "data": {
    "records": [...],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

---

## 3. 二手交易模块 (SecondHand)

### 3.1 发布商品
- **POST** `/secondhand/publish`
- **Headers**：`Authorization: Bearer <token>`
- **请求体**：
```json
{
  "title": "string",
  "description": "string",
  "price": 99.99,
  "category": "BOOKS",  // BOOKS, DIGITAL, APPLIANCE, DAILY, OTHER
  "images": ["url1", "url2"],
  "condition": "NEW"  // NEW, LIKE_NEW, GOOD
}
```
- **说明**：发布后状态为 `AUDITING` (审核中)

### 3.2 商品列表
- **GET** `/secondhand/list`
- **查询参数**：`page=1&size=10&category=&keyword=&sort=latest` // latest, price_asc, price_desc
- **说明**：仅返回 `ACTIVE` (在售) 状态的商品
- **响应**：
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "title": "string",
        "price": 99.99,
        "category": "BOOKS",
        "images": ["url1"],
        "condition": "NEW",
        "status": "ACTIVE",  // AUDITING, ACTIVE, SOLD
        "sellerId": 1,
        "sellerName": "string",
        "createTime": "2024-01-01 12:00:00"
      }
    ],
    "total": 50,
    "page": 1,
    "size": 10
  }
}
```

### 3.3 商品详情
- **GET** `/secondhand/{id}`
- **响应**：
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "title": "string",
    "description": "string",
    "price": 99.99,
    "category": "BOOKS",
    "images": ["url1", "url2"],
    "condition": "NEW",
    "status": "ACTIVE",
    "sellerId": 1,
    "sellerName": "string",
    "sellerPhone": "string",
    "viewCount": 100,
    "createTime": "2024-01-01 12:00:00"
  }
}
```

### 3.4 我的发布
- **GET** `/secondhand/my`
- **Headers**：`Authorization: Bearer <token>`
- **查询参数**：`page=1&size=10&status=` // AUDITING, ACTIVE, SOLD

### 3.5 下架商品
- **DELETE** `/secondhand/{id}`
- **Headers**：`Authorization: Bearer <token>`
- **说明**：仅发布者可以下架自己的商品

### 3.6 标记已售
- **PUT** `/secondhand/{id}/sold`
- **Headers**：`Authorization: Bearer <token>`
- **说明**：标记后状态变为 `SOLD`，同时向 notification 表插入系统消息

### 3.7 收藏商品
- **POST** `/secondhand/{id}/favorite`
- **Headers**：`Authorization: Bearer <token>`

### 3.8 取消收藏
- **DELETE** `/secondhand/{id}/favorite`
- **Headers**：`Authorization: Bearer <token>`

### 3.9 我的收藏列表
- **GET** `/secondhand/favorites`
- **Headers**：`Authorization: Bearer <token>`
- **查询参数**：`page=1&size=10`

### 3.10 检查是否已收藏
- **GET** `/secondhand/{id}/favorite/status`
- **Headers**：`Authorization: Bearer <token>`
- **响应**：
```json
{
  "code": 200,
  "data": true  // true: 已收藏, false: 未收藏
}
```

---

## 4. 失物招领模块 (LostFound)

### 4.1 发布失物/招领信息
- **POST** `/lostfound/publish`
- **Headers**：`Authorization: Bearer <token>`
- **请求体**：
```json
{
  "type": "LOST",  // LOST, FOUND
  "title": "string",
  "description": "string",
  "category": "ELECTRONICS",  // ELECTRONICS, DOCUMENTS, KEY, CLOTHING, OTHER
  "images": ["url1", "url2"],
  "location": "string",
  "reward": 0  // 悬赏金额，0表示无悬赏
}
```

### 4.2 列表
- **GET** `/lostfound/list`
- **查询参数**：`page=1&size=10&type=&category=&keyword=`
- **响应**：
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "type": "LOST",
        "title": "string",
        "category": "ELECTRONICS",
        "images": ["url1"],
        "location": "string",
        "reward": 0,
        "status": "OPEN",  // OPEN, RESOLVED
        "publisherId": 1,
        "publisherName": "string",
        "createTime": "2024-01-01 12:00:00"
      }
    ],
    "total": 30,
    "page": 1,
    "size": 10
  }
}
```

### 4.3 详情
- **GET** `/lostfound/{id}`

### 4.4 认领/发布者确认
- **POST** `/lostfound/{id}/claim`
- **Headers**：`Authorization: Bearer <token>`
- **请求体**：
```json
{
  "message": "string"
}
```

### 4.5 我的发布
- **GET** `/lostfound/my`
- **Headers**：`Authorization: Bearer <token>`

---

## 5. 消息交流模块 (Message)

### 5.1 发送消息
- **POST** `/message/send`
- **Headers**：`Authorization: Bearer <token>`
- **请求体**：
```json
{
  "receiverId": 2,
  "content": "string",
  "relatedType": "SECONDHAND",  // SECONDHAND, LOSTFOUND, SYSTEM
  "relatedId": 1
}
```

### 5.2 会话列表
- **GET** `/message/conversations`
- **Headers**：`Authorization: Bearer <token>`
- **响应**：
```json
{
  "code": 200,
  "data": [
    {
      "conversationId": 1,
      "userId": 2,
      "username": "string",
      "realName": "string",
      "lastMessage": "string",
      "lastTime": "2024-01-01 12:00:00",
      "unreadCount": 3
    }
  ]
}
```

### 5.3 消息详情
- **GET** `/message/history/{userId}`
- **Headers**：`Authorization: Bearer <token>`
- **查询参数**：`page=1&size=20`

### 5.4 已读标记
- **PUT** `/message/read/{userId}`
- **Headers**：`Authorization: Bearer <token>`

---

## 6. 宿舍管理模块 (Dormitory)

### 6.1 宿舍列表
- **GET** `/dormitory/list`
- **查询参数**：`page=1&size=10&building=&floor=&roomNo=`
- **响应**：
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "building": "A栋",
        "floor": 1,
        "roomNo": "101",
        "type": "4人间",
        "capacity": 4,
        "currentCount": 4,
        "gender": "MALE",  // MALE, FEMALE
        "headId": 1,
        "headName": "string"
      }
    ],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

### 6.2 宿舍详情
- **GET** `/dormitory/{id}`

### 6.3 我的宿舍（学生）
- **GET** `/dormitory/my`
- **Headers**：`Authorization: Bearer <token>`
- **响应**：返回当前用户所在宿舍信息

### 6.4 宿舍成员
- **GET** `/dormitory/{id}/members`
- **响应**：返回宿舍内所有成员

### 6.5 报修申请
- **POST** `/dormitory/repair`
- **Headers**：`Authorization: Bearer <token>`
- **请求体**：
```json
{
  "dormitoryId": 1,
  "description": "string",
  "images": ["url1"]
}
```

### 6.6 报修列表（学生）
- **GET** `/dormitory/repair/my`
- **Headers**：`Authorization: Bearer <token>`

### 6.7 报修列表（管理员）
- **GET** `/dormitory/repair/list`
- **Headers**：`Authorization: Bearer <token>`
- **查询参数**：`page=1&size=10&status=`

### 6.8 处理报修（管理员）
- **PUT** `/dormitory/repair/{id}`
- **Headers**：`Authorization: Bearer <token>`
- **请求体**：
```json
{
  "status": "PROCESSING",  // PROCESSING, COMPLETED
  "remark": "string"
}
```

---

## 7. 校园导航模块 (Navigation)

### 7.1 校园POI列表
- **GET** `/navigation/pois`
- **查询参数**：`category=TEACHING` (可选: TEACHING, DINING, LIBRARY, DORMITORY, SPORTS, ADMIN, SCENIC, ENTRANCE)
- **响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "西区教学楼",
      "category": "TEACHING",
      "description": "西区主要教学楼，包含多个多媒体教室",
      "latitude": 31.8395,
      "longitude": 117.2155,
      "floor": "1-6层",
      "openTime": "07:00-22:00"
    }
  ]
}
```

### 7.2 附近搜索（500米内建筑）
- **GET** `/navigation/nearby`
- **查询参数**：`lat=31.8384&lng=117.2167&radius=500`
- **响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "图书馆",
      "category": "LIBRARY",
      "distance": 50.5
    }
  ]
}
```

### 7.3 路径规划（Dijkstra算法）
- **POST** `/navigation/route`
- **请求体**：
```json
{
  "fromLat": 31.8384,
  "fromLng": 117.2167,
  "toLat": 31.8395,
  "toLng": 117.2155,
  "wayType": "WALK"  // 可选: WALK, CYCLE
}
```
- **响应**：
```json
{
  "code": 200,
  "data": {
    "fromLat": 31.8384,
    "fromLng": 117.2167,
    "toLat": 31.8395,
    "toLng": 117.2155,
    "totalDistance": 247.0,
    "estimatedTime": 177,
    "points": [
      {
        "nodeId": 4,
        "nodeName": "图书馆",
        "latitude": 31.8384,
        "longitude": 117.2167,
        "nodeType": "POI",
        "cumulativeDistance": 0.0
      }
    ],
    "description": "从 图书馆 到 西区教学楼，途经 5 个节点，总距离约 247 米"
  }
}
```

### 7.4 路径规划（高德地图代理）
- **POST** `/navigation/route/amap`
- **说明**：后端代理调用高德地图路径规划API，优先使用高德服务，失败后降级使用Dijkstra算法
- **请求体**：
```json
{
  "fromLat": 31.8384,
  "fromLng": 117.2167,
  "toLat": 31.8395,
  "toLng": 117.2155,
  "wayType": "WALK"
}
```
- **响应**：
```json
{
  "code": 200,
  "data": {
    "fromLat": 31.8384,
    "fromLng": 117.2167,
    "toLat": 31.8395,
    "toLng": 117.2155,
    "totalDistance": 350.0,
    "estimatedTime": 280,
    "points": [
      {
        "latitude": 31.8384,
        "longitude": 117.2167,
        "nodeType": "WAYPOINT",
        "cumulativeDistance": 0.0
      },
      {
        "latitude": 31.8386,
        "longitude": 117.2170,
        "nodeType": "WAYPOINT",
        "cumulativeDistance": 50.0
      }
    ],
    "description": "（高德路径规划）"
  }
}
```
- **降级响应**：如果高德服务失败，会返回Dijkstra计算的结果，`description` 会显示"（降级使用校园Dijkstra路径）"

### 7.5 附近搜索（500米内建筑）
- **GET** `/navigation/nearby`
- **查询参数**：`lat=31.8384&lng=117.2167&radius=500&category=TEACHING` (category可选)
- **响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "西区教学楼",
      "category": "TEACHING",
      "description": "西区主要教学楼",
      "latitude": 31.8395,
      "longitude": 117.2155,
      "floor": "1-6层",
      "openTime": "07:00-22:00",
      "distance": 150.5
    }
  ]
}
```

### 7.5 校园区域GeoJSON
- **GET** `/navigation/regions`
- **响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "田径场区域",
      "type": "SPORTS",
      "geojson": {
        "type": "Polygon",
        "coordinates": [[[117.2175,31.8365],[117.2190,31.8365],[117.2190,31.8360],[117.2175,31.8360],[117.2175,31.8365]]]
      }
    }
  ]
}
```

### 7.6 旧版接口（保留）
- **GET** `/navigation/places` - 地点列表
- **GET** `/navigation/places/{id}` - 地点详情
- **GET** `/navigation/map` - 校园地图信息

### 7.7 POI类别说明
| 类别代码 | 说明 |
|----------|------|
| TEACHING | 教学楼 |
| DINING | 食堂 |
| LIBRARY | 图书馆 |
| DORMITORY | 宿舍 |
| SPORTS | 运动场 |
| ADMIN | 行政楼 |
| SCENIC | 景点 |
| ENTRANCE | 校门 |

---

## 8. 通用响应格式

```json
{
  "code": 200,          // 200成功，400参数错误，401未登录，403无权限，404不存在，500服务器错误
  "message": "success", // 提示信息
  "data": {}            // 数据对象
}
```

---

## 9. 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录或token过期 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
