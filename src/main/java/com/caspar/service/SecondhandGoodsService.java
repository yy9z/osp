package com.caspar.service;

import com.caspar.common.PageResult;
import com.caspar.entity.dto.SecondhandGoodsVO;
import com.caspar.entity.dto.SecondhandPublishDTO;

/**
 * 二手商品服务接口
 */
public interface SecondhandGoodsService {

    /**
     * 发布商品
     * @param sellerId 卖家ID
     * @param publishDTO 发布信息
     * @return 商品ID
     */
    Long publish(Long sellerId, SecondhandPublishDTO publishDTO);

    /**
     * 获取商品列表（分页、分类、关键词搜索、排序）
     * @param page 页码
     * @param size 每页数量
     * @param category 分类
     * @param keyword 关键词
     * @param sort 排序方式
     * @return 分页结果
     */
    PageResult<SecondhandGoodsVO> getList(Integer page, Integer size, String category, String keyword, String sort);

    /**
     * 获取商品详情
     * @param id 商品ID
     * @return 商品详情
     */
    SecondhandGoodsVO getDetail(Long id);

    /**
     * 获取我的发布
     * @param sellerId 卖家ID
     * @param page 页码
     * @param size 每页数量
     * @param status 状态过滤
     * @return 分页结果
     */
    PageResult<SecondhandGoodsVO> getMyList(Long sellerId, Integer page, Integer size, String status);

    /**
     * 下架商品
     * @param id 商品ID
     * @param sellerId 卖家ID
     * @return 是否成功
     */
    boolean delete(Long id, Long sellerId);

    /**
     * 更新商品
     * @param id 商品ID
     * @param sellerId 卖家ID
     * @param publishDTO 更新信息
     * @return 是否成功
     */
    boolean update(Long id, Long sellerId, SecondhandPublishDTO publishDTO);

    /**
     * 标记已售
     * @param id 商品ID
     * @param sellerId 卖家ID
     * @return 是否成功
     */
    boolean markAsSold(Long id, Long sellerId);

    /**
     * 重新上架
     * @param id 商品ID
     * @param sellerId 卖家ID
     * @return 是否成功
     */
    boolean relist(Long id, Long sellerId);

    /**
     * 收藏商品
     * @param userId 用户ID
     * @param goodsId 商品ID
     * @return 是否成功
     */
    boolean favorite(Long userId, Long goodsId);

    /**
     * 取消收藏
     * @param userId 用户ID
     * @param goodsId 商品ID
     * @return 是否成功
     */
    boolean unfavorite(Long userId, Long goodsId);

    /**
     * 获取我的收藏列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 分页结果
     */
    PageResult<SecondhandGoodsVO> getMyFavorites(Long userId, Integer page, Integer size);

    /**
     * 检查是否已收藏
     * @param userId 用户ID
     * @param goodsId 商品ID
     * @return 是否已收藏
     */
    boolean isFavorited(Long userId, Long goodsId);

    PageResult<SecondhandGoodsVO> getPendingList(Integer page, Integer size, String category, String keyword);

    PageResult<SecondhandGoodsVO> getAllList(Integer page, Integer size, String category, String keyword, String status);

    boolean approve(Long id, Long adminId);

    boolean reject(Long id, Long adminId, String reason);

    boolean remove(Long id, Long adminId, String reason);

    boolean userRemove(Long id, Long sellerId, String reason);

    boolean adminDelete(Long id, Long adminId, String reason);

    boolean updateByUser(Long id, Long userId, SecondhandPublishDTO publishDTO);
}
