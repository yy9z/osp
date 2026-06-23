package com.caspar.service;

import com.caspar.common.PageResult;
import com.caspar.entity.dto.LostFoundClaimVO;
import com.caspar.entity.dto.LostFoundContactVO;
import com.caspar.entity.dto.LostFoundMatchVO;
import com.caspar.entity.dto.LostFoundPublishDTO;
import com.caspar.entity.dto.LostFoundVO;

import java.util.List;

/**
 * 失物招领服务接口
 */
public interface LostFoundService {

    /**
     * 发布失物/招领信息
     * @param publisherId 发布者ID
     * @param publishDTO 发布信息
     * @return ID
     */
    Long publish(Long publisherId, LostFoundPublishDTO publishDTO);

    /**
     * 获取列表（分页、类型、分类、关键词搜索）
     * @param page 页码
     * @param size 每页数量
     * @param type 类型
     * @param category 分类
     * @param keyword 关键词
     * @return 分页结果
     */
    PageResult<LostFoundVO> getList(Integer page, Integer size, String type, String category, String keyword);

    /**
     * 获取详情
     * @param id ID
     * @return 详情
     */
    LostFoundVO getDetail(Long id);

    /**
     * 获取我的发布
     * @param publisherId 发布者ID
     * @param page 页码
     * @param size 每页数量
     * @return 分页结果
     */
    PageResult<LostFoundVO> getMyList(Long publisherId, Integer page, Integer size, String type);

    /**
     * 认领
     * @param id 失物招领ID
     * @param claimerId 认领者ID
     * @param message 认领说明
     * @return 是否成功
     */
    boolean claim(Long id, Long claimerId, String message);

    /**
     * 获取认领记录
     * @param lostfoundId 失物招领ID
     * @return 认领记录列表
     */
    List<LostFoundClaimVO> getClaims(Long lostfoundId);

    /**
     * 登录后获取发布者为本条信息提供的联系方式。
     */
    LostFoundContactVO getContact(Long id);

    /**
     * 发布者批准或拒绝认领申请。
     */
    boolean reviewClaim(Long lostfoundId, Long claimId, Long publisherId, boolean approve);

    /**
     * 删除
     * @param id ID
     * @param publisherId 发布者ID
     * @return 是否成功
     */
    boolean delete(Long id, Long publisherId);

    /**
     * 标记已解决
     * @param id ID
     * @param publisherId 发布者ID
     * @return 是否成功
     */
    boolean resolve(Long id, Long publisherId);

    /**
     * 管理员下架。
     */
    boolean adminRemove(Long id, String reason);

    /**
     * 获取智能匹配结果（LOST 匹配 FOUND，FOUND 匹配 LOST）
     * @param id 帖子ID
     * @param limit 返回条数
     * @return 匹配列表
     */
    List<LostFoundMatchVO> getSmartMatches(Long id, Integer limit);
}
