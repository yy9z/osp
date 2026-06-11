package com.caspar.agent.service;

import java.util.List;

/**
 * 导航地点解析结果：包含起点、终点、途经点与澄清信息。
 */
public record ResolvedNavigation(
        AmapPlaceSearchService.ResolvedPlace origin,
        AmapPlaceSearchService.ResolvedPlace destination,
        List<AmapPlaceSearchService.ResolvedPlace> waypoints,
        AmapPlaceSearchService.ResolveResult clarificationResult,
        List<AmapPlaceSearchService.ResolvedPlace> destinationCandidates,
        String destinationMessage
) {
}
