# 校园综合服务系统 — 管理员与用户权限分离重构规格说明书

## 1. 项目概述

### 1.1 项目名称
校园综合服务系统 (Campus Service Platform)

### 1.2 项目目标
将现有校园综合服务系统重构为支持管理员（ADMIN/DORM_MANAGER）和普通用户（USER）角色分离的系统，实现基于角色的权限控制（RBAC）。

### 1.3 项目范围
- 二手交易模块
- 失物招领模块
- 宿舍管理模块
- 消息中心模块
- 校园导航模块
- 账号管理模块（仅管理员）

---

## 2. 技术架构

### 2.1 后端技术栈
- Spring Boot 3.x
- MyBatis
- MySQL
- Spring Security (JWT)

### 2.2 前端技术栈
- Vue 3
- Element Plus
- Pinia (状态管理)
- Vue Router

### 2.3 现有角色定义
```java
public enum UserRole {
    STUDENT("学生"),
    TEACHER("教师"),
    ADMIN("管理员"),
    DORM_MANAGER("宿管员");
}
```

---

## 3. 功能模块规格

### 3.1 二手交易模块

#### 3.1.1 商品状态枚举（需重构）
**现有状态：**
- AUDITING("审核中")
- ACTIVE("在售")
- SOLD("已售出")

**新状态（按需求）：**
- PENDING — 待审核（用户提交后默认状态）
- APPROVED — 已上架（管理员审核通过）
- REJECTED — 审核拒绝（管理员拒绝，用户可修改后重新提交）
- REMOVED — 已下架（管理员强制下架）
- SOLD — 已售出

#### 3.1.2 数据库变更
```sql
ALTER TABLE secondhand MODIFY COLUMN status ENUM('PENDING', 'APPROVED', 'REJECTED', 'REMOVED', 'SOLD') DEFAULT 'PENDING';
ALTER TABLE secondhand ADD COLUMN reject_reason VARCHAR(200) COMMENT '拒绝原因' AFTER status;
ALTER TABLE secondhand ADD COLUMN remove_reason VARCHAR(200) COMMENT '下架原因' AFTER reject_reason;
```
 
#### 3.1.3 接口设计

| 接口 | 方法 | 路径 | 角色 | 说明 |
|------|------|------|------|------|
| 发布商品 | POST | /api/user/secondhand/publish | USER | 用户发布，状态=PENDING |
| 商品列表（用户） | GET | /api/user/secondhand/list | USER | 仅显示APPROVED |
| 我的发布 | GET | /api/user/secondhand/my | USER | 查看自己所有商品及状态 |
| 修改被拒绝商品 | PUT | /api/user/secondhand/{id} | USER | 只能修改REJECTED状态的商品 |
| 购买商品 | POST | /api/user/secondhand/{id}/buy | USER | 标记为SOLD |
| 管理员审核列表 | GET | /api/admin/secondhand/pending | ADMIN | 查看所有PENDING商品 |
| 审核商品 | PUT | /api/admin/secondhand/{id}/approve | ADMIN | 改为APPROVED |
| 拒绝商品 | PUT | /api/admin/secondhand/{id}/reject | ADMIN | 改为REJECTED，需填写拒绝原因 |
| 管理员下架商品 | PUT | /api/admin/secondhand/{id}/remove | ADMIN | 改为REMOVED，需填写下架原因 |
| 管理员查看所有商品 | GET | /api/admin/secondhand/all | ADMIN | 查看所有状态商品 |

---

### 3.2 失物招领模块

#### 3.2.1 帖子状态枚举（需重构）
**现有状态：**
- OPEN - 有效
- RESOLVED - 已解决

**新状态（按需求）：**
- ACTIVE — 有效（发布后默认状态）
- CLAIMED — 已认领
- REMOVED — 已下架（管理员下架）

#### 3.2.2 数据库变更
```sql
ALTER TABLE lostfound MODIFY COLUMN status ENUM('ACTIVE', 'CLAIMED', 'REMOVED') DEFAULT 'ACTIVE';
ALTER TABLE lostfound ADD COLUMN remove_reason VARCHAR(200) COMMENT '下架原因' AFTER status;
```

#### 3.2.3 接口设计

| 接口 | 方法 | 路径 | 角色 | 说明 |
|------|------|------|------|------|
| 发布失物/招领 | POST | /api/user/lostfound/publish | USER | 发布后直接ACTIVE |
| 列表 | GET | /api/user/lostfound/list | USER | 仅显示ACTIVE |
| 我的发布 | GET | /api/user/lostfound/my | USER | 查看自己发布的 |
| 认领 | POST | /api/user/lostfound/{id}/claim | USER | 申请认领 |
| 标记已认领 | PUT | /api/user/lostfound/{id}/resolve | USER | 发布者标记CLAIMED |
| 管理员查看所有 | GET | /api/admin/lostfound/all | ADMIN | 查看所有状态 |
| 管理员下架 | PUT | /api/admin/lostfound/{id}/remove | ADMIN | 改为REMOVED，需填写原因 |

