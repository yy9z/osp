# 项目克隆与本地运行指南

本文档用于帮助首次接触本项目的开发者，从 Gitee/Git 仓库克隆代码后，在本地完成数据库恢复、环境配置以及前后端启动。

## 1. 环境要求

建议安装以下环境：

| 软件 | 建议版本 | 用途 |
| --- | --- | --- |
| Git | 2.x 或更高 | 克隆项目 |
| Java | JDK 21 | 运行 Spring Boot 后端 |
| MySQL | 8.0 或更高 | 业务数据库 |
| Node.js | 22 LTS | 运行 Vue 前端 |
| npm | 10 或更高 | 安装前端依赖 |
| Redis | 6/7，可选 | 保存 Agent 会话 |

项目已提供 Maven Wrapper，不需要单独安装 Maven。

检查环境：

```bash
git --version
java -version
mysql --version
node --version
npm --version
```

Java 版本必须是 21。Redis 没有启动时，Agent 会话会自动降级到进程内存，不影响后端基本启动。

## 2. 克隆项目

将下面的仓库地址替换成实际 Gitee 地址：

```bash
git clone https://gitee.com/你的用户名/你的仓库名.git
cd 你的仓库名
```

后续命令默认都在项目根目录执行。

## 3. 创建并导入数据库

项目使用的数据库名称是 `campus_platform`。仓库提供的完整数据库快照为：

```text
docs/sql/campus_platform_20260611.sql
```

该 SQL 文件已经包含 `CREATE DATABASE`、表结构和初始化数据，因此可直接导入：

```bash
mysql -u root -p < docs/sql/campus_platform_20260611.sql
```

执行后输入自己电脑上的 MySQL `root` 密码。

检查导入结果：

```bash
mysql -u root -p -e "USE campus_platform; SHOW TABLES;"
```

正常情况下应看到 22 张表。

如果数据库已经存在，并且希望完全重新导入，可以先手动备份，再执行：

```sql
DROP DATABASE campus_platform;
```

然后重新运行导入命令。不要在保存有重要数据的数据库上直接执行删除操作。

## 4. 配置后端环境变量

后端配置文件位于：

```text
src/main/resources/application.properties
```

不要把数据库密码、JWT 密钥和第三方 API Key 直接写进该文件并提交到仓库。启动前在终端设置环境变量。

### macOS/Linux

先生成一个 JWT 密钥：

```bash
openssl rand -base64 32
```

复制输出值，然后设置环境变量：

```bash
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=你的MySQL密码
export JWT_SECRET=上一步生成的Base64字符串
```

`JWT_SECRET` 是启动必填项，必须是 Base64 编码且解码后不少于 32 字节。同一套开发环境应固定使用同一个值，否则后端重启后原有登录 Token 会失效。

### Windows PowerShell

先准备一个符合要求的 Base64 JWT 密钥，然后执行：

```powershell
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="你的MySQL密码"
$env:JWT_SECRET="你的Base64密钥"
```

这些环境变量只对当前终端窗口生效。使用 IntelliJ IDEA 启动时，也可以在 Run Configuration 的 Environment variables 中填写。

### 可选功能配置

使用智能 Agent、校园地图或阿里云 OSS 时，再设置对应变量：

```bash
# 智能 Agent，项目默认兼容硅基流动 OpenAI 协议
export SPRING_AI_OPENAI_API_KEY=你的模型APIKey
export SPRING_AI_OPENAI_BASE_URL=https://api.siliconflow.cn
export LLM_MODEL=Qwen/Qwen2.5-72B-Instruct

# 后端高德地图 Web 服务
export AMAP_WEB_KEY=你的高德Web服务Key
export AMAP_WEB_SECURITY_CODE=你的高德安全密钥

# 阿里云 OSS
export ALIYUN_OSS_ACCESS_KEY_ID=你的AccessKeyId
export ALIYUN_OSS_ACCESS_KEY_SECRET=你的AccessKeySecret
```

