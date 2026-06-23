package com.caspar.agent.service;

import com.caspar.agent.mcp.McpProperties;
import com.caspar.agent.mcp.McpToolClient;
import com.caspar.agent.model.AgentCard;
import com.caspar.agent.model.ToolResult;
import com.caspar.agent.registry.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ToolExecutorServiceTest {

    @Test
    void knowledgeResultShouldBecomeKnowledgeCard() {
        Executor directExecutor = Runnable::run;
        ToolExecutorService service = new ToolExecutorService(
                mock(ToolRegistry.class),
                mock(McpToolClient.class),
                mock(McpProperties.class),
                directExecutor
        );
        Map<String, Object> data = Map.of(
                "type", "CAMPUS_KNOWLEDGE",
                "hit", true,
                "citations", List.of(Map.of("title", "如何提交宿舍报修"))
        );

        List<AgentCard> cards = service.toCards(
                List.of("campus_knowledge_query"),
                List.of(ToolResult.ok("已命中知识库", data))
        );

        assertEquals(1, cards.size());
        assertEquals("KNOWLEDGE", cards.get(0).getType());
        assertEquals(data, cards.get(0).getData());
    }
}
