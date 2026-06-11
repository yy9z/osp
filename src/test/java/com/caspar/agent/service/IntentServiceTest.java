package com.caspar.agent.service;

import com.caspar.agent.exception.LlmCallException;
import com.caspar.agent.llm.LlmClient;
import com.caspar.agent.model.IntentResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntentServiceTest {

    @Test
    void isPossiblyBusinessScenario_shouldTreatBudgetGoodsSearchAsSecondhandBusiness() {
        IntentService service = new IntentService(mock(LlmClient.class), new ObjectMapper());

        assertTrue(service.isPossiblyBusinessScenario("找个50元以内的台灯"));
    }

    @Test
    void isPossiblyBusinessScenario_shouldNotTreatGeneralBookRecommendationAsSecondhandBusiness() {
        IntentService service = new IntentService(mock(LlmClient.class), new ObjectMapper());

        assertFalse(service.isPossiblyBusinessScenario("推荐一本书"));
    }

    @Test
    void isPossiblyBusinessScenario_shouldTreatMessageNotificationQueryAsBusiness() {
        IntentService service = new IntentService(mock(LlmClient.class), new ObjectMapper());

        assertTrue(service.isPossiblyBusinessScenario("查看我的消息通知"));
    }

    @Test
    void isPossiblyBusinessScenario_shouldTreatCampusTipsQueryAsBusiness() {
        IntentService service = new IntentService(mock(LlmClient.class), new ObjectMapper());

        assertTrue(service.isPossiblyBusinessScenario("查看我的待办提醒"));
    }

    @Test
    void identify_shouldFallbackToSecondhandSearchWhenLlmFailsForBudgetGoodsSearch() {
        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.chat(anyList())).thenThrow(new LlmCallException("mock llm unavailable"));
        IntentService service = new IntentService(llmClient, new ObjectMapper());

        IntentResult result = service.identify("找个50元以内的台灯", List.<Map<String, String>>of());

        assertEquals("SECONDHAND_SEARCH", result.getIntent());
    }

    @Test
    void identify_shouldUseLlmSecondhandSearchForBudgetGoodsSearch() {
        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.chat(anyList())).thenReturn("{\"intent\":\"SECONDHAND_SEARCH\",\"confidence\":0.98}");
        IntentService service = new IntentService(llmClient, new ObjectMapper());

        IntentResult result = service.identify("找个50元以内的台灯", List.<Map<String, String>>of());

        assertEquals("SECONDHAND_SEARCH", result.getIntent());
    }

    @Test
    void identify_shouldFallbackToMessageQueryWhenLlmFailsForMessageNotificationQuery() {
        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.chat(anyList())).thenThrow(new LlmCallException("mock llm unavailable"));
        IntentService service = new IntentService(llmClient, new ObjectMapper());

        IntentResult result = service.identify("查看我的消息通知", List.<Map<String, String>>of());

        assertEquals("MESSAGE_QUERY", result.getIntent());
    }

    @Test
    void identify_shouldFallbackToCampusTipsWhenLlmFailsForCampusTipsQuery() {
        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.chat(anyList())).thenThrow(new LlmCallException("mock llm unavailable"));
        IntentService service = new IntentService(llmClient, new ObjectMapper());

        IntentResult result = service.identify("查看我的待办提醒", List.<Map<String, String>>of());

        assertEquals("CAMPUS_TIPS", result.getIntent());
    }
}
