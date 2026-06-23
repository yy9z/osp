package com.caspar.agent.service;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.caspar.agent.model.AgentSession;
import com.caspar.agent.model.CampusAgentRunResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Spring AI Alibaba ReactAgent 封装。
 */
@Slf4j
@Service
public class CampusReactAgentService {

    private static final int MAX_CONTEXT_MESSAGES = 8;

    private static final String SYSTEM_PROMPT = """
            你是高校校园一站式平台的校园事务 Agent。

            你可以使用以下本地工具办理真实业务：
            - dorm_repair：宿舍报修
            - dorm_query：查询宿舍信息
            - repair_query：查询报修工单
            - secondhand_search：搜索二手商品
            - secondhand_publish：发布二手商品
            - lostfound_lost：发布寻物
            - lostfound_found：发布招领
            - navigation_v2：校园导航
            - message_query：查询消息通知
            - campus_tips：查看待办提醒
            - campus_knowledge_query：检索平台使用说明、校园事务规则和常见问题

            如果工具列表中出现 mcp_* 工具，它们表示系统内跨模块 MCP 场景封装：
            - mcp_campus_overview：校园待办、消息和报修状态总览
            - mcp_dorm_repair_flow：宿舍信息检查 + 报修提交
            - mcp_secondhand_meetup_flow：二手搜索 + 面交地点 + 导航联动
            - mcp_lostfound_match_flow：失物发布 + 匹配候选

            规则：
            1. 校园业务必须优先调用工具，不要只凭模型编造业务结果。
            2. 用户询问平台怎么用、操作流程、规则、注意事项、FAQ 时，优先调用 campus_knowledge_query。
            3. 知识库工具只负责查说明；如果用户要真正办理报修、发布商品、发布失物、导航等事务，要调用对应业务工具。
            4. 不要编造价格、地点、宿舍号、用户身份或工单状态。
            5. 不要向工具参数传入 userId、token、authorization 等身份字段。
            6. 缺少必填信息时，先用自然语言追问，不要强行调用写操作工具。
            7. 工具返回失败或需要补充信息时，请直接解释原因并提示用户下一步。
            8. 回复要简洁，适合前端聊天窗口展示；使用知识库结果时，优先依据 answerContext 回答。
            """;

    private final ReactAgent agent;

    public CampusReactAgentService(ChatModel chatModel,
                                   @Qualifier("campusAgentToolCallbacks") List<ToolCallback> toolCallbacks,
                                   @Value("${agent.llm.timeout-ms:30000}") long timeoutMs,
                                   @Value("${agent.tool-executor.max-pool-size:32}") int maxParallelTools) {
        this.agent = ReactAgent.builder()
                .name("campus_task_agent")
                .model(chatModel)
                .tools(toolCallbacks)
                .systemPrompt(SYSTEM_PROMPT)
                .saver(new MemorySaver())
                .parallelToolExecution(true)
                .maxParallelTools(Math.max(1, Math.min(maxParallelTools, 32)))
                .toolExecutionTimeout(Duration.ofMillis(Math.max(timeoutMs, 1000L)))
                .build();
    }

    public CampusAgentRunResult run(String userInput, AgentSession session, Long userId, String traceId) {
        try {
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(session.getSessionId())
                    .addMetadata(CampusAgentContextKeys.USER_ID, userId)
                    .addMetadata(CampusAgentContextKeys.TRACE_ID, traceId)
                    .build();

            AssistantMessage response = agent.call(toMessages(session), config);
            String reply = response == null || response.getText() == null ? "" : response.getText();
            return new CampusAgentRunResult(reply);
        } catch (Exception e) {
            log.error("ReactAgent调用失败: sessionId={}", session == null ? null : session.getSessionId(), e);
            throw new IllegalStateException("ReactAgent调用失败: " + e.getMessage(), e);
        }
    }

    private List<Message> toMessages(AgentSession session) {
        List<Message> messages = new ArrayList<>();
        List<Map<String, String>> history = session == null ? List.of() : session.getHistory();
        if (history == null || history.isEmpty()) {
            return messages;
        }
        int start = Math.max(0, history.size() - MAX_CONTEXT_MESSAGES);
        for (int i = start; i < history.size(); i++) {
            Map<String, String> item = history.get(i);
            if (item == null) {
                continue;
            }
            String role = item.get("role");
            String content = item.get("content");
            if (content == null || content.isBlank()) {
                continue;
            }
            if ("assistant".equals(role)) {
                messages.add(new AssistantMessage(content));
            } else {
                messages.add(new UserMessage(content));
            }
        }
        return messages;
    }
}
