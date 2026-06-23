package com.caspar.agent.service;

import com.caspar.agent.framework.CampusAgentExecution;
import com.caspar.agent.framework.CampusAgentGraph;
import com.caspar.agent.model.AgentRequest;
import com.caspar.agent.model.AgentResponse;
import com.caspar.agent.model.AgentSession;
import com.caspar.agent.session.AgentSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Agent orchestration boundary: prepares sessions, invokes the graph and handles
 * failures that should not escape to the HTTP layer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final CampusAgentGraph campusAgentGraph;
    private final AgentSessionManager sessionManager;
    private final AgentHistoryService agentHistoryService;
    private final AgentBranchService branchService;
    private final AgentTurnRecorderService turnRecorderService;

    @Value("${agent.session.restore-history-limit:20}")
    private int restoreHistoryLimit;

    /**
     * Handles one user turn and delegates business routing to the Agent graph.
     */
    public AgentResponse process(AgentRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        AgentSession session = sessionManager.getOrCreate(request.getSessionId(), userId);
        String sessionId = session.getSessionId();
        String userInput = request.getMessage();

        if (request.getSessionId() != null && session.getHistory().isEmpty()) {
            agentHistoryService.restoreSessionHistory(
                    session, userId, request.getSessionId(), Math.max(1, restoreHistoryLimit)
            );
        }

        session.addHistory("user", userInput);
        session.setTurnCount(session.getTurnCount() + 1);
        int currentTurn = session.getTurnCount();
        branchService.mergeRequestContext(session, request.getContext());

        CampusAgentExecution execution = new CampusAgentExecution(
                session, userId, sessionId, userInput, currentTurn, startTime
        );
        try {
            return campusAgentGraph.execute(
                    execution, request.getConfirmationId(), request.getConfirmationDecision()
            );
        } catch (Exception e) {
            log.error("Agent Graph处理异常: sessionId={}", sessionId, e);
            String errorReply = "抱歉，处理过程中出现了问题，请稍后再试。";
            session.addHistory("assistant", errorReply);
            sessionManager.save(session);
            turnRecorderService.recordTurnSafely(
                    session, userId, currentTurn, userInput, session.getIntent(), session.getSlots(),
                    Collections.emptyList(), null, errorReply, false, e.getMessage(), elapsedMs(startTime)
            );

            return AgentResponse.builder()
                    .sessionId(sessionId)
                    .reply(errorReply)
                    .intent(session.getIntent())
                    .extractedSlots(session.getSlots())
                    .cards(Collections.emptyList())
                    .usedTools(Collections.emptyList())
                    .taskCompleted(false)
                    .followUpSuggestions(Collections.emptyList())
                    .build();
        }
    }

    /**
     * Releases any persisted graph state associated with a deleted session.
     */
    public void clearCheckpoint(String sessionId) {
        campusAgentGraph.clearCheckpoint(sessionId);
    }

    private int elapsedMs(long startTime) {
        return (int) (System.currentTimeMillis() - startTime);
    }
}
