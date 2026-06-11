package com.caspar.agent.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AgentResponse {
    private String sessionId;
    /** 自然语言回复文本 */
    private String reply;
    /** 非null时表示正在追问某个槽位 */
    private String askFor;
    /** 识别到的意图 */
    private String intent;
    /** 已提取的槽位参数 */
    private Map<String, Object> extractedSlots;
    /** 结果卡片列表（任务完成时非空） */
    private List<AgentCard> cards;
    /** 本次调用的工具名列表 */
    private List<String> usedTools;
    /** 是否已完成任务 */
    private boolean taskCompleted;
    /** 任务完成后的动态追问建议，追问过程中为空 */
    private List<String> followUpSuggestions;
    /** 结构化追问类型（如 campus_disambiguation） */
    private String followUpType;
    /** 结构化追问选项 */
    private List<String> followUpOptions;
}
