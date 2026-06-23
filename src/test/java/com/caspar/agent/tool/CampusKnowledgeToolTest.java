package com.caspar.agent.tool;

import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import com.caspar.agent.rag.KnowledgeDocumentLoader;
import com.caspar.agent.rag.RagRetrievalService;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampusKnowledgeToolTest {

    @Test
    void execute_shouldReturnKnowledgeChunksForQuestion() {
        CampusKnowledgeTool tool = newTool();

        ToolResult result = tool.execute(ToolArgs.builder()
                .toolName("campus_knowledge_query")
                .params(Map.of("question", "二手商品怎么面交比较安全", "top_k", 3))
                .userId(42L)
                .build());

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("CAMPUS_KNOWLEDGE", data.get("type"));
        assertEquals(Boolean.TRUE, data.get("hit"));
        assertTrue(String.valueOf(data.get("answerContext")).contains("面交"));
        assertTrue(String.valueOf(data.get("agentSummary")).contains("知识库"));
        assertTrue(((Number) data.get("topScore")).doubleValue() > 0);
        assertTrue(data.get("chunks") instanceof List<?> chunks && !chunks.isEmpty() && chunks.size() <= 3);
        assertTrue(data.get("citations") instanceof List<?> citations && !citations.isEmpty());
    }

    @Test
    void execute_shouldAskForQuestionWhenMissing() {
        CampusKnowledgeTool tool = newTool();

        ToolResult result = tool.execute(ToolArgs.builder()
                .toolName("campus_knowledge_query")
                .params(Map.of())
                .userId(42L)
                .build());

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals(Boolean.TRUE, data.get("clarificationRequired"));
        assertEquals("question", data.get("askFor"));
    }

    private CampusKnowledgeTool newTool() {
        RagRetrievalService retrievalService = new RagRetrievalService(new KnowledgeDocumentLoader());
        retrievalService.init();
        return new CampusKnowledgeTool(retrievalService);
    }
}
