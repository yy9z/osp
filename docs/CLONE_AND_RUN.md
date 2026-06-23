# 项目克隆与本地运行指南

本文档面向第一次运行本项目的开发者。完成以下步骤后，可以在本地启动 Spring Boot 后端和 Vue 前端。

## 1. 项目地址与目录

GitHub 仓库：

```text
https://github.com/yy9z/osp
```

克隆并进入项目：

```bash
git clone https://github.com/yy9z/osp.git
cd osp
```

后续命令如无特别说明，均在项目根目录执行。

## 2. 环境要求

| 软件 | 版本要求 | 用途 |
| --- | --- | --- |
| Git | 2.x 或更高 | 克隆和更新代码 |
| Java | JDK 21 | 运行 Spring Boot 后端 |
| MySQL | 8.0 或更高 | 保存业务数据 |
| Node.js | 20.19+ 或 22.12+ | 运行 Vite 前端 |
| npm | 随 Node.js 安装 | 安装前端依赖 |
| Redis | 6/7，推荐 | 缓存和保存 Agent 会话 |

项目包含 Maven Wrapper，不需要另外安装 Maven。

检查本机环境：

```bash
git --version
java -version
mysql --version
node --version
npm --version
```

请确认 `java -version` 显示 Java 21。Node.js 推荐直接使用 22 LTS。

## 3. 初始化 MySQL 数据库

项目默认连接：

```text
数据库：campus_platform
地址：localhost:3306
用户名：root
```

完整数据库快照位于：

```text
docs/sql/campus_platform_20260611.sql
```

该文件包含建库语句、22 张表的结构和演示数据。

### macOS/Linux

```bash
mysql -u root -p < docs/sql/campus_platform_20260611.sql
```

### Windows

在项目根目录通过 CMD 执行：

```bat
mysql -u root -p < docs\sql\campus_platform_20260611.sql
```

如果当前使用 PowerShell，可以调用 CMD：

```powershell
cmd /c "mysql -u root -p < docs\sql\campus_platform_20260611.sql"
```

根据提示输入本机 MySQL 密码。导入后检查数据库：

```bash
mysql -u root -p -e "USE campus_platform; SHOW TABLES;"
```

如果需要重新导入，请先备份已有数据，再删除旧数据库。不要对包含重要数据的数据库直接执行删除。

## 4. 配置后端

后端配置文件是：

```text
src/main/resources/application.properties
```

配置文件已经通过环境变量读取密码和第三方密钥，不要把真实凭据直接写入并提交到 Git。

### 4.1 必需变量

数据库密码和 JWT 密钥是本地启动所必需的。

先生成一个 JWT 密钥：

```bash
openssl rand -base64 32
```

保存输出结果。同一开发环境应保持这个值不变，否则后端重启后，之前签发的登录 Token 会失效。

macOS/Linux：

```bash
export SPRING_DATASOURCE_USERNAME='root'
export SPRING_DATASOURCE_PASSWORD='你的MySQL密码'
export JWT_SECRET='刚才生成的Base64字符串'
```

如果数据库不在默认地址，可额外设置：

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/campus_platform?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
```

Windows PowerShell：

```powershell
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="你的MySQL密码"
$env:JWT_SECRET="生成的Base64字符串"
```

环境变量只在当前终端窗口中有效。使用 IntelliJ IDEA 时，也可以把它们填写到 Run Configuration 的 Environment variables 中。

### 4.2 智能 Agent（可选）

项目当前通过 Spring AI Alibaba 调用 DashScope。启用智能 Agent 时设置：

```bash
export AI_DASHSCOPE_API_KEY='你的DashScope API Key'
export LLM_MODEL='qwen-plus'
```

Windows PowerShell：

```powershell
$env:AI_DASHSCOPE_API_KEY="你的DashScope API Key"
$env:LLM_MODEL="qwen-plus"
```

不配置 API Key 时，登录、二手交易、失物招领等普通业务仍可使用，但依赖大模型的 Agent 对话会失败或降级。

### 4.3 高德地图（可选）

后端地点搜索和路线规划需要高德 Web 服务 Key：

```bash
export AMAP_WEB_KEY='你的高德Web服务Key'
export AMAP_WEB_SECURITY_CODE='你的高德安全密钥'
```

前端地图 Key 在第 7 节单独配置。

### 4.4 阿里云 OSS（可选）

```bash
export ALIYUN_OSS_ACCESS_KEY_ID='你的AccessKeyId'
export ALIYUN_OSS_ACCESS_KEY_SECRET='你的AccessKeySecret'
```

未配置 OSS 时，单文件上传接口会尝试降级到本地 `campus-frontend/public/images/uploads/`。

## 5. 启动 Redis

Redis 用于业务缓存、Agent 会话和 Graph 检查点。为了获得完整功能，建议在启动后端前先启动 Redis。

使用 Docker：

```bash
docker run -d --name osp-redis -p 6379:6379 redis:7-alpine
```

容器已经创建时：

```bash
docker start osp-redis
```

检查连接：

```bash
redis-cli ping
```

正常结果为 `PONG`。默认连接地址是 `localhost:6379`，数据库编号为 `0`。

如果暂时不安装 Redis，可以在启动后端前关闭 Redis 缓存和 Graph Redis 检查点：

```bash
export SPRING_CACHE_TYPE='none'
export AGENT_GRAPH_REDIS_CHECKPOINT_ENABLED='false'
```

此时 Agent 会话使用进程内存降级存储，后端重启后会话会丢失，日志中也可能出现 Redis 降级提示。

## 6. 启动后端

确保 MySQL 已启动，并且在设置环境变量的同一个终端中执行：

macOS/Linux：

```bash
./mvnw spring-boot:run
```

Windows PowerShell：

```powershell
.\mvnw.cmd spring-boot:run
```

第一次启动会下载 Maven 依赖，需要保持网络可用。出现以下日志表示启动成功：

```text
Started NewOspfuApplication
```

后端默认地址：

```text
http://localhost:8080
```

验证公开接口：

```bash
curl "http://localhost:8080/api/secondhand/list?page=1&size=5"
```

如需启用 Swagger，在启动前设置：

```bash
export SPRINGDOC_SWAGGER_UI_ENABLED='true'
export SPRINGDOC_API_DOCS_ENABLED='true'
```

然后访问 `http://localhost:8080/swagger-ui.html`。

