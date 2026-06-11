# Agent MCP 可选扩展接入说明

本项目当前主链路已经改为单 Agent + Function Calling / 本地 `AgentTool` 调用，详见 `docs/AGENT_FUNCTION_CALLING.md`。

MCP 不再作为默认运行依赖，仅作为后续跨进程、跨语言或第三方系统工具接入的可选扩展。需要启用时，才在 `ToolExecutorService` 中优先通过 MCP 调用工具（stdio），失败后可回退本地工具。

## 1. 配置项

在 `src/main/resources/application.properties` 中配置：

```properties
agent.mcp.enabled=false
agent.mcp.fallback-to-local=true
# agent.mcp.command=npx
# agent.mcp.args=-y,@modelcontextprotocol/server-filesystem,/Users/yy/code/OSP
agent.mcp.startup-timeout-ms=15000
agent.mcp.call-timeout-ms=30000
agent.mcp.circuit-breaker-failure-threshold=3
agent.mcp.circuit-breaker-open-ms=60000
agent.mcp.pending-warning-threshold=5
```

- `enabled=false`：默认关闭，项目日常运行走 Function Calling / 本地 `AgentTool`
- `fallback-to-local=true`：MCP 失败时回退本地工具（推荐）
- `command/args`：启动 MCP stdio server 的命令
- `circuit-breaker-*`：MCP 连续失败熔断配置，避免每次请求都等待超时
- `pending-warning-threshold`：pending 请求数告警阈值（用于定位潜在响应关联异常）

## 2. 需要提供的 MCP 工具名

`PlannerService` 当前会规划以下工具名，请你的 MCP server 至少实现这些 `tools/call name`：

- `dorm_repair`
- `dorm_query`
- `repair_query`
- `secondhand_search`
- `secondhand_publish`
- `lostfound_lost`
- `lostfound_found`
- `navigation_v2`
- `message_query`
- `campus_tips`

如果你的 MCP 工具名不同，可用映射：

```properties
agent.mcp.tool-name-mapping.navigation_v2=navigation
```

## 3. 参数约定

每次 MCP `tools/call` 的 `arguments` 会包含：

- 槽位参数（来自 `SlotFillingService`）
- `userId`（当前登录用户）
- `user_id`（兼容字段）

你的 MCP server 可以按需读取这些参数并完成业务调用。

## 4. 监控与运维接口（管理员）

- `GET /api/agent/mcp/health`：查看 MCP 健康状态与指标快照
- `GET /api/agent/mcp/metrics`：查看 MCP 详细指标
- `POST /api/agent/mcp/reset-metrics`：重置 MCP 统计指标
- `POST /api/agent/mcp/reset-circuit`：手动关闭熔断状态
