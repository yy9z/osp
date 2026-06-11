package com.caspar.agent.service;

import java.util.List;
import java.util.Map;

/**
 * 对话后续建议策略。
 */
public interface FollowUpSuggestionStrategy {

    boolean supports(String intent);

    List<String> suggestions(String intent, Map<String, Object> slots);
}
