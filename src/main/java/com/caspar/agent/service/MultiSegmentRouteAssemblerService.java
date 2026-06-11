package com.caspar.agent.service;

import com.caspar.agent.model.NavigationSegmentSlots;
import com.caspar.agent.model.NavigationSlots;
import com.caspar.util.AmapRouteUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多段路线拼装服务：负责逐段选路、几何拼接与总结果聚合。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiSegmentRouteAssemblerService {

    private final SegmentRoutePlannerService segmentRoutePlannerService;
    private final MultiSegmentRouteAggregationService multiSegmentRouteAggregationService;

    public Map<String, Object> assemble(List<AmapRouteUtil.RoutePoint> itinerary,
                                        List<NavigationSegmentSlots> segmentSlotProfiles,
                                        NavigationSlots slots) {
        List<SegmentRoutePlan> plans = new ArrayList<>();

        for (int i = 0; i < itinerary.size() - 1; i++) {
            AmapRouteUtil.RoutePoint from = itinerary.get(i);
            AmapRouteUtil.RoutePoint to = itinerary.get(i + 1);
            NavigationSegmentSlots segmentSlots = segmentSlotProfiles != null && segmentSlotProfiles.size() > i
                    ? segmentSlotProfiles.get(i)
                    : null;
            SegmentRoutePlannerService.SegmentPlanningResult planningResult = segmentRoutePlannerService
                    .planSegment(i, from, to, segmentSlots, slots);
            if (!planningResult.success()) {
                Map<String, Object> failed = new LinkedHashMap<>();
                failed.put("success", false);
                failed.put("error", planningResult.error() != null ? planningResult.error() : "多段路线规划失败");
                failed.put("failedSegmentIndex", i);
                return failed;
            }
            plans.add(planningResult.plan());
        }

        log.info("多段路线完成分段规划，segmentCount={}", plans.size());
        return multiSegmentRouteAggregationService.aggregate(plans);
    }
}
