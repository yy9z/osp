package com.caspar.mapper;

import com.caspar.entity.LostFound;
import com.caspar.entity.LostFoundClaim;
import com.caspar.entity.dto.LostFoundVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 失物招领Mapper接口
 */
@Mapper
public interface LostFoundMapper {

    /**
     * 插入失物招领信息
     */
    int insert(LostFound lostFound);

    /**
     * 更新失物招领信息
     */
    int update(LostFound lostFound);

    /**
     * 根据ID查询
     */
    LostFound findById(@Param("id") Long id);

    /**
     * 查询详情（包含发布者信息）
     */
    LostFoundVO findDetailById(@Param("id") Long id);

    /**
     * 查询列表（分页、类型、分类、关键词搜索）
     */
    List<LostFoundVO> selectList(
            @Param("type") String type,
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );

    /**
     * 统计数量
     */
    Long count(
            @Param("type") String type,
            @Param("category") String category,
            @Param("keyword") String keyword
    );

    /**
     * 查询我的发布（分页）
     */
    List<LostFoundVO> selectMyList(
            @Param("publisherId") Long publisherId,
            @Param("type") String type,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );

    /**
     * 统计我的发布数量
     */
    Long countMy(@Param("publisherId") Long publisherId, @Param("type") String type);

    /**
     * 删除失物招领信息
     */
    int deleteById(@Param("id") Long id);

    /**
     * 更新状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 插入认领记录
     */
    int insertClaim(LostFoundClaim claim);

    /**
     * 查询认领记录
     */
    List<LostFoundClaim> selectClaimsByLostFoundId(@Param("lostfoundId") Long lostfoundId);

    /**
     * 更新认领状态
     */
    int updateClaimStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 检查是否已认领
     */
    int countClaimsByLostFoundIdAndClaimerId(@Param("lostfoundId") Long lostfoundId, @Param("claimerId") Long claimerId);

    List<LostFoundVO> selectAllList(@Param("type") String type, @Param("category") String category, @Param("keyword") String keyword, @Param("status") String status, @Param("offset") Integer offset, @Param("limit") Integer limit);

    Long countAll(@Param("type") String type, @Param("category") String category, @Param("keyword") String keyword, @Param("status") String status);

    int updateRemoveReason(@Param("id") Long id, @Param("reason") String reason);
}
