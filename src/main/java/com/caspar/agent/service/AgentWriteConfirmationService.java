package com.caspar.agent.service;

import com.caspar.agent.model.AgentConfirmationDecision;
import com.caspar.agent.model.AgentResponse;
import com.caspar.agent.model.FunctionCallPlan;
import com.caspar.agent.registry.AgentToolCatalog;
import com.caspar.agent.registry.AgentToolDefinition;
import com.caspar.agent.registry.AgentToolParameter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds safe write previews and parses confirmation decisions.
 */
@Service
@RequiredArgsConstructor
public class AgentWriteConfirmationService {

    private final AgentToolCatalog toolCatalog;

    public boolean requiresConfirmation(List<String> toolNames) {
        if (toolNames == null || toolNames.isEmpty()) {
            return false;
        }
        return toolNames.stream().anyMatch(name -> toolCatalog.findByName(name)
                .map(definition -> !definition.readOnly())
                .orElse(true));
    }

    public AgentConfirmationDecision parseDecision(String structuredDecision, String userInput) {
        return AgentConfirmationDecision.parse(structuredDecision, userInput);
    }

    public AgentResponse buildConfirmationResponse(String sessionId,
                                                   String confirmationId,
                                                   FunctionCallPlan plan) {
        Map<String, Object> preview = buildPreview(plan);
        String action = String.valueOf(preview.getOrDefault("action", "执行写操作"));
        return AgentResponse.builder()
                .sessionId(sessionId)
                .reply("即将" + action + "。请核对信息后确认是否执行。")
                .askFor("confirmation")
                .intent(plan.getIntent())
                .extractedSlots(plan.getSlots())
                .usedTools(plan.getToolNames())
                .taskCompleted(false)
                .followUpType("write_confirmation")
                .followUpOptions(List.of("确认执行", "取消"))
                .confirmationRequired(true)
                .confirmationId(confirmationId)
                .confirmationPreview(preview)
                .build();
    }

    private Map<String, Object> buildPreview(FunctionCallPlan plan) {
        Map<String, Object> preview = new LinkedHashMap<>();
        List<Map<String, Object>> parameters = new ArrayList<>();
        String action = "执行写操作";

        for (String toolName : plan.getToolNames()) {
            AgentToolDefinition definition = toolCatalog.findByName(toolName).orElse(null);
            if (definition == null) {
                continue;
            }
            action = definition.description();
            Map<String, AgentToolParameter> parameterDefinitions = new LinkedHashMap<>();
            for (AgentToolParameter parameter : definition.parameters()) {
                parameterDefinitions.put(parameter.name(), parameter);
            }
            for (Map.Entry<String, Object> entry : plan.getSlots().entrySet()) {
                if (entry.getValue() == null || String.valueOf(entry.getValue()).isBlank()) {
                    continue;
                }
                AgentToolParameter parameter = parameterDefinitions.get(entry.getKey());
                parameters.add(Map.of(
                        "name", entry.getKey(),
                        "label", parameter == null ? entry.getKey() : parameter.description(),
                        "value", entry.getValue()
                ));
            }
        }

        preview.put("action", action);
        preview.put("parameters", parameters);
        return preview;
    }
}
