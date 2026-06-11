# 管理员与用户权限分离重构 — 检查清单

## Phase 1: 数据库与枚举重构

### T1.1 修改用户表结构
- [ ] user表添加DORM_MANAGER角色
  - [ ] 执行: `ALTER TABLE user MODIFY COLUMN role ENUM('STUDENT','TEACHER','ADMIN','DORM_MANAGER')`
- [ ] user表添加enabled字段
  - [ ] 执行: `ALTER TABLE user ADD COLUMN enabled TINYINT DEFAULT 1 COMMENT '是否启用'`
  - [ ] 添加索引: `INDEX idx_enabled (enabled)`

### T1.2 重构二手商品状态枚举
- [ ] 修改GoodsStatus枚举
  - [ ] 修改为: PENDING, APPROVED, REJECTED, REMOVED, SOLD
  - [ ] 文件位置: `/src/main/java/com/caspar/entity/enums/GoodsStatus.java`
- [ ] secondhand表结构变更
  - [ ] 执行: `ALTER TABLE secondhand MODIFY COLUMN status ENUM('PENDING','APPROVED','REJECTED','REMOVED','SOLD') DEFAULT 'PENDING'`
  - [ ] 执行: `ALTER TABLE secondhand ADD COLUMN reject_reason VARCHAR(200) COMMENT '拒绝原因' AFTER status`
  - [ ] 执行: `ALTER TABLE secondhand ADD COLUMN remove_reason VARCHAR(200) COMMENT '下架原因' AFTER reject_reason`

### T1.3 重构失物招领状态枚举
- [ ] lostfound表结构变更
  - [ ] 执行: `ALTER TABLE lostfound MODIFY COLUMN status ENUM('ACTIVE','CLAIMED','REMOVED') DEFAULT 'ACTIVE'`
  - [ ] 执行: `ALTER TABLE lostfound ADD COLUMN remove_reason VARCHAR(200) COMMENT '下架原因' AFTER status`

### T1.4 扩展消息表
- [ ] message表结构变更
  - [ ] 执行: `ALTER TABLE message ADD COLUMN type VARCHAR(20) DEFAULT 'PERSONAL' COMMENT '类型: BROADCAST, PERSONAL'`
  - [ ] 执行: `ALTER TABLE message ADD COLUMN is_broadcast TINYINT DEFAULT 0 COMMENT '是否系统公告'`

### T1.5 扩展校园地点表
- [ ] campus_place表结构变更
  - [ ] 执行: `ALTER TABLE campus_place ADD COLUMN visibility VARCHAR(20) DEFAULT 'PUBLIC' COMMENT '可见性: PUBLIC, PRIVATE'`
  - [ ] 执行: `ALTER TABLE campus_place ADD COLUMN created_by BIGINT COMMENT '创建者ID'`

---

## Phase 2: 后端接口权限控制

### T2.1 创建统一权限校验工具
- [ ] 创建角色校验工具类
  - [ ] 文件: `/src/main/java/com/caspar/util/RoleCheckUtil.java`
- [ ] 修改Controller基类
  - [ ] 添加getCurrentUserRole()方法

### T2.2 重构二手交易Controller
- [ ] SecondhandUserController
  - [ ] POST /user/secondhand/publish - 发布商品
  - [ ] GET /user/secondhand/list - 商品列表(仅APPROVED)
  - [ ] GET /user/secondhand/my - 我的发布
  - [ ] PUT /user/secondhand/{id} - 修改商品(REJECTED→PENDING)
  - [ ] POST /user/secondhand/{id}/buy - 购买商品
- [ ] SecondhandAdminController
  - [ ] GET /admin/secondhand/pending - 待审核列表
  - [ ] GET /admin/secondhand/all - 所有商品
  - [ ] PUT /admin/secondhand/{id}/approve - 审核通过
  - [ ] PUT /admin/secondhand/{id}/reject - 审核拒绝
  - [ ] PUT /admin/secondhand/{id}/remove - 下架商品

