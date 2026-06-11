package com.caspar.agent.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeometryAssemblerServiceTest {

    private final GeometryAssemblerService service = new GeometryAssemblerService();

    @Test
    void mergeGeometrySource_shouldReturnMixedWhenSourcesDiffer() {
        String merged = service.mergeGeometrySource(List.of(
                GeometryAssemblerService.GEOMETRY_SOURCE_AMAP_V5,
                GeometryAssemblerService.GEOMETRY_SOURCE_AMAP_V3
        ));
        assertEquals(GeometryAssemblerService.GEOMETRY_SOURCE_MIXED, merged);
    }

    @Test
    void appendPathPoints_shouldDeduplicateAdjacentPoints() {
        List<List<Double>> target = new ArrayList<>();
        target.add(List.of(117.0, 31.0));

        service.appendPathPoints(target, List.of(
                List.of(117.0, 31.0),
                List.of(117.1, 31.1)
        ));

        assertEquals(2, target.size());
        assertEquals(List.of(117.1, 31.1), target.get(1));
    }
}
