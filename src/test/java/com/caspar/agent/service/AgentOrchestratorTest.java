package com.caspar.agent.service;

import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.caspar.agent.framework.CampusAgentGraph;
import com.caspar.agent.model.AgentRequest;
import com.caspar.agent.model.AgentResponse;
import com.caspar.agent.model.AgentSession;
import com.caspar.agent.model.FunctionCallPlan;
import com.caspar.agent.model.ToolResult;
import com.caspar.agent.session.AgentSessionManager;
import com.caspar.agent.registry.AgentToolCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentOrchestratorTest {

    @Test
    void missingRequiredSlotsShouldNeverExecuteToolAfterManyTurns() {
        IntentService intentService = mock(IntentService.class);
        FunctionCallingPlannerService planner = mock(FunctionCallingPlannerService.class);
        ToolExecutorService toolExecutor = mock(ToolExecutorService.class);
        ResponseGeneratorService responseGenerator = mock(ResponseGeneratorService.class);
        AgentSessionManager sessionManager = mock(AgentSessionManager.class);
        AgentHistoryService historyService = mock(AgentHistoryService.class);
        AgentBranchService branchService = mock(AgentBranchService.class);
        AgentTurnRecorderService recorder = mock(AgentTurnRecorderService.class);

        AgentSession session = new AgentSession();
        session.setSessionId("session-many-turns");
        session.setUserId(7L);
        session.setTurnCount(8);
        when(sessionManager.getOrCreate("session-many-turns", 7L)).thenReturn(session);
        when(intentService.isPossiblyBusinessScenario(anyString())).thenReturn(true);

        FunctionCallPlan plan = new FunctionCallPlan();
        plan.setIntent("SECONDHAND_PUBLISH");
        plan.setToolNames(List.of("secondhand_publish"));
        plan.setSlots(Map.of("title", "台灯", "category", "生活"));
        plan.setMissingSlots(List.of("price"));
        plan.setAskQuestion("请输入价格（元）：");
        when(planner.plan(anyString(), any(AgentSession.class))).thenReturn(plan);

        AgentResponse expected = AgentResponse.builder()
                .sessionId("session-many-turns")
                .intent("SECONDHAND_PUBLISH")
                .askFor("price")
                .reply("请输入价格（元）：")
                .taskCompleted(false)
                .build();
        when(branchService.handleSlotAsk(
                any(AgentSession.class), anyLong(), anyString(), anyString(), anyString(),
                any(), anyInt(), anyLong()
        )).thenReturn(expected);

        CampusAgentGraph graph = new CampusAgentGraph(
                intentService,
                planner,
                toolExecutor,
                responseGenerator,
                sessionManager,
                branchService,
                recorder,
                new AgentWriteConfirmationService(new AgentToolCatalog()),
                new MemorySaver()
        );
        AgentOrchestrator orchestrator = new AgentOrchestrator(
                graph, sessionManager, historyService, branchService, recorder
        );
        AgentRequest request = new AgentRequest();
        request.setSessionId("session-many-turns");
        request.setMessage("生活");

        AgentResponse actual = orchestrator.process(request, 7L);

        assertEquals("price", actual.getAskFor());
        assertFalse(actual.isTaskCompleted());
        verify(toolExecutor, never()).execute(any(), anyMap(), anyLong());
    }

    @Test
    void failedToolShouldNotMarkTaskCompletedOrBuildSuggestions() {
        IntentService intentService = mock(IntentService.class);
        FunctionCallingPlannerService planner = mock(FunctionCallingPlannerService.class);
        ToolExecutorService toolExecutor = mock(ToolExecutorService.class);
        ResponseGeneratorService responseGenerator = mock(ResponseGeneratorService.class);
        AgentSessionManager sessionManager = mock(AgentSessionManager.class);
        AgentHistoryService historyService = mock(AgentHistoryService.class);
        AgentBranchService branchService = mock(AgentBranchService.class);
        AgentTurnRecorderService recorder = mock(AgentTurnRecorderService.class);

        AgentSession session = new AgentSession();
        session.setSessionId("session-tool-failure");
        session.setUserId(7L);
        when(sessionManager.getOrCreate(null, 7L)).thenReturn(session);
        when(intentService.isPossiblyBusinessScenario(anyString())).thenReturn(true);

        FunctionCallPlan plan = new FunctionCallPlan();
        plan.setIntent("NAVIGATION");
        plan.setToolNames(List.of("navigation_v2"));
        plan.setSlots(Map.of("destination", "图书馆"));
        plan.setMissingSlots(List.of());
        when(planner.plan(anyString(), any(AgentSession.class))).thenReturn(plan);
        when(toolExecutor.execute(any(), anyMap(), anyLong()))
                .thenReturn(List.of(ToolResult.fail("地图服务未配置")));
        when(responseGenerator.generate(anyString(), any())).thenReturn("地图服务暂不可用");
        when(toolExecutor.toCards(any(), any())).thenReturn(List.of());

        CampusAgentGraph graph = new CampusAgentGraph(
                intentService,
                planner,
                toolExecutor,
                responseGenerator,
                sessionManager,
                branchService,
                recorder,
                new AgentWriteConfirmationService(new AgentToolCatalog()),
                new MemorySaver()
        );
        AgentOrchestrator orchestrator = new AgentOrchestrator(
                graph, sessionManager, historyService, branchService, recorder
        );
        AgentRequest request = new AgentRequest();
        request.setMessage("去图书馆怎么走");

        AgentResponse response = orchestrator.process(request, 7L);

        assertFalse(response.isTaskCompleted());
        assertTrue(response.getFollowUpSuggestions().isEmpty());
        verify(branchService, never()).buildFollowUpSuggestions(anyString(), anyMap());
    }
}
