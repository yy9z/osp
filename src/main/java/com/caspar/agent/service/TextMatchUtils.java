package com.caspar.agent.service;

import java.util.Locale;

/**
 * 文本匹配工具：提供大小写不敏感的关键词包含判断。
 */
public final class TextMatchUtils {

    private TextMatchUtils() {
    }

    public static boolean containsAnyIgnoreCase(String text, String... keywords) {
        if (text == null || keywords == null || keywords.length == 0) {
            return false;
        }
        String lowerText = text.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            if (lowerText.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
