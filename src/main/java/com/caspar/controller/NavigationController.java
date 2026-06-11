package com.caspar.controller;

import com.caspar.agent.service.AmapPlaceSearchService;
import com.caspar.common.Result;
import com.caspar.entity.CampusMap;
import com.caspar.entity.CampusPoi;
import com.caspar.common.PageResult;
import com.caspar.mapper.CampusMapMapper;
import com.caspar.mapper.CampusPoiMapper;
import com.caspar.util.AmapRouteUtil;
import com.caspar.util.PaginationUtils;
import org.springframework.cache.annotation.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/navigation")
public class NavigationController {

    private static final Logger logger = LoggerFactory.getLogger(NavigationController.class);

    @Autowired
    private CampusPoiMapper campusPoiMapper;

    @Autowired
    private CampusMapMapper campusMapMapper;

    @Autowired
    private AmapRouteUtil amapRouteUtil;

    @Autowired
    private AmapPlaceSearchService amapPlaceSearchService;

    @GetMapping("/places")
    @Cacheable(cacheNames = "navPlaceList", key = "T(String).format('%s:%s:%s:%s', #page, #size, #category, #keyword)")
    public Result<PageResult<CampusPoi>> getPlaceList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        try {
            int safePage = PaginationUtils.safePage(page);
            int safeSize = PaginationUtils.safeSize(size);
            int offset = PaginationUtils.offset(safePage, safeSize);
            List<CampusPoi> pois = campusPoiMapper.selectList(category, keyword, offset, safeSize);
            Long total = campusPoiMapper.count(category, keyword);
            return Result.success(new PageResult<>(pois, total, safePage, safeSize));
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取地点列表失败");
        }
    }

    @GetMapping("/places/{id}")
    @Cacheable(cacheNames = "navPlaceDetail", key = "#id")
    public Result<CampusPoi> getPlaceDetail(@PathVariable Long id) {
        try {
            CampusPoi poi = campusPoiMapper.findById(id);
            if (poi == null) {
                return Result.badRequest("地点不存在");
            }
            return Result.success(poi);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取地点详情失败");
        }
    }

    @GetMapping("/map")
    @Cacheable(cacheNames = "navCampusMap", key = "'default'")
    public Result<CampusMap> getCampusMap() {
        try {
            CampusMap campusMap = campusMapMapper.findDefault();
            return Result.success(campusMap);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取地图信息失败");
        }
    }

    @GetMapping("/all")
    @Cacheable(cacheNames = "navAllPois", key = "'all'")
    public Result<List<CampusPoi>> getAllPois() {
        try {
            List<CampusPoi> pois = campusPoiMapper.selectList(null, null, 0, 1000);
            return Result.success(pois);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取POI列表失败");
        }
    }

    @GetMapping("/category/{category}")
    @Cacheable(cacheNames = "navPoiByCategory", key = "#category")
    public Result<List<CampusPoi>> getPoisByCategory(@PathVariable String category) {
        try {
            List<CampusPoi> pois = campusPoiMapper.findByCategory(category);
            return Result.success(pois);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取分类POI失败");
        }
    }

    @GetMapping("/nearby")
    public Result<List<CampusPoi>> searchNearby(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng,
            @RequestParam(defaultValue = "1000") Integer radius,
            @RequestParam(required = false) String category) {
        try {
            List<CampusPoi> pois = campusPoiMapper.findNearby(lat, lng, radius);
            return Result.success(pois);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("附近搜索失败");
        }
    }

    @PostMapping("/resolve")
    public Result<Map<String, Object>> resolvePlace(@RequestBody Map<String, Object> request) {
        try {
            String keyword = request.getOrDefault("keyword", "").toString();
            String campus = request.get("campus") != null ? request.get("campus").toString() : null;
            Double userLat = parseDouble(request.get("userLat"));
            Double userLng = parseDouble(request.get("userLng"));

            AmapPlaceSearchService.ResolveResult result = amapPlaceSearchService.resolvePlace(keyword, campus, userLat, userLng);
            Map<String, Object> data = new HashMap<>();
            data.put("success", result.isSuccess());
            data.put("clarificationRequired", result.isClarificationRequired());
            data.put("message", result.getMessage());
            data.put("clarificationOptions", result.getClarificationOptions());
            data.put("candidates", result.getCandidates().stream().map(AmapPlaceSearchService.ResolvedPlace::toMap).toList());
            data.put("places", result.getCandidates().stream().map(AmapPlaceSearchService.ResolvedPlace::toMap).toList());
            if (result.getPlace() != null) {
                data.put("place", result.getPlace().toMap());
            }
            return Result.success(data);
        } catch (Exception e) {
            logger.error("地点解析失败", e);
            return Result.error("地点解析失败");
        }
    }

    @PostMapping("/route/amap")
    public Result<Map<String, Object>> calculateWalkingRoute(@RequestBody Map<String, Object> request) {
        try {
            double fromLat = Double.parseDouble(request.get("fromLat").toString());
            double fromLng = Double.parseDouble(request.get("fromLng").toString());
            double toLat = Double.parseDouble(request.get("toLat").toString());
            double toLng = Double.parseDouble(request.get("toLng").toString());
            String wayType = request.getOrDefault("wayType", "WALK").toString();

            logger.info("收到路径规划请求: from={},{} to={},{} wayType={}", fromLat, fromLng, toLat, toLng, wayType);

            Map<String, Object> routeResult = amapRouteUtil.walkingRoute(fromLng, fromLat, toLng, toLat);

            if (routeResult == null || !Boolean.TRUE.equals(routeResult.get("success"))) {
                String errorMsg = routeResult != null
                        ? String.valueOf(routeResult.getOrDefault("error", routeResult.getOrDefault("message", "路径规划失败")))
                        : "路径规划失败";
                return Result.error(errorMsg);
            }

            return Result.success(routeResult);
        } catch (Exception e) {
            logger.error("路径规划失败", e);
            return Result.error("路径规划失败");
        }
    }

    private Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
