package com.caspar.agent.model;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单次 Function Calling 规划结果：同时包含意图、函数名和结构化参数。
 */
@Data
public class FunctionCallPlan implements Serializable {
    /** 业务意图，如 SECONDHAND_SEARCH / NAVIGATION / UNKNOWN */
    private String intent = "UNKNOWN";
    /** 置信度 0~1 */
    private double confidence = 0.0;
    /** 本轮需要调用的本地函数名 */
    private List<String> toolNames = new ArrayList<>();
    /** 已合并的函数参数 */
    private Map<String, Object> slots = new HashMap<>();
    /** 仍缺失的必填参数 */
    private List<String> missingSlots = new ArrayList<>();
    /** 针对第一个缺失参数的追问文本 */
    private String askQuestion;
    /** 是否使用了旧三段式链路兜底 */
    private boolean fallbackUsed = false;
}
