package com.caspar.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmapPlaceSearchService {

    private static final String AMAP_PLACE_TEXT_URL = "https://restapi.amap.com/v3/place/text";
    private static final String AMAP_PLACE_AROUND_URL = "https://restapi.amap.com/v3/place/around";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UstcCampusResolver campusResolver;

    @Value("${amap.web.key}")
    private String amapKey;

    public ResolveResult resolvePlace(String query, String explicitCampus, Double userLat, Double userLng) {
        String keyword = query == null ? "" : query.trim();
        if (keyword.isBlank()) {
            return ResolveResult.failed("缺少地点名称");
        }

        String normalizedExplicitCampus = campusResolver.normalizeCampus(explicitCampus);
        String campusFromText = campusResolver.inferCampusFromText(keyword);
        boolean userSpecifiedCampus = normalizedExplicitCampus != null || campusFromText != null;

        UstcCampusResolver.CampusDecision campusDecision = null;
        String resolvedCampus;
        if (userSpecifiedCampus) {
            campusDecision = campusResolver.decideCampus(
                    normalizedExplicitCampus != null ? normalizedExplicitCampus : campusFromText,
                    userLat,
                    userLng
            );
            if (campusDecision.isClarificationRequired()) {
                return ResolveResult.clarification(campusDecision.getMessage(), campusDecision.getOptions());
            }
            resolvedCampus = campusDecision.getCampus();
        } else {
            resolvedCampus = campusResolver.preferCampus(userLat, userLng);
        }

        List<ResolvedPlace> candidates;
        try {
            candidates = searchPlaces(keyword, resolvedCampus, userLat, userLng, true);
        } catch (IllegalStateException e) {
            if (isAmapKeyException(e)) {
                return ResolveResult.failed("高德地图 Key 配置无效，请检查 AMAP_WEB_KEY / 安全密钥配置");
            }
            throw e;
        }
        if (candidates.isEmpty()) {
            return ResolveResult.failed("未找到中国科大校内相关地点，请补充更具体的楼宇、校区或正式名称");
        }

        ResolvedPlace best = candidates.get(0);
        String reason = campusDecision != null ? campusDecision.getMessage() : null;
        if (candidates.size() > 1) {
            String campusText = best.getCampus() != null ? best.getCampus() : "当前校区";
            reason = "已按距离优先推荐最近的" + campusText + "地点【" + best.getName() + "】，您也可以在候选地点中切换。";
        } else if ((reason == null || reason.isBlank()) && best.getCampus() != null) {
            reason = "已匹配到" + best.getCampus();
        } else if (reason == null || reason.isBlank()) {
            reason = "已优先匹配中国科学技术大学校内地点";
        }
        return ResolveResult.resolved(best, candidates, reason);
    }

    public List<ResolvedPlace> searchPlaces(String query, String campus, Double userLat, Double userLng) {
        return searchPlaces(query, campus, userLat, userLng, true);
    }

    public List<ResolvedPlace> searchPlaces(String query, String campus, Double userLat, Double userLng, boolean preferUstcOnly) {
        try {
            List<ResolvedPlace> results = new ArrayList<>();
            results.addAll(searchByText(query, campus, userLat, userLng));
            if (userLat != null && userLng != null) {
                results.addAll(searchNearby(query, campus, userLat, userLng));
            }
            if (results.isEmpty()) {
                String relaxedQuery = relaxQuery(query);
                if (relaxedQuery != null && !relaxedQuery.equalsIgnoreCase(query == null ? "" : query.trim())) {
                    log.info("地点检索首次为空，使用简化关键词重试: raw='{}', relaxed='{}', campus='{}'",
                            query, relaxedQuery, campus);
                    results.addAll(searchByText(relaxedQuery, campus, userLat, userLng));
                    if (userLat != null && userLng != null) {
                        results.addAll(searchNearby(relaxedQuery, campus, userLat, userLng));
                    }
                }
            }

            Map<String, ResolvedPlace> deduplicated = new java.util.LinkedHashMap<>();
            for (ResolvedPlace place : results) {
                String key = (place.getName() + "|" + place.getLat() + "|" + place.getLng()).toLowerCase();
                deduplicated.putIfAbsent(key, place);
            }
            List<ResolvedPlace> merged = new ArrayList<>(deduplicated.values());
            List<ResolvedPlace> allCandidates = new ArrayList<>(merged);
            if (preferUstcOnly) {
                List<ResolvedPlace> ustcPlaces = merged.stream()
                        .filter(this::isUstcPlace)
                        .toList();
                if (!ustcPlaces.isEmpty()) {
                    merged = new ArrayList<>(ustcPlaces);
                } else if (!allCandidates.isEmpty()) {
                    // 高德地址偶尔不带“科大”关键字，避免被严格过滤后误判“找不到地点”
                    log.warn("USTC过滤后无结果，回退到未过滤候选: query='{}', campus='{}'", query, campus);
                }
            }
            log.info("地点解析候选: query='{}', campus='{}', preferUstcOnly={}, textOrNearbyCount={}, filteredCount={}, candidates={}",
                    query,
                    campus,
                    preferUstcOnly,
                    results.size(),
                    merged.size(),
                    merged.stream().limit(6).map(place -> place.getName() + "@" + (place.getCampus() == null ? "-" : place.getCampus())).toList());
            merged.sort((a, b) -> {
                boolean aUstc = isUstcPlace(a);
                boolean bUstc = isUstcPlace(b);
                if (aUstc != bUstc) {
                    return aUstc ? -1 : 1;
                }
                if (campus != null) {
                    boolean aMatch = campus.equals(a.getCampus());
                    boolean bMatch = campus.equals(b.getCampus());
                    if (aMatch != bMatch) {
                        return aMatch ? -1 : 1;
                    }
                }
                if (a.getDistanceToUser() == null && b.getDistanceToUser() == null) {
                    return 0;
                }
                if (a.getDistanceToUser() == null) {
                    return 1;
                }
                if (b.getDistanceToUser() == null) {
                    return -1;
                }
                return Double.compare(a.getDistanceToUser(), b.getDistanceToUser());
            });
            return merged.stream().limit(6).toList();
        } catch (IllegalStateException e) {
            if (isAmapKeyException(e)) {
                throw e;
            }
            log.error("高德地点解析状态异常", e);
            return List.of();
        } catch (Exception e) {
            log.error("高德地点解析失败", e);
            return List.of();
        }
    }

    private String relaxQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String relaxed = query.trim()
                .replace("中国科学技术大学", "")
                .replace("中国科大", "")
                .replace("USTC", "")
                .replace("ustc", "")
                .trim();
        return relaxed.isBlank() ? null : relaxed;
    }

    private boolean isUstcPlace(ResolvedPlace place) {
        if (place == null) {
            return false;
        }
        return campusResolver.looksLikeUstcPlace(place.getName(), place.getAddress());
    }

    private List<ResolvedPlace> searchByText(String query, String campus, Double userLat, Double userLng) {
        try {
            String keyword = campusResolver.buildSearchKeyword(query, campus);
            log.info("高德文本地点搜索关键词: raw='{}', scoped='{}', campus='{}'", query, keyword, campus);
            String url = AMAP_PLACE_TEXT_URL
                    + "?keywords={keywords}"
                    + "&city={city}"
                    + "&citylimit=true&offset=8&page=1&extensions=base&key={key}";
            String response = restTemplate.getForObject(url, String.class, keyword, "合肥", amapKey);
            JsonNode root = objectMapper.readTree(response);
            if (!"1".equals(root.path("status").asText())) {
                String info = root.path("info").asText();
                if (isAmapKeyErrorCode(info)) {
                    throw new IllegalStateException("AMAP_KEY_INVALID:" + info);
                }
                log.warn("高德地点搜索失败: {}, URL: {}", info, url);
                return List.of();
            }
            return parsePois(root.path("pois"), userLat, userLng);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("高德文本地点搜索失败", e);
            return List.of();
        }
    }

    private List<ResolvedPlace> searchNearby(String query, String campus, Double userLat, Double userLng) {
        try {
            String keyword = campusResolver.buildSearchKeyword(query, campus);
            log.info("高德周边地点搜索关键词: raw='{}', scoped='{}', campus='{}'", query, keyword, campus);
            String url = AMAP_PLACE_AROUND_URL
                    + "?location={location}"
                    + "&keywords={keywords}"
                    + "&radius=5000&sortrule=distance&offset=10&page=1&extensions=base&key={key}";
            String location = userLng + "," + userLat;
            String response = restTemplate.getForObject(url, String.class, location, keyword, amapKey);
            JsonNode root = objectMapper.readTree(response);
            if (!"1".equals(root.path("status").asText())) {
                String info = root.path("info").asText();
                if (isAmapKeyErrorCode(info)) {
                    throw new IllegalStateException("AMAP_KEY_INVALID:" + info);
                }
                log.warn("高德周边地点搜索失败: {}, URL: {}", info, url);
                return List.of();
            }
            return parsePois(root.path("pois"), userLat, userLng);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("高德周边地点搜索失败: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean isAmapKeyErrorCode(String info) {
        if (info == null) {
            return false;
        }
        return "INVALID_USER_KEY".equals(info)
                || "INVALID_USER_SCODE".equals(info)
                || "USERKEY_PLAT_NOMATCH".equals(info)
                || "SERVICE_NOT_AVAILABLE".equals(info);
    }

    private boolean isAmapKeyException(IllegalStateException e) {
        return e != null
                && e.getMessage() != null
                && e.getMessage().startsWith("AMAP_KEY_INVALID:");
    }

    private List<ResolvedPlace> parsePois(JsonNode pois, Double userLat, Double userLng) {
        List<ResolvedPlace> results = new ArrayList<>();
        if (!pois.isArray()) {
            return results;
        }
        for (JsonNode poi : pois) {
            String location = poi.path("location").asText("");
            if (location.isBlank() || !location.contains(",")) {
                continue;
            }
            String[] parts = location.split(",");
            double lng = Double.parseDouble(parts[0]);
            double lat = Double.parseDouble(parts[1]);
            String address = poi.path("address").asText("");
            String name = poi.path("name").asText("");
            boolean explicitUstcPlace = campusResolver.looksLikeUstcPlace(name, address);
            String detectedCampus = campusResolver.detectCampusFromAddress(address)
                    .orElseGet(() -> Optional.ofNullable(campusResolver.inferCampusFromPlaceText(name)).orElse(null));
            if (detectedCampus == null && explicitUstcPlace) {
                detectedCampus = campusResolver.preferCampus(lat, lng);
            }
            Double distanceToUser = userLat != null && userLng != null
                    ? campusResolver.distanceMeters(userLat, userLng, lat, lng)
                    : null;
            results.add(new ResolvedPlace(
                    poi.path("id").asText(""),
                    name,
                    address,
                    lat,
                    lng,
                    detectedCampus,
                    "AMAP",
                    distanceToUser
            ));
        }
        return results;
    }

    @Getter
    public static class ResolveResult {
        private final boolean success;
        private final boolean clarificationRequired;
        private final String message;
        private final List<String> clarificationOptions;
        private final ResolvedPlace place;
        private final List<ResolvedPlace> candidates;

        private ResolveResult(boolean success, boolean clarificationRequired, String message,
                              List<String> clarificationOptions, ResolvedPlace place, List<ResolvedPlace> candidates) {
            this.success = success;
            this.clarificationRequired = clarificationRequired;
            this.message = message;
            this.clarificationOptions = clarificationOptions;
            this.place = place;
            this.candidates = candidates;
        }

        public static ResolveResult resolved(ResolvedPlace place, List<ResolvedPlace> candidates, String message) {
            return new ResolveResult(true, false, message, List.of(), place, candidates);
        }

        public static ResolveResult clarification(String message, List<String> options) {
            return new ResolveResult(false, true, message, options, null, List.of());
        }

        public static ResolveResult failed(String message) {
            return new ResolveResult(false, false, message, List.of(), null, List.of());
        }
    }

    @Getter
    public static class ResolvedPlace {
        private final String id;
        private final String name;
        private final String address;
        private final double lat;
        private final double lng;
        private final String campus;
        private final String source;
        private final Double distanceToUser;

        public ResolvedPlace(String id, String name, String address, double lat, double lng,
                             String campus, String source, Double distanceToUser) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.lat = lat;
            this.lng = lng;
            this.campus = campus;
            this.source = source;
            this.distanceToUser = distanceToUser;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("id", id);
            map.put("name", name);
            map.put("address", address);
            map.put("lat", lat);
            map.put("lng", lng);
            map.put("campus", campus);
            map.put("source", source);
            map.put("distanceToUser", distanceToUser);
            return map;
        }
    }
}
