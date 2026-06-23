package com.caspar.agent.registry;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Central catalog for intent, tool schema, required slots and clarification text.
 */
@Component
public class AgentToolCatalog {

    private static final Map<String, String> SLOT_QUESTIONS = Map.of(
            "fault_type", "好的，请问是什么故障？（空调/灯/网络/门锁/水管/其他）",
            "dorm_no", "请问您的宿舍号是多少？",
            "item_name", "请描述一下物品名称是什么？",
            "destination", "请问您要去哪里？",
            "campus", "中科大有多个校区，请问您想去哪个校区？（东校区/西校区/南校区/中校区/高新校区）",
            "title", "请输入商品的标题：",
            "category", "请选择分类：数码/书籍/生活/服装/其他",
            "price", "请输入价格（元）：",
            "question", "请告诉我你想查询哪方面的平台使用说明或校园事务规则。"
    );

    private final List<AgentToolDefinition> definitions;
    private final Map<String, AgentToolDefinition> byIntent;
    private final Map<String, AgentToolDefinition> byName;

    public AgentToolCatalog() {
        this.definitions = List.of(
                definition("DORM_REPAIR", "dorm_repair", "提交宿舍报修工单", false,
                        List.of(
                                parameter("fault_type", "string", "故障类型"),
                                parameter("dorm_no", "string", "宿舍号，可选，通常由用户档案补充"),
                                parameter("description", "string", "故障补充描述")
                        ), List.of("fault_type")),
                definition("DORM_QUERY", "dorm_query", "查询当前用户的宿舍信息", true,
                        List.of(), List.of()),
                definition("REPAIR_QUERY", "repair_query", "查询当前用户的宿舍报修工单", true,
                        List.of(
                                parameter("status", "string", "工单状态"),
                                parameter("keyword", "string", "查询关键词")
                        ), List.of()),
                definition("SECONDHAND_SEARCH", "secondhand_search", "搜索或推荐校园二手商品", true,
                        List.of(
                                parameter("keyword", "string", "商品关键词"),
                                parameter("category", "string", "商品分类"),
                                parameter("max_price", "number", "最高预算"),
                                parameter("sort_preference", "string", "排序偏好")
                        ), List.of()),
                definition("SECONDHAND_PUBLISH", "secondhand_publish", "发布校园二手商品", false,
                        List.of(
                                parameter("title", "string", "商品标题"),
                                parameter("category", "string", "商品分类"),
                                parameter("price", "number", "商品价格"),
                                parameter("description", "string", "商品描述"),
                                parameter("condition", "string", "商品成色")
                        ), List.of("title", "category", "price")),
                definition("LOSTFOUND_LOST", "lostfound_lost", "发布寻物信息", false,
                        lostFoundParameters(), List.of("item_name")),
                definition("LOSTFOUND_FOUND", "lostfound_found", "发布招领信息", false,
                        lostFoundParameters(), List.of("item_name")),
                definition("NAVIGATION", "navigation_v2", "规划校园导航路线", true,
                        List.of(
                                parameter("destination", "string", "目的地"),
                                parameter("origin", "string", "出发地"),
                                parameter("campus", "string", "校区"),
                                parameter("travel_mode", "string", "出行方式"),
                                parameter("preferences", "array", "路线偏好"),
                                parameter("waypoints", "array", "途经点"),
                                parameter("taskScene", "string", "任务场景"),
                                parameter("timeContext", "string", "时间上下文"),
                                parameter("timeSlot", "string", "时间段"),
                                parameter("urgencyMinutes", "integer", "剩余时间，单位分钟"),
                                parameter("destinationType", "string", "目的地类型"),
                                parameter("userLat", "number", "用户当前位置纬度"),
                                parameter("userLng", "number", "用户当前位置经度"),
                                parameter("originalQuery", "string", "用户原始导航表达")
                        ), List.of("destination")),
                definition("MESSAGE_QUERY", "message_query", "查询当前用户的消息和通知", true,
                        List.of(
                                parameter("keyword", "string", "消息关键词"),
                                parameter("unread_only", "boolean", "是否只查询未读消息")
                        ), List.of()),
                definition("CAMPUS_TIPS", "campus_tips", "查询当前用户的校园待办和主动提醒", true,
                        List.of(), List.of()),
                definition("CAMPUS_KNOWLEDGE", "campus_knowledge_query",
                        "检索平台使用说明、校园事务规则和常见问题", true,
                        List.of(
                                parameter("question", "string", "用户想查询的平台说明或规则问题"),
                                parameter("module", "string", "模块名称，例如宿舍报修、二手交易或失物招领"),
                                parameter("top_k", "integer", "返回知识片段数量，默认3")
                        ), List.of("question"))
        );

        this.byIntent = indexBy(AgentToolDefinition::intent);
        this.byName = indexBy(AgentToolDefinition::name);
    }