### T2.3 重构失物招领Controller
- [ ] LostFoundUserController
  - [ ] POST /user/lostfound/publish - 发布
  - [ ] GET /user/lostfound/list - 列表(仅ACTIVE)
  - [ ] GET /user/lostfound/my - 我的发布
  - [ ] POST /user/lostfound/{id}/claim - 认领
  - [ ] PUT /user/lostfound/{id}/resolve - 标记已认领
- [ ] LostFoundAdminController
  - [ ] GET /admin/lostfound/all - 所有帖子
  - [ ] PUT /admin/lostfound/{id}/remove - 下架帖子

### T2.4 重构宿舍管理Controller
- [ ] DormitoryUserController
  - [ ] GET /user/dormitory/my - 我的宿舍
  - [ ] POST /user/dormitory/repair - 提交报修
  - [ ] GET /user/dormitory/repair/my - 我的报修列表
- [ ] DormitoryAdminController
  - [ ] GET /admin/dormitory/list - 宿舍列表
  - [ ] POST /admin/dormitory/assign - 分配入住
  - [ ] POST /admin/dormitory/checkout - 退宿
  - [ ] GET /admin/dormitory/repair/all - 所有报修
  - [ ] PUT /admin/dormitory/repair/{id}/handle - 处理报修

### T2.5 重构消息Controller
- [ ] MessageUserController
  - [ ] GET /user/message/list - 我的消息
  - [ ] PUT /user/message/read/{id} - 标记已读
  - [ ] GET /user/message/unread-count - 未读数
- [ ] MessageAdminController
  - [ ] POST /admin/message/broadcast - 发送系统公告

### T2.6 重构校园导航Controller
- [ ] NavigationUserController
  - [ ] GET /user/navigation/places - 公共坐标
  - [ ] GET /user/navigation/my-places - 我的私有坐标
  - [ ] POST /user/navigation/places - 创建私有坐标
  - [ ] PUT /user/navigation/places/{id} - 编辑私有坐标
  - [ ] DELETE /user/navigation/places/{id} - 删除私有坐标
- [ ] NavigationAdminController
  - [ ] GET /admin/navigation/all - 所有公共坐标
  - [ ] POST /admin/navigation/places - 创建公共坐标
  - [ ] PUT /admin/navigation/places/{id} - 编辑公共坐标
  - [ ] DELETE /admin/navigation/places/{id} - 删除公共坐标

### T2.7 创建账号管理Controller
- [ ] UserManageController
  - [ ] GET /admin/user/list - 用户列表
  - [ ] POST /admin/user/create - 创建用户
  - [ ] PUT /admin/user/{id}/disable - 禁用用户
  - [ ] PUT /admin/user/{id}/enable - 启用用户
  - [ ] PUT /admin/user/{id}/reset-password - 重置密码

---

## Phase 3: 前端路由与菜单

### T3.1 修改路由配置
- [ ] 修改router/index.js
  - [ ] 路由meta添加role字段
  - [ ] 添加管理员专属路由
- [ ] 创建AdminLayout.vue
  - [ ] 文件: `/campus-frontend/src/layouts/AdminLayout.vue`

### T3.2 实现路由守卫
- [ ] 修改beforeEach守卫
  - [ ] 检查meta.role
  - [ ] 无权限重定向

### T3.3 修改侧边栏菜单
- [ ] MainLayout.vue修改
  - [ ] 根据userStore.role动态生成菜单
  - [ ] 管理员菜单项
  - [ ] 普通用户菜单项

### T3.4 修改UserStore
- [ ] stores/user.js
  - [ ] 添加isAdminOrDormManager计算属性

---

## Phase 4: 二手交易模块

### T4.1 后端实现
- [ ] SecondhandGoodsServiceImpl
  - [ ] 修改getList只返回APPROVED
  - [ ] 添加审核逻辑
  - [ ] 添加rejectReason/removeReason处理
- [ ] SecondhandGoodsMapper.xml
  - [ ] 添加rejectReason/removeReason字段

