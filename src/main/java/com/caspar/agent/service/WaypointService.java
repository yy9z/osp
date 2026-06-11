package com.caspar.agent.service;

import com.caspar.util.AmapRouteUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 途经点相关服务：负责行程点序列构建。
 */
@Service
public class WaypointService {

    public List<AmapRouteUtil.RoutePoint> buildItinerary(AmapPlaceSearchService.ResolvedPlace origin,
                                                         ResolvedNavigation resolvedNavigation) {
        List<AmapRouteUtil.RoutePoint> itinerary = new ArrayList<>();
        itinerary.add(new AmapRouteUtil.RoutePoint(
                origin.getName(),
                origin.getLat(),
                origin.getLng()
        ));
        for (AmapPlaceSearchService.ResolvedPlace waypoint : resolvedNavigation.waypoints()) {
            itinerary.add(new AmapRouteUtil.RoutePoint(waypoint.getName(), waypoint.getLat(), waypoint.getLng()));
        }
        AmapPlaceSearchService.ResolvedPlace destination = resolvedNavigation.destination();
        itinerary.add(new AmapRouteUtil.RoutePoint(destination.getName(), destination.getLat(), destination.getLng()));
        return itinerary;
    }
}
