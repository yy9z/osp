package com.caspar.agent.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 校园地点词典服务 - 语义识别增强
 */
@Service
public class CampusPoiDictionary {

    // 地点别名映射
    private static final Map<String, PoiEntry> POI_DICT = new HashMap<>();

    // 语义类型映射
    private static final Map<String, String> SEMANTIC_TYPE_MAP = new HashMap<>();

    static {
        // 初始化地点词典
        addPoi("东区学生食堂", "东区学生食堂", "canteen", "东食堂", "东区食堂", "东边食堂");
        addPoi("西区学生食堂", "西区学生食堂", "canteen", "西食堂", "西区食堂", "西边食堂");
        addPoi("校图书馆", "中国科学技术大学图书馆", "library", "图书馆", "图书馆主馆", "主图");
        addPoi("第三教学楼", "第三教学楼", "teaching_building", "教三", "三教", "教学楼3", "教室", "教师");
        addPoi("第一教学楼", "第一教学楼", "teaching_building", "教一", "一教", "教学楼1");
        addPoi("第二教学楼", "第二教学楼", "teaching_building", "教二", "二教", "教学楼2");
        addPoi("校园快递中心", "校园快递中心", "express", "快递点", "菜鸟", "菜鸟驿站", "快递站", "取快递");
        addPoi("校医院", "中国科学技术大学医院", "medical", "医院", "校医院", "医务室");
        addPoi("体育馆", "体育馆", "sports", "体育馆", "健身房", "运动场");
        addPoi("操场", "操场", "sports", "操场", "田径场", "跑道");
        addPoi("打印店", "打印店", "print_shop", "打印", "打印店", "复印店", "打印社");
        addPoi("超市", "超市", "convenience", "超市", "便利店", "小卖部");
        addPoi("自习室", "自习室", "study", "自习室", "自习", "学习室");

        // 初始化语义类型映射
        SEMANTIC_TYPE_MAP.put("吃饭", "canteen");
        SEMANTIC_TYPE_MAP.put("食堂", "canteen");
        SEMANTIC_TYPE_MAP.put("就餐", "canteen");
        SEMANTIC_TYPE_MAP.put("打印", "print_shop");
        SEMANTIC_TYPE_MAP.put("复印", "print_shop");
        SEMANTIC_TYPE_MAP.put("快递", "express");
        SEMANTIC_TYPE_MAP.put("取件", "express");
        SEMANTIC_TYPE_MAP.put("自习", "study");
        SEMANTIC_TYPE_MAP.put("学习", "study");
        SEMANTIC_TYPE_MAP.put("看书", "library");
        SEMANTIC_TYPE_MAP.put("借书", "library");
        SEMANTIC_TYPE_MAP.put("看病", "medical");
        SEMANTIC_TYPE_MAP.put("就医", "medical");
        SEMANTIC_TYPE_MAP.put("运动", "sports");
        SEMANTIC_TYPE_MAP.put("健身", "sports");
        SEMANTIC_TYPE_MAP.put("跑步", "sports");
        SEMANTIC_TYPE_MAP.put("上课", "teaching_building");
        SEMANTIC_TYPE_MAP.put("教室", "teaching_building");
        SEMANTIC_TYPE_MAP.put("教师", "teaching_building");
    }

    private static void addPoi(String key, String fullName, String category, String... aliases) {
        POI_DICT.put(key, new PoiEntry(key, fullName, category, Arrays.asList(aliases)));
    }

    /**
     * 匹配地点别名
     */
    public PoiMatch matchPlace(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        String normalized = query.trim();

        // 精确匹配
        for (PoiEntry entry : POI_DICT.values()) {
            if (entry.aliases.stream().anyMatch(alias -> alias.equalsIgnoreCase(normalized))) {
                return new PoiMatch(entry.fullName, entry.category, "alias_exact", entry.key);
            }
        }

        // 包含匹配
        for (PoiEntry entry : POI_DICT.values()) {
            if (entry.aliases.stream().anyMatch(normalized::contains)) {
                return new PoiMatch(entry.fullName, entry.category, "alias_contains", entry.key);
            }
        }

        return null;
    }

    /**
     * 识别语义类型
     */
    public String detectSemanticType(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        for (Map.Entry<String, String> entry : SEMANTIC_TYPE_MAP.entrySet()) {
            if (query.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * 判断是否"最近"需求
     */
    public boolean isNearestQuery(String query) {
        if (query == null) return false;
        return query.contains("最近") || query.contains("附近") || query.contains("离我近");
    }

    /**
     * 获取某类型的所有地点
     */
    public List<String> getPlacesByCategory(String category) {
        return POI_DICT.values().stream()
                .filter(entry -> category.equals(entry.category))
                .map(entry -> entry.fullName)
                .collect(Collectors.toList());
    }

    private record PoiEntry(String key, String fullName, String category, List<String> aliases) {}

    public record PoiMatch(String fullName, String category, String matchType, String key) {}
}
