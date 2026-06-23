package com.caspar.agent.framework;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.caspar.agent.model.AgentCard;
import com.caspar.agent.model.AgentConfirmationDecision;
import com.caspar.agent.model.AgentResponse;
import com.caspar.agent.model.AgentSession;
import com.caspar.agent.model.FunctionCallPlan;
import com.caspar.agent.model.SlotResult;
import com.caspar.agent.model.ToolResult;
import com.caspar.agent.service.AgentBranchService;
import com.caspar.agent.service.AgentTurnRecorderService;
import com.caspar.agent.service.AgentWriteConfirmationService;
import com.caspar.agent.service.FunctionCallingPlannerService;
import com.caspar.agent.service.IntentService;
import com.caspar.agent.service.ResponseGeneratorService;
import com.caspar.agent.service.ToolExecutorService;
import com.caspar.agent.session.AgentSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Spring AI Alibaba Graph workflow for one campus Agent turn.
 *
 * <p>The graph owns business routing and execution. HTTP/session preparation and
 * top-level exception handling remain in {@code AgentOrchestrator}.</p>
 */
@Slf4j
@Component
public class CampusAgentGraph {

    private static final String EXECUTION = "execution";
    private static final String LIKELY_BUSINESS = "likely_business";
    private static final String PLAN = "plan";
    private static final String TOOL_RESULTS = "tool_results";
    private static final String RESPONSE = "response";
    private static final String NEXT_NODE = "next_node";
    private static final String AWAITING_CONFIRMATION = "awaiting_confirmation";
    private static final String CONFIRMATION_ID = "confirmation_id";
    private static final String CONFIRMATION_DECISION = "confirmation_decision";

    private static final String ROUTE_REQUEST = "route_request";
    private static final String GENERAL_CHAT = "general_chat";
    private static final String PLAN_REQUEST = "plan_request";
    private static final String UNKNOWN_INTENT = "unknown_intent";
    private static final String ASK_FOR_SLOT = "ask_for_slot";
    private static final String PREPARE_WRITE_CONFIRMATION = "prepare_write_confirmation";
    private static final String CONFIRM_WRITE = "confirm_write";
    private static final String CANCEL_WRITE = "cancel_write";
    private static final String EXECUTE_TOOLS = "execute_tools";
    private static final String INSPECT_TOOL_RESULTS = "inspect_tool_results";
    private static final String PERSIST_TOOL_FOLLOW_UP = "persist_tool_follow_up";
    private static final String RENDER_TOOL_RESPONSE = "render_tool_response";

    private final IntentService intentService;
    private final FunctionCallingPlannerService plannerService;
    private final ToolExecutorService toolExecutorService;
    private final ResponseGeneratorService responseGeneratorService;
    private final AgentSessionManager sessionManager;
    private final AgentBranchService branchService;
    private final AgentTurnRecorderService turnRecorderService;
    private final AgentWriteConfirmationService confirmationService;
    private final BaseCheckpointSaver checkpointSaver;
    private final CompiledGraph graph;
    private final ConcurrentHashMap<String, Object> confirmationLocks = new ConcurrentHashMap<>();

    public CampusAgentGraph(IntentService intentService,
                            FunctionCallingPlannerService plannerService,
                            ToolExecutorService toolExecutorService,
                            ResponseGeneratorService responseGeneratorService,
                            AgentSessionManager sessionManager,
                            AgentBranchService branchService,
                            AgentTurnRecorderService turnRecorderService,
                            AgentWriteConfirmationService confirmationService,
                            BaseCheckpointSaver checkpointSaver) {
        this.intentService = intentService;
        this.plannerService = plannerService;
        this.toolExecutorService = toolExecutorService;
        this.responseGeneratorService = responseGeneratorService;
        this.sessionManager = sessionManager;
        this.branchService = branchService;
        this.turnRecorderService = turnRecorderService;
        this.confirmationService = confirmationService;
        this.checkpointSaver = checkpointSaver;
        this.graph = compileGraph();
    }

    public AgentResponse execute(CampusAgentExecution execution) {
        return execute(execution, null, null);
    }

