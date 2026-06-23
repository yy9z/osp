package com.caspar.agent.tool;

import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import com.caspar.agent.rag.KnowledgeChunk;
import com.caspar.agent.rag.RagAnswerContext;
import com.caspar.agent.rag.RagRetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 校园平台知识库 RAG 工具：只检索说明文档，不办理写操作。
 */
@Component
@RequiredArgsConstructor
public class CampusKnowledgeTool implements AgentTool {

    private final RagRetrievalService ragRetrievalService;

    @Override
    public String getName() {
        return "campus_knowledge_query";
    }

    @Override
    public ToolResult execute(ToolArgs args) {
        Map<String, Object> params = args.getParams() == null ? Map.of() : args.getParams();
        String question = stringValue(params.get("question"));
        String module = stringValue(params.get("module"));
        Integer topK = integerValue(params.get("top_k"));

        if (question == null || question.isBlank()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("clarificationRequired", true);
            data.put("clarificationType", "missing_slot");
            data.put("askFor", "question");
            data.put("missingSlot", "question");
            data.put("missingSlots", List.of("question"));
            data.put("agentSummary", "请告诉我你想查询哪方面的平台使用说明或校园事务规则。");
            return ToolResult.ok("请告诉我你想查询哪方面的平台使用说明或校园事务规则。", data);
        }

        RagAnswerContext context = ragRetrievalService.retrieve(question, module, topK);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "CAMPUS_KNOWLEDGE");
        data.put("question", context.question());
        data.put("hit", context.hit());
        data.put("topScore", context.topScore());
        data.put("answerContext", context.answerContext());
        data.put("chunks", context.chunks());
        data.put("citations", context.citations());
        data.put("agentSummary", buildSummary(context));

        return ToolResult.ok(buildSummary(context), data);
    }

    private String buildSummary(RagAnswerContext context) {
        if (!context.hit() || context.chunks().isEmpty()) {
            return "知识库里暂时没有找到足够相关的说明，请换一种问法，或说明你想了解的具体模块。";
        }
        KnowledgeChunk first = context.chunks().get(0);
        return "已从平台知识库检索到相关说明：" + first.title() + "。请结合 answerContext 回答用户，并简要列出依据。";
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private Integer integerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}

