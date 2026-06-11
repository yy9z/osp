package com.caspar.agent.service;

import com.caspar.agent.model.AgentCard;
import com.caspar.agent.model.AgentChatMessage;
import com.caspar.agent.model.AgentSession;
import com.caspar.agent.model.AgentSessionHistory;
import com.caspar.agent.model.AgentSessionSummary;
import com.caspar.entity.AgentLogRecord;
import com.caspar.mapper.AgentHistoryMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Agent 历史会话持久化服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentHistoryService {

    private static final int DEFAULT_SESSION_LIMIT = 30;
    private static final int DEFAULT_HISTORY_LIMIT = 200;

    private final AgentHistoryMapper agentHistoryMapper;
    private final ObjectMapper objectMapper;

    public boolean isSessionAccessible(Long userId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return true;
        }
        try {
            Long owner = agentHistoryMapper.findSessionOwner(sessionId);
            return owner == null || owner.equals(userId);
        } catch (Exception e) {
            log.warn("检查会话归属失败，按拒绝访问处理: {}", e.getMessage());
            return false;
        }
    }

    public List<AgentSessionSummary> listSessions(Long userId, Integer limit) {
        try {
            int safeLimit = normalizeLimit(limit, DEFAULT_SESSION_LIMIT, 100);
            List<AgentSessionSummary> summaries = agentHistoryMapper.selectSessionSummaries(userId, safeLimit);
            if (summaries == null || summaries.isEmpty()) {
                return Collections.emptyList();
            }
            for (AgentSessionSummary summary : summaries) {
                if (summary.getTitle() == null || summary.getTitle().isBlank()) {
                    summary.setTitle(buildFallbackTitle(summary.getSessionId()));
                }
                if (summary.getMessageCount() == null) {
                    summary.setMessageCount(0);
                }
            }
            return summaries;
        } catch (Exception e) {
            log.warn("查询 Agent 会话列表失败，降级为空: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public AgentSessionHistory getSessionHistory(Long userId, String sessionId, Integer limit) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId 不能为空");
        }
        if (!isSessionAccessible(userId, sessionId)) {
            throw new SecurityException("无权访问该会话");
        }
        int safeLimit = normalizeLimit(limit, DEFAULT_HISTORY_LIMIT, 500);
        List<AgentLogRecord> logs = loadSessionLogsWithFallback(userId, sessionId, safeLimit);
        List<AgentChatMessage> messages = new ArrayList<>();
        for (AgentLogRecord log : logs) {
            if (log.getUserInput() != null && !log.getUserInput().isBlank()) {
                messages.add(AgentChatMessage.builder()
                        .role("user")
                        .content(log.getUserInput())
                        .cards(Collections.emptyList())
                        .timestamp(log.getCreatedAt())
                        .build());
            }
            if (log.getReply() != null && !log.getReply().isBlank()) {
                messages.add(AgentChatMessage.builder()
                        .role("agent")
                        .content(log.getReply())
                        .cards(parseCardsFromPayload(log.getResponsePayload()))
                        .timestamp(log.getCreatedAt())
                        .build());
            }
        }
        return AgentSessionHistory.builder()
                .sessionId(sessionId)
                .messages(messages)
                .build();
    }

    public boolean deleteSession(Long userId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId 不能为空");
        }
        if (!isSessionAccessible(userId, sessionId)) {
            throw new SecurityException("无权删除该会话");
        }
        try {
            agentHistoryMapper.deleteSessionLogs(userId, sessionId);
            return agentHistoryMapper.deleteSession(userId, sessionId) > 0;
        } catch (RuntimeException e) {
            log.warn("删除 Agent 会话失败: sessionId={}, err={}", sessionId, e.getMessage());
            throw e;
        }
    }

    public void restoreSessionHistory(AgentSession session, Long userId, String sessionId, Integer maxTurns) {
        if (session == null || sessionId == null || sessionId.isBlank()) {
            return;
        }
        if (!isSessionAccessible(userId, sessionId)) {
            return;
        }
        int safeLimit = normalizeLimit(maxTurns, 20, 100);
        List<AgentLogRecord> logs = loadSessionLogsWithFallback(userId, sessionId, safeLimit);
        for (AgentLogRecord log : logs) {
            if (log.getUserInput() != null && !log.getUserInput().isBlank()) {
                session.addHistory("user", log.getUserInput());
            }
            if (log.getReply() != null && !log.getReply().isBlank()) {
                session.addHistory("assistant", log.getReply());
            }
        }
    }

    public void recordTurn(String sessionId,
                           Long userId,
                           Integer turn,
                           String userInput,
                           String intent,
                           Map<String, Object> slots,
                           List<String> toolsUsed,
                           Object responsePayload,
                           String reply,
                           boolean success,
                           String errorMessage,
                           Integer costMs) {
        try {
            String title = buildTitle(userInput);
            agentHistoryMapper.upsertSession(sessionId, userId, title, intent);

            AgentLogRecord logRecord = new AgentLogRecord();
            logRecord.setSessionId(sessionId);
            logRecord.setUserId(userId);
            logRecord.setTurn(turn == null ? 1 : turn);
            logRecord.setUserInput(userInput);
            logRecord.setIntent(intent);
            logRecord.setSlots(toJson(slots));
            logRecord.setToolsUsed(toJson(toolsUsed));
            logRecord.setResponsePayload(toJson(responsePayload));
            logRecord.setReply(reply);
            logRecord.setSuccess(success);
            logRecord.setErrorMsg(errorMessage);
            logRecord.setCostMs(costMs);
            try {
                agentHistoryMapper.insertLog(logRecord);
            } catch (Exception schemaEx) {
                // 兼容旧表结构（未执行 response_payload 增量脚本）
                agentHistoryMapper.insertLogLegacy(logRecord);
            }
            agentHistoryMapper.incrementSessionStats(sessionId, 2, intent);
        } catch (Exception e) {
            log.warn("记录 Agent 历史失败: sessionId={}, err={}", sessionId, e.getMessage());
        }
    }

    private List<AgentLogRecord> loadSessionLogsWithFallback(Long userId, String sessionId, Integer limit) {
        try {
            return agentHistoryMapper.selectSessionLogs(userId, sessionId, limit);
        } catch (Exception e) {
            try {
                return agentHistoryMapper.selectSessionLogsLegacy(userId, sessionId, limit);
            } catch (Exception legacyEx) {
                log.warn("查询 Agent 会话历史失败，降级为空: {}", legacyEx.getMessage());
                return Collections.emptyList();
            }
        }
    }

    private List<AgentCard> parseCardsFromPayload(String responsePayload) {
        if (responsePayload == null || responsePayload.isBlank()) {
            return Collections.emptyList();
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(
                    responsePayload,
                    new TypeReference<Map<String, Object>>() {}
            );
            Object cardsObj = payload.get("cards");
            if (cardsObj == null) {
                return Collections.emptyList();
            }
            List<AgentCard> cards = objectMapper.convertValue(
                    cardsObj,
                    new TypeReference<List<AgentCard>>() {}
            );
            return cards == null ? Collections.emptyList() : cards;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private int normalizeLimit(Integer limit, int defaultValue, int maxValue) {
        if (limit == null || limit <= 0) {
            return defaultValue;
        }
        return Math.min(limit, maxValue);
    }

    private String toJson(Object value) {
        try {
            if (value == null) {
                return null;
            }
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildTitle(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return "新会话";
        }
        String trimmed = userInput.trim().replaceAll("\\s+", " ");
        return trimmed.length() <= 20 ? trimmed : trimmed.substring(0, 20);
    }

    private String buildFallbackTitle(String sessionId) {
        if (sessionId == null || sessionId.length() < 6) {
            return "会话";
        }
        return "会话 " + sessionId.substring(0, 6);
    }
}