    public AgentResponse execute(CampusAgentExecution execution,
                                 String confirmationId,
                                 String structuredDecision) {
        Object lock = confirmationLocks.computeIfAbsent(execution.sessionId(), ignored -> new Object());
        try {
            synchronized (lock) {
                RunnableConfig config = config(execution.sessionId());
                Optional<StateSnapshot> pendingState = graph.stateOf(config)
                        .filter(snapshot -> isAwaitingConfirmation(snapshot.state()));
                if (pendingState.isPresent()) {
                    return resumeOrRemind(
                            execution, confirmationId, structuredDecision, config, pendingState.get().state()
                    );
                }
                OverAllState finalState = graph.invoke(Map.of(EXECUTION, execution), config)
                        .orElseThrow(() -> new IllegalStateException("Campus Agent graph returned no state"));
                return response(finalState);
            }
        } finally {
            confirmationLocks.remove(execution.sessionId(), lock);
        }
    }

    public void clearCheckpoint(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        try {
            checkpointSaver.release(config(sessionId));
        } catch (Exception e) {
            log.warn("清理 Agent Graph checkpoint 失败, sessionId={}: {}", sessionId, e.getMessage());
        }
    }

    private AgentResponse resumeOrRemind(CampusAgentExecution execution,
                                         String confirmationId,
                                         String structuredDecision,
                                         RunnableConfig config,
                                         OverAllState pendingState) {
        String expectedId = pendingState.value(CONFIRMATION_ID).map(String::valueOf).orElse("");
        AgentConfirmationDecision decision = confirmationService.parseDecision(
                structuredDecision, execution.userInput()
        );
        if (confirmationId != null && !confirmationId.isBlank() && !expectedId.equals(confirmationId)) {
            return remindPendingConfirmation(execution, pendingState, true);
        }
        if (decision == null) {
            return remindPendingConfirmation(execution, pendingState, false);
        }

        try {
            RunnableConfig updatedConfig = graph.updateState(config, Map.of(
                    EXECUTION, execution,
                    CONFIRMATION_DECISION, decision.name(),
                    AWAITING_CONFIRMATION, false
            ));
            OverAllState finalState = graph.invoke((Map<String, Object>) null, updatedConfig)
                    .orElseThrow(() -> new IllegalStateException("Campus Agent graph resume returned no state"));
            return response(finalState);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to resume Campus Agent graph", e);
        }
    }

    private AgentResponse remindPendingConfirmation(CampusAgentExecution execution,
                                                    OverAllState pendingState,
                                                    boolean staleConfirmationId) {
        AgentResponse pendingResponse = response(pendingState);
        String reply = staleConfirmationId
                ? "该确认请求与当前待办不一致，请核对后重新选择。"
                : "当前有一项写操作等待确认，请选择“确认执行”或“取消”。";
        AgentResponse reminder = AgentResponse.builder()
                .sessionId(execution.sessionId())
                .reply(reply)
                .askFor("confirmation")
                .intent(pendingResponse.getIntent())
                .extractedSlots(pendingResponse.getExtractedSlots())
                .usedTools(pendingResponse.getUsedTools())
                .taskCompleted(false)
                .followUpType("write_confirmation")
                .followUpOptions(List.of("确认执行", "取消"))
                .confirmationRequired(true)
                .confirmationId(pendingResponse.getConfirmationId())
                .confirmationPreview(pendingResponse.getConfirmationPreview())
                .build();
        execution.session().addHistory("assistant", reply);
        sessionManager.save(execution.session());
        turnRecorderService.recordTurnSafely(
                execution.session(), execution.userId(), execution.currentTurn(), execution.userInput(),
                pendingResponse.getIntent(), pendingResponse.getExtractedSlots(), Collections.emptyList(),
                confirmationPayload(reminder, "PENDING"), reply, true, null,
                elapsedMs(execution.startTime())
        );
        return reminder;
    }

    private AgentResponse response(OverAllState finalState) {
        return finalState.value(RESPONSE)
                .filter(AgentResponse.class::isInstance)
                .map(AgentResponse.class::cast)
                .orElseThrow(() -> new IllegalStateException("Campus Agent graph returned no response"));
    }

    CompiledGraph compiledGraph() {
        return graph;
    }

