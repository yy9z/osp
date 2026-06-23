package com.caspar.agent.service;

import com.caspar.agent.model.ToolResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 记录 ReactAgent 本轮实际调用过的本地工具，供前端卡片和 usedTools 字段复用。
 */
@Service
public class AgentToolExecutionRecorder {

    private final Map<String, List<AgentToolExecutionRecord>> records = new ConcurrentHashMap<>();

    public void begin(String traceId) {
        records.put(traceId, new CopyOnWriteArrayList<>());
    }

    public void record(String traceId, String toolName, Map<String, Object> params, Long userId, ToolResult result) {
        if (traceId == null || traceId.isBlank()) {
            return;
        }
        records.computeIfAbsent(traceId, key -> new CopyOnWriteArrayList<>())
                .add(new AgentToolExecutionRecord(
                        toolName,
                        params == null ? Collections.emptyMap() : new LinkedHashMap<>(params),
                        userId,
                        result
                ));
    }

    public List<AgentToolExecutionRecord> snapshot(String traceId) {
        List<AgentToolExecutionRecord> current = records.get(traceId);
        if (current == null) {
            return List.of();
        }
        return new ArrayList<>(current);
    }

    public void clear(String traceId) {
        if (traceId != null) {
            records.remove(traceId);
        }
    }
}
