package com.caspar.agent.service;

import com.caspar.agent.llm.LlmClient;
import com.caspar.agent.model.AgentSession;
import com.caspar.agent.model.FunctionCallPlan;
import com.caspar.agent.model.IntentResult;
import com.caspar.agent.model.LlmMessage;
import com.caspar.agent.registry.AgentToolCallbackProvider;
import com.caspar.agent.registry.AgentToolCatalog;
import com.caspar.agent.registry.AgentToolDefinition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Opt-in native Spring AI Tool Calling planner for read-only Agent tools.
 * Tool execution remains controlled by ToolExecutorService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NativeToolCallingPlannerService {

    private static final int MAX_CONTEXT_MESSAGES = 8;
    private static final Set<String> RESERVED_ARGUMENT_KEYS =
            Set.of("userId", "user_id", "token", "authorization");

    private static final String SYSTEM_PROMPT = """
            你是高校校园智能事务平台的工具规划器。
            请从提供的工具中选择最符合当前用户需求的一个工具，并填写结构化参数。
            只提取用户明确表达或会话状态中已经存在的参数，不要编造价格、地点、身份字段。
            用户是在回答上一轮追问时，要沿用 existing_intent 和 existing_slots。
            如果请求不属于任何提供的工具，不要调用工具。
            """;

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final IntentService intentService;
    private final AgentToolCatalog toolCatalog;
    private final AgentToolCallbackProvider toolCallbackProvider;

    @Value("${agent.tool-calling.native.enabled:false}")
    private boolean enabled;

    public Optional<FunctionCallPlan> tryPlan(String userInput, AgentSession session) {
        if (!enabled || !supportsReadOnlyPilot(userInput, session)) {
            return Optional.empty();
        }

        long startTime = System.currentTimeMillis();
        try {
            List<LlmMessage> messages = buildMessages(userInput, session);
            ChatResponse response = llmClient.chatForToolCalls(
                    messages,
                    toolCallbackProvider.getReadOnlyToolCallbacks()
            );
            Optional<FunctionCallPlan> plan = parseResponse(response, session);
            if (plan.isPresent()) {
                log.info("原生Tool Calling规划完成, intent={}, tools={}, 耗时={}ms",
                        plan.get().getIntent(), plan.get().getToolNames(),
                        System.currentTimeMillis() - startTime);
            }
            return plan;
        } catch (Exception e) {
            log.warn("原生Tool Calling规划失败，回退JSON规划器, 耗时={}ms: {}",
                    System.currentTimeMillis() - startTime, e.getMessage());
            return Optional.empty();
        }
    }

    private boolean supportsReadOnlyPilot(String userInput, AgentSession session) {
        String intent = session == null ? null : session.getIntent();
        if (intent == null || intent.isBlank() || "UNKNOWN".equals(intent)) {
            IntentResult heuristic = intentService.identifyByRules(userInput);
            intent = heuristic.getIntent();
        }
        return toolCatalog.findByIntent(intent)
                .map(AgentToolDefinition::readOnly)
                .orElse(false);
    }

    private List<LlmMessage> buildMessages(String userInput, AgentSession session) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.system(SYSTEM_PROMPT));

        List<Map<String, String>> history = session == null ? List.of() : session.getHistory();
        int start = Math.max(0, history.size() - MAX_CONTEXT_MESSAGES);
        for (int i = start; i < history.size(); i++) {
            Map<String, String> item = history.get(i);
            if (item == null || item.get("content") == null || item.get("content").isBlank()) {
                continue;
            }
            if (i == history.size() - 1 && "user".equals(item.get("role"))
                    && userInput != null && userInput.trim().equals(item.get("content").trim())) {
                continue;
            }
            if ("assistant".equals(item.get("role"))) {
                messages.add(LlmMessage.assistant(item.get("content")));
            } else {
                messages.add(LlmMessage.user(item.get("content")));
            }
        }

        Map<String, Object> state = new HashMap<>();
        state.put("current_user_input", userInput);
        state.put("existing_intent", session == null ? null : session.getIntent());
        state.put("existing_slots", session == null ? Map.of() : session.getSlots());
        state.put("pending_slots", session == null ? List.of() : session.getPendingSlots());
        messages.add(LlmMessage.user("请基于当前输入和状态选择工具：\n" + serialize(state)));
        return messages;
    }

    private Optional<FunctionCallPlan> parseResponse(ChatResponse response, AgentSession session) throws Exception {
        if (response == null || response.getResult() == null
                || response.getResult().getOutput() == null
                || !response.getResult().getOutput().hasToolCalls()) {
            return Optional.empty();
        }

        List<AssistantMessage.ToolCall> toolCalls = response.getResult().getOutput().getToolCalls();
        LinkedHashSet<String> toolNames = new LinkedHashSet<>();
        LinkedHashSet<String> intents = new LinkedHashSet<>();
        Map<String, Object> mergedSlots = new HashMap<>();
        if (session != null && session.getSlots() != null) {
            mergedSlots.putAll(session.getSlots());
        }

        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            Optional<AgentToolDefinition> definition = toolCatalog.findByName(toolCall.name())
                    .filter(AgentToolDefinition::readOnly);
            if (definition.isEmpty()) {
                continue;
            }
            toolNames.add(definition.get().name());
            intents.add(definition.get().intent());
            mergedSlots.putAll(parseArguments(toolCall.arguments()));
        }

        if (toolNames.isEmpty() || intents.size() != 1) {
            return Optional.empty();
        }

        RESERVED_ARGUMENT_KEYS.forEach(mergedSlots::remove);
        normalizeArgumentAliases(mergedSlots);
        mergedSlots.entrySet().removeIf(entry ->
                entry.getValue() == null || String.valueOf(entry.getValue()).isBlank());

        String intent = intents.iterator().next();
        List<String> missingSlots = findMissingSlots(intent, mergedSlots);

        FunctionCallPlan plan = new FunctionCallPlan();
        plan.setIntent(intent);
        plan.setConfidence(1.0);
        plan.setToolNames(new ArrayList<>(toolNames));
        plan.setSlots(mergedSlots);
        plan.setMissingSlots(missingSlots);
        plan.setAskQuestion(missingSlots.isEmpty()
                ? null
                : toolCatalog.getSlotQuestion(missingSlots.get(0)));
        return Optional.of(plan);
    }

    private Map<String, Object> parseArguments(String arguments) throws Exception {
        if (arguments == null || arguments.isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(arguments, new TypeReference<>() {});
    }

    private List<String> findMissingSlots(String intent, Map<String, Object> slots) {
        List<String> missing = new ArrayList<>();
        for (String required : toolCatalog.getRequiredSlots(intent)) {
            Object value = slots.get(required);
            if (value == null || String.valueOf(value).isBlank()) {
                missing.add(required);
            }
        }
        return missing;
    }

    private void normalizeArgumentAliases(Map<String, Object> arguments) {
        moveIfPresent(arguments, "time_context", "timeContext");
        moveIfPresent(arguments, "time_slot", "timeSlot");
        moveIfPresent(arguments, "urgency_minutes", "urgencyMinutes");
        moveIfPresent(arguments, "destination_type", "destinationType");
        moveIfPresent(arguments, "user_lat", "userLat");
        moveIfPresent(arguments, "user_lng", "userLng");
        moveIfPresent(arguments, "original_query", "originalQuery");
    }

    private void moveIfPresent(Map<String, Object> arguments, String from, String to) {
        if (arguments.containsKey(from) && !arguments.containsKey(to)) {
            arguments.put(to, arguments.get(from));
        }
        arguments.remove(from);
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
