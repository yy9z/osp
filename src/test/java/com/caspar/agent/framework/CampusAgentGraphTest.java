package com.caspar.agent.framework;

import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.caspar.agent.model.AgentCard;
import com.caspar.agent.model.AgentResponse;
import com.caspar.agent.model.AgentSession;
import com.caspar.agent.model.FunctionCallPlan;
import com.caspar.agent.model.ToolResult;
import com.caspar.agent.service.AgentBranchService;
import com.caspar.agent.service.AgentTurnRecorderService;
import com.caspar.agent.service.AgentWriteConfirmationService;
import com.caspar.agent.service.FunctionCallingPlannerService;
import com.caspar.agent.service.IntentService;
import com.caspar.agent.service.ResponseGeneratorService;
import com.caspar.agent.service.ToolExecutorService;
import com.caspar.agent.session.AgentSessionManager;
import com.caspar.agent.registry.AgentToolCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampusAgentGraphTest {

    @Mock
    private IntentService intentService;
    @Mock
    private FunctionCallingPlannerService plannerService;
    @Mock
    private ToolExecutorService toolExecutorService;
    @Mock
    private ResponseGeneratorService responseGeneratorService;
    @Mock
    private AgentSessionManager sessionManager;
    @Mock
    private AgentBranchService branchService;
    @Mock
    private AgentTurnRecorderService turnRecorderService;

    private CampusAgentGraph graph;

    @BeforeEach
    void setUp() {
        graph = new CampusAgentGraph(
                intentService,
                plannerService,
                toolExecutorService,
                responseGeneratorService,
                sessionManager,
                branchService,
                turnRecorderService,
                new AgentWriteConfirmationService(new AgentToolCatalog()),
                new MemorySaver()
        );
    }

    @Test
    void generalChatShouldBypassPlannerAndTools() {
        AgentSession session = session("general-session");
        AgentResponse expected = AgentResponse.builder()
                .sessionId(session.getSessionId())
                .reply("你好")
                .intent("GENERAL_CHAT")
                .taskCompleted(false)
                .build();
        when(intentService.isPossiblyBusinessScenario("你好")).thenReturn(false);
        when(branchService.handleGeneralChat(
                any(AgentSession.class), anyLong(), anyString(), anyString(), anyInt(), anyLong()
        )).thenReturn(expected);

        AgentResponse response = graph.execute(execution(session, "你好"));

        assertEquals("GENERAL_CHAT", response.getIntent());
        assertEquals("你好", response.getReply());
        verifyNoInteractions(plannerService, toolExecutorService);
    }

    @Test
    void successfulToolPathShouldRenderCardsAndResetSession() {
        AgentSession session = session("tool-session");
        FunctionCallPlan plan = new FunctionCallPlan();
        plan.setIntent("SECONDHAND_SEARCH");
        plan.setToolNames(List.of("secondhand_search"));
        plan.setSlots(Map.of("keyword", "台灯", "max_price", 50));
        plan.setMissingSlots(List.of());

        ToolResult toolResult = ToolResult.ok("找到商品", Map.of("id", 1L));
        AgentCard card = AgentCard.of("PRODUCT", Map.of("id", 1L));
        when(intentService.isPossiblyBusinessScenario(anyString())).thenReturn(true);
        when(plannerService.plan(anyString(), any(AgentSession.class))).thenReturn(plan);
        when(toolExecutorService.execute(plan.getToolNames(), plan.getSlots(), 7L))
                .thenReturn(List.of(toolResult));
        when(responseGeneratorService.generate(anyString(), any())).thenReturn("为您找到 1 件商品。");
        when(toolExecutorService.toCards(plan.getToolNames(), List.of(toolResult))).thenReturn(List.of(card));
        when(branchService.buildFollowUpSuggestions(plan.getIntent(), plan.getSlots()))
                .thenReturn(List.of("继续搜索"));

        AgentResponse response = graph.execute(execution(session, "找50元以内的台灯"));

        assertTrue(response.isTaskCompleted());
        assertEquals(List.of("secondhand_search"), response.getUsedTools());
        assertEquals(1, response.getCards().size());
        assertEquals(List.of("继续搜索"), response.getFollowUpSuggestions());
        ArgumentCaptor<AgentSession> savedSession = ArgumentCaptor.forClass(AgentSession.class);
        verify(sessionManager).save(savedSession.capture());
        assertNull(savedSession.getValue().getIntent());
        assertTrue(savedSession.getValue().getSlots().isEmpty());
        assertTrue(savedSession.getValue().getPendingSlots().isEmpty());
        assertEquals(0, savedSession.getValue().getTurnCount());
        assertEquals("为您找到 1 件商品。",
                savedSession.getValue().getHistory().getLast().get("content"));
        verify(turnRecorderService).recordTurnSafely(
                any(AgentSession.class), anyLong(), anyInt(), anyString(), anyString(),
                anyMap(), any(), any(), anyString(), anyBoolean(), any(), anyInt()
        );
    }

    @Test
    void failedToolPathShouldNotOfferSuggestions() {
        AgentSession session = session("failed-tool-session");
        FunctionCallPlan plan = new FunctionCallPlan();
        plan.setIntent("NAVIGATION");
        plan.setToolNames(List.of("navigation_v2"));
        plan.setSlots(Map.of("destination", "图书馆"));
        plan.setMissingSlots(List.of());
        ToolResult failed = ToolResult.fail("地图服务不可用");

        when(intentService.isPossiblyBusinessScenario(anyString())).thenReturn(true);
        when(plannerService.plan(anyString(), any(AgentSession.class))).thenReturn(plan);
        when(toolExecutorService.execute(any(), anyMap(), anyLong())).thenReturn(List.of(failed));
        when(responseGeneratorService.generate(anyString(), any())).thenReturn("地图服务不可用");
        when(toolExecutorService.toCards(any(), any())).thenReturn(List.of());

        AgentResponse response = graph.execute(execution(session, "去图书馆"));

        assertFalse(response.isTaskCompleted());
        assertTrue(response.getFollowUpSuggestions().isEmpty());
        verify(branchService, never()).buildFollowUpSuggestions(anyString(), anyMap());
    }

    @Test
    void writeToolShouldPauseUntilExplicitApproval() {
        AgentSession session = session("write-approval-session");
        FunctionCallPlan plan = writePlan();
        ToolResult toolResult = ToolResult.ok("发布成功", Map.of("id", 114L));

        when(intentService.isPossiblyBusinessScenario(anyString())).thenReturn(true);
        when(plannerService.plan(anyString(), any(AgentSession.class))).thenReturn(plan);
        when(toolExecutorService.execute(plan.getToolNames(), plan.getSlots(), 7L))
                .thenReturn(List.of(toolResult));
        when(responseGeneratorService.generate(anyString(), any())).thenReturn("商品已提交审核。");
        when(toolExecutorService.toCards(any(), any())).thenReturn(List.of());
        when(branchService.buildFollowUpSuggestions(anyString(), anyMap())).thenReturn(List.of());

        AgentResponse pending = graph.execute(execution(session, "发布一个50元的台灯"));

        assertTrue(pending.isConfirmationRequired());
        assertEquals("confirmation", pending.getAskFor());
        assertEquals("write_confirmation", pending.getFollowUpType());
        assertFalse(pending.getConfirmationId().isBlank());
        verify(toolExecutorService, never()).execute(any(), anyMap(), anyLong());

        AgentResponse completed = graph.execute(
                execution(session, "确认执行"), pending.getConfirmationId(), "APPROVE"
        );

        assertTrue(completed.isTaskCompleted());
        assertFalse(completed.isConfirmationRequired());
        verify(toolExecutorService).execute(plan.getToolNames(), plan.getSlots(), 7L);
    }

    @Test
    void rejectedWriteShouldFinishWithoutExecutingTool() {
        AgentSession session = session("write-rejection-session");
        FunctionCallPlan plan = writePlan();
        when(intentService.isPossiblyBusinessScenario(anyString())).thenReturn(true);
        when(plannerService.plan(anyString(), any(AgentSession.class))).thenReturn(plan);

        AgentResponse pending = graph.execute(execution(session, "发布一个50元的台灯"));
        AgentResponse rejected = graph.execute(
                execution(session, "取消"), pending.getConfirmationId(), "REJECT"
        );

        assertFalse(rejected.isTaskCompleted());
        assertFalse(rejected.isConfirmationRequired());
        assertTrue(rejected.getReply().contains("没有写入"));
        verify(toolExecutorService, never()).execute(any(), anyMap(), anyLong());
    }

    @Test
    void staleConfirmationIdShouldKeepWorkflowPaused() {
        AgentSession session = session("write-stale-session");
        FunctionCallPlan plan = writePlan();
        when(intentService.isPossiblyBusinessScenario(anyString())).thenReturn(true);
        when(plannerService.plan(anyString(), any(AgentSession.class))).thenReturn(plan);

        AgentResponse pending = graph.execute(execution(session, "发布一个50元的台灯"));
        AgentResponse reminder = graph.execute(
                execution(session, "确认执行"), "stale-confirmation-id", "APPROVE"
        );

        assertTrue(reminder.isConfirmationRequired());
        assertEquals(pending.getConfirmationId(), reminder.getConfirmationId());
        assertTrue(reminder.getReply().contains("不一致"));
        verify(toolExecutorService, never()).execute(any(), anyMap(), anyLong());
    }

    private AgentSession session(String sessionId) {
        AgentSession session = new AgentSession();
        session.setSessionId(sessionId);
        session.setUserId(7L);
        return session;
    }

    private CampusAgentExecution execution(AgentSession session, String input) {
        return new CampusAgentExecution(
                session, 7L, session.getSessionId(), input, 1, System.currentTimeMillis()
        );
    }

    private FunctionCallPlan writePlan() {
        FunctionCallPlan plan = new FunctionCallPlan();
        plan.setIntent("SECONDHAND_PUBLISH");
        plan.setToolNames(List.of("secondhand_publish"));
        plan.setSlots(Map.of("title", "台灯", "category", "生活", "price", 50));
        plan.setMissingSlots(List.of());
        return plan;
    }
}
