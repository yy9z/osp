package com.caspar.agent.service;

import com.caspar.agent.llm.LlmClient;
import com.caspar.agent.model.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ResponseGeneratorServiceTest {

    @Test
    void allFailedToolsShouldReturnDeterministicReplyWithoutCallingModel() {
        LlmClient llmClient = mock(LlmClient.class);
        WebSearchService webSearchService = mock(WebSearchService.class);
        ResponseGeneratorService service = new ResponseGeneratorService(llmClient, webSearchService);

        String reply = service.generate(
                "去图书馆怎么走",
                List.of(ToolResult.fail("地图服务未配置"))
        );

        assertEquals("抱歉，本次操作未完成：地图服务未配置。请稍后重试。", reply);
        verifyNoInteractions(llmClient);
    }

    @Test
    void successfulToolShouldUseGroundedSummaryWithoutCallingModel() {
        LlmClient llmClient = mock(LlmClient.class);
        WebSearchService webSearchService = mock(WebSearchService.class);
        ResponseGeneratorService service = new ResponseGeneratorService(llmClient, webSearchService);

        String reply = service.generate(
                "找个50元以内的台灯",
                List.of(ToolResult.ok("为您找到 3 件相关二手商品。", Map.of("total", 3)))
        );

        assertEquals("为您找到 3 件相关二手商品。", reply);
        verifyNoInteractions(llmClient);
    }

    @Test
    void knowledgeResultShouldRenderRetrievedContextWithoutCallingModel() {
        LlmClient llmClient = mock(LlmClient.class);
        WebSearchService webSearchService = mock(WebSearchService.class);
        ResponseGeneratorService service = new ResponseGeneratorService(llmClient, webSearchService);

        String reply = service.generate(
                "宿舍报修流程是什么",
                List.of(ToolResult.ok("已命中知识库", Map.of(
                        "type", "CAMPUS_KNOWLEDGE",
                        "hit", true,
                        "answerContext", "[1] 宿舍报修\n进入宿舍服务后提交故障信息。"
                )))
        );

        assertEquals("根据平台知识库：\n[1] 宿舍报修\n进入宿舍服务后提交故障信息。", reply);
        verifyNoInteractions(llmClient);
    }
}
