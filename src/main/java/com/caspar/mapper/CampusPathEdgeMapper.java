package com.caspar.mapper;

import com.caspar.entity.CampusPathEdge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 校园路径边数据访问层
 */
@Mapper
public interface CampusPathEdgeMapper {

    /**
     * 查询所有边
     *
     * @return 边列表
     */
    List<CampusPathEdge> selectAll();

    /**
     * 根据ID查询边
     *
     * @param id 边ID
     * @return 边信息
     */
    CampusPathEdge findById(@Param("id") Long id);

    /**
     * 查询从指定节点出发的所有边
     *
     * @param fromNodeId 起点节点ID
     * @return 边列表
     */
    List<CampusPathEdge> findByFromNode(@Param("fromNodeId") Long fromNodeId);

    /**
     * 查询到指定节点的所有边
     *
     * @param toNodeId 终点节点ID
     * @return 边列表
     */
    List<CampusPathEdge> findByToNode(@Param("toNodeId") Long toNodeId);

    /**
     * 查询两点之间的边
     *
     * @param fromNodeId 起点节点ID
     * @param toNodeId   终点节点ID
     * @return 边信息
     */
    CampusPathEdge findBetweenNodes(@Param("fromNodeId") Long fromNodeId, @Param("toNodeId") Long toNodeId);

    /**
     * 插入边
     *
     * @param edge 边对象
     * @return 影响行数
     */
    int insert(CampusPathEdge edge);

    /**
     * 批量插入边
     *
     * @param edges 边列表
     * @return 影响行数
     */
    int batchInsert(@Param("edges") List<CampusPathEdge> edges);

    /**
     * 更新边
     *
     * @param edge 边对象
     * @return 影响行数
     */
    int update(CampusPathEdge edge);

    /**
     * 删除边
     *
     * @param id 边ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 删除与指定节点相关的所有边
     *
     * @param nodeId 节点ID
     * @return 影响行数
     */
    int deleteByNodeId(@Param("nodeId") Long nodeId);
}
