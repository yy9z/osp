package com.caspar.mapper;

import com.caspar.entity.DormitoryRepair;
import com.caspar.entity.dto.RepairVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DormitoryRepairMapper {
    DormitoryRepair findById(@Param("id") Long id);

    RepairVO findVOById(@Param("id") Long id);

    List<RepairVO> selectListByUserId(@Param("userId") Long userId,
                                       @Param("offset") Integer offset,
                                       @Param("limit") Integer limit);

    Long countByUserId(@Param("userId") Long userId);

    List<RepairVO> selectList(@Param("status") String status,
                               @Param("building") String building,
                               @Param("urgency") String urgency,
                               @Param("offset") Integer offset,
                               @Param("limit") Integer limit);

    Long count(@Param("status") String status,
               @Param("building") String building,
               @Param("urgency") String urgency);

    List<Map<String, Object>> countByStatus(@Param("building") String building);

    int insert(DormitoryRepair repair);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("remark") String remark,
                     @Param("handleImages") String handleImages,
                     @Param("handlerId") Long handlerId);

    int cancelByUser(@Param("id") Long id, @Param("userId") Long userId, @Param("currentStatus") String currentStatus);

    int updateByUser(@Param("id") Long id, @Param("userId") Long userId, 
                     @Param("description") String description,
                     @Param("urgency") String urgency,
                     @Param("images") String images);

    int reapplyByUser(@Param("id") Long id, @Param("userId") Long userId);

    int deleteByUser(@Param("id") Long id, @Param("userId") Long userId);

    int updateResolvedLocation(@Param("id") Long id,
                               @Param("dormitoryId") Long dormitoryId,
                               @Param("campus") String campus,
                               @Param("building") String building,
                               @Param("roomNo") String roomNo);

    int startRepair(@Param("id") Long id, @Param("handlerId") Long handlerId);

    int completeRepair(@Param("id") Long id, 
                       @Param("handlerId") Long handlerId,
                       @Param("remark") String remark, 
                       @Param("handleImages") String handleImages);

    int updateBuildingAndCampus(@Param("oldBuilding") String oldBuilding,
                                @Param("newBuilding") String newBuilding,
                                @Param("campus") String campus);
}
