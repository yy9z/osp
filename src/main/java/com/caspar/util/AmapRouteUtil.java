package com.caspar.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

@Component
public class AmapRouteUtil {

    private static final Logger logger = LoggerFactory.getLogger(AmapRouteUtil.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${amap.web.key}")
    private String amapKey;

    @Value("${amap.web.security.code:}")
    private String securityCode;

    private static final String AMAP_WALKING_V5_URL = "https://restapi.amap.com/v5/direction/walking";
    private static final String AMAP_WALKING_V3_URL = "https://restapi.amap.com/v3/direction/walking";
    private static final String WALKING_DECISION_SHOW_FIELDS = "cost,navi,polyline";
    private static final String WALKING_IS_INDOOR = "0";
    private static final String WALKING_OUTPUT = "JSON";
    private static final int WALKING_ALTERNATIVE_ROUTE_COUNT = 3;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 决策用路线：优先保留 v5 多候选，即使暂时没有几何也返回给上层做路线选择；
     * 只有 v5 调用失败时才回退到 v3。
     */
    public Map<String, Object> walkingRouteForDecision(double originLng, double originLat, double destinationLng, double destinationLat) {
        Map<String, Object> v5Result = requestWalkingRouteV5(originLng, originLat, destinationLng, destinationLat);
        if (Boolean.TRUE.equals(v5Result.get("success"))) {
            return v5Result;
        }

        logger.warn("高德步行路线 v5 决策请求失败，回退到 v3。原因: {}", v5Result.getOrDefault("error", "v5 请求失败"));
        return requestWalkingRouteV3(originLng, originLat, destinationLng, destinationLat);
    }

    /**
     * 几何用路线：固定使用 v3，提供稳定的可绘制轨迹。
     */
    public Map<String, Object> walkingGeometryRouteV3(double originLng, double originLat, double destinationLng, double destinationLat) {
        return requestWalkingRouteV3(originLng, originLat, destinationLng, destinationLat);
    }

    public Map<String, Object> walkingRoute(double originLng, double originLat, double destinationLng, double destinationLat) {
        return requestWalkingRouteV3(originLng, originLat, destinationLng, destinationLat);
    }

    /**
     * 解析单条路线
     */
    private Map<String, Object> parseSinglePath(JsonNode pathNode, int pathIndex, boolean requireGeometryDiagnostics) {
        Map<String, Object> pathInfo = new LinkedHashMap<>();
        int distance = readInt(pathNode, "distance");
        int duration = readInt(pathNode, "duration");
        if (duration == 0) {
            duration = readInt(pathNode.path("cost"), "duration");
        }

        pathInfo.put("index", pathIndex);
        pathInfo.put("distance", distance);
        pathInfo.put("duration", duration);

        // 解析步骤
        List<Map<String, Object>> stepList = new ArrayList<>();
        List<List<Double>> pathPoints = new ArrayList<>();
        for (JsonNode stepNode : extractStepNodes(pathNode.path("steps"))) {
            Map<String, Object> stepInfo = new HashMap<>();
            String instruction = firstNonBlank(
                    stepNode.path("instruction").asText(""),
                    buildInstructionFromRoad(stepNode.path("road").asText(""), stepNode.path("road_name").asText(""))
            );
            int stepDistance = readInt(stepNode, "distance");
            if (stepDistance == 0) {
                stepDistance = readInt(stepNode, "step_distance");
            }
            int stepDuration = readInt(stepNode, "duration");
            if (stepDuration == 0) {
                stepDuration = readInt(stepNode.path("cost"), "duration");
            }

            stepInfo.put("instruction", instruction);
            stepInfo.put("distance", stepDistance);
            stepInfo.put("duration", stepDuration);
            stepList.add(stepInfo);

            appendPolyline(pathPoints, extractPolyline(stepNode));
        }

        if (requireGeometryDiagnostics && pathPoints.isEmpty()) {
            appendPolyline(pathPoints, extractPathLevelPolyline(pathNode));
            if (pathPoints.isEmpty()) {
                logger.warn("路线 {} 未解析到轨迹点，distance={}, duration={}, pathNodeKeys={}",
                        pathIndex, distance, duration, collectFieldNames(pathNode));
                logFirstStepGeometryDiagnostics(pathNode, pathIndex);
            }
        }

        pathInfo.put("steps", stepList);
        pathInfo.put("path", pathPoints);

        // 生成路线摘要（用于LLM评估）
        String pathSummary = generatePathSummary(stepList, distance, duration);
        pathInfo.put("summary", pathSummary);

        logger.debug("路线 {} 解析完成 - 距离: {} 米, 时间: {} 秒, 步骤: {} 个",
            pathIndex, distance, duration, stepList.size());

        return pathInfo;
    }

    private Map<String, Object> requestWalkingRouteV5(double originLng, double originLat, double destinationLng, double destinationLat) {
        Map<String, String> params = new TreeMap<>();
        params.put("origin", originLng + "," + originLat);
        params.put("destination", destinationLng + "," + destinationLat);
        params.put("alternative_route", String.valueOf(WALKING_ALTERNATIVE_ROUTE_COUNT));
        params.put("show_fields", WALKING_DECISION_SHOW_FIELDS);
        params.put("isindoor", WALKING_IS_INDOOR);
        params.put("output", WALKING_OUTPUT);
        params.put("key", amapKey);
        return requestWalkingRoute(AMAP_WALKING_V5_URL, params, "v5", true);
    }

    private Map<String, Object> requestWalkingRouteV3(double originLng, double originLat, double destinationLng, double destinationLat) {
        Map<String, String> params = new TreeMap<>();
        params.put("origin", originLng + "," + originLat);
        params.put("destination", destinationLng + "," + destinationLat);
        params.put("key", amapKey);
        return requestWalkingRoute(AMAP_WALKING_V3_URL, params, "v3", true);
    }

    private Map<String, Object> requestWalkingRoute(String apiUrl, Map<String, String> params, String apiVersion, boolean requireGeometry) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (securityCode != null && !securityCode.isEmpty()) {
                params.put("sig", generateSignature(params, securityCode));
                logger.info("高德{}路径规划请求参数（含签名）: {}", apiVersion, params);
            } else {
                logger.info("高德{}路径规划请求参数: {}", apiVersion, params);
            }

            String url = buildUrl(apiUrl, params);
            String response = restTemplate.getForObject(url, String.class);
            logger.info("高德{}路径规划响应: {}", apiVersion, response);

            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode routeNode = rootNode.path("route");
            List<JsonNode> pathNodes = extractPathNodes(routeNode.path("paths"));
            if (pathNodes.isEmpty()) {
                String status = rootNode.path("status").asText();
                String info = rootNode.path("info").asText();
                String infocode = rootNode.path("infocode").asText();
                logger.error("高德{}路径规划失败 - status: {}, info: {}, infocode: {}", apiVersion, status, info, infocode);
                result.put("error", info.isEmpty() ? "高德API返回的paths为空" : info);
                result.put("infocode", infocode);
                result.put("success", false);
                return result;
            }

            List<Map<String, Object>> allPaths = new ArrayList<>();
            for (int pathIndex = 0; pathIndex < pathNodes.size(); pathIndex++) {
                allPaths.add(parseSinglePath(pathNodes.get(pathIndex), pathIndex, requireGeometry));
            }

            boolean hasAnyGeometry = allPaths.stream().anyMatch(this::pathHasGeometry);
            if (requireGeometry && !hasAnyGeometry) {
                String status = rootNode.path("status").asText();
                String info = rootNode.path("info").asText();
                logger.warn("高德{}路径规划返回成功，但未携带可绘制轨迹。status={}, info={}, show_fields={}, firstPathKeys={}, firstStepKeys={}",
                        apiVersion,
                        status,
                        info,
                        params.get("show_fields"),
                        allPaths.isEmpty() ? List.of() : allPaths.get(0).keySet(),
                        firstStepFieldNames(pathNodes.isEmpty() ? null : pathNodes.get(0)));
                result.put("error", "高德返回成功，但未携带可绘制轨迹");
                result.put("success", true);
                result.put("paths", allPaths);
                result.put("pathCount", allPaths.size());
                result.put("distance", allPaths.isEmpty() ? 0 : allPaths.get(0).get("distance"));
                result.put("duration", allPaths.isEmpty() ? 0 : allPaths.get(0).get("duration"));
                result.put("steps", allPaths.isEmpty() ? List.of() : allPaths.get(0).get("steps"));
                result.put("path", allPaths.isEmpty() ? List.of() : allPaths.get(0).get("path"));
                result.put("amapApiVersion", apiVersion);
                result.put("routingStrategy", apiVersion.equals("v5")
                        ? "alternative_route=" + WALKING_ALTERNATIVE_ROUTE_COUNT
                        : "amap_default");
                return result;
            }

            Map<String, Object> firstPath = allPaths.get(0);
            result.put("paths", allPaths);
            result.put("pathCount", allPaths.size());
            result.put("distance", firstPath.get("distance"));
            result.put("duration", firstPath.get("duration"));
            result.put("steps", firstPath.get("steps"));
            result.put("path", firstPath.get("path"));
            result.put("success", true);
            result.put("amapApiVersion", apiVersion);
            result.put("routingStrategy", apiVersion.equals("v5")
                    ? "alternative_route=" + WALKING_ALTERNATIVE_ROUTE_COUNT
                    : "amap_default");

            if ("v5".equals(apiVersion)) {
                logger.info("高德{}路径规划成功 - 显式请求{}条候选路线，实际返回{}条", apiVersion, WALKING_ALTERNATIVE_ROUTE_COUNT, allPaths.size());
            } else {
                logger.info("高德{}路径规划成功 - 共 {} 条路线", apiVersion, allPaths.size());
            }
            return result;
        } catch (Exception e) {
            logger.error("调用高德{}路径规划API异常: ", apiVersion, e);
            result.put("error", e.getMessage());
            result.put("success", false);
            return result;
        }
    }

    /**
     * 生成路线摘要（供LLM评估）
     */
    private String generatePathSummary(List<Map<String, Object>> steps, int distance, int duration) {
        StringBuilder summary = new StringBuilder();
        summary.append("距离").append(distance).append("米，");
        summary.append("预计").append(duration / 60).append("分钟。");

        // 提取关键道路信息
        Set<String> roadKeywords = new LinkedHashSet<>();
        for (Map<String, Object> step : steps) {
            String instruction = (String) step.get("instruction");
            if (instruction != null) {
                // 提取道路名称和关键词
                if (instruction.contains("楼梯") || instruction.contains("台阶")) {
                    roadKeywords.add("有台阶");
                }
                if (instruction.contains("小路") || instruction.contains("小径")) {
                    roadKeywords.add("经过小路");
                }
                if (instruction.contains("主干道") || instruction.contains("大道")) {
                    roadKeywords.add("主干道");
                }
            }
        }

        if (!roadKeywords.isEmpty()) {
            summary.append("特点：").append(String.join("、", roadKeywords));
        }

        return summary.toString();
    }

    private String buildUrl(String apiUrl, Map<String, String> params) {
        StringBuilder urlBuilder = new StringBuilder(apiUrl).append("?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            urlBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        return urlBuilder.toString().replaceAll("&$", "");
    }

    private List<JsonNode> extractPathNodes(JsonNode pathsNode) {
        List<JsonNode> pathNodes = new ArrayList<>();
        if (pathsNode == null || pathsNode.isMissingNode() || pathsNode.isNull()) {
            return pathNodes;
        }
        if (pathsNode.isArray()) {
            pathsNode.forEach(pathNodes::add);
            return pathNodes;
        }
        if (pathsNode.isObject()) {
            pathNodes.add(pathsNode);
        }
        return pathNodes;
    }

    private List<JsonNode> extractStepNodes(JsonNode stepsNode) {
        List<JsonNode> stepNodes = new ArrayList<>();
        if (stepsNode == null || stepsNode.isMissingNode() || stepsNode.isNull()) {
            return stepNodes;
        }
        if (stepsNode.isArray()) {
            stepsNode.forEach(stepNodes::add);
            return stepNodes;
        }
        if (stepsNode.isObject()) {
            stepNodes.add(stepsNode);
        }
        return stepNodes;
    }

    private void appendPolyline(List<List<Double>> pathPoints, String polyline) {
        if (polyline == null || polyline.isBlank()) {
            return;
        }
        String[] pointStrs = polyline.split(";");
        for (String pointStr : pointStrs) {
            String[] coords = pointStr.split(",");
            if (coords.length != 2) {
                continue;
            }
            try {
                double lng = Double.parseDouble(coords[0]);
                double lat = Double.parseDouble(coords[1]);
                pathPoints.add(List.of(lat, lng));
            } catch (NumberFormatException e) {
                logger.warn("解析坐标失败: {}", pointStr);
            }
        }
    }

    private String extractPolyline(JsonNode stepNode) {
        return firstNonBlank(
                stepNode.path("polyline").asText(""),
                stepNode.path("navi").path("polyline").asText(""),
                stepNode.path("cost").path("navi").path("polyline").asText(""),
                findNestedText(stepNode.path("navi"), "polyline"),
                findNestedText(stepNode.path("cost"), "polyline")
        );
    }

    private String extractPathLevelPolyline(JsonNode pathNode) {
        return firstNonBlank(
                pathNode.path("polyline").asText(""),
                pathNode.path("navi").path("polyline").asText(""),
                pathNode.path("cost").path("polyline").asText(""),
                pathNode.path("cost").path("navi").path("polyline").asText(""),
                findNestedText(pathNode.path("navi"), "polyline"),
                findNestedText(pathNode.path("cost"), "polyline")
        );
    }

    private String findNestedText(JsonNode node, String targetField) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isObject()) {
            JsonNode direct = node.get(targetField);
            if (direct != null && direct.isTextual() && !direct.asText("").isBlank()) {
                return direct.asText("");
            }
            var fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String nested = findNestedText(entry.getValue(), targetField);
                if (!nested.isBlank()) {
                    return nested;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                String nested = findNestedText(item, targetField);
                if (!nested.isBlank()) {
                    return nested;
                }
            }
        }
        return "";
    }

    private int readInt(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return 0;
        }
        JsonNode field = node.path(fieldName);
        if (field.isNumber()) {
            return field.asInt();
        }
        if (field.isTextual()) {
            try {
                return Integer.parseInt(field.asText(""));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return "";
    }

    private String buildInstructionFromRoad(String road, String roadName) {
        String resolvedRoad = firstNonBlank(roadName, road);
        return resolvedRoad.isBlank() ? "" : "沿" + resolvedRoad + "步行";
    }

    private List<String> collectFieldNames(JsonNode node) {
        List<String> fieldNames = new ArrayList<>();
        if (node == null || !node.isObject()) {
            return fieldNames;
        }
        node.fieldNames().forEachRemaining(fieldNames::add);
        return fieldNames;
    }

    private boolean pathHasGeometry(Map<String, Object> pathInfo) {
        if (pathInfo == null) {
            return false;
        }
        return castPathList(pathInfo.get("path")).size() > 1;
    }

    private List<String> firstStepFieldNames(JsonNode pathNode) {
        if (pathNode == null || pathNode.isMissingNode() || pathNode.isNull()) {
            return List.of();
        }
        List<JsonNode> stepNodes = extractStepNodes(pathNode.path("steps"));
        if (stepNodes.isEmpty()) {
            return List.of();
        }
        return collectFieldNames(stepNodes.get(0));
    }

    private void logFirstStepGeometryDiagnostics(JsonNode pathNode, int pathIndex) {
        if (pathNode == null || pathNode.isMissingNode() || pathNode.isNull()) {
            return;
        }
        List<JsonNode> stepNodes = extractStepNodes(pathNode.path("steps"));
        if (stepNodes.isEmpty()) {
            logger.warn("路线 {} 无 steps，无法进一步诊断轨迹字段", pathIndex);
            return;
        }
        JsonNode firstStep = stepNodes.get(0);
        String extractedPolyline = extractPolyline(firstStep);
        logger.warn("路线 {} 首步轨迹诊断: extractedPolylineLength={}, stepPolyline='{}', stepNavi={}, stepCost={}, pathLevelPolylineLength={}",
                pathIndex,
                extractedPolyline == null ? 0 : extractedPolyline.length(),
                abbreviate(firstNonBlank(firstStep.path("polyline").asText(""), extractedPolyline), 120),
                abbreviate(firstStep.path("navi").toString(), 300),
                abbreviate(firstStep.path("cost").toString(), 300),
                extractPathLevelPolyline(pathNode).length());
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castMapList(Object raw) {
        if (raw instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> converted = new LinkedHashMap<>();
                    map.forEach((key, value) -> converted.put(String.valueOf(key), value));
                    result.add(converted);
                }
            }
            return result;
        }
        return List.of();
    }

    public Map<String, Object> walkingRouteWithWaypoints(List<RoutePoint> itinerary) {
        Map<String, Object> result = new HashMap<>();
        if (itinerary == null || itinerary.size() < 2) {
            result.put("success", false);
            result.put("error", "多段路线至少需要两个地点");
            return result;
        }

        int totalDistance = 0;
        int totalDuration = 0;
        List<Map<String, Object>> mergedSteps = new ArrayList<>();
        List<List<Double>> mergedPath = new ArrayList<>();
        List<Map<String, Object>> segments = new ArrayList<>();

        for (int i = 0; i < itinerary.size() - 1; i++) {
            RoutePoint from = itinerary.get(i);
            RoutePoint to = itinerary.get(i + 1);

            Map<String, Object> segmentRoute = walkingRoute(from.lng(), from.lat(), to.lng(), to.lat());
            if (!Boolean.TRUE.equals(segmentRoute.get("success"))) {
                result.put("success", false);
                result.put("error", segmentRoute.getOrDefault("error", "多段路线规划失败"));
                result.put("failedSegmentIndex", i);
                result.put("failedFrom", from.name());
                result.put("failedTo", to.name());
                return result;
            }

            int distance = ((Number) segmentRoute.getOrDefault("distance", 0)).intValue();
            int duration = ((Number) segmentRoute.getOrDefault("duration", 0)).intValue();
            totalDistance += distance;
            totalDuration += duration;

            List<Map<String, Object>> segmentSteps = castStepList(segmentRoute.get("steps"));
            List<List<Double>> segmentPath = castPathList(segmentRoute.get("path"));

            mergedSteps.addAll(segmentSteps);
            appendPathPoints(mergedPath, segmentPath);

            Map<String, Object> segmentInfo = new LinkedHashMap<>();
            segmentInfo.put("index", i);
            segmentInfo.put("from", toPointMap(from));
            segmentInfo.put("to", toPointMap(to));
            segmentInfo.put("distance", distance);
            segmentInfo.put("duration", duration);
            segmentInfo.put("steps", segmentSteps);
            segmentInfo.put("path", segmentPath);
            segments.add(segmentInfo);
        }

        result.put("success", true);
        result.put("distance", totalDistance);
        result.put("duration", totalDuration);
        result.put("steps", mergedSteps);
        result.put("path", mergedPath);
        result.put("segments", segments);
        return result;
    }

    private List<Map<String, Object>> castStepList(Object raw) {
        if (raw instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> converted = new LinkedHashMap<>();
                    map.forEach((key, value) -> converted.put(String.valueOf(key), value));
                    result.add(converted);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    private List<List<Double>> castPathList(Object raw) {
        if (raw instanceof List<?> list) {
            List<List<Double>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof List<?> point && point.size() >= 2) {
                    result.add(List.of(toDouble(point.get(0)), toDouble(point.get(1))));
                } else if (item instanceof double[] point && point.length >= 2) {
                    result.add(List.of(point[0], point[1]));
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    private void appendPathPoints(List<List<Double>> target, List<List<Double>> source) {
        for (List<Double> point : source) {
            if (!target.isEmpty() && !point.isEmpty()) {
                List<Double> last = target.get(target.size() - 1);
                if (last.size() >= 2 && point.size() >= 2
                    && Objects.equals(last.get(0), point.get(0))
                    && Objects.equals(last.get(1), point.get(1))) {
                    continue;
                }
            }
            target.add(point);
        }
    }

    private Map<String, Object> toPointMap(RoutePoint point) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", point.name());
        map.put("lat", point.lat());
        map.put("lng", point.lng());
        return map;
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    public record RoutePoint(String name, double lat, double lng) {}

    /**
     * 生成高德API数字签名
     * 签名算法：将所有参数（不含sig）按key的字母顺序排序，拼接成key1=value1&key2=value2格式，
     * 然后在末尾追加私钥，对结果进行MD5加密
     */
    private String generateSignature(Map<String, String> params, String securityCode) {
        try {
            // 按key的字母顺序排序
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!"sig".equals(entry.getKey())) {
                    sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }
            }
            // 追加私钥
            sb.append(securityCode);

            // MD5加密
            String str = sb.toString();
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(str.getBytes(StandardCharsets.UTF_8));

            // 转换为16进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            logger.debug("生成签名 - 原始字符串: {}, 签名: {}", sb.toString(), hexString.toString());
            return hexString.toString();
        } catch (Exception e) {
            logger.error("生成签名失败: ", e);
            return "";
        }
    }
}
