package com.caspar.agent.mcp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 调用配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent.mcp")
public class McpProperties {

    /**
     * 是否启用 MCP 工具调用链路。
     */
    private boolean enabled = false;

    /**
     * MCP 调用失败时是否回退到本地 AgentTool。
     */
    private boolean fallbackToLocal = true;

    /**
     * MCP stdio server 启动命令，如 npx / uvx / python。
     */
    private String command;

    /**
     * 启动命令参数。
     */
    private List<String> args = new ArrayList<>();

    /**
     * 附加环境变量。
     */
    private Map<String, String> env = new HashMap<>();

    /**
     * 系统内 MCP server 调用内部桥接接口时使用的共享密钥。
     */
    private String internalSecret;

    /**
     * 启动握手超时时间（毫秒）。
     */
    private long startupTimeoutMs = 15000;

    /**
     * 工具调用超时时间（毫秒）。
     */
    private long callTimeoutMs = 30000;

    /**
     * 熔断触发阈值：连续失败次数达到该值后打开熔断。
     */
    private int circuitBreakerFailureThreshold = 3;

    /**
     * 熔断打开时长（毫秒）。
     */
    private long circuitBreakerOpenMs = 60000;

    /**
     * 待处理请求数告警阈值（用于指标观测）。
     */
    private int pendingWarningThreshold = 5;

    /**
     * 本地工具名到 MCP 工具名映射。
     */
    private Map<String, String> toolNameMapping = new HashMap<>();
}