    private CompiledGraph compileGraph() {
        try {
            StateGraph workflow = new StateGraph(keyStrategies())
                    .addNode(ROUTE_REQUEST, node_async(this::routeRequest))
                    .addNode(GENERAL_CHAT, node_async(this::handleGeneralChat))
                    .addNode(PLAN_REQUEST, node_async(this::planRequest))
                    .addNode(UNKNOWN_INTENT, node_async(this::handleUnknownIntent))
                    .addNode(ASK_FOR_SLOT, node_async(this::askForSlot))
                    .addNode(PREPARE_WRITE_CONFIRMATION, node_async(this::prepareWriteConfirmation))
                    .addNode(CONFIRM_WRITE, node_async(this::confirmWrite))
                    .addNode(CANCEL_WRITE, node_async(this::cancelWrite))
                    .addNode(EXECUTE_TOOLS, node_async(this::executeTools))
                    .addNode(INSPECT_TOOL_RESULTS, node_async(this::inspectToolResults))
                    .addNode(PERSIST_TOOL_FOLLOW_UP, node_async(this::persistToolFollowUp))
                    .addNode(RENDER_TOOL_RESPONSE, node_async(this::renderToolResponse));

            workflow.addEdge(START, ROUTE_REQUEST);
            workflow.addConditionalEdges(ROUTE_REQUEST, edge_async(this::nextNode), Map.of(
                    GENERAL_CHAT, GENERAL_CHAT,
                    PLAN_REQUEST, PLAN_REQUEST
            ));
            workflow.addConditionalEdges(PLAN_REQUEST, edge_async(this::nextNode), Map.of(
                    UNKNOWN_INTENT, UNKNOWN_INTENT,
                    ASK_FOR_SLOT, ASK_FOR_SLOT,
                    PREPARE_WRITE_CONFIRMATION, PREPARE_WRITE_CONFIRMATION,
                    EXECUTE_TOOLS, EXECUTE_TOOLS
            ));
            workflow.addEdge(PREPARE_WRITE_CONFIRMATION, CONFIRM_WRITE);
            workflow.addConditionalEdges(CONFIRM_WRITE, edge_async(this::nextNode), Map.of(
                    CANCEL_WRITE, CANCEL_WRITE,
                    EXECUTE_TOOLS, EXECUTE_TOOLS
            ));
            workflow.addEdge(EXECUTE_TOOLS, INSPECT_TOOL_RESULTS);
            workflow.addConditionalEdges(INSPECT_TOOL_RESULTS, edge_async(this::nextNode), Map.of(
                    PERSIST_TOOL_FOLLOW_UP, PERSIST_TOOL_FOLLOW_UP,
                    RENDER_TOOL_RESPONSE, RENDER_TOOL_RESPONSE
            ));

            workflow.addEdge(GENERAL_CHAT, END);
            workflow.addEdge(UNKNOWN_INTENT, END);
            workflow.addEdge(ASK_FOR_SLOT, END);
            workflow.addEdge(CANCEL_WRITE, END);
            workflow.addEdge(PERSIST_TOOL_FOLLOW_UP, END);
            workflow.addEdge(RENDER_TOOL_RESPONSE, END);

            return workflow.compile(CompileConfig.builder()
                    .saverConfig(SaverConfig.builder().register(checkpointSaver).build())
                    .interruptBefore(CONFIRM_WRITE)
                    .releaseThread(true)
                    .build());
        } catch (GraphStateException e) {
            throw new IllegalStateException("Unable to compile Campus Agent graph", e);
        }
    }

    private KeyStrategyFactory keyStrategies() {
        return () -> {
            Map<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put(EXECUTION, new ReplaceStrategy());
            strategies.put(LIKELY_BUSINESS, new ReplaceStrategy());
            strategies.put(PLAN, new ReplaceStrategy());
            strategies.put(TOOL_RESULTS, new ReplaceStrategy());
            strategies.put(RESPONSE, new ReplaceStrategy());
            strategies.put(NEXT_NODE, new ReplaceStrategy());
            strategies.put(AWAITING_CONFIRMATION, new ReplaceStrategy());
            strategies.put(CONFIRMATION_ID, new ReplaceStrategy());
            strategies.put(CONFIRMATION_DECISION, new ReplaceStrategy());
            return strategies;
        };
    }

