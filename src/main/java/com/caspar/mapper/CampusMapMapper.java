package com.caspar.mapper;

import com.caspar.entity.CampusMap;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 校园地图Mapper接口
 */
@Mapper
public interface CampusMapMapper {

    /**
     * 获取默认校园地图
     *
     * @return 校园地图信息
     */
    CampusMap findDefault();

    /**
     * 根据ID查询校园地图
     *
     * @param id 地图ID
     * @return 校园地图信息
     */
    CampusMap findById(@Param("id") Long id);

    /**
     * 新增校园地图
     *
     * @param campusMap 地图信息
     * @return 影响行数
     */
    int insert(CampusMap campusMap);

    /**
     * 更新校园地图
     *
     * @param campusMap 地图信息
     * @return 影响行数
     */
    int update(CampusMap campusMap);

    /**
     * 删除校园地图
     *
     * @param id 地图ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}
