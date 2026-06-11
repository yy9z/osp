package com.caspar.util;

/**
 * 坐标转换工具类
 * 用于WGS-84（GPS原始坐标）到GCJ-02（高德坐标系）的转换
 * 
 * 中国境内使用的坐标系：
 * - WGS-84: GPS原始坐标，国际标准
 * - GCJ-02: 国家测绘局偏移坐标，高德地图使用
 * - BD-09: 百度坐标系，百度地图使用
 */
public class CoordinateConverter {
    
    private static final double PI = 3.1415926535897932384626;
    private static final double A = 6378245.0;
    private static final double EE = 0.00669342162296594323;
    
    /**
     * 判断坐标是否在中国境内
     */
    private static boolean isOutOfChina(double lat, double lon) {
        if (lon < 72.004 || lon > 137.8347) {
            return true;
        }
        if (lat < 0.8293 || lat > 55.8271) {
            return true;
        }
        return false;
    }
    
    /**
     * 转换纬度
     */
    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y
                + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }
    
    /**
     * 转换经度
     */
    private static double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }
    
    /**
     * WGS-84 转 GCJ-02
     * 
     * @param wgsLat WGS-84 纬度
     * @param wgsLon WGS-84 经度
     * @return GCJ-02 坐标 [纬度, 经度]
     */
    public static double[] wgs84ToGcj02(double wgsLat, double wgsLon) {
        if (isOutOfChina(wgsLat, wgsLon)) {
            return new double[]{wgsLat, wgsLon};
        }
        
        double dLat = transformLat(wgsLon - 105.0, wgsLat - 35.0);
        double dLon = transformLon(wgsLon - 105.0, wgsLat - 35.0);
        
        double radLat = wgsLat / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
        dLon = (dLon * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
        
        double gcjLat = wgsLat + dLat;
        double gcjLon = wgsLon + dLon;
        
        return new double[]{gcjLat, gcjLon};
    }
    
    /**
     * GCJ-02 转 WGS-84（近似）
     * 
     * @param gcjLat GCJ-02 纬度
     * @param gcjLon GCJ-02 经度
     * @return WGS-84 坐标 [纬度, 经度]
     */
    public static double[] gcj02ToWgs84(double gcjLat, double gcjLon) {
        if (isOutOfChina(gcjLat, gcjLon)) {
            return new double[]{gcjLat, gcjLon};
        }
        
        double dLat = transformLat(gcjLon - 105.0, gcjLat - 35.0);
        double dLon = transformLon(gcjLon - 105.0, gcjLat - 35.0);
        
        double radLat = gcjLat / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
        dLon = (dLon * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
        
        double wgsLat = gcjLat - dLat;
        double wgsLon = gcjLon - dLon;
        
        return new double[]{wgsLat, wgsLon};
    }
    
    /**
     * GCJ-02 转 BD-09（百度坐标系）
     * 
     * @param gcjLat GCJ-02 纬度
     * @param gcjLon GCJ-02 经度
     * @return BD-09 坐标 [纬度, 经度]
     */
    public static double[] gcj02ToBd09(double gcjLat, double gcjLon) {
        double x = gcjLon, y = gcjLat;
        double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * PI);
        double theta = Math.atan2(y, x) + 0.00003 * Math.cos(x * PI);
        double bdLon = z * Math.cos(theta) + 0.0065;
        double bdLat = z * Math.sin(theta) + 0.006;
        return new double[]{bdLat, bdLon};
    }
    
    /**
     * BD-09 转 GCJ-02
     * 
     * @param bdLat BD-09 纬度
     * @param bdLon BD-09 经度
     * @return GCJ-02 坐标 [纬度, 经度]
     */
    public static double[] bd09ToGcj02(double bdLat, double bdLon) {
        double x = bdLon - 0.0065;
        double y = bdLat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * PI);
        double theta = Math.atan2(y, x) - 0.00003 * Math.cos(x * PI);
        double gcjLon = z * Math.cos(theta);
        double gcjLat = z * Math.sin(theta);
        return new double[]{gcjLat, gcjLon};
    }
}