    private Map<String, Object> routeRequest(OverAllState state) {
        CampusAgentExecution execution = execution(state);
        String existingIntent = execution.session().getIntent();
        boolean likelyBusiness = intentService.isPossiblyBusinessScenario(execution.userInput());
        boolean generalChat = (existingIntent == null || "UNKNOWN".equals(existingIntent)) && !likelyBusiness;
        return Map.of(
                LIKELY_BUSINESS, likelyBusiness,
                NEXT_NODE, generalChat ? GENERAL_CHAT : PLAN_REQUEST
        );
    }

    private Map<String, Object> handleGeneralChat(OverAllState state) {
        CampusAgentExecution execution = execution(state);
        AgentResponse response = branchService.handleGeneralChat(
                execution.session(), execution.userId(), execution.sessionId(), execution.userInput(),
                execution.currentTurn(), execution.startTime()
        );
        return Map.of(RESPONSE, response);
    }

    private Map<String, Object> planRequest(OverAllState state) {
        CampusAgentExecution execution = execution(state);
        AgentSession session = execution.session();
        FunctionCallPlan plan = plannerService.plan(execution.userInput(), session);
        String intent = plan.getIntent();
        session.setIntent(intent);

        if ("UNKNOWN".equals(intent)) {
            return Map.of(PLAN, plan, NEXT_NODE, UNKNOWN_INTENT);
        }

        SlotResult slotResult = toSlotResult(plan);
        session.setSlots(slotResult.getSlots());
        session.setPendingSlots(slotResult.getMissingSlots());
        String nextNode;
        if (!slotResult.getMissingSlots().isEmpty()) {
            nextNode = ASK_FOR_SLOT;
        } else if (confirmationService.requiresConfirmation(plan.getToolNames())) {
            nextNode = PREPARE_WRITE_CONFIRMATION;
        } else {
            nextNode = EXECUTE_TOOLS;
        }
        return Map.of(PLAN, plan, NEXT_NODE, nextNode);
    }

    private Map<String, Object> handleUnknownIntent(OverAllState state) {
        CampusAgentExecution execution = execution(state);
        boolean likelyBusiness = state.value(LIKELY_BUSINESS)
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(false);
        AgentResponse response = branchService.handleUnknownIntent(
                execution.session(), execution.userId(), execution.sessionId(), execution.userInput(),
                execution.currentTurn(), likelyBusiness, execution.startTime()
        );
        return Map.of(RESPONSE, response);
    }

    private Map<String, Object> askForSlot(OverAllState state) {
        CampusAgentExecution execution = execution(state);
        FunctionCallPlan plan = plan(state);
        AgentResponse response = branchService.handleSlotAsk(
                execution.session(), execution.userId(), execution.sessionId(), execution.userInput(),
                plan.getIntent(), toSlotResult(plan), execution.currentTurn(), execution.startTime()
        );
        return Map.of(RESPONSE, response);
    }

    private Map<String, Object> prepareWriteConfirmation(OverAllState state) {
        CampusAgentExecution execution = execution(state);
        FunctionCallPlan plan = plan(state);
        String confirmationId = UUID.randomUUID().toString().replace("-", "");
        AgentResponse response = confirmationService.buildConfirmationResponse(
                execution.sessionId(), confirmationId, plan
        );

        execution.session().addHistory("assistant", response.getReply());
        sessionManager.save(execution.session());
        turnRecorderService.recordTurnSafely(
                execution.session(), execution.userId(), execution.currentTurn(), execution.userInput(),
                plan.getIntent(), safeSlots(plan), Collections.emptyList(),
                confirmationPayload(response, "PENDING"), response.getReply(), true, null,
                elapsedMs(execution.startTime())
        );
        log.info("Agent Graph等待写操作确认, intent={}, confirmationId={}",
                plan.getIntent(), confirmationId);
        return Map.of(
                RESPONSE, response,
                AWAITING_CONFIRMATION, true,
                CONFIRMATION_ID, confirmationId
        );
    }

    private Map<String, Object> confirmWrite(OverAllState state) {
        AgentConfirmationDecision decision = state.value(CONFIRMATION_DECISION)
                .map(String::valueOf)
                .map(AgentConfirmationDecision::valueOf)
                .orElseThrow(() -> new IllegalStateException("Write confirmation decision is missing"));
        return Map.of(
                AWAITING_CONFIRMATION, false,
                NEXT_NODE, decision == AgentConfirmationDecision.APPROVE ? EXECUTE_TOOLS : CANCEL_WRITE
        );
    }

