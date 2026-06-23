package com.caspar.agent.rag;

import java.util.List;

/**
 * 本地知识库分块。
 */
public record KnowledgeChunk(
        String chunkId,
        String title,
        String content,
        String source,
        List<String> tags
) {
}

