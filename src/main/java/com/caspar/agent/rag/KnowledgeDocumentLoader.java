package com.caspar.agent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 加载本地 Markdown 知识库并按标题/段落分块。
 */
@Slf4j
@Component
public class KnowledgeDocumentLoader {

    static final String DEFAULT_SOURCE = "rag/campus_knowledge.md";
    private static final int MAX_CHUNK_LENGTH = 900;

    public List<KnowledgeChunk> loadChunks() {
        return loadChunks(DEFAULT_SOURCE);
    }

    List<KnowledgeChunk> loadChunks(String sourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(sourcePath);
            String markdown = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            List<KnowledgeChunk> chunks = parseMarkdown(markdown, sourcePath);
            log.info("RAG知识库加载完成: source={}, chunks={}", sourcePath, chunks.size());
            return chunks;
        } catch (Exception e) {
            log.error("RAG知识库加载失败: {}", sourcePath, e);
            return List.of();
        }
    }

    List<KnowledgeChunk> parseMarkdown(String markdown, String sourcePath) {
        if (!StringUtils.hasText(markdown)) {
            return List.of();
        }
        List<KnowledgeChunk> chunks = new ArrayList<>();
        String currentTitle = "平台知识库";
        List<String> paragraphBuffer = new ArrayList<>();
        int[] sequence = {1};

        for (String rawLine : markdown.split("\\R")) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.startsWith("# ")) {
                continue;
            }
            if (line.startsWith("## ")) {
                flushSection(chunks, currentTitle, paragraphBuffer, sourcePath, sequence);
                currentTitle = line.substring(3).trim();
                paragraphBuffer.clear();
                continue;
            }
            if (StringUtils.hasText(line)) {
                paragraphBuffer.add(line);
            }
        }
        flushSection(chunks, currentTitle, paragraphBuffer, sourcePath, sequence);
        return chunks;
    }

    private void flushSection(List<KnowledgeChunk> chunks, String title, List<String> paragraphs,
                              String sourcePath, int[] sequence) {
        if (paragraphs == null || paragraphs.isEmpty()) {
            return;
        }
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (current.length() > 0 && current.length() + paragraph.length() + 2 > MAX_CHUNK_LENGTH) {
                addChunk(chunks, title, current.toString(), sourcePath, sequence[0]++);
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append("\n");
            }
            current.append(paragraph);
        }
        if (current.length() > 0) {
            addChunk(chunks, title, current.toString(), sourcePath, sequence[0]++);
        }
    }

    private void addChunk(List<KnowledgeChunk> chunks, String title, String content, String sourcePath, int sequence) {
        String normalizedTitle = StringUtils.hasText(title) ? title : "平台知识库";
        chunks.add(new KnowledgeChunk(
                "campus-knowledge-" + sequence,
                normalizedTitle,
                content,
                sourcePath,
                inferTags(normalizedTitle + "\n" + content)
        ));
    }

    private List<String> inferTags(String text) {
        Set<String> tags = new LinkedHashSet<>();
        addTagIfContains(tags, text, "宿舍", "报修", "工单", "维修");
        addTagIfContains(tags, text, "二手", "商品", "交易", "面交");
        addTagIfContains(tags, text, "失物", "寻物", "招领", "认领", "校园卡");
        addTagIfContains(tags, text, "消息", "通知", "会话", "未读");
        addTagIfContains(tags, text, "导航", "路线", "地点", "校区");
        addTagIfContains(tags, text, "提醒", "待办", "首页");
        addTagIfContains(tags, text, "账号", "权限", "安全", "身份");
        addTagIfContains(tags, text, "智能助手", "Agent", "工具", "知识库");
        return List.copyOf(tags);
    }

    private void addTagIfContains(Set<String> tags, String text, String... candidates) {
        for (String candidate : candidates) {
            if (text != null && text.contains(candidate)) {
                tags.add(candidate);
            }
        }
    }
}

