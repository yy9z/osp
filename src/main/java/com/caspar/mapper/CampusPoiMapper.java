package com.caspar.mapper;

import com.caspar.entity.CampusPoi;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 校园POI数据访问层
 */
@Mapper
public interface CampusPoiMapper {

    /**
     * 查询所有POI（分页）
     *
     * @param category 分类过滤
     * @param keyword  关键字搜索
     * @param offset   偏移量
     * @param limit    数量限制
     * @return POI列表
     */
    List<CampusPoi> selectList(@Param("category") String category,
                               @Param("keyword") String keyword,
                               @Param("offset") Integer offset,
                               @Param("limit") Integer limit);

    /**
     * 统计POI数量
     *
     * @param category 分类过滤
     * @param keyword  关键字搜索
     * @return 数量
     */
    Long count(@Param("category") String category, @Param("keyword") String keyword);

    /**
     * 根据ID查询POI
     *
     * @param id POI ID
     * @return POI信息
     */
    CampusPoi findById(@Param("id") Long id);

    /**
     * 附近搜索 - 使用空间函数计算距离
     *
     * @param latitude  纬度
     * @param longitude 经度
     * @param radius    搜索半径（米）
     * @return 附近的POI列表
     */
    List<CampusPoi> findNearby(@Param("lat") BigDecimal latitude,
                               @Param("lng") BigDecimal longitude,
                               @Param("radius") Integer radius);

    /**
     * 根据类别查询POI
     *
     * @param category 类别
     * @return POI列表
     */
    List<CampusPoi> findByCategory(@Param("category") String category);

    /**
     * 插入POI
     *
     * @param poi POI对象
     * @return 影响行数
     */
    int insert(CampusPoi poi);

    /**
     * 更新POI
     *
     * @param poi POI对象
     * @return 影响行数
     */
    int update(CampusPoi poi);

    /**
     * 删除POI
     *
     * @param id POI ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}
