package com.caspar.mapper;

import com.caspar.entity.GoodsFavorite;
import com.caspar.entity.dto.SecondhandGoodsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品收藏Mapper接口
 */
@Mapper
public interface GoodsFavoriteMapper {

    /**
     * 插入收藏记录
     */
    int insert(GoodsFavorite favorite);

    /**
     * 删除收藏记录（根据用户ID和商品ID）
     */
    int deleteByUserIdAndGoodsId(@Param("userId") Long userId, @Param("goodsId") Long goodsId);

    /**
     * 查询用户是否已收藏指定商品
     */
    GoodsFavorite findByUserIdAndGoodsId(@Param("userId") Long userId, @Param("goodsId") Long goodsId);

    /**
     * 查询用户的收藏列表（分页）
     */
    List<SecondhandGoodsVO> selectFavoritesByUserId(
            @Param("userId") Long userId,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );

    /**
     * 统计用户收藏数量
     */
    Long countByUserId(@Param("userId") Long userId);

    /**
     * 检查是否已收藏
     */
    boolean existsByUserIdAndGoodsId(@Param("userId") Long userId, @Param("goodsId") Long goodsId);
}
