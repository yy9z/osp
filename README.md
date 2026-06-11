# 高校校园一站式平台

面向高校校园日常事务的一站式服务平台，覆盖用户账号、二手交易、失物招领、宿舍管理、消息通知、校园导航与智能 Agent 助手。

## 技术栈

- 后端：Spring Boot 3.4、Java 21、Spring Security、JWT、MyBatis/MyBatis-Plus、MySQL、Redis、Caffeine
- 前端：Vue 3、Vite、Pinia、Element Plus、Leaflet、高德地图 Web 服务
- 智能能力：单 Agent + Function Calling，本地 `AgentTool` 业务函数调用，MCP 作为可选扩展通道

## 核心功能

- 用户体系：注册登录、个人信息、头像、密码修改、角色权限
- 二手交易：发布、审核、收藏、上下架、标记已售、求购订阅、面交建议、议价模板、卖家可信度
- 失物招领：寻物/招领发布、认领申请、我的发布、状态流转、智能匹配
- 宿舍管理：楼栋/宿舍分配、入住退宿、学生报修、宿管处理、管理员管理
- 消息中心：首页通知、站内会话、未读统计、系统公告
- 校园导航：地点解析、路线规划、Agent 语义导航、多场景偏好、分段路线与安全分析
- 智能助手：自然语言触发报修、二手搜索/发布、失物发布、导航、消息查询、校园待办提醒

## 当前亮点

- `AgentOrchestrator` 统一编排 Function Calling、工具执行、回复生成、会话恢复与历史记录。
- `ToolExecutorService` 支持本地工具并行执行，并保留 MCP 熔断、指标与本地回退能力。
- `campus_tips` 工具复用首页智能提醒，Agent 可以直接回答“我有什么待办/提醒”。
- 二手交易详情页的面交建议已联动校园导航，可将推荐面交点一键带入路线规划。
- 失物招领支持按分类、标题、描述、地点、时间进行智能匹配。

## 快速启动

首次克隆项目请先阅读：[项目克隆与本地运行指南](docs/CLONE_AND_RUN.md)。

### 后端

```bash
./mvnw spring-boot:run
```

默认后端地址：`http://localhost:8080`

常用环境变量：

```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/campus_platform?useUnicode=true\&characterEncoding=utf8\&useSSL=false\&serverTimezone=Asia/Shanghai\&allowPublicKeyRetrieval=true
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=your_password
export JWT_SECRET=your_jwt_secret
export SPRING_AI_OPENAI_API_KEY=your_llm_key
export AMAP_WEB_KEY=your_amap_key
```

### 前端

```bash
cd campus-frontend
npm install
npm run dev
```

默认前端地址：`http://localhost:5173`

## 常用验证

```bash
./mvnw -q -DskipTests compile

cd campus-frontend
npm run build
```

## 重要配置

- 数据库：`src/main/resources/application.properties`
- 前端接口代理/环境：`campus-frontend/.env`
- Agent 主链路说明：`docs/AGENT_FUNCTION_CALLING.md`
- MCP 可选扩展说明：`docs/AGENT_MCP_INTEGRATION.md`
- 导航升级说明：`NAVIGATION_UPGRADE.md`

## 演示建议

1. 首页查看智能提醒，进入 Agent 询问“我有什么待办提醒”。
2. 在 Agent 中完成一次宿舍报修或消息查询。
3. 进入二手商品详情，查看卖家可信度、生成议价模板、打开面交建议。
4. 从面交建议中选择地点，跳转到校园导航完成路线规划。
5. 发布失物或招领信息，展示智能匹配结果。
