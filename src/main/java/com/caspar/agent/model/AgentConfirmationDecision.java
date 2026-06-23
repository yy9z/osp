package com.caspar.agent.model;

import java.util.Locale;
import java.util.Set;

/**
 * Structured decision used to resume an interrupted write workflow.
 */
public enum AgentConfirmationDecision {
    APPROVE,
    REJECT;

    private static final Set<String> APPROVE_TEXT = Set.of(
            "确认", "确认执行", "执行", "同意", "确定", "是", "approve"
    );
    private static final Set<String> REJECT_TEXT = Set.of(
            "取消", "取消执行", "拒绝", "不同意", "否", "算了", "reject"
    );

    public static AgentConfirmationDecision parse(String structuredDecision, String userInput) {
        String structured = normalize(structuredDecision);
        if ("APPROVE".equals(structured)) {
            return APPROVE;
        }
        if ("REJECT".equals(structured)) {
            return REJECT;
        }

        String natural = normalize(userInput).toLowerCase(Locale.ROOT);
        if (APPROVE_TEXT.contains(natural)) {
            return APPROVE;
        }
        if (REJECT_TEXT.contains(natural)) {
            return REJECT;
        }
        return null;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