---

### 3.3 宿舍管理模块

#### 3.3.1 接口设计

| 接口 | 方法 | 路径 | 角色 | 说明 |
|------|------|------|------|------|
| 获取我的宿舍 | GET | /api/user/dormitory/my | USER | 获取当前用户宿舍信息 |
| 提交报修 | POST | /api/user/dormitory/repair | USER | 学生提交报修 |
| 我的报修列表 | GET | /api/user/dormitory/repair/my | USER | 查看我的报修 |
| 管理员宿舍列表 | GET | /api/admin/dormitory/list | ADMIN/DORM_MANAGER | 管理宿舍楼、房间 |
| 分配学生入住 | POST | /api/admin/dormitory/assign | ADMIN/DORM_MANAGER | 分配学生到宿舍 |
| 办理退宿 | POST | /api/admin/dormitory/checkout | ADMIN/DORM_MANAGER | 学生退宿 |
| 换寝审批 | PUT | /api/admin/dormitory/transfer/{id} | ADMIN/DORM_MANAGER | 审批换寝申请 |
| 管理员报修列表 | GET | /api/admin/dormitory/repair/all | ADMIN/DORM_MANAGER | 查看所有报修 |
| 处理报修 | PUT | /api/admin/dormitory/repair/{id}/handle | ADMIN/DORM_MANAGER | 处理报修（PROCESSING/COMPLETED） |

---

### 3.4 消息中心模块

#### 3.4.1 消息类型枚举
- BROADCAST — 系统公告（全局广播，所有用户可见）
- PERSONAL — 个人通知（针对特定用户）

#### 3.4.2 数据库变更
```sql
ALTER TABLE message ADD COLUMN type VARCHAR(20) DEFAULT 'PERSONAL' COMMENT '类型: BROADCAST, PERSONAL' AFTER content;
ALTER TABLE message ADD COLUMN is_broadcast TINYINT DEFAULT 0 COMMENT '是否系统公告' AFTER type;
```

#### 3.4.3 接口设计

| 接口 | 方法 | 路径 | 角色 | 说明 |
|------|------|------|------|------|
| 我的消息列表 | GET | /api/user/message/list | USER | 个人通知+系统公告 |
| 发送系统公告 | POST | /api/admin/message/broadcast | ADMIN | 发送全局公告 |
| 发送个人通知 | POST | /api/admin/message/notify | ADMIN | 发送指定用户通知 |
| 标记已读 | PUT | /api/user/message/read/{id} | USER | 标记单条消息已读 |
| 获取未读数 | GET | /api/user/message/unread-count | USER | 未读消息数量 |

---

### 3.5 校园导航模块

#### 3.5.1 坐标可见性枚举
- PUBLIC — 公共坐标（所有用户可见，仅管理员可操作）
- PRIVATE — 私有坐标（仅创建者本人可见）

#### 3.5.2 数据库变更
```sql
ALTER TABLE campus_place ADD COLUMN visibility VARCHAR(20) DEFAULT 'PUBLIC' COMMENT '可见性: PUBLIC, PRIVATE' AFTER floor;
ALTER TABLE campus_place ADD COLUMN created_by BIGINT COMMENT '创建者ID' AFTER visibility;
```

#### 3.5.3 接口设计

| 接口 | 方法 | 路径 | 角色 | 说明 |
|------|------|------|------|------|
| 获取公共坐标 | GET | /api/user/navigation/places | ALL | 获取所有PUBLIC坐标 |
| 我的私有坐标 | GET | /api/user/navigation/my-places | USER | 获取自己创建的PRIVATE坐标 |
| 创建私有坐标 | POST | /api/user/navigation/places | USER | 创建PRIVATE坐标 |
| 编辑私有坐标 | PUT | /api/user/navigation/places/{id} | USER | 只能编辑自己创建的 |
| 删除私有坐标 | DELETE | /api/user/navigation/places/{id} | USER | 只能删除自己创建的 |
| 管理员获取所有坐标 | GET | /api/admin/navigation/all | ADMIN | 获取所有PUBLIC坐标 |
| 管理员创建公共坐标 | POST | /api/admin/navigation/places | ADMIN | 创建PUBLIC坐标 |
| 管理员编辑公共坐标 | PUT | /api/admin/navigation/places/{id} | ADMIN | 编辑PUBLIC坐标 |
| 管理员删除公共坐标 | DELETE | /api/admin/navigation/places/{id} | ADMIN | 删除PUBLIC坐标 |

---

### 3.6 账号管理模块（仅管理员）

