package com.caspar.agent.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 后续建议编排服务。
 * 通过策略列表按意图生成建议文案。
 */
@Service
@RequiredArgsConstructor
public class FollowUpSuggestionService {

    private final List<FollowUpSuggestionStrategy> strategies;

    public List<String> build(String intent, Map<String, Object> slots) {
        if (intent == null) {
            return Collections.emptyList();
        }
        return strategies.stream()
                .filter(strategy -> strategy.supports(intent))
                .findFirst()
                .map(strategy -> strategy.suggestions(intent, slots))
                .orElse(Collections.emptyList());
    }
}
