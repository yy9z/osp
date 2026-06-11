# 管理员与用户权限分离重构 — 任务分解

## 任务概览

| 序号 | 任务阶段 | 预估工作量 |
|------|----------|------------|
| Phase 1 | 数据库与枚举重构 | 1天 |
| Phase 2 | 后端接口权限控制 | 2天 |
| Phase 3 | 前端路由与菜单 | 1天 |
| Phase 4 | 二手交易模块 | 2天 |
| Phase 5 | 失物招领模块 | 1天 |
| Phase 6 | 宿舍管理模块 | 1天 |
| Phase 7 | 消息中心模块 | 1天 |
| Phase 8 | 校园导航模块 | 1天 |
| Phase 9 | 账号管理模块 | 1天 |

---

## Phase 1: 数据库与枚举重构

### T1.1 修改用户表结构
- [ ] user表添加DORM_MANAGER角色
- [ ] user表添加enabled字段（账号启用/禁用）
- [ ] 更新init.sql

### T1.2 重构二手商品状态枚举
- [ ] 修改GoodsStatus枚举: PENDING, APPROVED, REJECTED, REMOVED, SOLD
- [ ] secondhand表添加reject_reason字段
- [ ] secondhand表添加remove_reason字段

### T1.3 重构失物招领状态枚举
- [ ] 修改lostfound表status: ACTIVE, CLAIMED, REMOVED
- [ ] lostfound表添加remove_reason字段

### T1.4 扩展消息表
- [ ] message表添加type字段(BROADCAST/PERSONAL)
- [ ] message表添加is_broadcast字段

### T1.5 扩展校园地点表
- [ ] campus_place表添加visibility字段(PUBLIC/PRIVATE)
- [ ] campus_place表添加created_by字段

---

## Phase 2: 后端接口权限控制

### T2.1 创建统一权限校验工具
- [ ] 创建RoleCheck注解
- [ ] 创建RoleCheckInterceptor拦截器
- [ ] 修改Controller基类添加getCurrentUserRole()方法

### T2.2 重构二手交易Controller
- [ ] 创建SecondhandUserController (用户端)
- [ ] 创建SecondhandAdminController (管理端)
- [ ] 添加审核/拒绝/下架接口

### T2.3 重构失物招领Controller
- [ ] 创建LostFoundUserController (用户端)
- [ ] 创建LostFoundAdminController (管理端)
- [ ] 添加管理员下架接口

### T2.4 重构宿舍管理Controller
- [ ] 创建DormitoryUserController (用户端)
- [ ] 创建DormitoryAdminController (管理端)
- [ ] 完善管理员功能

### T2.5 重构消息Controller
- [ ] 添加系统公告接口(Admin)
- [ ] 区分个人通知和系统公告(User)

### T2.6 重构校园导航Controller
- [ ] 添加用户私有坐标管理接口
- [ ] 添加管理员公共坐标管理接口
- [ ] 添加坐标可见性过滤

### T2.7 创建账号管理Controller
- [ ] 创建UserManageController
- [ ] 实现用户CRUD接口
- [ ] 实现禁用/启用/重置密码

---

## Phase 3: 前端路由与菜单

### T3.1 修改路由配置
- [ ] 路由meta添加role字段
- [ ] 创建AdminLayout布局
- [ ] 添加管理员路由

### T3.2 实现路由守卫
- [ ] 检查用户角色
- [ ] 无权限时重定向

### T3.3 修改侧边栏菜单
- [ ] 根据用户角色动态生成菜单
- [ ] 管理员菜单: 审核管理、宿舍管理、公告管理、导航管理、账号管理
- [ ] 普通用户菜单: 二手交易、失物招领、消息中心、我的宿舍、校园导航

### T3.4 修改UserStore
- [ ] 添加更多权限判断方法
- [ ] 添加isAdminOrDormManager()方法

---

## Phase 4: 二手交易模块

### T4.1 后端实现
- [ ] 修改SecondhandGoodsServiceImpl审核逻辑
- [ ] 添加rejectReason/removeReason字段处理
- [ ] 实现分页查询优化

### T4.2 前端-用户端
- [ ] 修改商品列表，只显示APPROVED
- [ ] 添加"我的发布"页面，显示所有状态
- [ ] 添加状态标签显示(待审核/已上架/已拒绝/已下架/已售出)
- [ ] 添加"重新提交"按钮(REJECTED状态)

### T4.管理端
-3 前端- [ ] 创建审核管理页面
- [ ] 待审核列表
- [ ] 审核通过/拒绝操作
- [ ] 拒绝/下架需填写原因
- [ ] 所有商品列表(可下架)

---

## Phase 5: 失物招领模块

### T5.1 后端实现
- [ ] 修改LostFoundServiceImpl
- [ ] 添加管理员下架逻辑

### T5.2 前端-用户端
- [ ] 修改列表只显示ACTIVE
- [ ] 添加"我的发布"页面
- [ ] 添加认领功能

### T5.3 前端-管理端
- [ ] 创建失物招领管理页面
- [ ] 所有帖子列表
- [ ] 下架操作需填写原因

---

## Phase 6: 宿舍管理模块

### T6.1 后端实现
- [ ] 分离用户/管理员接口
- [ ] 添加宿舍分配、退宿、换寝审批

### T6.2 前端-用户端
- [ ] 查看自己宿舍信息
- [ ] 提交报修(已有)
- [ ] 查看报修列表(已有)

### T6.3 前端-管理端
- [ ] 创建宿舍管理页面
- [ ] 宿舍楼、房间管理
- [ ] 入住分配
- [ ] 报修处理

---

## Phase 7: 消息中心模块

### T7.1 后端实现
- [ ] 添加系统公告类型
- [ ] 管理员发送公告接口
- [ ] 用户查看公告列表

### T7.2 前端-用户端
- [ ] 区分系统公告和个人消息
- [ ] 消息列表优化

### T7.3 前端-管理端
- [ ] 创建公告发布页面
- [ ] 发送全局公告

---

## Phase 8: 校园导航模块

### T8.1 后端实现
- [ ] 添加坐标可见性过滤
- [ ] 用户私有坐标管理
- [ ] 管理员公共坐标管理

### T8.2 前端-用户端
- [ ] 公共坐标显示
- [ ] 我的私有坐标管理
- [ ] 坐标创建/编辑/删除

### T8.3 前端-管理端
- [ ] 公共坐标管理页面
- [ ] 坐标分类管理

---

## Phase 9: 账号管理模块

### T9.1 后端实现
- [ ] 用户列表分页
- [ ] 创建用户
- [ ] 禁用/启用
- [ ] 重置密码

### T9.2 前端
- [ ] 创建用户管理页面
- [ ] 用户列表(分页)
- [ ] 新建用户弹窗
- [ ] 操作按钮(禁用/启用/重置密码)

---

## 任务依赖关系

```
Phase 1 (数据库)
    ↓
Phase 2 (后端接口)
    ↓
Phase 3 (前端路由)
    ↓
    ├→ Phase 4 (二手交易)
    ├→ Phase 5 (失物招领)
    ├→ Phase 6 (宿舍管理)
    ├→ Phase 7 (消息中心)
    ├→ Phase 8 (校园导航)
    └→ Phase 9 (账号管理)
```

---

## 关键里程碑

1. **M1**: Phase 1-3 完成 → 基础框架搭建完成
2. **M2**: Phase 4-5 完成 → 交易和招领模块完成
3. **M3**: Phase 6-9 完成 → 所有模块完成