### T4.2 前端-用户端
- [ ] 修改SecondHand.vue
  - [ ] 只显示APPROVED商品
  - [ ] 添加"我的发布"入口
- [ ] 创建SecondHandMy.vue
  - [ ] 显示所有状态
  - [ ] 状态标签
  - [ ] 重新提交按钮(REJECTED)
- [ ] 修改SecondHandDetail.vue
  - [ ] 根据状态显示不同按钮

### T4.3 前端-管理端
- [ ] 创建SecondHandAdmin.vue
  - [ ] 待审核列表
  - [ ] 审核通过按钮
  - [ ] 审核拒绝弹窗(填写原因)
  - [ ] 所有商品列表
  - [ ] 下架操作弹窗(填写原因)

---

## Phase 5: 失物招领模块

### T5.1 后端实现
- [ ] LostFoundServiceImpl
  - [ ] 修改getList只返回ACTIVE
  - [ ] 添加管理员下架逻辑

### T5.2 前端-用户端
- [ ] 修改LostFound.vue
  - [ ] 只显示ACTIVE帖子
  - [ ] 添加"我的发布"入口
- [ ] 创建LostFoundMy.vue

### T5.3 前端-管理端
- [ ] 创建LostFoundAdmin.vue
  - [ ] 所有帖子列表
  - [ ] 下架操作

---

## Phase 6: 宿舍管理模块

### T6.1 后端实现
- [ ] 完善宿舍管理Service
  - [ ] 分配入住
  - [ ] 退宿处理
  - [ ] 换寝审批

### T6.2 前端-用户端
- [ ] 修改DormitoryInfo.vue

### T6.3 前端-管理端
- [ ] 创建DormitoryAdmin.vue
  - [ ] 宿舍楼管理
  - [ ] 房间管理
  - [ ] 报修处理

---

## Phase 7: 消息中心模块

### T7.1 后端实现
- [ ] MessageServiceImpl
  - [ ] 区分BROADCAST/PERSONAL
  - [ ] 管理员发送公告

### T7.2 前端-用户端
- [ ] 修改Messages.vue
  - [ ] 区分公告和消息
  - [ ] 分类展示

### T7.3 前端-管理端
- [ ] 创建MessageBroadcast.vue
  - [ ] 发送公告表单

---

## Phase 8: 校园导航模块

### T8.1 后端实现
- [ ] NavigationService
  - [ ] 添加visibility过滤
  - [ ] 用户私有坐标管理
  - [ ] 管理员公共坐标管理

### T8.2 前端-用户端
- [ ] 修改Navigation.vue
  - [ ] 显示公共坐标
  - [ ] 显示私有坐标
  - [ ] 我的坐标管理

### T8.3 前端-管理端
- [ ] 创建NavigationAdmin.vue

---

## Phase 9: 账号管理模块

### T9.1 后端实现
- [ ] UserManageService
  - [ ] 用户CRUD
  - [ ] 禁用/启用
  - [ ] 重置密码

### T9.2 前端
- [ ] 创建UserManage.vue
  - [ ] 用户列表
  - [ ] 新建用户
  - [ ] 操作按钮

---

## 验收标准

### 功能验收
- [ ] ADMIN角色可以访问所有管理接口
- [ ] DORM_MANAGER可以访问宿舍管理接口
- [ ] USER只能访问用户端接口
- [ ] 二手商品审核流程完整
- [ ] 失物招领下架功能正常
- [ ] 消息中心公告功能正常
- [ ] 校园导航公共/私有坐标区分正常
- [ ] 账号管理功能正常

### 界面验收
- [ ] 管理员侧边栏显示管理菜单
- [ ] 普通用户侧边栏显示用户菜单
- [ ] 无权限访问时正确跳转
- [ ] 各模块页面样式与现有风格一致

### 安全验收
- [ ] 后端接口角色校验正常
- [ ] 用户无法通过URL直接访问管理页面
- [ ] 用户无法操作他人数据
