package com.caspar.agent.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SlotResult {
    /** 已提取的槽位参数 */
    private Map<String, Object> slots;
    /** 仍缺失的必填槽位（按优先级排序） */
    private List<String> missingSlots;
    /** 针对第一个缺失槽位的追问文本 */
    private String askQuestion;
}
