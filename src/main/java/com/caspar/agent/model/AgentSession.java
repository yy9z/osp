package com.caspar.agent.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 会话状态，存储在 Redis 中
 */
@Data
public class AgentSession {
    private String sessionId;
    private Long userId;
    /** 当前正在处理的意图 */
    private String intent;
    /** 已收集的槽位 */
    private Map<String, Object> slots = new HashMap<>();
    /** 仍缺失的槽位列表 */
    private List<String> pendingSlots = new ArrayList<>();
    /** 对话历史（role: user/assistant, content: text） */
    private List<Map<String, String>> history = new ArrayList<>();
    /** 当前轮次（用于限制最大追问轮次） */
    private int turnCount = 0;

    public void addHistory(String role, String content) {
        Map<String, String> entry = new HashMap<>();
        entry.put("role", role);
        entry.put("content", content);
        history.add(entry);
    }
}
