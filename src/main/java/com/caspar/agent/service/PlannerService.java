package com.caspar.agent.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 兼容兜底规划服务：根据意图和槽位，决定需要调用哪些本地函数以及调用顺序。
 * 主链路由 FunctionCallingPlannerService 一次性生成 tool_calls。
 */
@Service
public class PlannerService {

    /** 意图 → 本地函数名 的映射（顺序执行） */
    private static final Map<String, List<String>> INTENT_TOOLS = Map.of(
            "DORM_REPAIR",        List.of("dorm_repair"),
            "DORM_QUERY",         List.of("dorm_query"),
            "REPAIR_QUERY",       List.of("repair_query"),
            "SECONDHAND_SEARCH",  List.of("secondhand_search"),
            "SECONDHAND_PUBLISH", List.of("secondhand_publish"),
            "LOSTFOUND_LOST",     List.of("lostfound_lost"),
            "LOSTFOUND_FOUND",    List.of("lostfound_found"),
            "NAVIGATION",         List.of("navigation_v2"),  // 使用增强版导航工具
            "MESSAGE_QUERY",      List.of("message_query"),
            "CAMPUS_TIPS",        List.of("campus_tips")
    );

    /**
     * 根据意图返回需要执行的工具列表。
     */
    public List<String> plan(String intent, Map<String, Object> slots) {
        return INTENT_TOOLS.getOrDefault(intent, List.of());
    }
}
