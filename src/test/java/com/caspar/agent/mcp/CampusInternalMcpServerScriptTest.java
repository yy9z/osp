package com.caspar.agent.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampusInternalMcpServerScriptTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();
    private static final Path SCRIPT = PROJECT_ROOT.resolve("scripts/mcp/campus-internal-mcp-server.mjs");

    private HttpServer httpServer;
    private final AtomicInteger bridgeCallCount = new AtomicInteger(0);

    @BeforeEach
    void setUp() throws Exception {
        Assumptions.assumeTrue(isNodeAvailable(), "node is not available");
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/api/internal/mcp/tools/call", this::handleBridgeCall);
        httpServer.start();
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    void mcpServer_shouldExposeAndCallFourInternalScenarioTools() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder("node", SCRIPT.toString())
                .directory(PROJECT_ROOT.toFile())
                .redirectError(ProcessBuilder.Redirect.INHERIT);
        processBuilder.environment().put("CAMPUS_MCP_BASE_URL", "http://127.0.0.1:" + httpServer.getAddress().getPort());
        processBuilder.environment().put("CAMPUS_MCP_INTERNAL_SECRET", "secret");
        Process process = processBuilder.start();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

            Map<String, Object> initialize = send(writer, reader, 1, "initialize", Map.of());
            assertFalse(initialize.containsKey("error"));
            assertEquals("campus-internal-mcp-server", getNested(initialize, "result", "serverInfo", "name"));

            Map<String, Object> toolsList = send(writer, reader, 2, "tools/list", Map.of());
            List<Map<String, Object>> tools = nestedList(toolsList, "result", "tools");
            assertEquals(4, tools.size());

            callScenario(writer, reader, 3, "mcp_campus_overview", Map.of("userId", 42));
            callScenario(writer, reader, 4, "mcp_dorm_repair_flow", Map.of("userId", 42, "fault_type", "空调"));
            callScenario(writer, reader, 5, "mcp_secondhand_meetup_flow", Map.of("userId", 42, "keyword", "台灯"));
            callScenario(writer, reader, 6, "mcp_lostfound_match_flow", Map.of("userId", 42, "item_name", "校园卡"));

            assertTrue(bridgeCallCount.get() >= 7);
        } finally {
            process.destroy();
            process.waitFor(2, TimeUnit.SECONDS);
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private void callScenario(BufferedWriter writer, BufferedReader reader, int id, String name, Map<String, Object> args)
            throws Exception {
        Map<String, Object> response = send(writer, reader, id, "tools/call", Map.of(
                "name", name,
                "arguments", args
        ));
        assertFalse(response.containsKey("error"));
        assertEquals(Boolean.FALSE, getNested(response, "result", "isError"));
        assertNotNull(getNested(response, "result", "structuredContent", "scenario"));
    }

    private Map<String, Object> send(BufferedWriter writer, BufferedReader reader, int id, String method, Map<String, Object> params)
            throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", method);
        request.put("params", params);
        writer.write(OBJECT_MAPPER.writeValueAsString(request));
        writer.newLine();
        writer.flush();

        String line = reader.readLine();
        assertNotNull(line);
        return OBJECT_MAPPER.readValue(line, new TypeReference<>() {
        });
    }

    private void handleBridgeCall(HttpExchange exchange) {
        try {
            bridgeCallCount.incrementAndGet();
            if (!"secret".equals(exchange.getRequestHeaders().getFirst("X-Campus-MCP-Secret"))) {
                writeJson(exchange, 403, Map.of("code", 403, "message", "forbidden"));
                return;
            }
            Map<String, Object> request = OBJECT_MAPPER.readValue(exchange.getRequestBody(), new TypeReference<>() {
            });
            String toolName = String.valueOf(request.get("toolName"));
            Map<String, Object> data = Map.of(
                    "toolName", toolName,
                    "scenarioData", true
            );
            Map<String, Object> toolResult = Map.of(
                    "success", true,
                    "summary", "ok " + toolName,
                    "data", data
            );
            writeJson(exchange, 200, Map.of(
                    "code", 200,
                    "message", "success",
                    "data", toolResult
            ));
        } catch (Exception e) {
            try {
                writeJson(exchange, 500, Map.of("code", 500, "message", e.getMessage()));
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    private void writeJson(HttpExchange exchange, int status, Map<String, Object> body) throws Exception {
        byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(body);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private boolean isNodeAvailable() {
        try {
            Process process = new ProcessBuilder("node", "--version")
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return process.waitFor(Duration.ofSeconds(3).toMillis(), TimeUnit.MILLISECONDS) && process.exitValue() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Object getNested(Map<String, Object> map, String... path) {
        Object current = map;
        for (String part : path) {
            if (!(current instanceof Map<?, ?> currentMap)) {
                return null;
            }
            current = ((Map<String, Object>) currentMap).get(part);
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> nestedList(Map<String, Object> map, String first, String second) {
        Object value = getNested(map, first, second);
        return (List<Map<String, Object>>) value;
    }
}