## 7. 配置并启动前端

保持后端运行，打开第二个终端：

```bash
cd campus-frontend
```

复制前端环境变量示例：

macOS/Linux：

```bash
cp .env.example .env
```

Windows PowerShell：

```powershell
Copy-Item .env.example .env
```

如果需要完整地图功能，编辑 `campus-frontend/.env`：

```dotenv
VITE_AMAP_KEY=你的高德Web端JS API Key
VITE_AMAP_SECURITY_CODE=你的高德安全密钥
```

安装锁定版本的依赖并启动：

```bash
npm ci
npm run dev
```

浏览器打开：

```text
http://localhost:5173
```

前端请求使用 `/api`，Vite 会自动代理到 `http://localhost:8080`，本地开发不需要修改接口地址。

## 8. 首次使用

1. 打开 `http://localhost:5173`。
2. 使用注册页面创建普通用户。
3. 登录后即可使用二手交易、失物招领、宿舍管理等业务。
4. 配置 DashScope Key 后再测试智能 Agent。
5. 配置高德 Key 后再测试地图和路线规划。

导入的完整快照包含若干演示账号，但密码没有作为运行凭据公开。新环境建议直接注册新用户。

注册 `ADMIN` 或 `DORM_MANAGER` 角色时，后端还要求设置 `ADMIN_REGISTER_KEY`，并在注册请求中提供相同的注册码。

## 9. 推荐启动顺序

1. 启动 MySQL。
2. 首次运行时导入数据库快照。
3. 启动 Redis，或者设置无 Redis 的降级变量。
4. 设置数据库密码和 `JWT_SECRET`。
5. 可选设置 DashScope、高德和 OSS 凭据。
6. 在项目根目录启动后端。
7. 在 `campus-frontend` 目录启动前端。
8. 打开 `http://localhost:5173`。

## 10. 测试与构建

运行后端测试：

```bash
./mvnw test
```

构建后端：

```bash
./mvnw -DskipTests package
```

构建前端：

```bash
cd campus-frontend
npm run build
```

前端构建产物位于 `campus-frontend/dist/`。

## 11. 常见问题

### Java 版本不正确

如果 Maven 提示不支持 Java 版本，请确认：

```bash
java -version
./mvnw -version
```

两条命令都应显示 Java 21。

### MySQL 连接失败

确认 MySQL 已启动、密码正确，并检查数据库：

```bash
mysql -u root -p -e "SHOW DATABASES LIKE 'campus_platform';"
```

如果 MySQL 不在本机或端口不是 3306，请设置 `SPRING_DATASOURCE_URL`。

### JWT 密钥错误

如果日志提示 JWT 未配置、格式无效或长度不足，请重新生成：

```bash
openssl rand -base64 32
```

将完整输出保存到 `JWT_SECRET`，不要使用过短的普通字符串。

### Redis 连接失败

优先启动 Redis。暂时不需要 Redis 时，设置：

```bash
export SPRING_CACHE_TYPE='none'
export AGENT_GRAPH_REDIS_CHECKPOINT_ENABLED='false'
```

### Node.js 或 Vite 版本错误

Vite 7.3.1 要求 Node.js `^20.19.0 || >=22.12.0`。推荐安装 Node.js 22 LTS，然后重新执行：

```bash
rm -rf node_modules
npm ci
```

### 前端请求接口失败

确认：

- 后端正在 `http://localhost:8080` 运行。
- 前端通过 `npm run dev` 启动，而不是直接打开 HTML 文件。
- `campus-frontend/vite.config.js` 中的代理地址仍是 `http://localhost:8080`。
- 浏览器中访问的是 `http://localhost:5173`。

### 端口被占用

macOS/Linux：

```bash
lsof -i :8080
lsof -i :5173
```

可以结束占用进程，或修改后端 `server.port` 和前端 `vite.config.js`。

### Agent、地图或上传功能不可用

- Agent：检查 `AI_DASHSCOPE_API_KEY` 和 `LLM_MODEL`。
- 后端地图服务：检查 `AMAP_WEB_KEY`。
- 前端地图：检查 `VITE_AMAP_KEY` 和 `VITE_AMAP_SECURITY_CODE`。
- OSS：检查两个 `ALIYUN_OSS_*` 环境变量。

## 12. 安全提醒

- 不要提交 `.env`、数据库密码、JWT 密钥或第三方 API Key。
- 不要把 Token 或密钥直接粘贴到 Issue、聊天记录或截图中。
- 完整 SQL 快照包含演示数据，只应在本地或受信任环境使用。
- 如果仓库需要公开发布，应改用脱敏数据或仅保留数据库结构。
- SQL 中部分历史图片使用远程 OSS URL，离线环境可能无法显示。