#### 3.6.1 接口设计

| 接口 | 方法 | 路径 | 角色 | 说明 |
|------|------|------|------|------|
| 用户列表 | GET | /api/admin/user/list | ADMIN | 分页获取所有用户 |
| 创建用户 | POST | /api/admin/user/create | ADMIN | 可指定角色 |
| 禁用用户 | PUT | /api/admin/user/{id}/disable | ADMIN | 禁用账号 |
| 启用用户 | PUT | /api/admin/user/{id}/enable | ADMIN | 启用账号 |
| 重置密码 | PUT | /api/admin/user/{id}/reset-password | ADMIN | 重置为默认密码 |

---

## 4. 前端页面规格

### 4.1 路由权限守卫
```javascript
// 路由元信息增加 role 字段
{
  path: '/admin',
  component: AdminLayout,
  meta: { requiresAuth: true, role: 'ADMIN' },
  children: [...]
}

// 路由守卫检查角色
router.beforeEach((to, from, next) => {
  if (to.meta.role && userStore.role !== to.meta.role) {
    return next('/') // 无权限跳转首页
  }
  next()
})
```

### 4.2 菜单显示规则

**管理员（ADMIN/DORM_MANAGER）侧边栏：**
- 二手交易审核管理
- 失物招领管理
- 宿舍管理
- 系统公告管理
- 校园导航坐标管理
- 账号管理（仅ADMIN）

**普通用户（USER）侧边栏：**
- 二手交易
- 失物招领
- 消息中心
- 我的宿舍
- 校园导航

### 4.3 页面组件拆分

#### 二手交易
- `/views/secondhand/UserGoodsList.vue` — 用户浏览/发布
- `/views/secondhand/AdminGoodsList.vue` — 管理员审核列表
- `/views/secondhand/GoodsDetail.vue` — 商品详情（通用）

#### 失物招领
- `/views/lostfound/UserLostFound.vue` — 用户发布/浏览
- `/views/lostfound/AdminLostFound.vue` — 管理员下架管理
- `/views/lostfound/LostFoundDetail.vue` — 详情（通用）

#### 宿舍管理
- `/views/dormitory/MyDormitory.vue` — 学生查看宿舍
- `/views/dormitory/AdminDormitory.vue` — 管理员宿舍管理

#### 消息中心
- `/views/message/UserMessages.vue` — 用户消息列表
- `/views/message/AdminAnnouncements.vue` — 管理员发布公告

#### 校园导航
- `/views/navigation/UserMap.vue` — 用户地图（公共+私有坐标）
- `/views/navigation/AdminMap.vue` — 管理员坐标管理

#### 账号管理（仅ADMIN）
- `/views/admin/UserManagement.vue` — 用户账号管理

---

## 5. 数据库设计汇总

### 5.1 表结构变更

| 表名 | 变更类型 | 变更内容 |
|------|----------|----------|
| user | 修改 | role ENUM增加DORM_MANAGER，添加enabled字段 |
| secondhand | 修改 | status ENUM重构，添加reject_reason、remove_reason |
| lostfound | 修改 | status ENUM重构，添加remove_reason |
| campus_place | 修改 | 添加visibility、created_by字段 |
| message | 修改 | 添加type、is_broadcast字段 |

### 5.2 新增表（如需要）

| 表名 | 说明 |
|------|------|
| dormitory_building | 宿舍楼表（增强现有dormitory） |
| dormitory_transfer | 换寝申请表 |

---

## 6. 安全控制

### 6.1 权限验证层次

1. **路由层**：前端路由守卫检查用户角色
2. **接口层**：后端接口验证用户角色
3. **业务层**：Service层验证操作权限

### 6.2 接口角色验证示例
```java
@PutMapping("/admin/secondhand/{id}/approve")
public Result<Void> approve(@PathVariable Long id) {
    Long userId = getCurrentUserId();
    String role = getCurrentUserRole();
    
    if (!"ADMIN".equals(role)) {
        return Result.forbidden("无权限执行此操作");
    }
    // ... 业务逻辑
}
```

---

## 7. 实施优先级

### 第一阶段（核心）
1. 数据库表结构变更
2. 后端枚举类重构
3. 后端接口权限控制
4. 前端路由守卫

### 第二阶段（功能）
5. 二手交易模块完整功能
6. 失物招领模块完整功能

### 第三阶段（完善）
7. 宿舍管理模块
8. 消息中心模块
9. 校园导航模块
10. 账号管理模块

---

## 8. 兼容性考虑

### 8.1 向后兼容
- 保留现有接口路径，通过角色判断返回不同数据
- 渐进式迁移，非ADMIN用户不受影响

### 8.2 数据迁移
- 现有AUDITING → PENDING
- 现有ACTIVE → APPROVED
- 现有OPEN → ACTIVE
