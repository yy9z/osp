package com.caspar.agent.service;

import com.caspar.agent.model.NavigationSegmentSlots;
import com.caspar.agent.model.NavigationSlots;
import com.caspar.agent.model.NavigationTaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 分段分析服务：负责将多段路线转换为分段对象并附加LLM分析结果。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SegmentAnalysisService {

    private final RouteAnalysisService routeAnalysisService;
    private final GeometryAssemblerService geometryAssemblerService;

    public List<NavigationTaskResponse.RouteSegment> buildRouteSegmentsWithAnalysis(
            List<Map<String, Object>> segments,
            NavigationSlots slots,
            List<NavigationSegmentSlots> segmentSlotProfiles) {
        if (segments == null || segments.isEmpty()) {
            return List.of();
        }

        List<NavigationTaskResponse.RouteSegment> result = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            Map<String, Object> segment = segments.get(i);

            String fromName = "起点";
            String toName = "终点";
            Object fromObj = segment.get("from");
            Object toObj = segment.get("to");
            if (fromObj instanceof Map<?, ?> fromMap) {
                Object nameObj = fromMap.get("name");
                fromName = nameObj != null ? String.valueOf(nameObj) : "起点";
            }
            if (toObj instanceof Map<?, ?> toMap) {
                Object nameObj = toMap.get("name");
                toName = nameObj != null ? String.valueOf(nameObj) : "终点";
            }

            NavigationSegmentSlots segmentSlots = segmentSlotProfiles != null && segmentSlotProfiles.size() > i
                    ? segmentSlotProfiles.get(i)
                    : null;

            NavigationTaskResponse.RouteSegment.RouteSegmentBuilder builder = NavigationTaskResponse.RouteSegment.builder()
                    .index(getIntValue(segment.get("index"), i))
                    .from(toPlaceInfo(fromObj))
                    .to(toPlaceInfo(toObj))
                    .distance(getIntValue(segment.get("distance"), 0))
                    .duration(getIntValue(segment.get("duration"), 0))
                    .steps(geometryAssemblerService.castStepList(segment.get("steps")))
                    .path(geometryAssemblerService.castPathList(segment.get("path")))
                    .geometrySource(segment.get("geometrySource") != null ? String.valueOf(segment.get("geometrySource")) : null)
                    .selectedRouteIndex(getNullableIntValue(segment.get("selectedRouteIndex")))
                    .totalRoutes(getNullableIntValue(segment.get("totalRoutes")))
                    .selectionReason(segment.get("selectionReason") != null ? String.valueOf(segment.get("selectionReason")) : null)
                    .alternativeRoutes(geometryAssemblerService.castStepList(segment.get("alternativeRoutes")))
                    .slotProfile(toSegmentSlotProfile(segmentSlots));

            try {
                RouteAnalysisService.SegmentAnalysis segmentAnalysis = segmentSlots != null
                        ? routeAnalysisService.analyzeSegment(segment, fromName, toName, i, segmentSlots)
                        : routeAnalysisService.analyzeSegment(segment, fromName, toName, i, slots);

                if (segmentAnalysis != null) {
                    builder.analysis(NavigationTaskResponse.SegmentAnalysis.builder()
                            .score(segmentAnalysis.getScore())
                            .reason(segmentAnalysis.getReason())
                            .warnings(segmentAnalysis.getWarnings())
                            .suggestions(segmentAnalysis.getSuggestions())
                            .build());

                    log.info("段{}评估完成: score={}, reason={}", i, segmentAnalysis.getScore(), segmentAnalysis.getReason());
                }
            } catch (Exception e) {
                log.warn("段{}评估失败: {}", i, e.getMessage());
            }

            result.add(builder.build());
        }

        return result;
    }

    private NavigationTaskResponse.SegmentSlotProfile toSegmentSlotProfile(NavigationSegmentSlots segmentSlots) {
        if (segmentSlots == null) {
            return null;
        }
        return NavigationTaskResponse.SegmentSlotProfile.builder()
                .index(segmentSlots.getIndex())
                .destination(segmentSlots.getDestination())
                .segmentGoal(segmentSlots.getSegmentGoal())
                .taskScene(segmentSlots.getTaskScene())
                .preferences(segmentSlots.getPreferences())
                .constraints(segmentSlots.getConstraints())
                .timeContext(segmentSlots.getTimeContext())
                .timeSlot(segmentSlots.getTimeSlot())
                .urgencyMinutes(segmentSlots.getUrgencyMinutes())
                .notes(segmentSlots.getNotes())
                .build();
    }

    private NavigationTaskResponse.PlaceInfo toPlaceInfo(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }
        return NavigationTaskResponse.PlaceInfo.builder()
                .name(asString(map.get("name")))
                .lat(asDouble(map.get("lat")))
                .lng(asDouble(map.get("lng")))
                .source(asString(map.get("source")))
                .address(asString(map.get("address")))
                .build();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return null;
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private int getIntValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Integer getNullableIntValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
