package com.caspar.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 地理围栏工具类
 * 用于验证坐标是否在校园范围内
 */
public class GeofenceUtils {
    
    /**
     * 校园围栏配置
     * key: 校区名称
     * value: [纬度, 经度, 半径(米)]
     */
    private static final Map<String, double[]> CAMPUS_FENCE = new HashMap<>();
    
    static {
        // 东校区 - 行政与底蕴中心（金寨路）
        CAMPUS_FENCE.put("EAST", new double[]{31.8375, 117.2670, 2000.0});
        // 西校区 - 科研与地标中心（黄山路）
        CAMPUS_FENCE.put("WEST", new double[]{31.8385, 117.2545, 1500.0});
        // 中校区 - 体育与艺术枢纽
        CAMPUS_FENCE.put("MIDDLE", new double[]{31.8305, 117.2715, 1000.0});
        // 南校区 - 徽州大道
        CAMPUS_FENCE.put("SOUTH", new double[]{31.8220, 117.2840, 800.0});
        // 高新园区 - 复兴路
        CAMPUS_FENCE.put("HIGH_TECH", new double[]{31.8235, 117.1287, 1000.0});
    }
    
    /**
     * 默认校园围栏（东校区）- 2km半径
     */
    private static final double[] DEFAULT_FENCE = new double[]{31.8375, 117.2670, 2000.0};
    
    /**
     * 计算两点之间的距离（米）
     * 使用Haversine公式
     * 
     * @param lat1 纬度1
     * @param lon1 经度1
     * @param lat2 纬度2
     * @param lon2 经度2
     * @return 距离（米）
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // 地球半径（米）
        
        double latRad1 = Math.toRadians(lat1);
        double latRad2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(latRad1) * Math.cos(latRad2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    /**
     * 检查坐标是否在默认校园围栏内
     * 
     * @param lat 纬度
     * @param lon 经度
     * @return true如果在围栏内
     */
    public static boolean isInCampus(double lat, double lon) {
        return isInFence(lat, lon, DEFAULT_FENCE);
    }
    
    /**
     * 检查坐标是否在指定校区围栏内
     * 
     * @param lat 纬度
     * @param lon 经度
     * @param campus 校区名称
     * @return true如果在围栏内
     */
    public static boolean isInCampus(double lat, double lon, String campus) {
        double[] fence = CAMPUS_FENCE.get(campus);
        if (fence == null) {
            return isInCampus(lat, lon); // 使用默认围栏
        }
        return isInFence(lat, lon, fence);
    }
    
    /**
     * 检查坐标是否在围栏内
     * 
     * @param lat 纬度
     * @param lon 经度
     * @param fence 围栏配置 [中心纬度, 中心经度, 半径]
     * @return true如果在围栏内
     */
    private static boolean isInFence(double lat, double lon, double[] fence) {
        if (fence == null || fence.length < 3) {
            return true; // 无围栏配置，默认允许
        }
        
        double centerLat = fence[0];
        double centerLon = fence[1];
        double radius = fence[2];
        
        double distance = calculateDistance(lat, lon, centerLat, centerLon);
        return distance <= radius;
    }
    
    /**
     * 获取围栏信息
     * 
     * @return 围栏配置信息
     */
    public static Map<String, double[]> getCampusFences() {
        return new HashMap<>(CAMPUS_FENCE);
    }
    
    /**
     * 获取默认围栏中心点
     * 
     * @return [纬度, 经度]
     */
    public static double[] getDefaultCenter() {
        return new double[]{DEFAULT_FENCE[0], DEFAULT_FENCE[1]};
    }
    
    /**
     * 获取默认围栏半径（米）
     * 
     * @return 半径
     */
    public static double getDefaultRadius() {
        return DEFAULT_FENCE[2];
    }
}
