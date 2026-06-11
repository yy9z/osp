# 前端API接口文档

## 概述
本项目前端基于Vue 3 + Axios，API请求封装位于 `src/api/` 目录。

## 基础配置
- Base URL: 通过Vite代理配置，访问 `/api` 即转发到 `http://localhost:8080/api`
- 认证: 使用JWT Token，登录后自动存储在localStorage

---

## 1. 用户模块 (user.js)

### 1.1 用户登录
```javascript
userApi.login(data)
// 参数: { username, password }
```

### 1.2 用户注册
```javascript
userApi.register(data)
// 参数: { username, password, realName, phone, email, role }
```

### 1.3 获取用户信息
```javascript
userApi.getUserInfo()
```

### 1.4 修改密码
```javascript
userApi.updatePassword(data)
// 参数: { oldPassword, newPassword }
```

---

## 2. 二手交易模块 (secondhand.js)

### 2.1 获取商品列表
```javascript
secondhandApi.getList(params)
// 参数: { page, size, category, keyword, sort }
```

### 2.2 获取商品详情
```javascript
secondhandApi.getDetail(id)
```

### 2.3 发布商品
```javascript
secondhandApi.publish(data)
// 参数: { title, description, price, category, images, condition }
```

### 2.4 我的发布
```javascript
secondhandApi.getMyList(params)
```

### 2.5 删除商品
```javascript
secondhandApi.delete(id)
```

### 2.6 标记已售
```javascript
secondhandApi.markSold(id)
```

---

## 3. 失物招领模块 (lostfound.js)

### 3.1 获取列表
```javascript
lostfoundApi.getList(params)
// 参数: { page, size, type, category, keyword }
```

### 3.2 发布信息
```javascript
lostfoundApi.publish(data)
// 参数: { type, title, description, category, images, location, reward }
```

### 3.3 认领
```javascript
lostfoundApi.claim(id, data)
// 参数: { message }
```

### 3.4 我的发布
```javascript
lostfoundApi.getMyList()
```

---

## 4. 消息模块 (message.js)

### 4.1 发送消息
```javascript
messageApi.send(data)
// 参数: { receiverId, content, relatedType, relatedId }
```

### 4.2 获取会话列表
```javascript
messageApi.getConversations()
```

### 4.3 获取消息历史
```javascript
messageApi.getHistory(userId, params)
// 参数: { page, size }
```

### 4.4 标记已读
```javascript
messageApi.markAsRead(userId)
```

---

## 5. 宿舍管理模块 (dormitory.js)

### 5.1 获取宿舍列表
```javascript
dormitoryApi.getList(params)
// 参数: { page, size, building, floor, roomNo }
```

### 5.2 我的宿舍
```javascript
dormitoryApi.getMyDormitory()
```

### 5.3 宿舍成员
```javascript
dormitoryApi.getMembers(id)
```

### 5.4 提交报修
```javascript
dormitoryApi.submitRepair(data)
// 参数: { dormitoryId, description, images }
```

### 5.5 我的报修
```javascript
dormitoryApi.getMyRepairs()
```

### 5.6 管理员-报修列表
```javascript
dormitoryApi.getRepairList(params)
// 参数: { page, size, status }
```

### 5.7 管理员-处理报修
```javascript
dormitoryApi.handleRepair(id, data)
// 参数: { status, remark }
```

---

## 6. 校园导航模块 (navigation.js)

### 6.1 获取所有POI
```javascript
navigationApi.getPois()
```

### 6.2 按类别获取POI
```javascript
navigationApi.getPoisByCategory(category)
// category: TEACHING, DINING, LIBRARY, DORMITORY, SPORTS, ADMIN, SCENIC, ENTRANCE
```

### 6.3 附近搜索
```javascript
navigationApi.getNearby(params)
// 参数: { lat, lng, radius } (radius默认500米)
```

### 6.4 路径规划 ⚠️ 参数名已修正
```javascript
navigationApi.calculateRoute(data)
// 参数: { fromLat, fromLng, toLat, toLng, wayType }
// 注意: 使用 fromLat/fromLng/toLat/toLng，不要使用 startLat/startLng
```

### 6.5 获取区域GeoJSON
```javascript
navigationApi.getRegions()
```

### 6.6 获取地点详情
```javascript
navigationApi.getPoiDetail(id)
```

### 6.7 搜索地点
```javascript
navigationApi.searchPois(keyword)
```

---

## 7. POI类别说明

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

## 8. 请求拦截器配置

位于 `src/api/request.js`:

```javascript
// 请求拦截器
- 自动添加Token到请求头
- 处理登录超时跳转

// 响应拦截器
- 统一处理错误提示
- 处理401跳转登录
```

---

## 9. 状态管理

位于 `src/stores/user.js`:
- 存储用户登录信息
- 提供登录/登出方法
- 提供Token存取

---

## 环境变量

在 `.env` 文件中配置:
```
VITE_API_BASE_URL=/api
VITE_MAP_TILE_URL=https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png
```
