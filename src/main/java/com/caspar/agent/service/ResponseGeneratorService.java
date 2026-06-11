package com.caspar.agent.service;

import com.caspar.agent.llm.LlmClient;
import com.caspar.agent.model.LlmMessage;
import com.caspar.agent.model.ToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 回复生成服务：汇总工具结果，调用大模型生成自然语言回复。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResponseGeneratorService {

    private final LlmClient llmClient;
    private final WebSearchService webSearchService;

    private static final String SYSTEM_PROMPT = """
            你是一个高校校园智能助手，负责将业务操作结果整理为友好的自然语言回复。
            当前导航场景默认为中国科学技术大学校园内导航。
            语气亲切、简洁，使用中文。如有多条结果，请逐条列出。
            如果结果是在多个校区之间需要澄清，请直接追问用户，不要假装已经完成导航。
            如果有路线安全分析结果，请特别提醒用户注意安全警告和建议。
            回复不要超过200字。
            """;

    private static final String UNKNOWN_CHAT_PROMPT = """
            你是校园平台内置助手的通用问答模式。
            对于非校园业务问题，请像一个正常的通用AI助手那样回答。
            要求：
            1) 优先准确、清晰、有条理，语气自然友好；
            2) 用户问概念时先给结论再解释；
            3) 用户问操作时给出可执行步骤或示例；
            4) 默认使用中文，除非用户指定其他语言；
            5) 不必刻意超短，按问题复杂度给出合适长度。
            """;

    private static final String UNKNOWN_CHAT_PROMPT_WITH_SEARCH = """
            你是校园平台内置助手的通用问答模式。
            你会收到一组“联网检索结果”，请优先基于这些结果回答用户问题。
            要求：
            1) 优先使用检索结果中的信息，避免凭空编造。
            2) 若检索结果不足以回答，请明确说“检索信息不足”，并给出下一步建议。
            3) 回答要像通用AI助手一样自然、有条理，默认使用中文。
            """;

    /**
     * 根据工具执行结果和用户原始输入，生成最终自然语言回复。
     */
    public String generate(String userInput, List<ToolResult> toolResults) {
        long startTime = System.currentTimeMillis();
        if (toolResults.isEmpty()) {
            return "抱歉，我暂时无法处理您的请求，请稍后再试或直接前往对应功能页面操作。";
        }

        String deterministicReply = buildDeterministicToolReply(toolResults);
        if (deterministicReply != null) {
            return deterministicReply;
        }

        StringBuilder context = new StringBuilder();
        context.append("用户需求：").append(userInput).append("\n\n");
        context.append("业务处理结果：\n");
        for (int i = 0; i < toolResults.size(); i++) {
            ToolResult r = toolResults.get(i);
            if (r.isSuccess()) {
                context.append("✓ ").append(r.getSummary()).append("\n");

                // 新增：提取并展示路线安全分析结果
                appendSafetyAnalysis(context, r);
            } else {
                context.append("✗ ").append(r.getErrorMessage()).append("\n");
            }
        }

        try {
            List<LlmMessage> messages = new ArrayList<>();
            messages.add(LlmMessage.system(SYSTEM_PROMPT));
            messages.add(LlmMessage.user(context.toString()));
            String response = llmClient.chat(messages);
            log.info("回复生成完成, 耗时={}ms", System.currentTimeMillis() - startTime);
            return response;
        } catch (Exception e) {
            log.warn("回复生成失败，使用摘要回退, 耗时={}ms: {}", System.currentTimeMillis() - startTime, e.getMessage());
            // LLM 失败时使用工具摘要作为回复
            StringBuilder fallback = new StringBuilder();
            for (ToolResult r : toolResults) {
                if (r.isSuccess()) {
                    fallback.append(r.getSummary()).append(" ");
                    // 回退时也包含安全分析
                    appendSafetyAnalysisFallback(fallback, r);
                } else {
                    fallback.append("操作失败：").append(r.getErrorMessage()).append(" ");
                }
            }
            return fallback.toString().trim();
        }
    }

    private String buildDeterministicToolReply(List<ToolResult> toolResults) {
        if (toolResults.size() != 1) {
            return null;
        }
        ToolResult result = toolResults.get(0);
        if (!result.isSuccess() || !(result.getData() instanceof Map<?, ?> data)) {
            return null;
        }

        Object type = data.get("type");
        if (!"CAMPUS_TIPS".equals(String.valueOf(type))) {
            return null;
        }
        return buildCampusTipsReply(data);
    }

    private String buildCampusTipsReply(Map<?, ?> data) {
        Object rawTips = data.get("tips");
        if (!(rawTips instanceof List<?> tips) || tips.isEmpty()) {
            return "当前没有需要优先处理的校园事务。";
        }

        StringBuilder reply = new StringBuilder("好的，我为你整理了")
                .append(tips.size())
                .append("条待处理提醒：");
        for (int i = 0; i < tips.size(); i++) {
            Object item = tips.get(i);
            if (!(item instanceof Map<?, ?> tip)) {
                continue;
            }
            String title = safeText(tip.get("title"), "待处理提醒");
            String content = safeText(tip.get("content"), "");
            reply.append("\n").append(i + 1).append(". ").append(title);
            if (!content.isBlank()) {
                reply.append("：").append(content);
            }
        }
        return reply.toString();
    }

    private String safeText(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }

    /**
     * 提取并展示路线安全分析结果
     */
    @SuppressWarnings("unchecked")
    private void appendSafetyAnalysis(StringBuilder context, ToolResult result) {
        if (result.getData() == null) {
            return;
        }

        Object safetyObj = result.getData() instanceof Map ?
                ((Map<String, Object>) result.getData()).get("safetyAnalysis") : null;

        if (safetyObj instanceof Map<?, ?> safety) {
            context.append("\n路线安全分析结果：\n");

            Object score = safety.get("safetyScore");
            if (score != null) {
                context.append("- 安全评分：").append(score).append("/10\n");
            }

            Object warnings = safety.get("warnings");
            if (warnings instanceof List<?> warningList && !warningList.isEmpty()) {
                context.append("- 警告：\n");
                for (Object w : warningList) {
                    context.append("  · ").append(w).append("\n");
                }
            }

            Object suggestions = safety.get("suggestions");
            if (suggestions instanceof List<?> suggestionList && !suggestionList.isEmpty()) {
                context.append("- 建议：\n");
                for (Object s : suggestionList) {
                    context.append("  · ").append(s).append("\n");
                }
            }

            if (Boolean.TRUE.equals(safety.get("hasAlternative"))) {
                Object alternative = safety.get("alternativeSummary");
                if (alternative != null) {
                    context.append("- 替代路线：").append(alternative).append("\n");
                }
            }

            context.append("\n请根据以上安全分析信息，友好地提醒用户注意安全。\n");
        }
    }

    /**
     * 回退时追加安全分析信息
     */
    @SuppressWarnings("unchecked")
    private void appendSafetyAnalysisFallback(StringBuilder fallback, ToolResult result) {
        if (result.getData() == null) {
            return;
        }

        Object safetyObj = result.getData() instanceof Map ?
                ((Map<String, Object>) result.getData()).get("safetyAnalysis") : null;

        if (safetyObj instanceof Map<?, ?> safety) {
            Object warnings = safety.get("warnings");
            if (warnings instanceof List<?> warningList && !warningList.isEmpty()) {
                fallback.append("注意：");
                for (Object w : warningList) {
                    fallback.append(w).append("；");
                }
            }
        }
    }

    /**
     * 生成 UNKNOWN 意图的兜底回复。
     */
    public String generateUnknown(String userInput) {
        String text = userInput == null ? "" : userInput.trim();
        String lower = text.toLowerCase(Locale.ROOT);
        if (TextMatchUtils.containsAnyIgnoreCase(lower, "你是谁", "你是誰", "介绍一下你", "介紹一下你", "你叫什么", "你叫什麼", "who are you")) {
            return "我是校园智能助手。校园事务我可以直接帮你办，其他问题我也可以做通用问答。";
        }
        if (TextMatchUtils.containsAnyIgnoreCase(lower, "你好", "hi", "hello", "在吗", "在嗎", "有人吗", "有人嗎")) {
            return "你好，我在。校园事务和一般问题都可以直接问我。";
        }
        if (TextMatchUtils.containsAnyIgnoreCase(lower, "谢谢", "感謝", "感谢", "thx", "thanks")) {
            return "不客气，有需要随时问我。";
        }

        List<Map<String, String>> searchResults = webSearchService.search(text, 3);
        if (!searchResults.isEmpty()) {
            try {
                StringBuilder context = new StringBuilder();
                context.append("用户问题：").append(text).append("\n\n");
                context.append("联网检索结果：\n");
                for (int i = 0; i < searchResults.size(); i++) {
                    Map<String, String> item = searchResults.get(i);
                    context.append("[").append(i + 1).append("] ")
                            .append(item.getOrDefault("title", "结果"))
                            .append("：")
                            .append(item.getOrDefault("snippet", ""))
                            .append("\n");
                    String url = item.getOrDefault("url", "");
                    if (!url.isBlank()) {
                        context.append("来源：").append(url).append("\n");
                    }
                }

                List<LlmMessage> messages = List.of(
                        LlmMessage.system(UNKNOWN_CHAT_PROMPT_WITH_SEARCH),
                        LlmMessage.user(context.toString())
                );
                String reply = llmClient.chat(messages);
                if (reply != null && !reply.isBlank()) {
                    return reply.trim();
                }
            } catch (Exception e) {
                log.warn("联网检索增强问答失败，降级到普通通用问答: {}", e.getMessage());
            }
        }

        try {
            List<LlmMessage> messages = List.of(
                    LlmMessage.system(UNKNOWN_CHAT_PROMPT),
                    LlmMessage.user(text)
            );
            String reply = llmClient.chat(messages);
            if (reply != null && !reply.isBlank()) {
                return reply.trim();
            }
        } catch (Exception e) {
            log.warn("UNKNOWN 通用问答生成失败，使用回退话术: {}", e.getMessage());
        }
        return "这个问题我暂时没法准确回答。你也可以继续问我校园事务，比如报修、二手、失物招领、校园导航。";
    }
}
