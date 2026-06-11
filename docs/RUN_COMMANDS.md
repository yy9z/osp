# 服务启动命令

本文档记录前后端服务的启动和重启命令。

## 前端服务

```bash
cd /Users/caspar/Documents/IdeaProjects/NewOSPFU/campus-frontend
npm run dev
```

前端默认运行在：http://localhost:5173

## 后端服务

```bash
cd /Users/caspar/Documents/IdeaProjects/NewOSPFU
OPENAI_API_KEY="$OPENAI_API_KEY" OPENAI_BASE_URL="$OPENAI_BASE_URL" LLM_MODEL="glm-4.7" ./mvnw spring-boot:run
```

后端默认运行在：http://localhost:8080

## 快速重启

### 1. 检查当前服务状态

```bash
lsof -i :5173 -i :8080
```

### 2. 停止服务

```bash
# 停止 8080 端口（后端）
lsof -ti :8080 | xargs kill -9

# 停止 5173 端口（前端）
lsof -ti :5173 | xargs kill -9
```

### 3. 启动服务

先启动后端，再启动前端：

```bash
# 终端1 - 启动后端
cd /Users/caspar/Documents/IdeaProjects/NewOSPFU
./mvnw spring-boot:run

# 终端2 - 启动前端
cd /Users/caspar/Documents/IdeaProjects/NewOSPFU/campus-frontend
npm run dev
```

## 注意事项

- 确保 MySQL 数据库已启动（默认 localhost:3306）
- 确保数据库 `campus_platform` 已创建并导入初始数据
- 前端依赖 Node.js 环境，后端依赖 Java/Maven 环境
