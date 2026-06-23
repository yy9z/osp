package com.caspar.agent.service;

import com.caspar.agent.exception.LlmCallException;
import com.caspar.agent.llm.LlmClient;
import com.caspar.agent.model.IntentResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
    void isPossiblyBusinessScenario_shouldTreatExplicitNavigationRequestAsBusiness() {
        IntentService service = new IntentService(mock(LlmClient.class), new ObjectMapper());

        assertTrue(service.isPossiblyBusinessScenario("导航去东区图书馆"));
    }

    @Test
    void isPossiblyBusinessScenario_shouldTreatPlatformProcedureQuestionAsKnowledgeBusiness() {
        IntentService service = new IntentService(mock(LlmClient.class), new ObjectMapper());

        assertTrue(service.isPossiblyBusinessScenario("宿舍报修的办理流程是什么"));
        assertEquals("CAMPUS_KNOWLEDGE",
                service.identifyByRules("宿舍报修的办理流程是什么").getIntent());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "我宿舍灯坏了",
            "空调不工作",
            "看看我的室友",
            "我最近的报修状态",
            "我最近提交的报修处理了吗",
            "拾到一张校园卡",
            "我的钥匙丢失了",
            "去图书馆",
            "教学楼在哪里",
            "有新消息吗",
            "今天需要看什么",
            "转让一个蓝牙音箱",
            "卖一本Java教材",
            "找一本算法教材",
            "帮我找 ' OR '1'='1 的商品"
    })
    void isPossiblyBusinessScenario_shouldNotSkipKnownBusinessExpressions(String input) {
        IntentService service = new IntentService(mock(LlmClient.class), new ObjectMapper());

        assertTrue(service.isPossiblyBusinessScenario(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "用Java写一个排序算法",
            "这道数学题怎么做",
            "帮我写一篇宿舍报修论文"
    })
    void isPossiblyBusinessScenario_shouldKeepClearGeneralAiTasksInGeneralChat(String input) {
        IntentService service = new IntentService(mock(LlmClient.class), new ObjectMapper());

        assertFalse(service.isPossiblyBusinessScenario(input));
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

    @Test
    void identify_shouldFallbackToCampusKnowledgeWhenLlmFailsForProcedureQuestion() {
        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.chat(anyList())).thenThrow(new LlmCallException("mock llm unavailable"));
        IntentService service = new IntentService(llmClient, new ObjectMapper());

        IntentResult result = service.identify("失物招领的认领流程是什么", List.<Map<String, String>>of());

        assertEquals("CAMPUS_KNOWLEDGE", result.getIntent());
    }

    @Test
    void identify_shouldFallbackToRepairQueryForNaturalStatusQuestion() {
        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.chat(anyList())).thenThrow(new LlmCallException("mock llm unavailable"));
        IntentService service = new IntentService(llmClient, new ObjectMapper());

        IntentResult result = service.identify("我最近提交的报修处理了吗", List.<Map<String, String>>of());

        assertEquals("REPAIR_QUERY", result.getIntent());
    }

    @Test
    void identify_shouldPreferLostFoundOverLocationWordsWhenLlmFails() {
        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.chat(anyList())).thenThrow(new LlmCallException("mock llm unavailable"));
        IntentService service = new IntentService(llmClient, new ObjectMapper());

        IntentResult lost = service.identify("我丢了一个黑色保温杯，昨天在图书馆", List.<Map<String, String>>of());
        IntentResult found = service.identify("我在教学楼捡到一张校园卡", List.<Map<String, String>>of());

        assertEquals("LOSTFOUND_LOST", lost.getIntent());
        assertEquals("LOSTFOUND_FOUND", found.getIntent());
    }
}
