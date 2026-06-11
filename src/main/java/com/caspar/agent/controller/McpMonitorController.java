package com.caspar.agent.controller;

import com.caspar.agent.service.ToolExecutorService;
import com.caspar.common.Result;
import com.caspar.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * MCP 监控接口，暴露健康状态与运行指标。
 */
@RestController
@RequestMapping("/api/agent/mcp")
@RequiredArgsConstructor
public class McpMonitorController {

    private final ToolExecutorService toolExecutorService;

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }
        if (!isAdminRole()) {
            return Result.forbidden();
        }
        Map<String, Object> metrics = toolExecutorService.getMcpMetricsSnapshot();
        boolean circuitOpen = Boolean.TRUE.equals(metrics.get("circuitOpen"));
        return Result.success(Map.of(
                "status", circuitOpen ? "DEGRADED" : "UP",
                "metrics", metrics
        ));
    }

    @GetMapping("/metrics")
    public Result<Map<String, Object>> metrics() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }
        if (!isAdminRole()) {
            return Result.forbidden();
        }
        return Result.success(toolExecutorService.getMcpMetricsSnapshot());
    }

    @PostMapping("/reset-metrics")
    public Result<Void> resetMetrics() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }
        if (!isAdminRole()) {
            return Result.forbidden();
        }
        toolExecutorService.resetMcpMetrics();
        return Result.success("MCP 指标已重置", null);
    }

    @PostMapping("/reset-circuit")
    public Result<Void> resetCircuit() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }
        if (!isAdminRole()) {
            return Result.forbidden();
        }
        toolExecutorService.resetMcpCircuit();
        return Result.success("MCP 熔断状态已重置", null);
    }

    private boolean isAdminRole() {
        return "ADMIN".equalsIgnoreCase(SecurityUtils.getCurrentUserRole());
    }
}