    private Map<String, Object> cancelWrite(OverAllState state) {
        CampusAgentExecution execution = execution(state);
        FunctionCallPlan plan = plan(state);
        String confirmationId = state.value(CONFIRMATION_ID).map(String::valueOf).orElse(null);
        String reply = "已取消本次操作，没有写入任何业务数据。";
        AgentSession session = execution.session();
        session.addHistory("assistant", reply);
        session.setIntent(null);
        session.setSlots(Collections.emptyMap());
        session.setPendingSlots(Collections.emptyList());
        session.setTurnCount(0);
        sessionManager.save(session);

        AgentResponse response = AgentResponse.builder()
                .sessionId(execution.sessionId())
                .reply(reply)
                .intent(plan.getIntent())
                .extractedSlots(safeSlots(plan))
                .usedTools(Collections.emptyList())
                .taskCompleted(false)
                .confirmationRequired(false)
                .confirmationId(confirmationId)
                .build();
        turnRecorderService.recordTurnSafely(
                session, execution.userId(), execution.currentTurn(), execution.userInput(),
                plan.getIntent(), safeSlots(plan), Collections.emptyList(),
                confirmationPayload(response, "REJECTED"), reply, true, null,
                elapsedMs(execution.startTime())
        );
        log.info("Agent Graph写操作已取消, intent={}, confirmationId={}",
                plan.getIntent(), confirmationId);
        return Map.of(RESPONSE, response);
    }

    private Map<String, Object> executeTools(OverAllState state) {
        CampusAgentExecution execution = execution(state);
        FunctionCallPlan plan = plan(state);
        List<ToolResult> toolResults = toolExecutorService.execute(
                plan.getToolNames(), safeSlots(plan), execution.userId()
        );
        return Map.of(TOOL_RESULTS, toolResults == null ? List.of() : toolResults);
    }

    private Map<String, Object> inspectToolResults(OverAllState state) {
        CampusAgentExecution execution = execution(state);
        FunctionCallPlan plan = plan(state);
        List<ToolResult> toolResults = toolResults(state);
        AgentResponse followUpResponse = branchService.buildToolFollowUpResponse(
                execution.session(), execution.sessionId(), plan.getIntent(), plan.getToolNames(),
                toolResults, safeSlots(plan)
        );
        if (followUpResponse == null) {
            return Map.of(NEXT_NODE, RENDER_TOOL_RESPONSE);
        }
        return Map.of(
                RESPONSE, followUpResponse,
                NEXT_NODE, PERSIST_TOOL_FOLLOW_UP
        );
    }

    private Map<String, Object> persistToolFollowUp(OverAllState state) {
        CampusAgentExecution execution = execution(state);
        FunctionCallPlan plan = plan(state);
        AgentResponse response = response(state);
        sessionManager.save(execution.session());

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("followUpType", response.getFollowUpType());
        responseData.put("followUpOptions", response.getFollowUpOptions() == null
                ? Collections.emptyList() : response.getFollowUpOptions());
        turnRecorderService.recordTurnSafely(
                execution.session(), execution.userId(), execution.currentTurn(), execution.userInput(),
                plan.getIntent(), safeSlots(plan), plan.getToolNames(), responseData,
                response.getReply(), true, null, elapsedMs(execution.startTime())
        );
        log.info("Agent Graph完成(澄清), intent={}, 总耗时={}ms",
                plan.getIntent(), System.currentTimeMillis() - execution.startTime());
        return Map.of(RESPONSE, response);
    }

