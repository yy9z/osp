package com.caspar.agent.service;

import com.caspar.util.AmapRouteUtil;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 路径几何与结构化数据拼装服务。
 */
@Service
public class GeometryAssemblerService {

    public static final String GEOMETRY_SOURCE_AMAP_V5 = "amap_v5";
    public static final String GEOMETRY_SOURCE_AMAP_V3 = "amap_v3";
    public static final String GEOMETRY_SOURCE_MIXED = "mixed_amap_v5_v3";
    public static final String GEOMETRY_SOURCE_UNAVAILABLE = "unavailable";

    public String geometrySourceFromApiVersion(String apiVersion) {
        return "v3".equalsIgnoreCase(apiVersion)
                ? GEOMETRY_SOURCE_AMAP_V3
                : GEOMETRY_SOURCE_AMAP_V5;
    }

    public String mergeGeometrySource(List<String> geometrySources) {
        List<String> nonBlankSources = geometrySources.stream()
                .filter(source -> source != null && !source.isBlank())
                .distinct()
                .toList();
        if (nonBlankSources.isEmpty()) {
            return GEOMETRY_SOURCE_UNAVAILABLE;
        }
        if (nonBlankSources.size() == 1) {
            return nonBlankSources.get(0);
        }
        return GEOMETRY_SOURCE_MIXED;
    }

    public void appendPathPoints(List<List<Double>> target, List<List<Double>> source) {
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

    public Map<String, Object> toPointMap(AmapRouteUtil.RoutePoint point) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", point.name());
        map.put("lat", point.lat());
        map.put("lng", point.lng());
        return map;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> castMapList(Object value) {
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> castStepList(Object value) {
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    @SuppressWarnings("unchecked")
    public List<List<Double>> castPathList(Object value) {
        return value instanceof List<?> list ? (List<List<Double>>) list : List.of();
    }
}
