package com.caspar.agent.service;

import com.caspar.agent.model.AgentSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Agent 回合持久化服务。
 * 对 recordTurn 提供统一异常保护，避免历史记录失败影响主流程返回。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentTurnRecorderService {

    private final AgentHistoryService agentHistoryService;

    public void recordTurnSafely(AgentSession session, Long userId, int turn, String userInput,
                                 String intent, Map<String, Object> slots, List<String> usedTools,
                                 Object responsePayload, String reply, boolean success,
                                 String errorMessage, int costMs) {
        try {
            agentHistoryService.recordTurn(
                    session.getSessionId(), userId, turn, userInput, intent, slots,
                    usedTools, responsePayload, reply, success, errorMessage, costMs);
        } catch (Exception e) {
            log.warn("记录对话历史失败, sessionId={}: {}", session.getSessionId(), e.getMessage());
        }
    }
}