不配置模型 API Key 时，普通业务功能仍可运行，但智能 Agent 无法正常调用大模型。不配置 OSS 时，文件上传会尝试降级到本地目录。

## 5. 启动后端

macOS/Linux：

```bash
./mvnw spring-boot:run
```

Windows：

```powershell
.\mvnw.cmd spring-boot:run
```

首次启动需要下载 Maven 依赖，请保持网络可用。看到类似下面的日志表示后端启动成功：

```text
Started NewOspfuApplication
```

后端默认地址：

```text
http://localhost:8080
```

可以用公开接口验证数据库和后端：

```bash
curl "http://localhost:8080/api/secondhand/list?page=1&size=5"
```

## 6. 配置并启动前端

打开第二个终端，进入前端目录：

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

如需使用完整地图功能，编辑 `campus-frontend/.env`，填写自己的高德地图 Key：

```dotenv
VITE_AMAP_KEY=your_amap_key
VITE_AMAP_SECURITY_CODE=your_amap_security_code
```

安装依赖并启动：

```bash
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:5173
```

Vite 已将 `/api` 请求代理到 `http://localhost:8080`，本地开发时不需要修改接口地址。

## 7. 可选：启动 Redis

Redis 用于保存 Agent 会话。未启动 Redis 时，系统会使用内存存储；后端重启后内存会话会丢失。

使用 Docker 启动 Redis：

```bash
docker run -d --name osp-redis -p 6379:6379 redis:7-alpine
```

已有容器再次启动：

```bash
docker start osp-redis
```

默认 Redis 地址为 `localhost:6379`，数据库编号为 `0`。

## 8. 推荐启动顺序

1. 启动 MySQL。
2. 导入 `docs/sql/campus_platform_20260611.sql`。
3. 设置数据库密码和 `JWT_SECRET`。
4. 可选启动 Redis。
5. 在项目根目录启动后端。
6. 在 `campus-frontend` 目录启动前端。
7. 浏览器打开 `http://localhost:5173`。

## 9. 构建检查

后端编译：

```bash
./mvnw -q -DskipTests compile
```

前端生产构建：

```bash
cd campus-frontend
npm run build
```

## 10. 常见问题

### 后端提示数据库连接失败

确认 MySQL 已启动、密码正确，并检查数据库是否存在：

```bash
mysql -u root -p -e "SHOW DATABASES LIKE 'campus_platform';"
```

### 后端提示 JWT 密钥未配置

设置 `JWT_SECRET`，并确保它是 Base64 编码、解码后至少 32 字节：

```bash
export JWT_SECRET="$(openssl rand -base64 32)"
```

### 端口已被占用

检查占用进程：

```bash
lsof -i :8080
lsof -i :5173
```

也可以临时修改后端 `server.port` 或前端 `vite.config.js` 中的端口。

### 前端请求接口失败

确认：

- 后端已经运行在 `http://localhost:8080`。
- 前端通过 `npm run dev` 运行在 `http://localhost:5173`。
- 没有直接双击 HTML 文件打开前端。
- `campus-frontend/vite.config.js` 中的代理地址没有被修改。

### Redis 连接警告

如果日志提示 Redis 不可用，但普通功能正常，这是预期的内存降级行为。需要保留 Agent 会话时再启动 Redis。

### 地图或智能 Agent 不可用

地图需要有效的高德 Key，智能 Agent 需要有效的模型 API Key。这些第三方凭证不会随 Git 仓库上传，需要克隆者自行申请和配置。

## 11. 数据与安全说明

- `.env`、数据库密码、JWT 密钥和第三方 API Key 不应提交到 Git。
- 当前完整 SQL 快照可能包含测试账号、手机号、邮箱等数据，只应在受信任环境中使用。
- 如果仓库准备公开，请先生成脱敏数据或只上传数据库表结构。
- SQL 中部分图片为阿里云 OSS URL，查看这些历史图片时需要能够访问对应的远程地址。

