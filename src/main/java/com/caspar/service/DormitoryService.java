package com.caspar.service;

import com.caspar.common.PageResult;
import com.caspar.entity.Dormitory;
import com.caspar.entity.DormBuilding;
import com.caspar.entity.dto.DormitoryAssignDTO;
import com.caspar.entity.dto.DormitoryAssignableUserVO;
import com.caspar.entity.dto.DormBuildingCreateDTO;
import com.caspar.entity.dto.DormBuildingManagerAssignDTO;
import com.caspar.entity.dto.DormBuildingUpdateDTO;
import com.caspar.entity.dto.DormitoryVO;
import com.caspar.entity.dto.RepairCreateDTO;
import com.caspar.entity.dto.RepairHandleDTO;
import com.caspar.entity.dto.RepairVO;

import java.util.List;
import java.util.Map;

public interface DormitoryService {
    PageResult<Dormitory> getDormitoryList(Long operatorId, String operatorRole, Integer page, Integer size, String building, Integer floor, String roomNo);

    DormitoryVO getDormitoryDetail(Long operatorId, String operatorRole, Long id);

    DormitoryVO getMyDormitory(Long userId);

    List<DormitoryVO.MemberVO> getDormitoryMembers(Long operatorId, String operatorRole, Long dormitoryId);

    Long createRepair(Long userId, RepairCreateDTO createDTO);

    PageResult<RepairVO> getMyRepairs(Long userId, Integer page, Integer size);

    PageResult<RepairVO> getRepairList(Long operatorId, String operatorRole, Integer page, Integer size, String status, String building, String urgency);

    RepairVO getRepairDetail(Long id);

    boolean processRepair(Long id, String status, String remark, String handleImages, Long handlerId, String handlerRole);

    boolean handleRepair(Long id, RepairHandleDTO handleDTO, Long handlerId, String handlerRole);

    boolean cancelRepairByUser(Long id, Long userId);

    boolean updateRepairByUser(Long id, Long userId, String description, String urgency, String images);

    boolean reapplyRepairByUser(Long id, Long userId);

    boolean deleteRepairByUser(Long id, Long userId);

    boolean closeRepair(Long id, Long handlerId, String handlerRole);

    Map<String, Integer> getRepairStats(Long operatorId, String operatorRole, String building);

    List<DormBuilding> getAllBuildings();

    DormBuilding createBuilding(Long operatorId, String operatorRole, DormBuildingCreateDTO createDTO);

    DormBuilding updateBuilding(Long operatorId, String operatorRole, Long buildingId, DormBuildingUpdateDTO updateDTO);

    DormBuilding assignBuildingManager(Long operatorId, Long buildingId, Long managerId);

    DormBuilding getBuildingByManager(Long managerId);

    PageResult<DormitoryAssignableUserVO> getAssignableUsers(Long operatorId, String operatorRole, Integer page, Integer size, String keyword, Boolean assigned);

    void assignDormitory(Long operatorId, String operatorRole, DormitoryAssignDTO assignDTO);

    void checkoutDormitory(Long operatorId, String operatorRole, Long userId);

    boolean isUserInDormitory(Long userId, Long dormitoryId);

    boolean startRepair(Long id, Long handlerId, String handlerRole);

    boolean completeRepair(Long id, Long handlerId, String handlerRole, String remark, String handleImages);
}