    public List<AgentToolDefinition> getDefinitions() {
        return definitions;
    }

    public List<AgentToolDefinition> getReadOnlyDefinitions() {
        return definitions.stream().filter(AgentToolDefinition::readOnly).toList();
    }

    public Optional<AgentToolDefinition> findByIntent(String intent) {
        return Optional.ofNullable(byIntent.get(intent));
    }

    public Optional<AgentToolDefinition> findByName(String toolName) {
        return Optional.ofNullable(byName.get(toolName));
    }

    public boolean hasIntent(String intent) {
        return byIntent.containsKey(intent);
    }

    public boolean hasTool(String toolName) {
        return byName.containsKey(toolName);
    }

    public String getToolName(String intent) {
        AgentToolDefinition definition = byIntent.get(intent);
        return definition == null ? null : definition.name();
    }

    public List<String> getToolNames(String intent) {
        String toolName = getToolName(intent);
        return toolName == null ? List.of() : List.of(toolName);
    }

    public List<String> getRequiredSlots(String intent) {
        AgentToolDefinition definition = byIntent.get(intent);
        return definition == null ? List.of() : definition.requiredSlots();
    }

    public String getSlotQuestion(String slot) {
        return SLOT_QUESTIONS.getOrDefault(slot, "请提供更多信息：");
    }

    public String buildPlannerFunctionPrompt() {
        return IntStream.range(0, definitions.size())
                .mapToObj(index -> formatDefinition(index + 1, definitions.get(index)))
                .collect(Collectors.joining("\n"));
    }

    public String buildSupportedIntentPrompt() {
        return definitions.stream()
                .map(AgentToolDefinition::intent)
                .collect(Collectors.joining(", "));
    }

    private String formatDefinition(int index, AgentToolDefinition definition) {
        String arguments = definition.parameters().isEmpty()
                ? "{}"
                : definition.parameters().stream()
                .map(parameter -> parameter.name() + "(" + parameter.type() + ")")
                .collect(Collectors.joining(", "));
        String required = definition.requiredSlots().isEmpty()
                ? ""
                : "\n   required: " + String.join(", ", definition.requiredSlots());
        return index + ". " + definition.name() + "：" + definition.description()
                + "\n   arguments: " + arguments + required;
    }

    private Map<String, AgentToolDefinition> indexBy(
            java.util.function.Function<AgentToolDefinition, String> keyExtractor) {
        Map<String, AgentToolDefinition> index = new LinkedHashMap<>();
        for (AgentToolDefinition definition : definitions) {
            index.put(keyExtractor.apply(definition), definition);
        }
        return Map.copyOf(index);
    }

    private static AgentToolDefinition definition(String intent,
                                                  String name,
                                                  String description,
                                                  boolean readOnly,
                                                  List<AgentToolParameter> parameters,
                                                  List<String> requiredSlots) {
        return new AgentToolDefinition(intent, name, description, parameters, requiredSlots, readOnly);
    }

    private static AgentToolParameter parameter(String name, String type, String description) {
        return new AgentToolParameter(name, type, description);
    }

    private static List<AgentToolParameter> lostFoundParameters() {
        return List.of(
                parameter("item_name", "string", "物品名称"),
                parameter("color", "string", "物品颜色"),
                parameter("location", "string", "丢失或拾取地点"),
                parameter("time", "string", "丢失或拾取时间"),
                parameter("description", "string", "物品补充描述")
        );
    }
}
