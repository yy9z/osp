# Agent Function Calling 主链路说明

本项目当前采用单 Agent + Function Calling / Tool Calling 作为主运行链路。这里的 Function Calling 指的是：大模型负责理解用户自然语言并生成结构化业务意图与参数，后端再调用本地 Java 函数完成真实业务操作。

## 1. 主链路

```text
用户输入
  -> AgentOrchestrator
  -> FunctionCallingPlannerService 一次性生成 tool_calls 和 arguments
  -> ToolExecutorService 调用 AgentTool
  -> ResponseGeneratorService 汇总函数结果并生成回复
```

对应代码位置：

- `AgentOrchestrator`：单 Agent 编排入口
- `FunctionCallingPlannerService`：意图识别、参数抽取、函数选择合并后的 Function Calling 规划器
- `ToolExecutorService`：函数执行器
- `AgentTool`：本地业务函数接口
- `IntentService` / `SlotFillingService` / `PlannerService`：保留为 Function Calling 规划失败时的兼容兜底链路

## 2. 当前函数清单

`FunctionCallingPlannerService` 当前会生成以下本地函数调用：

| 意图 | 函数名 | 业务能力 |
|------|--------|----------|
| `DORM_REPAIR` | `dorm_repair` | 宿舍报修 |
| `DORM_QUERY` | `dorm_query` | 宿舍信息查询 |
| `REPAIR_QUERY` | `repair_query` | 报修工单查询 |
| `SECONDHAND_SEARCH` | `secondhand_search` | 二手商品搜索 |
| `SECONDHAND_PUBLISH` | `secondhand_publish` | 二手商品发布 |
| `LOSTFOUND_LOST` | `lostfound_lost` | 寻物发布 |
| `LOSTFOUND_FOUND` | `lostfound_found` | 招领发布 |
| `NAVIGATION` | `navigation_v2` | 校园导航 |
| `MESSAGE_QUERY` | `message_query` | 消息查询 |
| `CAMPUS_TIPS` | `campus_tips` | 校园待办与主动提醒 |

## 3. 为什么主链路不用 MCP

当前宿舍、二手、失物招领、导航、消息等业务能力都在同一个 Spring Boot 后端内，直接调用本地 service/mapper 更短、更稳定，也更容易排查问题。

MCP 适合后续接入跨进程、跨语言或第三方系统工具；在当前项目中它不是运行必需项。因此配置中默认关闭：

```properties
agent.mcp.enabled=false
agent.mcp.fallback-to-local=true
```

## 4. 新增函数的步骤

1. 新增一个实现 `AgentTool` 的 Spring Bean。
2. 在 `getName()` 中返回函数名。
3. 在 `execute(ToolArgs args)` 中读取结构化参数并调用业务 service。
4. 在 `PlannerService` 中把对应意图映射到函数名。
5. 如有必填参数，在 `SlotFillingService` 中补充必填槽位和追问文案。
6. 在 `FunctionCallingPlannerService` 的函数清单、必填参数和提示词中同步新增函数定义。

这样新增能力仍然走同一条 Function Calling 主链路，不需要额外启动 MCP server。
