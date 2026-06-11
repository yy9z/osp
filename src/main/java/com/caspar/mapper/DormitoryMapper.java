package com.caspar.mapper;

import com.caspar.entity.Dormitory;
import com.caspar.entity.DormitoryMember;
import com.caspar.entity.User;
import com.caspar.entity.dto.DormitoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 宿舍Mapper接口
 */
@Mapper
public interface DormitoryMapper {

    /**
     * 根据ID查询宿舍
     *
     * @param id 宿舍ID
     * @return 宿舍信息
     */
    Dormitory findById(@Param("id") Long id);

    Dormitory findByBuildingAndRoom(@Param("building") String building, @Param("roomNo") String roomNo);

    /**
     * 分页查询宿舍列表
     *
     * @param building 楼栋号
     * @param floor    楼层
     * @param roomNo   房间号
     * @param offset   偏移量
     * @param limit    数量限制
     * @return 宿舍列表
     */
    List<Dormitory> selectList(@Param("building") String building,
                               @Param("floor") Integer floor,
                               @Param("roomNo") String roomNo,
                               @Param("offset") Integer offset,
                               @Param("limit") Integer limit);

    List<Dormitory> selectManagedList(@Param("building") String building,
                                      @Param("buildingAlias") String buildingAlias,
                                      @Param("floor") Integer floor,
                                      @Param("roomNo") String roomNo,
                                      @Param("offset") Integer offset,
                                      @Param("limit") Integer limit);

    /**
     * 查询宿舍总数
     *
     * @param building 楼栋号
     * @param floor    楼层
     * @param roomNo   房间号
     * @return 宿舍总数
     */
    Long count(@Param("building") String building,
               @Param("floor") Integer floor,
               @Param("roomNo") String roomNo);

    Long countManaged(@Param("building") String building,
                      @Param("buildingAlias") String buildingAlias,
                      @Param("floor") Integer floor,
                      @Param("roomNo") String roomNo);

    /**
     * 新增宿舍
     *
     * @param dormitory 宿舍信息
     * @return 影响行数
     */
    int insert(Dormitory dormitory);

    /**
     * 更新宿舍信息
     *
     * @param dormitory 宿舍信息
     * @return 影响行数
     */
    int update(Dormitory dormitory);

    /**
     * 删除宿舍
     *
     * @param id 宿舍ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据用户ID查询宿舍成员关系
     *
     * @param userId 用户ID
     * @return 宿舍成员关系
     */
    DormitoryMember findMemberByUserId(@Param("userId") Long userId);

    /**
     * 根据宿舍ID查询成员列表（包含用户信息）
     *
     * @param dormitoryId 宿舍ID
     * @return 成员列表
     */
    List<DormitoryVO.MemberVO> selectMembersByDormitoryId(@Param("dormitoryId") Long dormitoryId);

    /**
     * 新增宿舍成员
     *
     * @param member 宿舍成员
     * @return 影响行数
     */
    int insertMember(DormitoryMember member);

    /**
     * 删除宿舍成员
     *
     * @param dormitoryId 宿舍ID
     * @param userId      用户ID
     * @return 影响行数
     */
    int deleteMember(@Param("dormitoryId") Long dormitoryId, @Param("userId") Long userId);

    /**
     * 更新宿舍当前人数
     *
     * @param dormitoryId 宿舍ID
     * @param count       人数（正数增加，负数减少）
     * @return 影响行数
     */
    int updateCurrentCount(@Param("dormitoryId") Long dormitoryId, @Param("count") Integer count);

    int updateHeadId(@Param("id") Long id, @Param("headId") Long headId);

    int updateByBuilding(@Param("oldBuilding") String oldBuilding,
                         @Param("newBuilding") String newBuilding,
                         @Param("gender") String gender,
                         @Param("capacity") Integer capacity,
                         @Param("type") String type);

    Integer countOverCapacity(@Param("buildingName") String buildingName,
                              @Param("buildingCode") String buildingCode,
                              @Param("capacity") Integer capacity);
}
