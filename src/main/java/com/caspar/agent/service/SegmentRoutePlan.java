package com.caspar.agent.service;

import com.caspar.util.AmapRouteUtil;

import java.util.List;
import java.util.Map;

/**
 * 单段选路后的结构化结果。
 */
public record SegmentRoutePlan(
        int index,
        AmapRouteUtil.RoutePoint from,
        AmapRouteUtil.RoutePoint to,
        int distance,
        int duration,
        List<Map<String, Object>> steps,
        List<List<Double>> path,
        String geometrySource,
        String geometryFallbackReason,
        int selectedRouteIndex,
        String selectionReason,
        int totalRoutes,
        List<Map<String, Object>> alternativeRoutes,
        String amapApiVersion,
        String routingStrategy
) {
}
