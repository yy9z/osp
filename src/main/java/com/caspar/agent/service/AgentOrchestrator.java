package com.caspar.agent.service;

import com.caspar.agent.model.AgentCard;
import com.caspar.agent.model.AgentRequest;
import com.caspar.agent.model.AgentResponse;
import com.caspar.agent.model.AgentSession;
import com.caspar.agent.model.CampusAgentRunResult;
import com.caspar.agent.model.ToolResult;
import com.caspar.agent.session.AgentSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent 核心编排服务：协调 ReactAgent、本地工具回调、前端卡片和会话记录。
 * 从 AgentController 中抽离，使 Controller 只负责 HTTP 层职责。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private static final int MAX_ASK_TURNS = 3;

    private final IntentService intentService;
    private final CampusReactAgentService campusReactAgentService;
    private final AgentToolExecutionRecorder toolExecutionRecorder;
    private final AgentToolCardService agentToolCardService;
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

        String traceId = null;
        try {
            String existingIntent = session.getIntent();
            boolean likelyBusiness = intentService.isPossiblyBusinessScenario(userInput);

            if ((existingIntent == null || "UNKNOWN".equals(existingIntent)) && !likelyBusiness) {
                return branchService.handleGeneralChat(session, userId, sessionId, userInput, currentTurn, startTime);
            }

            traceId = UUID.randomUUID().toString();
            toolExecutionRecorder.begin(traceId);
            CampusAgentRunResult runResult = campusReactAgentService.run(userInput, session, userId, traceId);
            List<AgentToolExecutionRecord> toolRecords = toolExecutionRecorder.snapshot(traceId);
            toolExecutionRecorder.clear(traceId);

            List<String> toolNames = toolRecords.stream().map(AgentToolExecutionRecord::toolName).toList();
            List<ToolResult> toolResults = toolRecords.stream().map(AgentToolExecutionRecord::result).toList();
            String intent = resolveIntent(toolNames, existingIntent, likelyBusiness);
            Map<String, Object> extractedSlots = mergeExtractedSlots(session.getSlots(), toolRecords);

            session.setIntent(intent);
            session.setSlots(extractedSlots);
            session.setPendingSlots(Collections.emptyList());

            AgentResponse followUpResponse = branchService.buildToolFollowUpResponse(session, sessionId, intent, toolNames, toolResults, extractedSlots);
            if (followUpResponse != null) {
                sessionManager.save(session);
                turnRecorderService.recordTurnSafely(session, userId, currentTurn, userInput, intent, extractedSlots, toolNames,
                        Map.of(
                                "followUpType", followUpResponse.getFollowUpType(),
                                "followUpOptions", followUpResponse.getFollowUpOptions() == null ? Collections.emptyList() : followUpResponse.getFollowUpOptions()
                        ),
                        followUpResponse.getReply(), true, null, elapsedMs(startTime));
                log.info("Agent请求完成(澄清), intent={}, 总耗时={}ms", intent, System.currentTimeMillis() - startTime);
                return followUpResponse;
            }

            if (toolResults.isEmpty() && "UNKNOWN".equals(intent)) {
                return branchService.handleUnknownIntent(session, userId, sessionId, userInput, currentTurn, likelyBusiness, startTime);
            }

            String reply = resolveReply(runResult.reply(), userInput, toolResults);
            List<AgentCard> cards = agentToolCardService.toCards(toolRecords);
            boolean taskCompleted = !toolResults.isEmpty() && toolResults.stream().allMatch(ToolResult::isSuccess);
            if (toolResults.isEmpty() && currentTurn >= MAX_ASK_TURNS) {
                taskCompleted = false;
                session.setTurnCount(0);
            }

            session.addHistory("assistant", reply);
            if (taskCompleted) {
                session.setIntent(null);
                session.setSlots(Collections.emptyMap());
                session.setPendingSlots(Collections.emptyList());
                session.setTurnCount(0);
            }
            sessionManager.save(session);

            List<String> followUpSuggestions = taskCompleted
                    ? branchService.buildFollowUpSuggestions(intent, extractedSlots)
                    : Collections.emptyList();
            turnRecorderService.recordTurnSafely(session, userId, currentTurn, userInput, intent, extractedSlots, toolNames,
                    Map.of("cards", cards, "followUpSuggestions", followUpSuggestions),
                    reply, true, null, elapsedMs(startTime));

            log.info("Agent请求完成(ReactAgent), intent={}, tools={}, completed={}, 总耗时={}ms",
                    intent, toolNames, taskCompleted, System.currentTimeMillis() - startTime);
            return AgentResponse.builder()
                    .sessionId(sessionId)
                    .reply(reply)
                    .intent(intent)
                    .extractedSlots(extractedSlots)
                    .cards(cards)
                    .usedTools(toolNames)
                    .taskCompleted(taskCompleted)
                    .followUpSuggestions(followUpSuggestions)
                    .build();

        } catch (Exception e) {
            toolExecutionRecorder.clear(traceId);
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

    private String resolveReply(String agentReply, String userInput, List<ToolResult> toolResults) {
        if (agentReply != null && !agentReply.isBlank()) {
            return agentReply;
        }
        if (toolResults != null && !toolResults.isEmpty()) {
            return responseGeneratorService.generate(userInput, toolResults);
        }
        return "我还需要更多信息才能办理这个校园事务，请补充一下具体需求。";
    }

    private Map<String, Object> mergeExtractedSlots(Map<String, Object> currentSlots, List<AgentToolExecutionRecord> records) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (currentSlots != null) {
            merged.putAll(currentSlots);
        }
        if (records != null) {
            for (AgentToolExecutionRecord record : records) {
                if (record.params() != null) {
                    merged.putAll(record.params());
                }
            }
        }
        return merged;
    }

    private String resolveIntent(List<String> toolNames, String existingIntent, boolean likelyBusiness) {
        if (toolNames == null || toolNames.isEmpty()) {
            if (existingIntent != null && !existingIntent.isBlank()) {
                return existingIntent;
            }
            return likelyBusiness ? "UNKNOWN" : "GENERAL_CHAT";
        }
        return switch (toolNames.get(0)) {
            case "dorm_repair" -> "DORM_REPAIR";
            case "dorm_query" -> "DORM_QUERY";
            case "repair_query" -> "REPAIR_QUERY";
            case "secondhand_search" -> "SECONDHAND_SEARCH";
            case "secondhand_publish" -> "SECONDHAND_PUBLISH";
            case "lostfound_lost" -> "LOSTFOUND_LOST";
            case "lostfound_found" -> "LOSTFOUND_FOUND";
            case "navigation_v2", "navigation" -> "NAVIGATION";
            case "message_query" -> "MESSAGE_QUERY";
            case "campus_tips" -> "CAMPUS_TIPS";
            case "campus_knowledge_query" -> "CAMPUS_KNOWLEDGE";
            default -> "UNKNOWN";
        };
    }
}
