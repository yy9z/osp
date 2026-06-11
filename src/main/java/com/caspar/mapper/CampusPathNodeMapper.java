package com.caspar.mapper;

import com.caspar.entity.CampusPathNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 校园路径节点数据访问层
 */
@Mapper
public interface CampusPathNodeMapper {

    /**
     * 查询所有路径节点
     *
     * @return 节点列表
     */
    List<CampusPathNode> selectAll();

    /**
     * 根据ID查询节点
     *
     * @param id 节点ID
     * @return 节点信息
     */
    CampusPathNode findById(@Param("id") Long id);

    /**
     * 根据POI ID查询关联的节点
     *
     * @param poiId POI ID
     * @return 节点信息
     */
    CampusPathNode findByPoiId(@Param("poiId") Long poiId);

    /**
     * 根据节点类型查询
     *
     * @param nodeType 节点类型
     * @return 节点列表
     */
    List<CampusPathNode> findByNodeType(@Param("nodeType") String nodeType);

    /**
     * 查找最近的节点
     *
     * @param latitude  纬度
     * @param longitude 经度
     * @return 最近的节点
     */
    CampusPathNode findNearest(@Param("lat") Double latitude, @Param("lng") Double longitude);

    /**
     * 插入节点
     *
     * @param node 节点对象
     * @return 影响行数
     */
    int insert(CampusPathNode node);

    /**
     * 更新节点
     *
     * @param node 节点对象
     * @return 影响行数
     */
    int update(CampusPathNode node);

    /**
     * 删除节点
     *
     * @param id 节点ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}
