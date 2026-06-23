package com.caspar.agent.rag;

import java.util.List;
import java.util.Map;

/**
 * RAG 检索结果上下文，供 Agent 工具返回给 ReactAgent。
 */
public record RagAnswerContext(
        String question,
        boolean hit,
        double topScore,
        String answerContext,
        List<KnowledgeChunk> chunks,
        List<Map<String, Object>> citations
) {
}

