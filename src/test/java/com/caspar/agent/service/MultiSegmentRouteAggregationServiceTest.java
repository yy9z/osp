package com.caspar.agent.service;

import com.caspar.util.AmapRouteUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultiSegmentRouteAggregationServiceTest {

    @Test
    void aggregate_shouldMergePlansIntoSingleRouteResult() {
        MultiSegmentRouteAggregationService service = new MultiSegmentRouteAggregationService(new GeometryAssemblerService());

        SegmentRoutePlan first = new SegmentRoutePlan(
                0,
                new AmapRouteUtil.RoutePoint("A", 31.0, 117.0),
                new AmapRouteUtil.RoutePoint("B", 31.1, 117.1),
                200,
                120,
                List.of(Map.of("instruction", "walk-1")),
                List.of(List.of(117.0, 31.0), List.of(117.1, 31.1)),
                GeometryAssemblerService.GEOMETRY_SOURCE_AMAP_V5,
                null,
                1,
                "第1段选更优路线",
                2,
                List.of(Map.of("index", 0, "selected", false), Map.of("index", 1, "selected", true)),
                "v5",
                "alternative_route=3"
        );

        SegmentRoutePlan second = new SegmentRoutePlan(
                1,
                new AmapRouteUtil.RoutePoint("B", 31.1, 117.1),
                new AmapRouteUtil.RoutePoint("C", 31.2, 117.2),
                300,
                180,
                List.of(Map.of("instruction", "walk-2")),
                List.of(List.of(117.1, 31.1), List.of(117.2, 31.2)),
                GeometryAssemblerService.GEOMETRY_SOURCE_AMAP_V3,
                "第2段: v5 候选路线未返回可绘制几何",
                0,
                "第2段默认路线",
                1,
                List.of(Map.of("index", 0, "selected", true)),
                "v3",
                "amap_default"
        );

        Map<String, Object> result = service.aggregate(List.of(first, second));

        assertEquals(Boolean.TRUE, result.get("success"));
        assertEquals(500, ((Number) result.get("distance")).intValue());
        assertEquals(300, ((Number) result.get("duration")).intValue());
        assertEquals("v5+v3", result.get("amapApiVersion"));
        assertEquals("alternative_route=3+amap_default", result.get("routingStrategy"));
        assertEquals(GeometryAssemblerService.GEOMETRY_SOURCE_MIXED, result.get("geometrySource"));
        assertEquals("第2段: v5 候选路线未返回可绘制几何", result.get("geometryFallbackReason"));

        @SuppressWarnings("unchecked")
        List<List<Double>> path = (List<List<Double>>) result.get("path");
        assertEquals(3, path.size());
        assertEquals(List.of(117.0, 31.0), path.get(0));
        assertEquals(List.of(117.2, 31.2), path.get(2));
    }
}
