package com.caspar.agent.service;

import com.caspar.agent.model.AgentCard;
import com.caspar.agent.model.ToolResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 将本地工具结果转换为前端可渲染的 AgentCard。
 */
@Service
public class AgentToolCardService {

    public List<AgentCard> toCards(List<AgentToolExecutionRecord> records) {
        List<AgentCard> cards = new ArrayList<>();
        if (records == null) {
            return cards;
        }
        for (AgentToolExecutionRecord record : records) {
            ToolResult result = record.result();
            if (result != null && result.isSuccess() && result.getData() != null) {
                cards.add(AgentCard.of(resolveCardType(record.toolName()), result.getData()));
            }
        }
        return cards;
    }

    private String resolveCardType(String toolName) {
        return switch (toolName) {
            case "dorm_repair", "repair_query" -> "REPAIR_ORDER";
            case "secondhand_search" -> "PRODUCT";
            case "lostfound_lost", "lostfound_found" -> "LOST_FOUND";
            case "navigation", "navigation_v2" -> "ROUTE";
            case "message_query" -> "MESSAGE";
            case "campus_tips" -> "PROACTIVE_TIPS";
            case "campus_knowledge_query" -> "KNOWLEDGE";
            default -> "INFO";
        };
    }
}
