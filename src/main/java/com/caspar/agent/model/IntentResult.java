package com.caspar.agent.model;

import lombok.Data;

@Data
public class IntentResult {
    /** 意图名称 */
    private String intent;
    /** 置信度 0~1 */
    private double confidence;
}
