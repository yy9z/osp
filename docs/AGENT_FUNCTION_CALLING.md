# Agent Function Calling 主链路说明

本项目采用 Spring AI Alibaba Graph + 单 Agent + Function Calling / Tool Calling 运行链路。Graph 负责显式编排路由、规划、补槽、工具执行、澄清和响应节点；大模型负责理解用户自然语言并生成结构化业务意图与参数，后端负责参数校验和真实业务操作。

项目使用 Spring Boot 3.5.15、Spring AI 1.1.8 与 Spring AI Alibaba Graph Core 1.1.2.2，并增加 Spring AI 原生 Tool Calling 只读试点。试点默认关闭，开启后只向模型暴露查询和导航类工具；写工具仍走受控 JSON 规划链路。

## 1. 主链路

```text
用户输入
  -> AgentOrchestrator
  -> CampusAgentGraph
       -> route_request
       -> general_chat / plan_request
       -> unknown_intent / ask_for_slot
       -> execute_tools (read-only tools)
       -> prepare_write_confirmation (write tools)
            -> [interrupt and persist checkpoint]
            -> confirm_write / cancel_write
       -> inspect_tool_results
       -> persist_tool_follow_up / render_tool_response
  -> AgentResponse
```

对应代码位置：

- `CampusAgentGraph`：Spring AI Alibaba Graph 运行时，维护节点、状态键策略和条件边
- `CampusAgentExecution`：单次请求在图中的不可变执行上下文
- `AgentGraphCheckpointConfiguration` / `ResilientCheckpointSaver`：Redis 检查点及内存回退
- `AgentOrchestrator`：会话准备、历史恢复、请求上下文合并和顶层异常处理
- `AgentWriteConfirmationService`：写操作预览、确认编号及确认/取消响应
- `AgentToolCatalog`：工具名称、意图、参数 Schema、必填槽位和读写属性的单一事实源
- `NativeToolCallingPlannerService`：基于 Spring AI 原生 Tool Calling 的只读规划试点
- `AgentToolCallbackProvider`：将现有 `AgentTool` 适配为 Spring AI `ToolCallback`
- `FunctionCallingPlannerService`：意图识别、参数抽取、函数选择合并后的 Function Calling 规划器
- `ToolExecutorService`：函数执行器
- `AgentTool`：本地业务函数接口
- `IntentService` / `SlotFillingService` / `PlannerService`：保留为 Function Calling 规划失败时的兼容兜底链路

原生 Tool Calling 仅负责选择工具和生成参数，Spring AI 内部自动执行被关闭。最终工具执行仍由 `ToolExecutorService` 完成，因此用户身份注入、MCP 回退、熔断和卡片生成逻辑保持不变。

Graph 检查点与现有会话、历史记录分工不同：`AgentSessionManager` 保存对话上下文，`AgentHistoryService` 保存可展示的 MySQL 消息历史，Graph Checkpoint 保存可恢复的运行位置和执行状态。三者都使用原有 `sessionId` 关联，因此框架迁移不改变现有会话接口。

## 2. 检查点与写操作确认

Graph 默认使用 RedisSaver 持久化检查点，并用 MemorySaver 作为运行期镜像和故障回退：

```properties
agent.graph.checkpoint.redis.enabled=${AGENT_GRAPH_REDIS_CHECKPOINT_ENABLED:true}
```

写工具由 `AgentToolCatalog.readOnly=false` 识别。规划完成后不会直接调用工具，而是先进入 `prepare_write_confirmation` 生成安全预览，并在 `confirm_write` 前暂停：

1. 首次请求返回 `confirmationRequired=true`、`confirmationId` 和参数预览，业务数据尚未写入。
2. 前端使用结构化的 `confirmationId` 与 `confirmationDecision=APPROVE|REJECT` 提交决定。
3. 确认编号匹配时从检查点恢复；批准后仅执行一次工具，拒绝时直接结束且不执行工具。
4. 服务在等待期间重启后，仍可从 Redis 继续；Redis 启动失败时新请求退回内存检查点，但内存状态不具备跨进程恢复能力。
5. 会话删除时同步清理 Graph 检查点。已完成线程标记为 released，不再作为待确认任务恢复。

当前需要确认的写工具包括商品发布、宿舍报修、寻物发布和招领发布。只读查询、导航和消息读取保持直接执行。

## 3. 原生 Tool Calling 试点

默认配置：

```properties
agent.tool-calling.native.enabled=${AGENT_NATIVE_TOOL_CALLING_ENABLED:false}
```

启用方式：

```bash
export AGENT_NATIVE_TOOL_CALLING_ENABLED=true
```

当前试点工具：

- `dorm_query`
- `repair_query`
- `secondhand_search`
- `navigation_v2`
- `message_query`
- `campus_tips`

发布商品、报修和失物招领发布属于写操作，不会进入原生试点。原生调用失败、模型未返回工具或返回非法工具时，会自动回退到原有 JSON 规划器。

## 4. 当前函数清单

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

## 5. 为什么主链路不用 MCP

当前宿舍、二手、失物招领、导航、消息等业务能力都在同一个 Spring Boot 后端内，直接调用本地 service/mapper 更短、更稳定，也更容易排查问题。

MCP 适合后续接入跨进程、跨语言或第三方系统工具；在当前项目中它不是运行必需项。因此配置中默认关闭：

```properties
agent.mcp.enabled=false
agent.mcp.fallback-to-local=true
```

## 6. 新增函数的步骤

1. 新增一个实现 `AgentTool` 的 Spring Bean。
2. 在 `getName()` 中返回函数名。
3. 在 `execute(ToolArgs args)` 中读取结构化参数并调用业务 service。
4. 在 `AgentToolCatalog` 中新增工具定义，配置意图、参数、必填槽位和读写属性。
5. 如有新的槽位追问文案，在 `AgentToolCatalog` 的 `SLOT_QUESTIONS` 中补充。
6. 将查询类工具标记为 `readOnly=true`；会修改业务数据的工具必须保持 `readOnly=false`，由 Graph 自动接入确认节点。

提示词、工具白名单、必填槽位和 Spring AI JSON Schema 都会从目录生成，不再需要在多个 Service 中重复维护。
