package com.caspar.agent.service;

import com.caspar.agent.model.NavigationSlots;
import com.caspar.agent.model.ToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 地点解析服务：负责校区语义增强、地点解析与澄清响应构建。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceResolverService {

    private final AmapPlaceSearchService placeSearchService;
    private final UstcCampusResolver campusResolver;
    private final CampusPoiDictionary poiDictionary;

    /**
     * 校园语义增强
     * 注意：途经点不做标准化，保留用户原始描述
     */
    public void enhanceWithCampusSemantics(NavigationSlots slots, Map<String, Object> params) {
        String destination = slots.getDestination();
        if (destination != null) {
            CampusPoiDictionary.PoiMatch match = poiDictionary.matchPlace(destination);
            if (match != null) {
                slots.setDestinationType(match.category());
            }
        }
    }

    /**
     * 当用户没有明确指定校区时，根据定位自动推断。
     */
    public void inferCampusIfMissing(NavigationSlots slots) {
        if (slots.getCampus() != null && !slots.getCampus().isBlank()) {
            log.info("校区已指定: {}", slots.getCampus());
            return;
        }

        log.info("校区为空，检查是否可以自动推断...");
        if (slots.getUserLat() == null || slots.getUserLng() == null) {
            log.info("无法推断校区：缺少用户位置信息");
            return;
        }

        log.info("有用户位置，开始自动推断校区...");
        UstcCampusResolver.CampusDecision campusDecision = campusResolver.decideCampus(
                null, slots.getUserLat(), slots.getUserLng());
        if (!campusDecision.isClarificationRequired()) {
            slots.setCampus(campusDecision.getCampus());
            log.info("自动推断校区: {} (原因: {})", campusDecision.getCampus(), campusDecision.getMessage());
        } else {
            log.info("校区推断需要澄清: {}", campusDecision.getMessage());
        }
    }

    public ResolvedNavigation resolveNavigationPlaces(NavigationSlots slots) {
        AmapPlaceSearchService.ResolvedPlace originPlace = null;
        if (isCurrentLocationOrigin(slots)) {
            if (slots.getUserLat() != null && slots.getUserLng() != null) {
                originPlace = buildCurrentLocationPlace(slots);
            }
        } else if (slots.getOrigin() != null && !slots.getOrigin().isBlank()) {
            String originCampusHint = inferCampusHint(slots.getOrigin(), slots.getCampus());
            AmapPlaceSearchService.ResolveResult originResult = resolvePlaceWithFallback(
                    slots.getOrigin(),
                    originCampusHint,
                    slots.getUserLat(),
                    slots.getUserLng()
            );
            if (originResult.isClarificationRequired()) {
                return new ResolvedNavigation(null, null, List.of(), originResult, List.of(), originResult.getMessage());
            }
            if (!originResult.isSuccess() || originResult.getPlace() == null) {
                return new ResolvedNavigation(null, null, List.of(), AmapPlaceSearchService.ResolveResult.failed(originResult.getMessage()), List.of(), originResult.getMessage());
            }
            originPlace = originResult.getPlace();
        }

        String destinationCampusHint = inferCampusHint(slots.getDestination(), slots.getCampus());
        AmapPlaceSearchService.ResolveResult destResult = resolvePlaceWithFallback(
                slots.getDestination(),
                destinationCampusHint,
                slots.getUserLat(),
                slots.getUserLng()
        );
        if (destResult.isClarificationRequired()) {
            return new ResolvedNavigation(originPlace, null, List.of(), destResult, List.of(), destResult.getMessage());
        }
        if (!destResult.isSuccess() || destResult.getPlace() == null) {
            return new ResolvedNavigation(originPlace, null, List.of(), null, List.of(), destResult.getMessage());
        }

        List<AmapPlaceSearchService.ResolvedPlace> waypointPlaces = new java.util.ArrayList<>();
        if (slots.getWaypoints() != null) {
            for (String waypoint : slots.getWaypoints()) {
                String waypointCampusHint = inferCampusHint(waypoint, slots.getCampus());
                AmapPlaceSearchService.ResolveResult waypointResult = resolvePlaceWithFallback(
                        waypoint,
                        waypointCampusHint,
                        slots.getUserLat(),
                        slots.getUserLng()
                );
                if (waypointResult.isClarificationRequired()) {
                    return new ResolvedNavigation(originPlace, null, List.of(), waypointResult, List.of(), waypointResult.getMessage());
                }
                if (!waypointResult.isSuccess() || waypointResult.getPlace() == null) {
                    return new ResolvedNavigation(originPlace, null, List.of(), AmapPlaceSearchService.ResolveResult.failed(waypointResult.getMessage()), List.of(), waypointResult.getMessage());
                }
                waypointPlaces.add(waypointResult.getPlace());
            }
        }

        return new ResolvedNavigation(originPlace, destResult.getPlace(), waypointPlaces, null, destResult.getCandidates(), destResult.getMessage());
    }

    public ToolResult buildClarificationResponse(NavigationSlots slots, AmapPlaceSearchService.ResolveResult result) {
        Map<String, Object> data = new HashMap<>();
        data.put("destinationText", slots.getDestination());
        data.put("originText", slots.getOrigin());
        data.put("mode", slots.getTravelMode());
        data.put("campus", slots.getCampus());
        data.put("taskType", slots.getTaskScene());
        data.put("preferences", slots.getPreferences());
        data.put("timeContext", slots.getTimeContext());
        data.put("timeSlot", slots.getTimeSlot());
        data.put("urgencyMinutes", slots.getUrgencyMinutes());
        data.put("clarificationRequired", true);
        data.put("clarificationType", "campus_disambiguation");
        data.put("clarificationOptions", result.getClarificationOptions());
        data.put("agentSummary", result.getMessage());
        if (slots.getWaypoints() != null && !slots.getWaypoints().isEmpty()) {
            data.put("waypoints", slots.getWaypoints().stream().map(name -> Map.of("name", name)).toList());
            data.put("routeType", "multi_waypoint");
        }
        return ToolResult.ok(result.getMessage(), data);
    }

    private String inferCampusHint(String placeText, String fallbackCampus) {
        String fromText = campusResolver.inferCampusFromText(placeText);
        return (fromText != null && !fromText.isBlank()) ? fromText : fallbackCampus;
    }

    private AmapPlaceSearchService.ResolveResult resolvePlaceWithFallback(String query,
                                                                          String campusHint,
                                                                          Double userLat,
                                                                          Double userLng) {
        AmapPlaceSearchService.ResolveResult first = placeSearchService.resolvePlace(query, campusHint, userLat, userLng);
        if (first.isSuccess() || first.isClarificationRequired()) {
            return first;
        }
        if (campusHint != null && !campusHint.isBlank() && campusResolver.inferCampusFromText(query) == null) {
            AmapPlaceSearchService.ResolveResult second = placeSearchService.resolvePlace(query, null, userLat, userLng);
            if (second.isSuccess() || second.isClarificationRequired()) {
                return second;
            }
        }
        return first;
    }

    private boolean isCurrentLocationOrigin(NavigationSlots slots) {
        String origin = slots.getOrigin();
        return origin == null || origin.isBlank() || "当前位置".equals(origin) || "我的位置".equals(origin);
    }

    private AmapPlaceSearchService.ResolvedPlace buildCurrentLocationPlace(NavigationSlots slots) {
        return new AmapPlaceSearchService.ResolvedPlace(
                "current_location",
                "当前位置",
                null,
                slots.getUserLat(),
                slots.getUserLng(),
                slots.getCampus(),
                "device_location",
                0d
        );
    }
}
