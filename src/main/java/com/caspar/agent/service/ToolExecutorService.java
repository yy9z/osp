package com.caspar.agent.service;

import com.caspar.agent.mcp.McpProperties;
import com.caspar.agent.mcp.McpToolClient;
import com.caspar.agent.model.AgentCard;
import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import com.caspar.agent.registry.ToolRegistry;
import com.caspar.agent.tool.AgentTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Legacy 工具执行服务：按旧规划器选择的本地函数调用 AgentTool，收集结果。
 * <p>
 * 当前 Agent 主链路通过 Spring AI Alibaba ToolCallback 执行本地工具，本类保留给
 * 监控接口和旧链路兼容使用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolExecutorService {

    private final ToolRegistry toolRegistry;
    private final McpToolClient mcpToolClient;
    private final McpProperties mcpProperties;
    @Qualifier("toolExecutor")
    private final Executor toolExecutor;
    private final AtomicInteger mcpConsecutiveFailures = new AtomicInteger(0);
    private final AtomicLong mcpCircuitOpenUntilMs = new AtomicLong(0L);
    private final AtomicLong mcpCallSuccessCount = new AtomicLong(0L);
    private final AtomicLong mcpCallFailureCount = new AtomicLong(0L);
    private final AtomicLong mcpFallbackCount = new AtomicLong(0L);
    private final AtomicLong mcpCircuitOpenedCount = new AtomicLong(0L);
    private final AtomicLong mcpTotalLatencyMs = new AtomicLong(0L);

    /**
     * 执行工具列表，返回所有工具结果。
     */
    public List<ToolResult> execute(List<String> toolNames, Map<String, Object> slots, Long userId) {
        Map<String, Object> safeSlots = slots == null ? Map.of() : slots;

        if (toolNames == null || toolNames.isEmpty()) {
            return List.of();
        }

        if (toolNames.size() == 1) {
            return List.of(executeSingleTool(toolNames.get(0), safeSlots, userId));
        }

        List<CompletableFuture<ToolResult>> futures = toolNames.stream()
                .map(toolName -> CompletableFuture.supplyAsync(
                        () -> executeSingleTool(toolName, safeSlots, userId),
                        toolExecutor
                ))
                .toList();

        List<ToolResult> results = new ArrayList<>(futures.size());
        for (int i = 0; i < futures.size(); i++) {
            try {
                results.add(futures.get(i).join());
            } catch (Exception e) {
                String toolName = toolNames.get(i);
                log.error("工具执行异常(并行聚合): {}", toolName, e);
                results.add(ToolResult.fail("工具执行失败: " + e.getMessage()));
            }
        }
        return results;
    }

    private ToolResult executeSingleTool(String toolName, Map<String, Object> safeSlots, Long userId) {
        if (mcpToolClient.isEnabled()) {
            if (isMcpCircuitOpen()) {
                mcpFallbackCount.incrementAndGet();
                log.warn("MCP 熔断开启中，跳过远程调用: tool={}, fallbackToLocal={}",
                        toolName, mcpProperties.isFallbackToLocal());
                if (!mcpProperties.isFallbackToLocal()) {
                    return ToolResult.fail("MCP服务暂不可用（熔断中）");
                }
            } else {
                long mcpStart = System.currentTimeMillis();
                try {
                    ToolResult mcpResult = mcpToolClient.call(toolName, safeSlots, userId);
                    onMcpSuccess(System.currentTimeMillis() - mcpStart);
                    return mcpResult;
                } catch (Exception mcpEx) {
                    onMcpFailure(System.currentTimeMillis() - mcpStart, mcpEx);
                    log.warn("MCP 工具调用失败: tool={}, fallbackToLocal={}, err={}",
                            toolName, mcpProperties.isFallbackToLocal(), mcpEx.getMessage());
                    if (!mcpProperties.isFallbackToLocal()) {
                        return ToolResult.fail("MCP工具执行失败: " + mcpEx.getMessage());
                    }
                    mcpFallbackCount.incrementAndGet();
                }
            }
        }

        return executeLocalTool(toolName, safeSlots, userId);
    }

    private ToolResult executeLocalTool(String toolName, Map<String, Object> safeSlots, Long userId) {
        AgentTool tool = toolRegistry.getTool(toolName);
        if (tool == null) {
            log.warn("工具不存在: {}", toolName);
            return ToolResult.fail("工具 [" + toolName + "] 不存在");
        }
        try {
            ToolArgs args = ToolArgs.builder()
                    .toolName(toolName)
                    .params(safeSlots)
                    .userId(userId)
                    .build();
            return tool.execute(args);
        } catch (Exception e) {
            log.error("工具执行异常: {}", toolName, e);
            return ToolResult.fail("工具执行失败: " + e.getMessage());
        }
    }

    public Map<String, Object> getMcpMetricsSnapshot() {
        long success = mcpCallSuccessCount.get();
        long failure = mcpCallFailureCount.get();
        long total = success + failure;
        double avgLatency = total == 0 ? 0 : (double) mcpTotalLatencyMs.get() / total;
        long circuitOpenUntil = mcpCircuitOpenUntilMs.get();
        long now = System.currentTimeMillis();

        return Map.of(
                "circuitOpen", circuitOpenUntil > now,
                "circuitOpenUntilMs", circuitOpenUntil,
                "consecutiveFailures", mcpConsecutiveFailures.get(),
                "mcpCallSuccessCount", success,
                "mcpCallFailureCount", failure,
                "mcpFallbackCount", mcpFallbackCount.get(),
                "mcpCircuitOpenedCount", mcpCircuitOpenedCount.get(),
                "mcpAverageLatencyMs", avgLatency,
                "mcpClient", mcpToolClient.metricsSnapshot()
        );
    }

    public void resetMcpMetrics() {
        mcpConsecutiveFailures.set(0);
        mcpCircuitOpenUntilMs.set(0L);
        mcpCallSuccessCount.set(0L);
        mcpCallFailureCount.set(0L);
        mcpFallbackCount.set(0L);
        mcpCircuitOpenedCount.set(0L);
        mcpTotalLatencyMs.set(0L);
        mcpToolClient.resetMetrics();
    }

    public void resetMcpCircuit() {
        mcpConsecutiveFailures.set(0);
        mcpCircuitOpenUntilMs.set(0L);
    }

    private boolean isMcpCircuitOpen() {
        long openUntil = mcpCircuitOpenUntilMs.get();
        if (openUntil <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now >= openUntil) {
            if (mcpCircuitOpenUntilMs.compareAndSet(openUntil, 0L)) {
                mcpConsecutiveFailures.set(0);
                log.info("MCP 熔断窗口结束，恢复远程调用探测");
            }
            return false;
        }
        return true;
    }

    private void onMcpSuccess(long latencyMs) {
        mcpCallSuccessCount.incrementAndGet();
        mcpTotalLatencyMs.addAndGet(Math.max(0, latencyMs));
        mcpConsecutiveFailures.set(0);
        mcpCircuitOpenUntilMs.set(0L);
    }

    private void onMcpFailure(long latencyMs, Exception cause) {
        mcpCallFailureCount.incrementAndGet();
        mcpTotalLatencyMs.addAndGet(Math.max(0, latencyMs));

        int consecutive = mcpConsecutiveFailures.incrementAndGet();
        int failureThreshold = Math.max(1, mcpProperties.getCircuitBreakerFailureThreshold());
        if (consecutive >= failureThreshold) {
            long openMs = Math.max(1000L, mcpProperties.getCircuitBreakerOpenMs());
            long openUntil = System.currentTimeMillis() + openMs;
            mcpCircuitOpenUntilMs.set(openUntil);
            mcpCircuitOpenedCount.incrementAndGet();
            log.warn("MCP 熔断触发: consecutiveFailures={}, threshold={}, openMs={}, error={}",
                    consecutive, failureThreshold, openMs, cause.getMessage());
        }
    }

    /**
     * 将工具结果转换为 AgentCard 列表。
     */
    public List<AgentCard> toCards(List<String> toolNames, List<ToolResult> results) {
        List<AgentCard> cards = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            ToolResult r = results.get(i);
            if (r.isSuccess() && r.getData() != null) {
                String cardType = resolveCardType(toolNames.size() > i ? toolNames.get(i) : "");
                cards.add(AgentCard.of(cardType, r.getData()));
            }
        }
        return cards;
    }

    private String resolveCardType(String toolName) {
        return switch (toolName) {
            case "dorm_repair", "repair_query" -> "REPAIR_ORDER";
            case "secondhand_search" -> "PRODUCT";
            case "lostfound_lost", "lostfound_found" -> "LOST_FOUND";
            case "navigation", "navigation_v2" -> "ROUTE";
            case "message_query" -> "MESSAGE";
            case "campus_tips" -> "PROACTIVE_TIPS";
            case "campus_knowledge_query" -> "KNOWLEDGE";
            default -> "INFO";
        };
    }
}
