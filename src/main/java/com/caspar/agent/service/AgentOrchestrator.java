package com.caspar.agent.service;

import com.caspar.agent.model.AgentCard;
import com.caspar.agent.model.AgentRequest;
import com.caspar.agent.model.AgentResponse;
import com.caspar.agent.model.AgentSession;
import com.caspar.agent.model.FunctionCallPlan;
import com.caspar.agent.model.SlotResult;
import com.caspar.agent.model.ToolResult;
import com.caspar.agent.session.AgentSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Agent 核心编排服务：协调 Function Calling 规划、本地函数调用、回复生成。
 * 从 AgentController 中抽离，使 Controller 只负责 HTTP 层职责。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private static final int MAX_ASK_TURNS = 3;

    private final IntentService intentService;
    private final FunctionCallingPlannerService functionCallingPlannerService;
    private final ToolExecutorService toolExecutorService;
    private final ResponseGeneratorService responseGeneratorService;
    private final AgentSessionManager sessionManager;
    private final AgentHistoryService agentHistoryService;
    private final AgentBranchService branchService;
    private final AgentTurnRecorderService turnRecorderService;

    @Value("${agent.session.restore-history-limit:20}")
    private int restoreHistoryLimit;

    /**
     * 处理一次用户对话，返回 Agent 回复。
     */
    public AgentResponse process(AgentRequest request, Long userId) {
        long startTime = System.currentTimeMillis();

        AgentSession session = sessionManager.getOrCreate(request.getSessionId(), userId);
        String sessionId = session.getSessionId();
        String userInput = request.getMessage();

        if (request.getSessionId() != null && session.getHistory().isEmpty()) {
            agentHistoryService.restoreSessionHistory(session, userId, request.getSessionId(), Math.max(1, restoreHistoryLimit));
        }

        session.addHistory("user", userInput);
        session.setTurnCount(session.getTurnCount() + 1);
        int currentTurn = session.getTurnCount();
        branchService.mergeRequestContext(session, request.getContext());

        try {
            String existingIntent = session.getIntent();
            boolean likelyBusiness = intentService.isPossiblyBusinessScenario(userInput);

            if ((existingIntent == null || "UNKNOWN".equals(existingIntent)) && !likelyBusiness) {
                return branchService.handleGeneralChat(session, userId, sessionId, userInput, currentTurn, startTime);
            }

            FunctionCallPlan functionCallPlan = functionCallingPlannerService.plan(userInput, session);
            String intent = functionCallPlan.getIntent();
            session.setIntent(intent);

            if ("UNKNOWN".equals(intent)) {
                return branchService.handleUnknownIntent(session, userId, sessionId, userInput, currentTurn, likelyBusiness, startTime);
            }

            SlotResult slotResult = toSlotResult(functionCallPlan);
            session.setSlots(slotResult.getSlots());
            session.setPendingSlots(slotResult.getMissingSlots());

            if (!slotResult.getMissingSlots().isEmpty() && session.getTurnCount() <= MAX_ASK_TURNS) {
                return branchService.handleSlotAsk(session, userId, sessionId, userInput, intent, slotResult, currentTurn, startTime);
            }

            List<String> toolNames = functionCallPlan.getToolNames();
            List<ToolResult> toolResults = toolExecutorService.execute(toolNames, slotResult.getSlots(), userId);

            AgentResponse followUpResponse = branchService.buildToolFollowUpResponse(session, sessionId, intent, toolNames, toolResults, slotResult.getSlots());
            if (followUpResponse != null) {
                sessionManager.save(session);
                turnRecorderService.recordTurnSafely(session, userId, currentTurn, userInput, intent, slotResult.getSlots(), toolNames,
                        Map.of(
                                "followUpType", followUpResponse.getFollowUpType(),
                                "followUpOptions", followUpResponse.getFollowUpOptions() == null ? Collections.emptyList() : followUpResponse.getFollowUpOptions()
                        ),
                        followUpResponse.getReply(), true, null, elapsedMs(startTime));
                log.info("Agent请求完成(澄清), intent={}, 总耗时={}ms", intent, System.currentTimeMillis() - startTime);
                return followUpResponse;
            }

            String reply = responseGeneratorService.generate(userInput, toolResults);
            List<AgentCard> cards = toolExecutorService.toCards(toolNames, toolResults);

            session.addHistory("assistant", reply);
            session.setIntent(null);
            session.setSlots(Collections.emptyMap());
            session.setPendingSlots(Collections.emptyList());
            session.setTurnCount(0);
            sessionManager.save(session);

            List<String> followUpSuggestions = branchService.buildFollowUpSuggestions(intent, slotResult.getSlots());
            turnRecorderService.recordTurnSafely(session, userId, currentTurn, userInput, intent, slotResult.getSlots(), toolNames,
                    Map.of("cards", cards, "followUpSuggestions", followUpSuggestions),
                    reply, true, null, elapsedMs(startTime));

            log.info("Agent请求完成(成功), intent={}, 总耗时={}ms", intent, System.currentTimeMillis() - startTime);
            return AgentResponse.builder()
                    .sessionId(sessionId)
                    .reply(reply)
                    .intent(intent)
                    .extractedSlots(slotResult.getSlots())
                    .cards(cards)
                    .usedTools(toolNames)
                    .taskCompleted(true)
                    .followUpSuggestions(followUpSuggestions)
                    .build();

        } catch (Exception e) {
            log.error("Agent处理异常: sessionId={}", sessionId, e);
            String errorReply = "抱歉，处理过程中出现了问题，请稍后再试。";
            session.addHistory("assistant", errorReply);
            sessionManager.save(session);
            turnRecorderService.recordTurnSafely(session, userId, currentTurn, userInput, session.getIntent(), session.getSlots(),
                    Collections.emptyList(), null, errorReply, false, e.getMessage(), elapsedMs(startTime));

            return AgentResponse.builder()
                    .sessionId(sessionId)
                    .reply(errorReply)
                    .intent(session.getIntent())
                    .extractedSlots(session.getSlots())
                    .cards(Collections.emptyList())
                    .usedTools(Collections.emptyList())
                    .taskCompleted(false)
                    .build();
        }
    }

    private int elapsedMs(long startTime) {
        return (int) (System.currentTimeMillis() - startTime);
    }

    private SlotResult toSlotResult(FunctionCallPlan functionCallPlan) {
        SlotResult slotResult = new SlotResult();
        slotResult.setSlots(functionCallPlan.getSlots() == null ? Collections.emptyMap() : functionCallPlan.getSlots());
        slotResult.setMissingSlots(functionCallPlan.getMissingSlots() == null ? Collections.emptyList() : functionCallPlan.getMissingSlots());
        slotResult.setAskQuestion(functionCallPlan.getAskQuestion());
        return slotResult;
    }
}
