package com.caspar.agent.service;

import com.caspar.agent.model.AgentResponse;
import com.caspar.agent.model.AgentSession;
import com.caspar.agent.model.SlotResult;
import com.caspar.agent.model.ToolResult;
import com.caspar.agent.session.AgentSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Agent 分支处理服务：负责通用问答、未知意图、槽位追问和工具澄清分支的响应构建。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentBranchService {

    private final ResponseGeneratorService responseGeneratorService;
    private final AgentSessionManager sessionManager;
    private final AgentTurnRecorderService turnRecorderService;
    private final FollowUpSuggestionService followUpSuggestionService;

    public AgentResponse handleGeneralChat(AgentSession session, Long userId, String sessionId,
                                           String userInput, int currentTurn, long startTime) {
        String reply = responseGeneratorService.generateUnknown(userInput);
        session.addHistory("assistant", reply);
        session.setIntent(null);
        session.setTurnCount(0);
        sessionManager.save(session);
        turnRecorderService.recordTurnSafely(session, userId, currentTurn, userInput, "GENERAL_CHAT", session.getSlots(),
                Collections.emptyList(), null, reply, true, null, elapsedMs(startTime));
        log.info("Agent请求完成(FAST-GENERAL), 总耗时={}ms", System.currentTimeMillis() - startTime);

        return AgentResponse.builder()
                .sessionId(sessionId)
                .reply(reply)
                .intent("GENERAL_CHAT")
                .extractedSlots(Collections.emptyMap())
                .cards(Collections.emptyList())
                .usedTools(Collections.emptyList())
                .taskCompleted(false)
                .build();
    }

    public AgentResponse handleUnknownIntent(AgentSession session, Long userId, String sessionId,
                                             String userInput, int currentTurn, boolean likelyBusiness, long startTime) {
        if (likelyBusiness) {
            String clarifyReply = """
                    我会优先按校园业务为你办理。请问你更接近哪类需求？
                    1) 宿舍报修
                    2) 二手交易
                    3) 失物招领
                    4) 校园导航
                    5) 消息通知
                    你也可以直接说完整需求，例如"我宿舍灯坏了，帮我报修"。
                    """.trim();
            List<String> options = Arrays.asList("宿舍报修", "二手交易", "失物招领", "校园导航", "消息通知", "通用问答");
            session.addHistory("assistant", clarifyReply);
            sessionManager.save(session);
            turnRecorderService.recordTurnSafely(session, userId, currentTurn, userInput, "UNKNOWN", session.getSlots(),
                    Collections.emptyList(), null, clarifyReply, true, null, elapsedMs(startTime));
            log.info("Agent请求完成(UNKNOWN-业务分流), 总耗时={}ms", System.currentTimeMillis() - startTime);

            return AgentResponse.builder()
                    .sessionId(sessionId)
                    .reply(clarifyReply)
                    .askFor("business_intent")
                    .intent("UNKNOWN")
                    .extractedSlots(Collections.emptyMap())
                    .cards(Collections.emptyList())
                    .usedTools(Collections.emptyList())
                    .taskCompleted(false)
                    .followUpType("business_intent_clarification")
                    .followUpOptions(options)
                    .build();
        }

        String reply = responseGeneratorService.generateUnknown(userInput);
        session.addHistory("assistant", reply);
        session.setIntent(null);
        session.setTurnCount(0);
        sessionManager.save(session);
        turnRecorderService.recordTurnSafely(session, userId, currentTurn, userInput, "GENERAL_CHAT", session.getSlots(),
                Collections.emptyList(), null, reply, true, null, elapsedMs(startTime));
        log.info("Agent请求完成(UNKNOWN), 总耗时={}ms", System.currentTimeMillis() - startTime);

        return AgentResponse.builder()
                .sessionId(sessionId)
                .reply(reply)
                .intent("GENERAL_CHAT")
                .extractedSlots(Collections.emptyMap())
                .cards(Collections.emptyList())
                .usedTools(Collections.emptyList())
                .taskCompleted(false)
                .build();
    }

    public AgentResponse handleSlotAsk(AgentSession session, Long userId, String sessionId,
                                       String userInput, String intent, SlotResult slotResult,
                                       int currentTurn, long startTime) {
        String askReply = slotResult.getAskQuestion();
        session.addHistory("assistant", askReply);
        sessionManager.save(session);
        turnRecorderService.recordTurnSafely(session, userId, currentTurn, userInput, intent, slotResult.getSlots(),
                Collections.emptyList(), null, askReply, true, null, elapsedMs(startTime));
        log.info("Agent请求完成(追问), intent={}, 总耗时={}ms", intent, System.currentTimeMillis() - startTime);

        return AgentResponse.builder()
                .sessionId(sessionId)
                .reply(askReply)
                .askFor(slotResult.getMissingSlots().get(0))
                .intent(intent)
                .extractedSlots(slotResult.getSlots())
                .cards(Collections.emptyList())
                .usedTools(Collections.emptyList())
                .taskCompleted(false)
                .build();
    }

    public AgentResponse buildToolFollowUpResponse(AgentSession session, String sessionId, String intent,
                                                   List<String> toolNames, List<ToolResult> toolResults,
                                                   Map<String, Object> extractedSlots) {
        for (ToolResult result : toolResults) {
            if (!result.isSuccess() || !(result.getData() instanceof Map<?, ?> data)) {
                continue;
            }
            if (!Boolean.TRUE.equals(data.get("clarificationRequired"))) {
                continue;
            }
            String reply = String.valueOf(data.containsKey("agentSummary") ? data.get("agentSummary") : result.getSummary());
            List<String> options = data.get("clarificationOptions") instanceof List<?> list
                    ? list.stream().map(String::valueOf).toList()
                    : Collections.emptyList();
            String followUpType = String.valueOf(data.containsKey("clarificationType") ? data.get("clarificationType") : "clarification");
            session.addHistory("assistant", reply);
            return AgentResponse.builder()
                    .sessionId(sessionId)
                    .reply(reply)
                    .askFor("campus")
                    .intent(intent)
                    .extractedSlots(extractedSlots)
                    .cards(Collections.emptyList())
                    .usedTools(toolNames)
                    .taskCompleted(false)
                    .followUpType(followUpType)
                    .followUpOptions(options)
                    .build();
        }
        return null;
    }

    public void mergeRequestContext(AgentSession session, Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return;
        }
        Map<String, Object> slots = session.getSlots();
        if (slots == null || slots.isEmpty()) {
            slots = new java.util.HashMap<>();
            session.setSlots(slots);
        }
        Map<String, Object> targetSlots = slots;
        context.forEach((key, value) -> {
            if (value != null && !String.valueOf(value).isBlank()) {
                targetSlots.put(key, value);
            }
        });
    }

    public List<String> buildFollowUpSuggestions(String intent, Map<String, Object> slots) {
        return followUpSuggestionService.build(intent, slots);
    }

    private int elapsedMs(long startTime) {
        return (int) (System.currentTimeMillis() - startTime);
    }
}
