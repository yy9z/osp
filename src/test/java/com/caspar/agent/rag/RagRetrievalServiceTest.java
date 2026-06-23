package com.caspar.agent.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagRetrievalServiceTest {

    @Test
    void loader_shouldLoadMarkdownAndCreateChunks() {
        KnowledgeDocumentLoader loader = new KnowledgeDocumentLoader();

        List<KnowledgeChunk> chunks = loader.loadChunks();

        assertFalse(chunks.isEmpty());
        KnowledgeChunk first = chunks.get(0);
        assertNotNull(first.chunkId());
        assertNotNull(first.title());
        assertNotNull(first.content());
        assertNotNull(first.source());
        assertFalse(first.tags().isEmpty());
    }

    @Test
    void retrieval_shouldHitDormRepairFlow() {
        RagRetrievalService service = newService();

        RagAnswerContext context = service.retrieve("怎么提交宿舍报修", null, 3);

        assertTrue(context.hit());
        assertTrue(context.answerContext().contains("宿舍报修"));
        assertTrue(context.answerContext().contains("PENDING"));
    }

    @Test
    void retrieval_shouldHitSecondhandMeetupSafety() {
        RagRetrievalService service = newService();

        RagAnswerContext context = service.retrieve("二手商品怎么面交比较安全", null, 3);

        assertTrue(context.hit());
        assertTrue(context.answerContext().contains("面交"));
        assertTrue(context.answerContext().contains("不要提前转账"));
    }

    @Test
    void retrieval_shouldHitLostFoundCampusCard() {
        RagRetrievalService service = newService();

        RagAnswerContext context = service.retrieve("丢了校园卡怎么办", null, 3);

        assertTrue(context.hit());
        assertTrue(context.answerContext().contains("校园卡"));
        assertTrue(context.answerContext().contains("失物"));
    }

    @Test
    void retrieval_shouldReturnNoHitForUnrelatedQuestion() {
        RagRetrievalService service = newService();

        RagAnswerContext context = service.retrieve("量子力学薛定谔方程怎么推导", null, 3);

        assertFalse(context.hit());
        assertTrue(context.chunks().isEmpty());
    }

    private RagRetrievalService newService() {
        RagRetrievalService service = new RagRetrievalService(new KnowledgeDocumentLoader());
        service.init();
        return service;
    }
}

