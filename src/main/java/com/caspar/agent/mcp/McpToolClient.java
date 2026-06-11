package com.caspar.agent.mcp;

import com.caspar.agent.model.ToolResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MCP 工具调用客户端（stdio）。
 * 采用长连接复用进程，减少每次调用的冷启动开销。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolClient {

    private static final long IDLE_RESTART_MS = TimeUnit.MINUTES.toMillis(10);
    private static final int MAX_ARGUMENT_DEPTH = 6;
    private static final int MAX_ARGUMENT_ENTRIES = 200;
    private static final int MAX_ARGUMENT_STRING_LENGTH = 4000;
    private static final int MAX_ARGUMENT_KEY_LENGTH = 128;
    private static final int STDERR_WARN_LIMIT_PER_MINUTE = 20;
    private static final long STDERR_WARN_WINDOW_MS = TimeUnit.MINUTES.toMillis(1);

    private final ObjectMapper objectMapper;
    private final McpProperties mcpProperties;
    private final Object processLock = new Object();
    private final Object writerLock = new Object();
    private final Object stderrRateLimitLock = new Object();
    private final ConcurrentHashMap<Integer, CompletableFuture<JsonNode>> pendingResponses = new ConcurrentHashMap<>();
    private final AtomicInteger requestIdSequence = new AtomicInteger(0);
    private final AtomicBoolean placeholderConfigWarned = new AtomicBoolean(false);
    private final AtomicLong callSuccessCount = new AtomicLong(0);
    private final AtomicLong callFailureCount = new AtomicLong(0);
    private final AtomicLong processStartCount = new AtomicLong(0);
    private final AtomicLong processRestartCount = new AtomicLong(0);
    private final AtomicInteger stderrWarnCountInWindow = new AtomicInteger(0);
    private final AtomicLong stderrWarnWindowStartMs = new AtomicLong(0L);

    private volatile Process process;
    private volatile BufferedWriter writer;
    private volatile Thread stdoutThread;
    private volatile Thread stderrThread;
    private volatile boolean initialized = false;
    private volatile long lastUsedAtMs = 0L;

    public boolean isEnabled() {
        if (!mcpProperties.isEnabled() || !StringUtils.hasText(mcpProperties.getCommand())) {
            return false;
        }
        if (containsPlaceholderConfig()) {
            if (placeholderConfigWarned.compareAndSet(false, true)) {
                log.warn("检测到 MCP 占位符命令参数，已自动禁用 MCP 调用，请配置真实 MCP server 后再启用");
            }
            return false;
        }
        return true;
    }

    public ToolResult call(String localToolName, Map<String, Object> slots, Long userId) {
        if (!isEnabled()) {
            throw new IllegalStateException("MCP 未启用或未配置命令");
        }

        String remoteToolName = resolveRemoteToolName(localToolName);
        Map<String, Object> arguments = buildArguments(slots, userId);

        try {
            ensureProcessReady();
            JsonNode toolResponse = sendRequestAndAwait(
                    "tools/call",
                    Map.of("name", remoteToolName, "arguments", arguments),
                    mcpProperties.getCallTimeoutMs()
            );
            JsonNode resultNode = ensureSuccessResponse("tools/call", toolResponse);
            lastUsedAtMs = System.currentTimeMillis();

            if (resultNode.path("isError").asBoolean(false)) {
                String errorText = extractText(resultNode);
                if (!StringUtils.hasText(errorText)) {
                    errorText = "MCP 工具返回错误";
                }
                return ToolResult.fail(errorText);
            }

            Object data = extractStructuredData(resultNode);
            String summary = extractText(resultNode);
            if (!StringUtils.hasText(summary)) {
                summary = "工具执行成功";
            }
            callSuccessCount.incrementAndGet();
            return ToolResult.ok(summary, data);
        } catch (Exception e) {
            callFailureCount.incrementAndGet();
            if (shouldResetProcess(e)) {
                synchronized (processLock) {
                    destroyProcessLocked("MCP 进程状态异常，已重置", e);
                }
            }
            throw new RuntimeException("MCP 调用失败: " + e.getMessage(), e);
        } finally {
            lastUsedAtMs = System.currentTimeMillis();
        }
    }

    @PreDestroy
    public void shutdown() {
        synchronized (processLock) {
            destroyProcessLocked("MCP 客户端关闭", null, false);
        }
    }

    private List<String> buildCommand() {
        List<String> command = new ArrayList<>();
        command.add(mcpProperties.getCommand());
        if (mcpProperties.getArgs() != null && !mcpProperties.getArgs().isEmpty()) {
            command.addAll(mcpProperties.getArgs());
        }
        return command;
    }

    private String resolveRemoteToolName(String localToolName) {
        if (mcpProperties.getToolNameMapping() == null) {
            return localToolName;
        }
        return mcpProperties.getToolNameMapping().getOrDefault(localToolName, localToolName);
    }

    private Map<String, Object> buildArguments(Map<String, Object> slots, Long userId) {
        Map<String, Object> args = new HashMap<>();
        if (slots != null) {
            args.putAll(slots);
        }
        if (userId != null) {
            args.putIfAbsent("userId", userId);
            args.putIfAbsent("user_id", userId);
        }
        return sanitizeMap(args, 0);
    }

    private Map<String, Object> buildInitializeParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("protocolVersion", "2024-11-05");
        params.put("capabilities", Map.of());
        params.put("clientInfo", Map.of(
                "name", "NewOSPFU-Agent",
                "version", "1.0.0"
        ));
        return params;
    }

    private void ensureProcessReady() throws Exception {
        Process currentProcess = process;
        BufferedWriter currentWriter = writer;
        if (currentProcess != null && currentProcess.isAlive() && initialized && currentWriter != null) {
            long idleMs = System.currentTimeMillis() - lastUsedAtMs;
            if (idleMs <= IDLE_RESTART_MS) {
                return;
            }
        }
        synchronized (processLock) {
            ensureProcessReadyLocked();
        }
    }

    private void ensureProcessReadyLocked() throws Exception {
        long now = System.currentTimeMillis();
        if (process != null
                && lastUsedAtMs > 0
                && now - lastUsedAtMs > IDLE_RESTART_MS
                && pendingResponses.isEmpty()) {
            destroyProcessLocked("MCP 进程空闲超时，执行重启", null);
        }
        if (process == null || !process.isAlive() || writer == null) {
            startProcessLocked();
        }
        if (!initialized) {
            initializeLocked();
        }
        lastUsedAtMs = now;
    }

    private void startProcessLocked() throws IOException {
        Process started = null;
        BufferedWriter startedWriter = null;
        ProcessBuilder processBuilder = new ProcessBuilder(buildCommand());
        processBuilder.redirectErrorStream(false);
        if (mcpProperties.getEnv() != null && !mcpProperties.getEnv().isEmpty()) {
            processBuilder.environment().putAll(mcpProperties.getEnv());
        }
        try {
            started = processBuilder.start();
            startedWriter = new BufferedWriter(new OutputStreamWriter(started.getOutputStream(), StandardCharsets.UTF_8));
            process = started;
            writer = startedWriter;
            stdoutThread = startStdoutReader(started);
            stderrThread = startStderrReader(started);
            initialized = false;
            lastUsedAtMs = System.currentTimeMillis();
            processStartCount.incrementAndGet();
        } catch (IOException e) {
            closeQuietly(startedWriter);
            if (started != null && started.isAlive()) {
                started.destroyForcibly();
            }
            process = null;
            writer = null;
            stdoutThread = null;
            stderrThread = null;
            initialized = false;
            lastUsedAtMs = 0L;
            throw e;
        }
    }

    private void initializeLocked() throws Exception {
        JsonNode initializeResponse = sendRequestAndAwaitInternal(
                "initialize",
                buildInitializeParams(),
                mcpProperties.getStartupTimeoutMs()
        );
        ensureSuccessResponse("initialize", initializeResponse);
        sendNotification("notifications/initialized", Map.of());
        initialized = true;
    }

    private int nextRequestId() {
        int next = requestIdSequence.updateAndGet(prev -> prev >= Integer.MAX_VALUE - 10 ? 1 : prev + 1);
        if (next <= 0) {
            requestIdSequence.set(1);
            return 1;
        }
        return next;
    }

    private JsonNode sendRequestAndAwait(String method, Map<String, Object> params, long timeoutMs) throws Exception {
        return sendRequestAndAwaitInternal(method, params, timeoutMs);
    }

    private JsonNode sendRequestAndAwaitInternal(String method, Map<String, Object> params, long timeoutMs)
            throws Exception {
        int requestId = nextRequestId();
        CompletableFuture<JsonNode> responseFuture = new CompletableFuture<>();
        pendingResponses.put(requestId, responseFuture);
        try {
            sendRequest(requestId, method, params);
            return awaitResponse(requestId, responseFuture, timeoutMs);
        } finally {
            pendingResponses.remove(requestId);
        }
    }

    private void sendRequest(int id, String method, Map<String, Object> params) throws IOException {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", method);
        request.set("params", objectMapper.valueToTree(params));
        writeLine(request);
    }

    private void sendNotification(String method, Map<String, Object> params) throws IOException {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("method", method);
        request.set("params", objectMapper.valueToTree(params));
        writeLine(request);
    }

    private void writeLine(ObjectNode request) throws IOException {
        synchronized (writerLock) {
            BufferedWriter currentWriter = writer;
            if (currentWriter == null) {
                throw new IllegalStateException("MCP writer 未初始化");
            }
            currentWriter.write(objectMapper.writeValueAsString(request));
            currentWriter.newLine();
            currentWriter.flush();
        }
    }

    private JsonNode awaitResponse(int requestId, CompletableFuture<JsonNode> responseFuture, long timeoutMs)
            throws Exception {
        long effectiveTimeout = Math.max(timeoutMs, 1000L);
        try {
            return responseFuture.get(effectiveTimeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            if (!isProcessAlive()) {
                throw new IllegalStateException("等待 MCP 响应超时且进程已退出: requestId=" + requestId, e);
            }
            throw new IllegalStateException("等待 MCP 响应超时: requestId=" + requestId, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待 MCP 响应被中断: requestId=" + requestId, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new RuntimeException(cause);
        }
    }

    private JsonNode ensureSuccessResponse(String method, JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("MCP 响应为空: " + method);
        }
        if (response.has("error")) {
            JsonNode errorNode = response.get("error");
            String message = errorNode != null && errorNode.has("message")
                    ? errorNode.get("message").asText()
                    : "未知错误";
            throw new IllegalStateException(method + " 失败: " + message);
        }
        JsonNode resultNode = response.get("result");
        if (resultNode == null || resultNode.isNull()) {
            throw new IllegalStateException(method + " 响应缺少 result");
        }
        return resultNode;
    }

    private Object extractStructuredData(JsonNode resultNode) {
        JsonNode structured = resultNode.get("structuredContent");
        if (structured != null && !structured.isNull()) {
            return objectMapper.convertValue(structured, Object.class);
        }
        JsonNode content = resultNode.get("content");
        if (content != null && !content.isNull()) {
            return objectMapper.convertValue(content, Object.class);
        }
        return Map.of();
    }

    private String extractText(JsonNode resultNode) {
        JsonNode contentNode = resultNode.get("content");
        if (contentNode == null || !contentNode.isArray()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (JsonNode item : contentNode) {
            if (!"text".equals(item.path("type").asText())) {
                continue;
            }
            String text = item.path("text").asText();
            if (StringUtils.hasText(text)) {
                parts.add(text.trim());
            }
        }
        return String.join("\n", parts);
    }

    private Thread startStdoutReader(Process readerProcess) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(readerProcess.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!StringUtils.hasText(line)) {
                        continue;
                    }
                    try {
                        JsonNode node = objectMapper.readTree(line);
                        Integer responseId = extractResponseId(node);
                        if (responseId != null) {
                            CompletableFuture<JsonNode> pending = pendingResponses.get(responseId);
                            if (pending != null) {
                                pending.complete(node);
                            } else {
                                log.warn("收到未匹配 requestId={} 的 MCP 响应，已忽略", responseId);
                            }
                            continue;
                        }
                        if (node.has("method")) {
                            log.debug("收到 MCP 通知: method={}", node.path("method").asText());
                        } else {
                            log.debug("收到无 requestId 的 MCP 消息: {}", line);
                        }
                    } catch (Exception parseException) {
                        log.debug("忽略非 JSON stdout: {}", line);
                    }
                }
            } catch (IOException e) {
                log.debug("读取 MCP stdout 结束: {}", e.getMessage());
            } finally {
                if (process == readerProcess) {
                    synchronized (processLock) {
                        if (process == readerProcess) {
                            closeQuietly(writer);
                            writer = null;
                            process = null;
                            initialized = false;
                            lastUsedAtMs = 0L;
                            failAllPendingResponses(new IllegalStateException("MCP stdout 通道关闭"));
                        }
                    }
                }
            }
        }, "mcp-stdout-reader");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private void destroyProcessLocked(String reason, Exception cause) {
        destroyProcessLocked(reason, cause, true);
    }

    private void destroyProcessLocked(String reason, Exception cause, boolean countAsRestart) {
        if (cause == null) {
            log.warn(reason);
        } else {
            log.warn("{}: {}", reason, cause.getMessage());
        }
        Process currentProcess = process;
        boolean hadState = currentProcess != null || writer != null || stdoutThread != null || stderrThread != null;
        if (countAsRestart && hadState) {
            processRestartCount.incrementAndGet();
        }

        failAllPendingResponses(new IllegalStateException(reason, cause));

        closeQuietly(writer);
        writer = null;

        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroy();
            try {
                currentProcess.waitFor(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (currentProcess.isAlive()) {
                currentProcess.destroyForcibly();
            }
        }
        process = null;
        initialized = false;
        lastUsedAtMs = 0L;

        joinQuietly(stdoutThread);
        joinQuietly(stderrThread);
        stdoutThread = null;
        stderrThread = null;
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
            // ignore
        }
    }

    private Thread startStderrReader(Process process) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (StringUtils.hasText(line)) {
                        logStderrWithRateLimit(line);
                    }
                }
            } catch (IOException e) {
                log.warn("读取 MCP stderr 结束: {}", e.getMessage());
            }
        }, "mcp-stderr-reader");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private Integer extractResponseId(JsonNode node) {
        if (node == null || !node.has("id")) {
            return null;
        }
        JsonNode idNode = node.get("id");
        if (idNode == null || idNode.isNull()) {
            return null;
        }
        if (idNode.isInt() || idNode.isLong()) {
            return idNode.asInt();
        }
        if (idNode.isTextual()) {
            try {
                return Integer.parseInt(idNode.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean isProcessAlive() {
        Process current = process;
        return current != null && current.isAlive();
    }

    private boolean shouldResetProcess(Exception exception) {
        if (exception instanceof IOException) {
            return true;
        }
        if (exception instanceof IllegalStateException) {
            String message = exception.getMessage();
            if (message == null) {
                return false;
            }
            return message.contains("进程已退出")
                    || message.contains("writer 未初始化")
                    || message.contains("等待 MCP 响应超时")
                    || message.contains("响应为空");
        }
        return false;
    }

    public Map<String, Object> metricsSnapshot() {
        int pendingCount = pendingResponses.size();
        int pendingThreshold = Math.max(1, mcpProperties.getPendingWarningThreshold());
        return Map.ofEntries(
                Map.entry("enabled", isEnabled()),
                Map.entry("processAlive", isProcessAlive()),
                Map.entry("initialized", initialized),
                Map.entry("pendingRequests", pendingCount),
                Map.entry("pendingWarningThreshold", pendingThreshold),
                Map.entry("pendingOverThreshold", pendingCount > pendingThreshold),
                Map.entry("callSuccessCount", callSuccessCount.get()),
                Map.entry("callFailureCount", callFailureCount.get()),
                Map.entry("processStartCount", processStartCount.get()),
                Map.entry("processRestartCount", processRestartCount.get()),
                Map.entry("lastUsedAtMs", lastUsedAtMs)
        );
    }

    public void resetMetrics() {
        callSuccessCount.set(0L);
        callFailureCount.set(0L);
        processStartCount.set(0L);
        processRestartCount.set(0L);
        stderrWarnCountInWindow.set(0);
        stderrWarnWindowStartMs.set(0L);
    }

    private void failAllPendingResponses(Exception cause) {
        if (cause == null || pendingResponses.isEmpty()) {
            return;
        }
        pendingResponses.forEach((id, future) -> future.completeExceptionally(cause));
        pendingResponses.clear();
    }

    private Map<String, Object> sanitizeMap(Map<?, ?> input, int depth) {
        if (input == null || depth > MAX_ARGUMENT_DEPTH) {
            return Map.of();
        }
        Map<String, Object> sanitized = new HashMap<>();
        int count = 0;
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            if (count >= MAX_ARGUMENT_ENTRIES) {
                break;
            }
            String key = sanitizeKey(entry.getKey());
            if (!StringUtils.hasText(key)) {
                continue;
            }
            Object value = sanitizeValue(entry.getValue(), depth + 1);
            if (value == null) {
                continue;
            }
            sanitized.put(key, value);
            count++;
        }
        return sanitized;
    }

    private List<Object> sanitizeList(List<?> input, int depth) {
        if (input == null || depth > MAX_ARGUMENT_DEPTH) {
            return List.of();
        }
        List<Object> sanitized = new ArrayList<>();
        int limit = Math.min(input.size(), MAX_ARGUMENT_ENTRIES);
        for (int i = 0; i < limit; i++) {
            Object value = sanitizeValue(input.get(i), depth + 1);
            if (value != null) {
                sanitized.add(value);
            }
        }
        return sanitized;
    }

    private Object sanitizeValue(Object value, int depth) {
        if (value == null || depth > MAX_ARGUMENT_DEPTH) {
            return null;
        }
        if (value instanceof String text) {
            return truncate(text);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof Map<?, ?> mapValue) {
            return sanitizeMap(mapValue, depth + 1);
        }
        if (value instanceof List<?> listValue) {
            return sanitizeList(listValue, depth + 1);
        }
        Class<?> valueClass = value.getClass();
        if (valueClass.isArray()) {
            int length = Math.min(Array.getLength(value), MAX_ARGUMENT_ENTRIES);
            List<Object> converted = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                Object item = sanitizeValue(Array.get(value, i), depth + 1);
                if (item != null) {
                    converted.add(item);
                }
            }
            return converted;
        }
        try {
            Object converted = objectMapper.convertValue(value, Object.class);
            if (converted == value) {
                return truncate(String.valueOf(value));
            }
            return sanitizeValue(converted, depth + 1);
        } catch (IllegalArgumentException ignored) {
            return truncate(String.valueOf(value));
        }
    }

    private String sanitizeKey(Object rawKey) {
        if (rawKey == null) {
            return null;
        }
        String key = String.valueOf(rawKey).trim();
        if (!StringUtils.hasText(key)) {
            return null;
        }
        if (key.length() > MAX_ARGUMENT_KEY_LENGTH) {
            key = key.substring(0, MAX_ARGUMENT_KEY_LENGTH);
        }
        return key.replaceAll("[^a-zA-Z0-9_\\-.]", "_");
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        if (text.length() <= MAX_ARGUMENT_STRING_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_ARGUMENT_STRING_LENGTH);
    }

    private boolean containsPlaceholderConfig() {
        List<String> args = mcpProperties.getArgs();
        if (args == null || args.isEmpty()) {
            return false;
        }
        for (String arg : args) {
            if (!StringUtils.hasText(arg)) {
                continue;
            }
            if (arg.contains("@your-org/your-mcp-server")) {
                return true;
            }
        }
        return false;
    }

    private void logStderrWithRateLimit(String line) {
        long now = System.currentTimeMillis();
        synchronized (stderrRateLimitLock) {
            long windowStart = stderrWarnWindowStartMs.get();
            if (windowStart <= 0 || now - windowStart >= STDERR_WARN_WINDOW_MS) {
                stderrWarnWindowStartMs.set(now);
                stderrWarnCountInWindow.set(0);
            }
            int count = stderrWarnCountInWindow.incrementAndGet();
            if (count <= STDERR_WARN_LIMIT_PER_MINUTE) {
                log.warn("MCP stderr: {}", line);
                return;
            }
            if (count == STDERR_WARN_LIMIT_PER_MINUTE + 1) {
                log.warn("MCP stderr 日志超过每分钟{}条，后续同窗口降级为 debug", STDERR_WARN_LIMIT_PER_MINUTE);
            }
            log.debug("MCP stderr(suppressed): {}", line);
        }
    }

    private void joinQuietly(Thread thread) {
        if (thread == null || thread == Thread.currentThread()) {
            return;
        }
        try {
            thread.join(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
