package com.caspar.service;

import com.caspar.common.PageResult;
import com.caspar.entity.CampusMap;
import com.caspar.entity.CampusPoi;
import com.caspar.entity.CampusRegion;
import com.caspar.entity.dto.RouteRequest;
import com.caspar.entity.dto.RouteResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * 校园导航服务接口
 */
public interface NavigationService {

    // ==================== 原有方法 ====================

    /**
     * 获取地点列表（分页、分类、关键词搜索）
     *
     * @param page     页码
     * @param size     每页数量
     * @param category 分类过滤
     * @param keyword  关键字搜索
     * @return 分页结果
     */
    PageResult<CampusPoi> getPlaceList(Integer page, Integer size, String category, String keyword);

    /**
     * 获取地点详情
     *
     * @param id 地点ID
     * @return 地点信息
     */
    CampusPoi getPlaceById(Long id);

    /**
     * 获取校园地图信息
     *
     * @return 校园地图信息
     */
    CampusMap getCampusMap();

    // ==================== 新增方法 ====================

    /**
     * 附近搜索 - 查询指定坐标附近的POI
     *
     * @param lat      纬度
     * @param lng      经度
     * @param radius   搜索半径（米）
     * @param category 类别过滤（可选）
     * @return 附近的POI列表
     */
    List<CampusPoi> searchNearby(BigDecimal lat, BigDecimal lng, Integer radius, String category);

    /**
     * 获取所有POI列表
     *
     * @return POI列表
     */
    List<CampusPoi> getAllPois();

    /**
     * 根据类别获取POI
     *
     * @param category 类别
     * @return POI列表
     */
    List<CampusPoi> getPoisByCategory(String category);

    /**
     * 路径规划 - 使用Dijkstra算法计算最短路径
     *
     * @param request 路径规划请求
     * @return 路径规划结果
     */
    RouteResponse calculateRoute(RouteRequest request);

    /**
     * 获取所有区域（GeoJSON）
     *
     * @return 区域列表
     */
    List<CampusRegion> getRegions();

    /**
     * 坐标转换（WGS-84转GCJ-02）
     *
     * @param wgsLat WGS-84纬度
     * @param wgsLon WGS-84经度
     * @return GCJ-02坐标 [纬度, 经度]
     */
    double[] convertToGcj02(double wgsLat, double wgsLon);

    /**
     * 检查坐标是否在校园围栏内
     *
     * @param lat 纬度
     * @param lon 经度
     * @return true如果在围栏内
     */
    boolean isInCampus(double lat, double lon);
}
