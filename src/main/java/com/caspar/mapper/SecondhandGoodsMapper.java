package com.caspar.mapper;

import com.caspar.entity.SecondhandGoods;
import com.caspar.entity.dto.SecondhandGoodsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 二手商品Mapper接口
 */
@Mapper
public interface SecondhandGoodsMapper {

    /**
     * 插入商品
     */
    int insert(SecondhandGoods goods);

    /**
     * 更新商品
     */
    int update(SecondhandGoods goods);

    /**
     * 根据ID查询商品
     */
    SecondhandGoods findById(@Param("id") Long id);

    /**
     * 查询商品详情（包含卖家信息）
     */
    SecondhandGoodsVO findDetailById(@Param("id") Long id);

    /**
     * 查询商品列表（分页、分类、关键词搜索、排序）
     */
    List<SecondhandGoodsVO> selectList(
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("sort") String sort,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );

    /**
     * 统计商品数量
     */
    Long count(
            @Param("category") String category,
            @Param("keyword") String keyword
    );

    /**
     * 查询我的发布（分页）
     */
    List<SecondhandGoodsVO> selectMyList(
            @Param("sellerId") Long sellerId,
            @Param("status") String status,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );

    /**
     * 统计我的发布数量
     */
    Long countMy(
            @Param("sellerId") Long sellerId,
            @Param("status") String status
    );

    /**
     * 删除商品
     */
    int deleteById(@Param("id") Long id);

    /**
     * 更新商品状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 增加浏览次数
     */
    int incrementViewCount(@Param("id") Long id);

    List<SecondhandGoodsVO> selectPendingList(@Param("category") String category, @Param("keyword") String keyword, @Param("offset") Integer offset, @Param("limit") Integer limit);

    Long countPending(@Param("category") String category, @Param("keyword") String keyword);

    List<SecondhandGoodsVO> selectAllList(@Param("category") String category, @Param("keyword") String keyword, @Param("status") String status, @Param("offset") Integer offset, @Param("limit") Integer limit);

    Long countAll(@Param("category") String category, @Param("keyword") String keyword, @Param("status") String status);

    int updateHandler(@Param("id") Long id, @Param("handlerId") Long handlerId);

    int updateHandleTime(@Param("id") Long id);

    int updateRejectReason(@Param("id") Long id, @Param("reason") String reason);

    int updateRemoveReason(@Param("id") Long id, @Param("reason") String reason);

    int updateStatusWithCondition(@Param("id") Long id, @Param("status") String status, @Param("allowedStatuses") String[] allowedStatuses);

    int softDelete(@Param("id") Long id, @Param("deletedBy") Long deletedBy, @Param("deleteReason") String deleteReason);

    Long countBySeller(@Param("sellerId") Long sellerId);

    Long countBySellerAndStatus(@Param("sellerId") Long sellerId, @Param("status") String status);
}