    private Map<String, Object> renderToolResponse(OverAllState state) {
        CampusAgentExecution execution = execution(state);
        AgentSession session = execution.session();
        FunctionCallPlan plan = plan(state);
        List<ToolResult> toolResults = toolResults(state);

        String reply = responseGeneratorService.generate(execution.userInput(), toolResults);
        List<AgentCard> cards = toolExecutorService.toCards(plan.getToolNames(), toolResults);
        boolean taskCompleted = !toolResults.isEmpty() && toolResults.stream().allMatch(ToolResult::isSuccess);

        session.addHistory("assistant", reply);
        session.setIntent(null);
        session.setSlots(Collections.emptyMap());
        session.setPendingSlots(Collections.emptyList());
        session.setTurnCount(0);
        sessionManager.save(session);

        List<String> followUpSuggestions = taskCompleted
                ? branchService.buildFollowUpSuggestions(plan.getIntent(), safeSlots(plan))
                : Collections.emptyList();
        turnRecorderService.recordTurnSafely(
                session, execution.userId(), execution.currentTurn(), execution.userInput(),
                plan.getIntent(), safeSlots(plan), plan.getToolNames(),
                Map.of("cards", cards, "followUpSuggestions", followUpSuggestions),
                reply, taskCompleted, firstToolError(toolResults), elapsedMs(execution.startTime())
        );

        AgentResponse response = AgentResponse.builder()
                .sessionId(execution.sessionId())
                .reply(reply)
                .intent(plan.getIntent())
                .extractedSlots(safeSlots(plan))
                .cards(cards)
                .usedTools(plan.getToolNames())
                .taskCompleted(taskCompleted)
                .followUpSuggestions(followUpSuggestions)
                .build();
        log.info("Agent Graph完成({}), intent={}, 总耗时={}ms",
                taskCompleted ? "成功" : "工具失败", plan.getIntent(),
                System.currentTimeMillis() - execution.startTime());
        return Map.of(RESPONSE, response);
    }

    private String nextNode(OverAllState state) {
        return state.value(NEXT_NODE).map(String::valueOf)
                .orElseThrow(() -> new IllegalStateException("Campus Agent graph has no next node"));
    }

    private RunnableConfig config(String sessionId) {
        return RunnableConfig.builder().threadId(sessionId).build();
    }

    private boolean isAwaitingConfirmation(OverAllState state) {
        return state.value(AWAITING_CONFIRMATION)
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(false);
    }

    private Map<String, Object> confirmationPayload(AgentResponse response, String status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("followUpType", response.getFollowUpType());
        payload.put("followUpOptions", response.getFollowUpOptions() == null
                ? Collections.emptyList() : response.getFollowUpOptions());
        payload.put("confirmationRequired", response.isConfirmationRequired());
        payload.put("confirmationId", response.getConfirmationId());
        payload.put("confirmationPreview", response.getConfirmationPreview());
        payload.put("confirmationStatus", status);
        return payload;
    }

    private CampusAgentExecution execution(OverAllState state) {
        return state.value(EXECUTION)
                .filter(CampusAgentExecution.class::isInstance)
                .map(CampusAgentExecution.class::cast)
                .orElseThrow(() -> new IllegalStateException("Campus Agent graph has no execution context"));
    }

    private FunctionCallPlan plan(OverAllState state) {
        return state.value(PLAN)
                .filter(FunctionCallPlan.class::isInstance)
                .map(FunctionCallPlan.class::cast)
                .orElseThrow(() -> new IllegalStateException("Campus Agent graph has no plan"));
    }

    @SuppressWarnings("unchecked")
    private List<ToolResult> toolResults(OverAllState state) {
        return state.value(TOOL_RESULTS)
                .filter(List.class::isInstance)
                .map(value -> (List<ToolResult>) value)
                .orElseGet(List::of);
    }

    private Map<String, Object> safeSlots(FunctionCallPlan plan) {
        return plan.getSlots() == null ? Collections.emptyMap() : plan.getSlots();
    }

    private SlotResult toSlotResult(FunctionCallPlan plan) {
        SlotResult slotResult = new SlotResult();
        slotResult.setSlots(safeSlots(plan));
        slotResult.setMissingSlots(plan.getMissingSlots() == null ? Collections.emptyList() : plan.getMissingSlots());
        slotResult.setAskQuestion(plan.getAskQuestion());
        return slotResult;
    }

    private String firstToolError(List<ToolResult> toolResults) {
        return toolResults.stream()
                .filter(result -> result != null && !result.isSuccess())
                .map(ToolResult::getErrorMessage)
                .filter(message -> message != null && !message.isBlank())
                .findFirst()
                .orElse(null);
    }

    private int elapsedMs(long startTime) {
        return (int) (System.currentTimeMillis() - startTime);
    }
}
