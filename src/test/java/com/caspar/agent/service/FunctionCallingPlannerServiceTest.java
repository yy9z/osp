package com.caspar.agent.service;

import com.caspar.agent.llm.LlmClient;
import com.caspar.agent.model.AgentSession;
import com.caspar.agent.model.FunctionCallPlan;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FunctionCallingPlannerServiceTest {

    @Test
    void plan_shouldParseToolCallAndArguments() {
        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.chat(anyList())).thenReturn("""
                {
                  "intent": "SECONDHAND_SEARCH",
                  "confidence": 0.98,
                  "tool_calls": [
                    {
                      "name": "secondhand_search",
                      "arguments": {
                        "keyword": "台灯",
                        "max_price": 50
                      }
                    }
                  ],
                  "missing_slots": [],
                  "ask_question": null
                }
                """);

        FunctionCallingPlannerService service = newService(llmClient);
        FunctionCallPlan plan = service.plan("找个50元以内的台灯", new AgentSession());

        assertEquals("SECONDHAND_SEARCH", plan.getIntent());
        assertEquals(List.of("secondhand_search"), plan.getToolNames());
        assertEquals("台灯", plan.getSlots().get("keyword"));
        assertEquals(50, plan.getSlots().get("max_price"));
        assertTrue(plan.getMissingSlots().isEmpty());
    }

    @Test
    void plan_shouldInferMissingRequiredSlotWhenModelOmitsIt() {
        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.chat(anyList())).thenReturn("""
                {
                  "intent": "SECONDHAND_PUBLISH",
                  "confidence": 0.95,
                  "tool_calls": [
                    {
                      "name": "secondhand_publish",
                      "arguments": {
                        "title": "自行车",
                        "category": "生活"
                      }
                    }
                  ],
                  "missing_slots": []
                }
                """);

        FunctionCallingPlannerService service = newService(llmClient);
        FunctionCallPlan plan = service.plan("我想出一辆自行车", new AgentSession());

        assertEquals("SECONDHAND_PUBLISH", plan.getIntent());
        assertEquals(List.of("price"), plan.getMissingSlots());
        assertEquals("请输入价格（元）：", plan.getAskQuestion());
    }

    @Test
    void plan_shouldIgnoreModelHallucinatedDormRepairMissingSlots() {
        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.chat(anyList())).thenReturn("""
                {
                  "intent": "DORM_REPAIR",
                  "confidence": 0.96,
                  "tool_calls": [
                    {
                      "name": "dorm_repair",
                      "arguments": {
                        "fault_type": "空调"
                      }
                    }
                  ],
                  "missing_slots": ["dorm_no", "description"],
                  "ask_question": "请提供您的宿舍号和故障描述，以便我们尽快处理。"
                }
                """);

        FunctionCallingPlannerService service = newService(llmClient);
        FunctionCallPlan plan = service.plan("宿舍空调坏了帮我报修", new AgentSession());

        assertEquals("DORM_REPAIR", plan.getIntent());
        assertEquals(List.of("dorm_repair"), plan.getToolNames());
        assertEquals("空调", plan.getSlots().get("fault_type"));
        assertTrue(plan.getMissingSlots().isEmpty());
        assertNull(plan.getAskQuestion());
    }

    @Test
    void plan_shouldStripIdentityArgumentsFromModelOutput() {
        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.chat(anyList())).thenReturn("""
                {
                  "intent": "MESSAGE_QUERY",
                  "confidence": 0.9,
                  "tool_calls": [
                    {
                      "name": "message_query",
                      "arguments": {
                        "keyword": "未读",
                        "userId": 99,
                        "token": "bad"
                      }
                    }
                  ],
                  "missing_slots": []
                }
                """);

        FunctionCallingPlannerService service = newService(llmClient);
        FunctionCallPlan plan = service.plan("查看我的未读消息", new AgentSession());

        assertEquals("MESSAGE_QUERY", plan.getIntent());
        assertEquals("未读", plan.getSlots().get("keyword"));
        assertFalse(plan.getSlots().containsKey("userId"));
        assertFalse(plan.getSlots().containsKey("token"));
    }

    @Test
    void plan_shouldParseCampusTipsToolCall() {
        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.chat(anyList())).thenReturn("""
                {
                  "intent": "CAMPUS_TIPS",
                  "confidence": 0.93,
                  "tool_calls": [
                    {
                      "name": "campus_tips",
                      "arguments": {}
                    }
                  ],
                  "missing_slots": []
                }
                """);

        FunctionCallingPlannerService service = newService(llmClient);
        FunctionCallPlan plan = service.plan("查看我的待办提醒", new AgentSession());

        assertEquals("CAMPUS_TIPS", plan.getIntent());
        assertEquals(List.of("campus_tips"), plan.getToolNames());
        assertTrue(plan.getMissingSlots().isEmpty());
    }

    @Test
    void plan_shouldSupportOpenAiNestedFunctionCallShape() {
        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.chat(anyList())).thenReturn("""
                {
                  "intent": "NAVIGATION",
                  "confidence": 0.92,
                  "tool_calls": [
                    {
                      "type": "function",
                      "function": {
                        "name": "navigation_v2",
                        "arguments": "{\\"destination\\":\\"图书馆\\",\\"travel_mode\\":\\"walking\\"}"
                      }
                    }
                  ],
                  "missing_slots": []
                }
                """);

        FunctionCallingPlannerService service = newService(llmClient);
        FunctionCallPlan plan = service.plan("去图书馆怎么走", new AgentSession());

        assertEquals("NAVIGATION", plan.getIntent());
        assertEquals(List.of("navigation_v2"), plan.getToolNames());
        assertEquals("图书馆", plan.getSlots().get("destination"));
        assertEquals("walking", plan.getSlots().get("travel_mode"));
    }

    private FunctionCallingPlannerService newService(LlmClient llmClient) {
        return new FunctionCallingPlannerService(
                llmClient,
                new ObjectMapper(),
                mock(IntentService.class),
                mock(SlotFillingService.class),
                mock(PlannerService.class)
        );
    }
}
