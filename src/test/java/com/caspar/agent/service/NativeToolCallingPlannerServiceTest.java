package com.caspar.agent.service;

import com.caspar.agent.llm.LlmClient;
import com.caspar.agent.model.AgentSession;
import com.caspar.agent.model.FunctionCallPlan;
import com.caspar.agent.model.IntentResult;
import com.caspar.agent.registry.AgentToolCallbackProvider;
import com.caspar.agent.registry.AgentToolCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NativeToolCallingPlannerServiceTest {

    @Test
    void shouldParseNativeReadOnlyToolCall() {
        LlmClient llmClient = mock(LlmClient.class);
        IntentService intentService = mock(IntentService.class);
        AgentToolCallbackProvider callbackProvider = mock(AgentToolCallbackProvider.class);
        ToolCallback callback = mock(ToolCallback.class);

        when(intentService.identifyByRules("找个50元以内的台灯"))
                .thenReturn(intent("SECONDHAND_SEARCH"));
        when(callbackProvider.getReadOnlyToolCallbacks()).thenReturn(List.of(callback));
        when(llmClient.chatForToolCalls(anyList(), anyList()))
                .thenReturn(toolCallResponse(
                        "secondhand_search",
                        "{\"keyword\":\"台灯\",\"max_price\":50,\"userId\":999}"
                ));

        NativeToolCallingPlannerService service = new NativeToolCallingPlannerService(
                llmClient,
                new ObjectMapper(),
                intentService,
                new AgentToolCatalog(),
                callbackProvider
        );
        ReflectionTestUtils.setField(service, "enabled", true);

        Optional<FunctionCallPlan> result = service.tryPlan(
                "找个50元以内的台灯",
                new AgentSession()
        );

        assertTrue(result.isPresent());
        FunctionCallPlan plan = result.orElseThrow();
        assertEquals("SECONDHAND_SEARCH", plan.getIntent());
        assertEquals(List.of("secondhand_search"), plan.getToolNames());
        assertEquals("台灯", plan.getSlots().get("keyword"));
        assertEquals(50, plan.getSlots().get("max_price"));
        assertTrue(plan.getMissingSlots().isEmpty());
        assertTrue(!plan.getSlots().containsKey("userId"));
    }

    @Test
    void shouldSkipNativePilotForWriteIntent() {
        LlmClient llmClient = mock(LlmClient.class);
        IntentService intentService = mock(IntentService.class);
        AgentToolCallbackProvider callbackProvider = mock(AgentToolCallbackProvider.class);
        when(intentService.identifyByRules("发布一个二手台灯"))
                .thenReturn(intent("SECONDHAND_PUBLISH"));

        NativeToolCallingPlannerService service = new NativeToolCallingPlannerService(
                llmClient,
                new ObjectMapper(),
                intentService,
                new AgentToolCatalog(),
                callbackProvider
        );
        ReflectionTestUtils.setField(service, "enabled", true);

        assertTrue(service.tryPlan("发布一个二手台灯", new AgentSession()).isEmpty());
        verify(llmClient, never()).chatForToolCalls(anyList(), anyList());
    }

    private IntentResult intent(String intent) {
        IntentResult result = new IntentResult();
        result.setIntent(intent);
        result.setConfidence(0.8);
        return result;
    }

    private ChatResponse toolCallResponse(String toolName, String arguments) {
        AssistantMessage message = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "call-1",
                        "function",
                        toolName,
                        arguments
                )))
                .build();
        return new ChatResponse(List.of(new Generation(message)));
    }
}
