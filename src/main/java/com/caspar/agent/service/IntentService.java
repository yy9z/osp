package com.caspar.agent.service;

import com.caspar.agent.llm.LlmClient;
import com.caspar.agent.model.IntentResult;
import com.caspar.agent.model.LlmMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 意图识别服务：调用大模型将用户自然语言映射为预定义意图。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentService {

    private static final String SYSTEM_PROMPT = """
            你是一个高校校园智能事务平台的意图识别模块，当前场景是中国科学技术大学校园服务。
            你的任务是判断用户输入属于哪个意图类别，并返回 JSON 格式结果。
            仅返回 JSON，不要输出其他任何文字，不要添加 markdown 代码块。

            涉及图书馆、食堂、教学楼、校区、怎么走、去哪里、导航、从哪到哪等表达，都优先识别为 NAVIGATION。

            意图类别：
            - DORM_REPAIR: 宿舍报修相关（空调、灯、网络、门锁等故障）
            - DORM_QUERY: 宿舍信息查询（查看我的宿舍、室友等）
            - REPAIR_QUERY: 查询报修工单状态
            - SECONDHAND_SEARCH: 搜索/推荐二手商品（找、买、求购、有没有、多少钱）
            - SECONDHAND_PUBLISH: 发布二手商品（卖、出、转让）
            - LOSTFOUND_LOST: 丢了东西，想找（我的...丢了、有没有看到...）
            - LOSTFOUND_FOUND: 捡到东西，想找失主（捡到、发现）
            - NAVIGATION: 问路、导航、去哪里怎么走
            - MESSAGE_QUERY: 查看消息、通知
            - CAMPUS_TIPS: 查看待办、主动提醒、优先处理事项
            - UNKNOWN: 无法识别

            示例：
            用户：宿舍灯坏了
            返回：{"intent": "DORM_REPAIR", "confidence": 0.99}

            用户：找一个50元以内的台灯
            返回：{"intent": "SECONDHAND_SEARCH", "confidence": 0.98}
            
            用户：哪里有卖自行车的
            返回：{"intent": "SECONDHAND_SEARCH", "confidence": 0.95}

            用户：捡到一张校园卡
            返回：{"intent": "LOSTFOUND_FOUND", "confidence": 0.98}

            返回格式：{"intent": "DORM_REPAIR", "confidence": 0.95}
            """;

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private static final int MAX_CONTEXT_MESSAGES = 6;
    private static final Pattern PRICE_BUDGET_PATTERN = Pattern.compile("\\d+(\\.\\d+)?\\s*(元|块|rmb|￥|¥)\\s*(以内|以下|左右|预算)?");
    private static final Set<String> SUPPORTED_INTENTS = Set.of(
            "DORM_REPAIR",
            "DORM_QUERY",
            "REPAIR_QUERY",
            "SECONDHAND_SEARCH",
            "SECONDHAND_PUBLISH",
            "LOSTFOUND_LOST",
            "LOSTFOUND_FOUND",
            "NAVIGATION",
            "MESSAGE_QUERY",
            "CAMPUS_TIPS",
            "UNKNOWN"
    );

    public IntentResult identify(String userInput) {
        return identify(userInput, List.of());
    }

    public IntentResult identify(String userInput, List<Map<String, String>> sessionHistory) {
        long startTime = System.currentTimeMillis();
        IntentResult heuristicFallback = heuristicIdentify(userInput);
        try {
            List<LlmMessage> messages = new java.util.ArrayList<>();
            messages.add(LlmMessage.system(SYSTEM_PROMPT));
            appendRecentHistory(messages, userInput, sessionHistory);
            String raw = llmClient.chat(messages);
            
            // 增强鲁棒性：提取 JSON 部分
            int start = raw.indexOf("{");
            int end = raw.lastIndexOf("}");
            if (start != -1 && end != -1) {
                raw = raw.substring(start, end + 1);
            } else {
                // 如果找不到 JSON 结构，尝试简单的清理
                raw = raw.replaceAll("```json\\s*|```\\s*", "").trim();
            }

            IntentResult result = objectMapper.readValue(raw, IntentResult.class);
            normalizeIntent(result);

            if (!SUPPORTED_INTENTS.contains(result.getIntent())) {
                log.warn("意图识别返回未知类别='{}'，使用规则兜底", result.getIntent());
                return heuristicOrUnknown(heuristicFallback);
            }
            if ("UNKNOWN".equals(result.getIntent()) && !"UNKNOWN".equals(heuristicFallback.getIntent())) {
                log.info("LLM返回UNKNOWN，规则兜底命中 intent={}", heuristicFallback.getIntent());
                return heuristicFallback;
            }
            log.info("意图识别完成, intent={}, confidence={}, 耗时={}ms",
                    result.getIntent(), result.getConfidence(), System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            log.warn("意图识别失败，降级为 UNKNOWN, 耗时={}ms: {}", System.currentTimeMillis() - startTime, e.getMessage());
            return heuristicOrUnknown(heuristicFallback);
        }
    }

    private void appendRecentHistory(List<LlmMessage> messages, String userInput, List<Map<String, String>> sessionHistory) {
        boolean hasCurrentInput = false;
        if (sessionHistory != null && !sessionHistory.isEmpty()) {
            int start = Math.max(0, sessionHistory.size() - MAX_CONTEXT_MESSAGES);
            for (int i = start; i < sessionHistory.size(); i++) {
                Map<String, String> item = sessionHistory.get(i);
                if (item == null) {
                    continue;
                }
                String role = item.get("role");
                String content = item.get("content");
                if (content == null || content.isBlank()) {
                    continue;
                }
                if ("user".equals(role)) {
                    messages.add(LlmMessage.user(content));
                    if (userInput != null && content.trim().equals(userInput.trim()) && i == sessionHistory.size() - 1) {
                        hasCurrentInput = true;
                    }
                } else if ("assistant".equals(role)) {
                    messages.add(LlmMessage.assistant(content));
                }
            }
        }
        if (!hasCurrentInput) {
            messages.add(LlmMessage.user(userInput));
        }
    }

    /**
     * 在进入通用问答前，先做一次“是否可能是校园业务场景”的宽松判断。
     * 这个判断比意图识别更保守：宁可多提示一次业务分流，也尽量不提前进入通用问答。
     */
    public boolean isPossiblyBusinessScenario(String userInput) {
        String text = userInput == null ? "" : userInput.trim();
        if (text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        // 明显是通用AI任务（写作/编程/翻译/学习等）时，优先走通用问答
        if (TextMatchUtils.containsAnyIgnoreCase(lower,
                "写代码", "编程", "python", "java", "javascript", "sql",
                "翻译", "润色", "总结", "改写", "写作", "论文", "简历",
                "算法", "数学", "题目", "面试", "英语", "学习计划")) {
            return false;
        }

        // 仅在出现较强校园业务词时才触发业务分流，避免误伤普通问答
        if (TextMatchUtils.containsAnyIgnoreCase(lower,
                "宿舍报修", "报修", "维修工单", "工单进度", "我的宿舍", "宿舍信息",
                "二手", "闲置", "求购", "发布商品", "上架", "下架", "我的收藏",
                "失物招领", "寻物", "招领", "捡到", "丢了", "遗失",
                "校园导航", "怎么走", "路线规划", "从哪到哪", "校区", "图书馆怎么走", "食堂怎么走",
                "消息中心", "消息通知", "未读消息", "站内消息",
                "智能提醒", "主动提醒", "待办", "有什么要处理", "优先事项")) {
            return true;
        }

        return isLikelySecondhandScenario(lower) || isLikelyMessageScenario(lower);
    }

    private boolean isLikelySecondhandScenario(String lower) {
        boolean hasSearchAction = TextMatchUtils.containsAnyIgnoreCase(lower,
                "找", "买", "收", "求购", "有没有", "多少钱", "预算", "便宜");
        boolean hasPublishAction = TextMatchUtils.containsAnyIgnoreCase(lower,
                "卖", "出", "转让", "处理掉", "挂上去", "挂一下");
        boolean hasGoodsWord = TextMatchUtils.containsAnyIgnoreCase(lower,
                "台灯", "自行车", "电动车", "耳机", "电脑", "笔记本", "手机", "书", "教材",
                "鼠标", "键盘", "显示器", "充电器", "插排", "风扇", "水杯", "杯子",
                "篮球", "羽毛球拍", "球拍", "衣服", "椅子", "桌子", "床垫");
        boolean hasBudget = TextMatchUtils.containsAnyIgnoreCase(lower,
                "元以内", "块以内", "以内", "以下", "预算")
                || PRICE_BUDGET_PATTERN.matcher(lower).find();

        return hasGoodsWord && (hasPublishAction || hasSearchAction || hasBudget);
    }

    private boolean isLikelyMessageScenario(String lower) {
        boolean hasMessageWord = TextMatchUtils.containsAnyIgnoreCase(lower,
                "消息", "通知", "公告", "未读");
        boolean hasQueryAction = TextMatchUtils.containsAnyIgnoreCase(lower,
                "查看", "查询", "查", "看", "看看", "打开", "我的", "最近", "最新");

        return hasMessageWord && hasQueryAction;
    }

    private void normalizeIntent(IntentResult result) {
        if (result == null || result.getIntent() == null) {
            return;
        }
        result.setIntent(result.getIntent().trim().toUpperCase(Locale.ROOT));
    }

    private IntentResult heuristicOrUnknown(IntentResult heuristicFallback) {
        if (heuristicFallback != null && heuristicFallback.getIntent() != null
                && !"UNKNOWN".equals(heuristicFallback.getIntent())) {
            return heuristicFallback;
        }
        IntentResult fallback = new IntentResult();
        fallback.setIntent("UNKNOWN");
        fallback.setConfidence(0.0);
        return fallback;
    }

    /**
     * LLM 异常时的本地关键词兜底，保障基础事务不因模型抖动而完全不可用。
     */
    private IntentResult heuristicIdentify(String userInput) {
        String text = userInput == null ? "" : userInput.trim();
        String lower = text.toLowerCase(Locale.ROOT);

        String intent = "UNKNOWN";
        double confidence = 0.0;

        if (TextMatchUtils.containsAnyIgnoreCase(lower, "导航", "怎么走", "去哪里", "从哪到哪", "图书馆", "食堂", "教学楼", "校区")) {
            intent = "NAVIGATION";
            confidence = 0.78;
        } else if (TextMatchUtils.containsAnyIgnoreCase(lower, "报修", "维修", "故障", "坏了", "坏掉", "不工作")
                && TextMatchUtils.containsAnyIgnoreCase(lower, "宿舍", "寝室", "空调", "灯", "门锁", "网络", "水管")) {
            intent = "DORM_REPAIR";
            confidence = 0.76;
        } else if (TextMatchUtils.containsAnyIgnoreCase(lower, "工单", "报修进度", "报修状态", "维修状态")) {
            intent = "REPAIR_QUERY";
            confidence = 0.75;
        } else if (TextMatchUtils.containsAnyIgnoreCase(lower, "捡到", "拾到", "发现了", "找到失主")) {
            intent = "LOSTFOUND_FOUND";
            confidence = 0.74;
        } else if (TextMatchUtils.containsAnyIgnoreCase(lower, "丢了", "遗失", "找不到", "丢失")) {
            intent = "LOSTFOUND_LOST";
            confidence = 0.74;
        } else if (TextMatchUtils.containsAnyIgnoreCase(lower, "卖", "出", "转让", "闲置", "出手")
                && TextMatchUtils.containsAnyIgnoreCase(lower, "二手", "台灯", "自行车", "书", "耳机", "电脑")) {
            intent = "SECONDHAND_PUBLISH";
            confidence = 0.72;
        } else if (TextMatchUtils.containsAnyIgnoreCase(lower, "找", "买", "求购", "有没有", "多少钱", "预算", "元以内", "便宜", "推荐")
                && TextMatchUtils.containsAnyIgnoreCase(lower, "台灯", "自行车", "耳机", "电脑", "书", "二手")) {
            intent = "SECONDHAND_SEARCH";
            confidence = 0.73;
        } else if (TextMatchUtils.containsAnyIgnoreCase(lower, "消息", "通知", "未读", "公告")) {
            intent = "MESSAGE_QUERY";
            confidence = 0.71;
        } else if (TextMatchUtils.containsAnyIgnoreCase(lower, "待办", "提醒", "要处理", "优先事项", "今天需要看什么", "有什么事")) {
            intent = "CAMPUS_TIPS";
            confidence = 0.72;
        } else if (TextMatchUtils.containsAnyIgnoreCase(lower, "我的宿舍", "室友", "寝室信息", "宿舍信息")) {
            intent = "DORM_QUERY";
            confidence = 0.71;
        }

        IntentResult result = new IntentResult();
        result.setIntent(intent);
        result.setConfidence(confidence);
        if (!"UNKNOWN".equals(intent)) {
            log.info("规则兜底命中 intent={}, confidence={}", intent, confidence);
        }
        return result;
    }
}
