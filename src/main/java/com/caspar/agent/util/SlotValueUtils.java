package com.caspar.agent.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Agent 槽位/参数值解析工具，统一字符串与数值提取逻辑，减少重复实现。
 */
@Slf4j
public final class SlotValueUtils {

    private SlotValueUtils() {
    }

    public static String getString(Map<String, Object> source, String key) {
        if (source == null || key == null) {
            return null;
        }
        Object value = source.get(key);
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
    }

    public static String getString(Map<String, Object> source, String... keys) {
        if (source == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            String value = getString(source, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static Integer getInteger(Map<String, Object> source, String key) {
        if (source == null || key == null) {
            return null;
        }
        Object value = source.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            log.warn("整数解析失败, key={}, value={}", key, value);
            return null;
        }
    }

    public static Double getDouble(Map<String, Object> source, String... keys) {
        if (source == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            Object value = source.get(key);
            if (value == null) {
                continue;
            }
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException ex) {
                log.warn("浮点数解析失败, key={}, value={}", key, value);
            }
        }
        return null;
    }
}
