package com.caspar.agent.rag;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 轻量 RAG 检索服务：关键词 + 字符重叠，不依赖向量数据库。
 */
@Service
@RequiredArgsConstructor
public class RagRetrievalService {

    private static final int DEFAULT_TOP_K = 3;
    private static final int MAX_TOP_K = 5;
    private static final double MIN_HIT_SCORE = 1.2;

    private static final List<String> DOMAIN_TERMS = List.of(
            "宿舍", "报修", "工单", "维修", "状态", "进度",
            "二手", "商品", "发布", "搜索", "面交", "交易", "安全", "审核",
            "失物", "寻物", "招领", "认领", "匹配", "校园卡", "证件",
            "消息", "通知", "未读", "会话", "公告",
            "导航", "路线", "地点", "校区", "食堂", "图书馆",
            "智能助手", "助手", "Agent", "工具", "待办", "提醒",
            "账号", "权限", "登录", "身份", "平台", "规则", "流程"
    );

    private final KnowledgeDocumentLoader documentLoader;
    private List<KnowledgeChunk> chunks = List.of();

    @PostConstruct
    public void init() {
        chunks = documentLoader.loadChunks();
    }

    public RagAnswerContext retrieve(String question, String module, Integer topK) {
        if (!StringUtils.hasText(question) || chunks.isEmpty()) {
            return emptyContext(question);
        }

        int safeTopK = normalizeTopK(topK);
        List<String> queryTerms = extractTerms(question + " " + (module == null ? "" : module));
        List<ScoredChunk> scoredChunks = chunks.stream()
                .map(chunk -> new ScoredChunk(chunk, scoreChunk(chunk, question, module, queryTerms)))
                .filter(scored -> scored.score() > 0)
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(safeTopK)
                .toList();

        double topScore = scoredChunks.isEmpty() ? 0.0 : scoredChunks.get(0).score();
        boolean hit = topScore >= MIN_HIT_SCORE;
        List<KnowledgeChunk> selected = hit
                ? scoredChunks.stream().map(ScoredChunk::chunk).toList()
                : List.of();

        return new RagAnswerContext(
                question,
                hit,
                round(topScore),
                buildAnswerContext(selected),
                selected,
                buildCitations(selected)
        );
    }

    public List<KnowledgeChunk> allChunks() {
        return chunks;
    }

    private RagAnswerContext emptyContext(String question) {
        return new RagAnswerContext(question, false, 0.0, "", List.of(), List.of());
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, MAX_TOP_K);
    }

    private double scoreChunk(KnowledgeChunk chunk, String question, String module, List<String> queryTerms) {
        String title = normalize(chunk.title());
        String content = normalize(chunk.content());
        String tags = normalize(String.join(" ", chunk.tags()));

        double score = 0.0;
        for (String term : queryTerms) {
            String normalizedTerm = normalize(term);
            if (!StringUtils.hasText(normalizedTerm)) {
                continue;
            }
            if (title.contains(normalizedTerm)) {
                score += 4.0;
            }
            if (tags.contains(normalizedTerm)) {
                score += 2.5;
            }
            if (content.contains(normalizedTerm)) {
                score += normalizedTerm.length() >= 2 ? 1.6 : 0.4;
            }
        }

        if (StringUtils.hasText(module)) {
            String normalizedModule = normalize(module);
            if (title.contains(normalizedModule) || tags.contains(normalizedModule) || content.contains(normalizedModule)) {
                score += 2.0;
            }
        }

        if (!queryTerms.isEmpty()) {
            score += cjkOverlapScore(question, chunk.title() + chunk.content());
        }
        return score;
    }

    private List<String> extractTerms(String text) {
        Set<String> terms = new LinkedHashSet<>();
        String normalized = normalize(text);
        for (String term : DOMAIN_TERMS) {
            if (normalized.contains(normalize(term))) {
                terms.add(term);
            }
        }
        for (String token : normalized.split("[^a-z0-9]+")) {
            if (token.length() >= 2 && token.length() <= 20) {
                terms.add(token);
            }
        }
        return new ArrayList<>(terms);
    }

    private double cjkOverlapScore(String query, String target) {
        Set<Integer> queryChars = cjkChars(query);
        if (queryChars.isEmpty()) {
            return 0.0;
        }
        Set<Integer> targetChars = cjkChars(target);
        long common = queryChars.stream().filter(targetChars::contains).count();
        return (double) common / Math.max(1, queryChars.size()) * 1.0;
    }

    private Set<Integer> cjkChars(String text) {
        Set<Integer> chars = new LinkedHashSet<>();
        if (text == null) {
            return chars;
        }
        text.codePoints()
                .filter(codePoint -> codePoint >= 0x4E00 && codePoint <= 0x9FFF)
                .forEach(chars::add);
        return chars;
    }

    private String buildAnswerContext(List<KnowledgeChunk> selected) {
        if (selected == null || selected.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < selected.size(); i++) {
            KnowledgeChunk chunk = selected.get(i);
            if (i > 0) {
                builder.append("\n\n");
            }
            builder.append("[").append(i + 1).append("] ")
                    .append(chunk.title()).append("\n")
                    .append(chunk.content());
        }
        return builder.toString();
    }

    private List<Map<String, Object>> buildCitations(List<KnowledgeChunk> selected) {
        List<Map<String, Object>> citations = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            KnowledgeChunk chunk = selected.get(i);
            Map<String, Object> citation = new LinkedHashMap<>();
            citation.put("index", i + 1);
            citation.put("chunkId", chunk.chunkId());
            citation.put("title", chunk.title());
            citation.put("source", chunk.source());
            citation.put("tags", chunk.tags());
            citations.add(citation);
        }
        return citations;
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record ScoredChunk(KnowledgeChunk chunk, double score) {
    }
}
