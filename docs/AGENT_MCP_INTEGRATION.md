# Agent MCP 系统内场景接入说明

本项目默认主链路仍然是 Spring AI Alibaba ReactAgent + 本地 `ToolCallback` + 10 个 `AgentTool`。

MCP 作为可选扩展通道，用于把系统内跨模块流程封装成协议化工具。默认关闭；启用 `mcp-demo` profile 后，ReactAgent 会额外获得 4 个 `mcp_*` 场景工具。

## 1. MCP 场景工具

- `mcp_campus_overview`：首页提醒 + 消息通知 + 报修状态汇总
- `mcp_dorm_repair_flow`：宿舍档案检查 + 宿舍报修提交
- `mcp_secondhand_meetup_flow`：二手商品搜索 + 校内面交点 + 导航联动
- `mcp_lostfound_match_flow`：寻物/招领发布 + 匹配候选返回

这些工具不新增外部系统，也不保存业务数据。MCP server 只负责编排流程，真实业务仍由后端已有 `AgentTool` 和 Java 业务组件执行。

## 2. 启用方式

默认配置：

```properties
agent.mcp.enabled=false
agent.mcp.fallback-to-local=true
agent.mcp.internal-secret=${AGENT_MCP_INTERNAL_SECRET:}
```

演示配置位于 `src/main/resources/application-mcp-demo.properties`：

```properties
agent.mcp.enabled=true
agent.mcp.command=node
agent.mcp.args=scripts/mcp/campus-internal-mcp-server.mjs
agent.mcp.internal-secret=${AGENT_MCP_INTERNAL_SECRET:dev-mcp-secret}
agent.mcp.env.CAMPUS_MCP_BASE_URL=http://127.0.0.1:${server.port:8080}
agent.mcp.env.CAMPUS_MCP_INTERNAL_SECRET=${AGENT_MCP_INTERNAL_SECRET:dev-mcp-secret}
```

启动示例：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mcp-demo
```

## 3. 安全边界

MCP server 通过内部桥接接口调用已有工具：

```text
MCP tools/call
→ scripts/mcp/campus-internal-mcp-server.mjs
→ POST /api/internal/mcp/tools/call
→ ToolRegistry
→ AgentTool
→ Java 业务组件
```

内部桥接接口有三层限制：

- 只接受本机 loopback 请求。
- 必须携带 `X-Campus-MCP-Secret`。
- 只允许调用宿舍、二手、失物、导航、消息和提醒相关白名单工具。

模型传入的 `userId`、`token`、`authorization` 等身份字段会被过滤，真实用户身份由 ReactAgent 工具上下文传递到 MCP 调用。

## 4. 运维接口

管理员可查看 MCP 运行状态：

- `GET /api/agent/mcp/health`
- `GET /api/agent/mcp/metrics`
- `POST /api/agent/mcp/reset-metrics`
- `POST /api/agent/mcp/reset-circuit`

## 5. 答辩口径

推荐表述：

> 当前核心业务通过本地工具稳定执行；MCP 用于把系统内跨模块场景封装成协议化工具，例如待办总览、报修流程、二手面交导航和失物匹配。它不是新增外部功能，而是为已有校园业务提供跨进程、可观测、可熔断的扩展调用通道。
