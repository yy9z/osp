package com.caspar.service.impl;

import com.caspar.common.PageResult;
import com.caspar.entity.*;
import com.caspar.entity.dto.DormBuildingCreateDTO;
import com.caspar.entity.dto.DormitoryAssignDTO;
import com.caspar.entity.dto.DormitoryAssignableUserVO;
import com.caspar.entity.dto.DormBuildingManagerAssignDTO;
import com.caspar.entity.dto.DormBuildingUpdateDTO;
import com.caspar.entity.dto.DormitoryVO;
import com.caspar.entity.dto.RepairCreateDTO;
import com.caspar.entity.dto.RepairHandleDTO;
import com.caspar.entity.dto.RepairVO;
import com.caspar.mapper.DormBuildingMapper;
import com.caspar.mapper.DormitoryMapper;
import com.caspar.mapper.DormitoryRepairMapper;
import com.caspar.mapper.UserDormitoryMapper;
import com.caspar.mapper.UserMapper;
import com.caspar.service.DormitoryService;
import com.caspar.service.NotificationService;
import com.caspar.util.PaginationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DormitoryServiceImpl implements DormitoryService {

    @Autowired
    private DormitoryMapper dormitoryMapper;

    @Autowired
    private DormitoryRepairMapper repairMapper;

    @Autowired
    private DormBuildingMapper buildingMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserDormitoryMapper userDormitoryMapper;

    @Autowired
    private NotificationService notificationService;

    @Override
    public PageResult<Dormitory> getDormitoryList(Long operatorId, String operatorRole, Integer page, Integer size, String building, Integer floor, String roomNo) {
        int safePage = PaginationUtils.safePage(page);
        int safeSize = PaginationUtils.safeSize(size);

        if ("ADMIN".equals(operatorRole)) {
            int offset = PaginationUtils.offset(safePage, safeSize);
            List<Dormitory> records = dormitoryMapper.selectList(building, floor, roomNo, offset, safeSize);
            Long total = dormitoryMapper.count(building, floor, roomNo);
            return new PageResult<>(records, total, safePage, safeSize);
        }

        if (!"DORM_MANAGER".equals(operatorRole)) {
            throw new IllegalArgumentException("无权限查看宿舍列表");
        }

        DormBuilding managedBuilding = getRequiredManagedBuilding(operatorId);
        if (building != null && !building.trim().isEmpty() && !matchesManagedBuilding(building.trim(), managedBuilding)) {
            return new PageResult<>(new ArrayList<>(), 0L, safePage, safeSize);
        }

        int offset = PaginationUtils.offset(safePage, safeSize);
        List<Dormitory> records = dormitoryMapper.selectManagedList(managedBuilding.getName(), managedBuilding.getCode(), floor, roomNo, offset, safeSize);
        Long total = dormitoryMapper.countManaged(managedBuilding.getName(), managedBuilding.getCode(), floor, roomNo);
        return new PageResult<>(records, total, safePage, safeSize);
    }

    @Override
    public DormitoryVO getDormitoryDetail(Long operatorId, String operatorRole, Long id) {
        Dormitory dormitory = dormitoryMapper.findById(id);
        if (dormitory == null) {
            throw new IllegalArgumentException("宿舍不存在");
        }
        ensureDormitoryReadPermission(operatorId, operatorRole, dormitory);

        DormitoryVO vo = new DormitoryVO();
        vo.setId(dormitory.getId());
        vo.setBuilding(dormitory.getBuilding());
        vo.setFloor(dormitory.getFloor());
        vo.setRoomNo(dormitory.getRoomNo());
        vo.setType(dormitory.getType());
        vo.setCapacity(dormitory.getCapacity());
        vo.setCurrentCount(dormitory.getCurrentCount());
        vo.setGender(dormitory.getGender());
        vo.setHeadId(dormitory.getHeadId());
        vo.setCreateTime(dormitory.getCreateTime());
        vo.setUpdateTime(dormitory.getUpdateTime());

        if (dormitory.getHeadId() != null) {
            User head = userMapper.findById(dormitory.getHeadId());
            vo.setHead(head);
        }

        List<DormitoryVO.MemberVO> members = dormitoryMapper.selectMembersByDormitoryId(id);
        vo.setMembers(members);

        return vo;
    }

    @Override
    public DormitoryVO getMyDormitory(Long userId) {
        DormitoryMember member = dormitoryMapper.findMemberByUserId(userId);
        if (member != null) {
            Dormitory dormitory = dormitoryMapper.findById(member.getDormitoryId());
            if (dormitory != null) {
                return buildDormitoryVO(dormitory);
            }
        }

        UserDormitory userDormitory = userDormitoryMapper.findByUserId(userId);
        if (userDormitory == null || userDormitory.getBuilding() == null || userDormitory.getRoom() == null) {
            return null;
        }

        Dormitory dormitory = dormitoryMapper.findByBuildingAndRoom(userDormitory.getBuilding(), userDormitory.getRoom());
        if (dormitory == null) {
            return null;
        }

        return buildDormitoryVO(dormitory);
    }

    private DormitoryVO buildDormitoryVO(Dormitory dormitory) {

        DormitoryVO vo = new DormitoryVO();
        vo.setId(dormitory.getId());
        vo.setBuilding(dormitory.getBuilding());
        vo.setFloor(dormitory.getFloor());
        vo.setRoomNo(dormitory.getRoomNo());
        vo.setType(dormitory.getType());
        vo.setCapacity(dormitory.getCapacity());
        vo.setCurrentCount(dormitory.getCurrentCount());
        vo.setGender(dormitory.getGender());
        vo.setHeadId(dormitory.getHeadId());
        vo.setCreateTime(dormitory.getCreateTime());
        vo.setUpdateTime(dormitory.getUpdateTime());
        if (dormitory.getHeadId() != null) {
            User head = userMapper.findById(dormitory.getHeadId());
            vo.setHead(head);
        }
        vo.setMembers(dormitoryMapper.selectMembersByDormitoryId(dormitory.getId()));
        return vo;
    }

    @Override
    public List<DormitoryVO.MemberVO> getDormitoryMembers(Long operatorId, String operatorRole, Long dormitoryId) {
        Dormitory dormitory = dormitoryMapper.findById(dormitoryId);
        if (dormitory == null) {
            throw new IllegalArgumentException("宿舍不存在");
        }
        ensureDormitoryReadPermission(operatorId, operatorRole, dormitory);
        return dormitoryMapper.selectMembersByDormitoryId(dormitoryId);
    }

    @Override
    @Transactional
    public Long createRepair(Long userId, RepairCreateDTO createDTO) {
        Long dormitoryId = createDTO.getDormitoryId();
        String campus;
        String building;
        String roomNo;
        
        if (createDTO.getDormitoryId() != null) {
            if (!isUserInDormitory(userId, createDTO.getDormitoryId())) {
                throw new IllegalArgumentException("您不属于该宿舍，无法提交报修");
            }
            Dormitory dormitory = dormitoryMapper.findById(createDTO.getDormitoryId());
            if (dormitory == null) {
                throw new IllegalArgumentException("宿舍不存在");
            }
            DormBuilding buildingInfo = findBuildingByDormitory(dormitory);
            campus = buildingInfo != null ? buildingInfo.getCampus() : createDTO.getCampus();
            building = dormitory.getBuilding();
            roomNo = dormitory.getRoomNo();
        } else if (createDTO.getCampus() != null && createDTO.getBuilding() != null && createDTO.getRoomNo() != null) {
            UserDormitory userDormitory = userDormitoryMapper.findByUserId(userId);
            if (userDormitory == null || userDormitory.getBuilding() == null || userDormitory.getRoom() == null) {
                throw new IllegalArgumentException("您还未分配宿舍，无法提交报修");
            }

            building = userDormitory.getBuilding();
            roomNo = userDormitory.getRoom();
            campus = userDormitory.getCampus();

            Dormitory dormitory = dormitoryMapper.findByBuildingAndRoom(building, roomNo);
            if (dormitory != null) {
                dormitoryId = dormitory.getId();
                DormBuilding buildingInfo = findBuildingByDormitory(dormitory);
                if (buildingInfo != null && buildingInfo.getCampus() != null) {
                    campus = buildingInfo.getCampus();
                }
            }
        } else {
            throw new IllegalArgumentException("请提供宿舍信息");
        }

        DormitoryRepair repair = new DormitoryRepair();
        repair.setDormitoryId(dormitoryId);
        repair.setCampus(campus);
        repair.setBuilding(building);
        repair.setRoomNo(roomNo);
        repair.setUserId(userId);
        repair.setDescription(createDTO.getDescription());
        repair.setImages(createDTO.getImages());
        repair.setUrgency(createDTO.getUrgency() != null ? createDTO.getUrgency() : "low");
        repair.setStatus("PENDING");
        repair.setCreateTime(LocalDateTime.now());

        repairMapper.insert(repair);

        LocalDateTime notificationTime = LocalDateTime.now();
        User applicant = userMapper.findById(userId);
        String applicantName = applicant != null && applicant.getRealName() != null && !applicant.getRealName().isBlank()
                ? applicant.getRealName()
                : (applicant != null ? applicant.getUsername() : "用户");

        DormBuilding buildingInfo = null;
        if (dormitoryId != null) {
            Dormitory resolvedDormitory = dormitoryMapper.findById(dormitoryId);
            buildingInfo = findBuildingByDormitory(resolvedDormitory);
        }
        if (buildingInfo == null && building != null) {
            buildingInfo = buildingMapper.findByName(building);
            if (buildingInfo == null) {
                buildingInfo = buildingMapper.findByCode(building);
            }
        }

        if (buildingInfo != null && buildingInfo.getManagerId() != null) {
            Notification managerNotification = new Notification();
            managerNotification.setUserId(buildingInfo.getManagerId());
            managerNotification.setType("DORMITORY_REPAIR");
            managerNotification.setTitle("新的宿舍报修申请");
            managerNotification.setContent(applicantName + " 提交了 " + building + "-" + roomNo + " 室的报修申请");
            managerNotification.setRelatedId(repair.getId());
            managerNotification.setRelatedType("DORMITORY_REPAIR");
            managerNotification.setCreateTime(notificationTime);
            notificationService.saveNotification(managerNotification);
        }

        return repair.getId();
    }

    @Override
    public PageResult<RepairVO> getMyRepairs(Long userId, Integer page, Integer size) {
        int safePage = PaginationUtils.safePage(page);
        int safeSize = PaginationUtils.safeSize(size);
        int offset = PaginationUtils.offset(safePage, safeSize);
        List<RepairVO> records = repairMapper.selectListByUserId(userId, offset, safeSize);
        normalizeRepairRecords(records);
        Long total = repairMapper.countByUserId(userId);
        return new PageResult<>(records, total, safePage, safeSize);
    }

    @Override
    public PageResult<RepairVO> getRepairList(Long operatorId, String operatorRole, Integer page, Integer size, String status, String building, String urgency) {
        int safePage = PaginationUtils.safePage(page);
        int safeSize = PaginationUtils.safeSize(size);
        String scopedBuilding = resolveRepairBuildingScope(operatorId, operatorRole, building);
        if (scopedBuilding == null && "DORM_MANAGER".equals(operatorRole) && building != null && !building.trim().isEmpty()) {
            return new PageResult<>(new ArrayList<>(), 0L, safePage, safeSize);
        }

        int offset = PaginationUtils.offset(safePage, safeSize);
        List<RepairVO> records = repairMapper.selectList(status, scopedBuilding, urgency, offset, safeSize);
        normalizeRepairRecords(records);
        Long total = repairMapper.count(status, scopedBuilding, urgency);
        return new PageResult<>(records, total, safePage, safeSize);
    }

    @Override
    public RepairVO getRepairDetail(Long id) {
        RepairVO repair = repairMapper.findVOById(id);
        if (repair == null) {
            throw new IllegalArgumentException("报修记录不存在");
        }
        normalizeRepairRecord(repair);
        return repair;
    }

    @Override
    @Transactional
    public boolean processRepair(Long id, String status, String remark, String handleImages, Long handlerId, String handlerRole) {
        DormitoryRepair repair = repairMapper.findById(id);
        if (repair == null) {
            throw new IllegalArgumentException("报修记录不存在");
        }
        ensureRepairManagePermission(handlerId, handlerRole, repair);

        if (!isValidStatus(status)) {
            throw new IllegalArgumentException("无效的报修状态");
        }

        int result = repairMapper.updateStatus(id, status, remark, handleImages, handlerId);

        if (result > 0 && "COMPLETED".equals(status)) {
            Notification notification = new Notification();
            notification.setUserId(repair.getUserId());
            notification.setType("REPAIR_COMPLETED");
            notification.setTitle("报修已完成");
            notification.setContent("您的报修申请已处理完成");
            notification.setRelatedId(id);
            notification.setRelatedType("DORMITORY_REPAIR");
            notification.setCreateTime(LocalDateTime.now());
            notificationService.saveNotification(notification);
        }

        return result > 0;
    }

    @Override
    @Transactional
    public boolean handleRepair(Long id, RepairHandleDTO handleDTO, Long handlerId, String handlerRole) {
        return processRepair(id, handleDTO.getStatus(), handleDTO.getRemark(), handleDTO.getHandleImages(), handlerId, handlerRole);
    }

    @Override
    @Transactional
    public boolean cancelRepairByUser(Long id, Long userId) {
        DormitoryRepair repair = repairMapper.findById(id);
        if (repair == null) {
            throw new IllegalArgumentException("报修记录不存在");
        }
        if (!repair.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权撤销此报修");
        }
        if (!"PENDING".equals(repair.getStatus())) {
            throw new IllegalArgumentException("当前状态不允许撤销");
        }
        
        int result = repairMapper.cancelByUser(id, userId, "PENDING");
        return result > 0;
    }

    @Override
    @Transactional
    public boolean updateRepairByUser(Long id, Long userId, String description, String urgency, String images) {
        DormitoryRepair repair = repairMapper.findById(id);
        if (repair == null) {
            throw new IllegalArgumentException("报修记录不存在");
        }
        if (!repair.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权编辑此报修");
        }
        if (!"CANCELLED".equals(repair.getStatus())) {
            throw new IllegalArgumentException("只有已撤销的报修可以编辑");
        }
        
        int result = repairMapper.updateByUser(id, userId, description, urgency, images);
        return result > 0;
    }

    @Override
    @Transactional
    public boolean reapplyRepairByUser(Long id, Long userId) {
        DormitoryRepair repair = repairMapper.findById(id);
        if (repair == null) {
            throw new IllegalArgumentException("报修记录不存在");
        }
        if (!repair.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此报修");
        }
        if (!"CANCELLED".equals(repair.getStatus())) {
            throw new IllegalArgumentException("只有已撤销的报修可以重新申请");
        }
        
        int result = repairMapper.reapplyByUser(id, userId);
        return result > 0;
    }

    @Override
    @Transactional
    public boolean deleteRepairByUser(Long id, Long userId) {
        DormitoryRepair repair = repairMapper.findById(id);
        if (repair == null) {
            throw new IllegalArgumentException("报修记录不存在");
        }
        if (!repair.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除此报修");
        }
        if (!"COMPLETED".equals(repair.getStatus()) && !"CANCELLED".equals(repair.getStatus())) {
            throw new IllegalArgumentException("只有已完成或已撤销的报修可以删除");
        }

        return repairMapper.deleteByUser(id, userId) > 0;
    }

    @Override
    @Transactional
    public boolean closeRepair(Long id, Long handlerId, String handlerRole) {
        return processRepair(id, "COMPLETED", "维修完成", null, handlerId, handlerRole);
    }

    @Override
    public Map<String, Integer> getRepairStats(Long operatorId, String operatorRole, String building) {
        String scopedBuilding = resolveRepairBuildingScope(operatorId, operatorRole, building);
        if (scopedBuilding == null && "DORM_MANAGER".equals(operatorRole) && building != null && !building.trim().isEmpty()) {
            return emptyRepairStats();
        }

        List<Map<String, Object>> statusCounts = repairMapper.countByStatus(scopedBuilding);
        Map<String, Integer> stats = new HashMap<>();
        stats.put("pending", 0);
        stats.put("processing", 0);
        stats.put("completed", 0);
        stats.put("total", 0);

        int total = 0;
        for (Map<String, Object> item : statusCounts) {
            String status = (String) item.get("status");
            Long cnt = (Long) item.get("cnt");
            int count = cnt != null ? cnt.intValue() : 0;
            total += count;

            if ("PENDING".equals(status)) {
                stats.put("pending", count);
            } else if ("PROCESSING".equals(status)) {
                stats.put("processing", count);
            } else if ("COMPLETED".equals(status)) {
                stats.put("completed", count);
            }
        }
        stats.put("total", total);

        return stats;
    }

    private Map<String, Integer> emptyRepairStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("pending", 0);
        stats.put("processing", 0);
        stats.put("completed", 0);
        stats.put("total", 0);
        return stats;
    }

    private void normalizeRepairRecords(List<RepairVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (RepairVO record : records) {
            normalizeRepairRecord(record);
        }
    }

    private void normalizeRepairRecord(RepairVO repair) {
        if (repair == null) {
            return;
        }

        Dormitory resolvedDormitory = null;
        if (repair.getDormitoryId() != null) {
            resolvedDormitory = dormitoryMapper.findById(repair.getDormitoryId());
        }

        if (resolvedDormitory == null && repair.getBuilding() != null && repair.getRoomNo() != null) {
            resolvedDormitory = dormitoryMapper.findByBuildingAndRoom(repair.getBuilding(), repair.getRoomNo());
        }

        UserDormitory userDormitory = null;
        if (repair.getUserId() != null) {
            userDormitory = userDormitoryMapper.findByUserId(repair.getUserId());
            if (resolvedDormitory == null && userDormitory != null && userDormitory.getBuilding() != null && userDormitory.getRoom() != null) {
                resolvedDormitory = dormitoryMapper.findByBuildingAndRoom(userDormitory.getBuilding(), userDormitory.getRoom());
            }
        }

        Long resolvedDormitoryId = repair.getDormitoryId();
        String resolvedBuilding = repair.getBuilding();
        String resolvedRoomNo = repair.getRoomNo();
        String resolvedCampus = repair.getCampus();

        if (resolvedDormitory != null) {
            resolvedDormitoryId = resolvedDormitory.getId();
            if (resolvedBuilding == null || resolvedBuilding.isBlank()) {
                resolvedBuilding = resolvedDormitory.getBuilding();
            }
            if (resolvedRoomNo == null || resolvedRoomNo.isBlank()) {
                resolvedRoomNo = resolvedDormitory.getRoomNo();
            }

            DormBuilding dormBuilding = findBuildingByDormitory(resolvedDormitory);
            if ((resolvedCampus == null || resolvedCampus.isBlank()) && dormBuilding != null && dormBuilding.getCampus() != null) {
                resolvedCampus = dormBuilding.getCampus();
            }
        }

        if (userDormitory != null) {
            if (resolvedBuilding == null || resolvedBuilding.isBlank()) {
                resolvedBuilding = userDormitory.getBuilding();
            }
            if (resolvedRoomNo == null || resolvedRoomNo.isBlank()) {
                resolvedRoomNo = userDormitory.getRoom();
            }
            if (resolvedCampus == null || resolvedCampus.isBlank()) {
                resolvedCampus = userDormitory.getCampus();
            }
        }

        boolean changed = !java.util.Objects.equals(repair.getDormitoryId(), resolvedDormitoryId)
                || !java.util.Objects.equals(repair.getCampus(), resolvedCampus)
                || !java.util.Objects.equals(repair.getBuilding(), resolvedBuilding)
                || !java.util.Objects.equals(repair.getRoomNo(), resolvedRoomNo);

        repair.setDormitoryId(resolvedDormitoryId);
        repair.setCampus(resolvedCampus);
        repair.setBuilding(resolvedBuilding);
        repair.setRoomNo(resolvedRoomNo);

        if (changed && repair.getId() != null) {
            repairMapper.updateResolvedLocation(repair.getId(), resolvedDormitoryId, resolvedCampus, resolvedBuilding, resolvedRoomNo);
        }
    }

    @Override
    public List<DormBuilding> getAllBuildings() {
        return buildingMapper.selectAll();
    }

    @Override
    @Transactional
    public DormBuilding createBuilding(Long operatorId, String operatorRole, DormBuildingCreateDTO createDTO) {
        if (!"ADMIN".equals(operatorRole)) {
            throw new IllegalArgumentException("只有管理员可以新增楼栋");
        }
        if (createDTO.getName() == null || createDTO.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("楼栋名称不能为空");
        }
        if (createDTO.getCampus() == null || createDTO.getCampus().trim().isEmpty()) {
            throw new IllegalArgumentException("校区不能为空");
        }
        if (createDTO.getFloors() == null || createDTO.getFloors() < 1) {
            throw new IllegalArgumentException("楼层数必须大于 0");
        }
        if (createDTO.getRoomsPerFloor() == null || createDTO.getRoomsPerFloor() < 1) {
            throw new IllegalArgumentException("每层房间数必须大于 0");
        }
        if (createDTO.getCapacityPerRoom() == null || createDTO.getCapacityPerRoom() < 1) {
            throw new IllegalArgumentException("每间房入住人数必须大于 0");
        }
        if (buildingMapper.findByName(createDTO.getName().trim()) != null) {
            throw new IllegalArgumentException("楼栋名称已存在");
        }

        String generatedCode = buildDormBuildingCode(createDTO.getCampus().trim(), createDTO.getName().trim());
        if (buildingMapper.findByCode(generatedCode) != null) {
            generatedCode = generatedCode + "-" + System.currentTimeMillis();
        }

        DormBuilding building = new DormBuilding();
        building.setName(createDTO.getName().trim());
        building.setCode(generatedCode);
        building.setCampus(createDTO.getCampus().trim());
        building.setFloors(createDTO.getFloors());
        building.setRoomsPerFloor(createDTO.getRoomsPerFloor());
        building.setCapacityPerRoom(createDTO.getCapacityPerRoom());
        building.setGender(createDTO.getGender());
        building.setManagerId(null);
        buildingMapper.insert(building);

        createDormitoriesForBuilding(building);

        if (createDTO.getManagerId() != null) {
            return assignBuildingManager(operatorId, building.getId(), createDTO.getManagerId());
        }
        return buildingMapper.findById(building.getId());
    }

    @Override
    @Transactional
    public DormBuilding updateBuilding(Long operatorId, String operatorRole, Long buildingId, DormBuildingUpdateDTO updateDTO) {
        if (!"ADMIN".equals(operatorRole)) {
            throw new IllegalArgumentException("只有管理员可以修改楼栋");
        }
        if (buildingId == null) {
            throw new IllegalArgumentException("楼栋ID不能为空");
        }
        if (updateDTO == null) {
            throw new IllegalArgumentException("缺少楼栋更新参数");
        }

        DormBuilding existing = buildingMapper.findById(buildingId);
        if (existing == null) {
            throw new IllegalArgumentException("楼栋不存在");
        }

        String newName = updateDTO.getName() == null ? null : updateDTO.getName().trim();
        String newCampus = updateDTO.getCampus() == null ? null : updateDTO.getCampus().trim();
        Integer newCapacityPerRoom = updateDTO.getCapacityPerRoom();
        String newGender = normalizeBuildingGender(updateDTO.getGender());

        if (newName == null || newName.isEmpty()) {
            throw new IllegalArgumentException("楼栋名称不能为空");
        }
        if (newCampus == null || newCampus.isEmpty()) {
            throw new IllegalArgumentException("校区不能为空");
        }
        if (newCapacityPerRoom == null || newCapacityPerRoom < 1) {
            throw new IllegalArgumentException("每间房入住人数必须大于 0");
        }

        DormBuilding duplicateByName = buildingMapper.findByName(newName);
        if (duplicateByName != null && !duplicateByName.getId().equals(buildingId)) {
            throw new IllegalArgumentException("楼栋名称已存在");
        }

        Integer overCapacityCount = dormitoryMapper.countOverCapacity(existing.getName(), existing.getCode(), newCapacityPerRoom);
        if (overCapacityCount != null && overCapacityCount > 0) {
            throw new IllegalArgumentException("存在已入住人数超过新容量的宿舍，请先调整人员分配");
        }

        String oldName = existing.getName();
        String oldCode = existing.getCode();

        existing.setName(newName);
        existing.setCampus(newCampus);
        existing.setCapacityPerRoom(newCapacityPerRoom);
        existing.setGender(newGender);
        buildingMapper.update(existing);

        String roomType = newCapacityPerRoom + "人间";
        syncBuildingRelatedRecords(oldName, oldCode, newName, newCampus, newGender, newCapacityPerRoom, roomType);

        return buildingMapper.findById(buildingId);
    }

    @Override
    @Transactional
    public DormBuilding assignBuildingManager(Long operatorId, Long buildingId, Long managerId) {
        DormBuilding building = buildingMapper.findById(buildingId);
        if (building == null) {
            throw new IllegalArgumentException("楼栋不存在");
        }

        if (managerId != null) {
            User manager = userMapper.findById(managerId);
            if (manager == null) {
                throw new IllegalArgumentException("宿管不存在");
            }
            if (!"DORM_MANAGER".equals(manager.getRole())) {
                throw new IllegalArgumentException("只能将楼栋分配给宿管角色");
            }
            buildingMapper.clearManagerBinding(managerId);
        }

        building.setManagerId(managerId);
        buildingMapper.update(building);
        return buildingMapper.findById(buildingId);
    }

    @Override
    public DormBuilding getBuildingByManager(Long managerId) {
        return buildingMapper.findByManagerId(managerId);
    }

    @Override
    public PageResult<DormitoryAssignableUserVO> getAssignableUsers(Long operatorId, String operatorRole, Integer page, Integer size, String keyword, Boolean assigned) {
        int safePage = PaginationUtils.safePage(page);
        int safeSize = PaginationUtils.safeSize(size);
        int offset = PaginationUtils.offset(safePage, safeSize);
        boolean allowSelfDormManager = "DORM_MANAGER".equals(operatorRole);
        List<DormitoryAssignableUserVO> records = userMapper.selectDormitoryAssignableUsers(operatorId, allowSelfDormManager, keyword, assigned, offset, safeSize);
        Long total = userMapper.countDormitoryAssignableUsers(operatorId, allowSelfDormManager, keyword, assigned);
        return new PageResult<>(records, total, safePage, safeSize);
    }

    @Override
    @Transactional
    public void assignDormitory(Long operatorId, String operatorRole, DormitoryAssignDTO assignDTO) {
        if (assignDTO.getUserId() == null || assignDTO.getDormitoryId() == null) {
            throw new IllegalArgumentException("请先选择用户和宿舍");
        }
        if (assignDTO.getBed() == null || assignDTO.getBed().trim().isEmpty()) {
            throw new IllegalArgumentException("请填写床位信息");
        }

        User user = userMapper.findById(assignDTO.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        boolean allowedCommonRole = "STUDENT".equals(user.getRole()) || "TEACHER".equals(user.getRole());
        boolean allowedDormManagerSelfAssign = "DORM_MANAGER".equals(user.getRole())
                && "DORM_MANAGER".equals(operatorRole)
                && assignDTO.getUserId().equals(operatorId);
        if (!allowedCommonRole && !allowedDormManagerSelfAssign) {
            throw new IllegalArgumentException("当前仅支持给学生或教师分配宿舍");
        }

        Dormitory dormitory = dormitoryMapper.findById(assignDTO.getDormitoryId());
        if (dormitory == null) {
            throw new IllegalArgumentException("宿舍不存在");
        }
        ensureDormitoryPermission(operatorId, operatorRole, dormitory);

        DormitoryMember existingMember = dormitoryMapper.findMemberByUserId(assignDTO.getUserId());
        if (existingMember != null) {
            throw new IllegalArgumentException("该用户已分配宿舍，请先退宿");
        }
        if (dormitory.getCapacity() != null && dormitory.getCurrentCount() != null
                && dormitory.getCurrentCount() >= dormitory.getCapacity()) {
            throw new IllegalArgumentException("该宿舍已满员，无法继续分配");
        }

        DormitoryMember member = new DormitoryMember();
        member.setDormitoryId(dormitory.getId());
        member.setUserId(assignDTO.getUserId());
        dormitoryMapper.insertMember(member);
        dormitoryMapper.updateCurrentCount(dormitory.getId(), 1);

        UserDormitory userDormitory = new UserDormitory();
        userDormitory.setUserId(assignDTO.getUserId());
        DormBuilding buildingInfo = findBuildingByDormitory(dormitory);
        userDormitory.setCampus(buildingInfo != null && buildingInfo.getCampus() != null
            ? buildingInfo.getCampus()
            : (assignDTO.getCampus() != null ? assignDTO.getCampus() : "未设置"));
        userDormitory.setBuilding(dormitory.getBuilding());
        userDormitory.setRoom(dormitory.getRoomNo());
        userDormitory.setBed(assignDTO.getBed());
        userDormitory.setCheckInDate((assignDTO.getCheckInDate() != null
            ? assignDTO.getCheckInDate()
            : LocalDate.now()).atStartOfDay());

        UserDormitory existingDormitory = userDormitoryMapper.findByUserId(assignDTO.getUserId());
        if (existingDormitory != null) {
            userDormitory.setId(existingDormitory.getId());
            userDormitoryMapper.update(userDormitory);
        } else {
            userDormitoryMapper.insert(userDormitory);
        }
    }

    @Override
    @Transactional
    public void checkoutDormitory(Long operatorId, String operatorRole, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        DormitoryMember member = dormitoryMapper.findMemberByUserId(userId);
        if (member == null) {
            throw new IllegalArgumentException("该用户当前未分配宿舍");
        }

        Dormitory dormitory = dormitoryMapper.findById(member.getDormitoryId());
        if (dormitory == null) {
            throw new IllegalArgumentException("宿舍不存在");
        }
        ensureDormitoryPermission(operatorId, operatorRole, dormitory);

        dormitoryMapper.deleteMember(member.getDormitoryId(), userId);
        dormitoryMapper.updateCurrentCount(member.getDormitoryId(), -1);
        userDormitoryMapper.deleteByUserId(userId);

        if (dormitory.getHeadId() != null && dormitory.getHeadId().equals(userId)) {
            dormitoryMapper.updateHeadId(dormitory.getId(), null);
        }
    }

    @Override
    public boolean isUserInDormitory(Long userId, Long dormitoryId) {
        return isUserAssignedToDormitory(userId, dormitoryId);
    }

    private void ensureDormitoryPermission(Long operatorId, String operatorRole, Dormitory dormitory) {
        if ("ADMIN".equals(operatorRole)) {
            return;
        }
        if (!"DORM_MANAGER".equals(operatorRole)) {
            throw new IllegalArgumentException("无权限操作宿舍分配");
        }

        DormBuilding managedBuilding = buildingMapper.findByManagerId(operatorId);
        if (managedBuilding == null) {
            throw new IllegalArgumentException("当前宿管未绑定管理楼栋");
        }

        String building = dormitory.getBuilding();
        boolean matched = building != null && (
                building.equalsIgnoreCase(managedBuilding.getCode())
                        || building.equalsIgnoreCase(managedBuilding.getName())
        );
        if (!matched) {
            throw new IllegalArgumentException("只能管理自己负责楼栋的宿舍");
        }
    }

    private void ensureDormitoryReadPermission(Long operatorId, String operatorRole, Dormitory dormitory) {
        if ("ADMIN".equals(operatorRole)) {
            return;
        }
        if ("DORM_MANAGER".equals(operatorRole)) {
            ensureDormitoryPermission(operatorId, operatorRole, dormitory);
            return;
        }

        if (!isUserAssignedToDormitory(operatorId, dormitory.getId())) {
            throw new IllegalArgumentException("无权限查看该宿舍信息");
        }
    }

    private boolean isUserAssignedToDormitory(Long userId, Long dormitoryId) {
        if (userId == null || dormitoryId == null) {
            return false;
        }

        DormitoryMember member = dormitoryMapper.findMemberByUserId(userId);
        if (member != null && dormitoryId.equals(member.getDormitoryId())) {
            return true;
        }

        UserDormitory userDormitory = userDormitoryMapper.findByUserId(userId);
        if (userDormitory == null || userDormitory.getBuilding() == null || userDormitory.getRoom() == null) {
            return false;
        }

        Dormitory dormitory = dormitoryMapper.findByBuildingAndRoom(userDormitory.getBuilding(), userDormitory.getRoom());
        return dormitory != null && dormitoryId.equals(dormitory.getId());
    }

    private DormBuilding getRequiredManagedBuilding(Long operatorId) {
        DormBuilding managedBuilding = buildingMapper.findByManagerId(operatorId);
        if (managedBuilding == null) {
            throw new IllegalArgumentException("当前宿管未绑定管理楼栋");
        }
        return managedBuilding;
    }

    private boolean matchesManagedBuilding(String buildingName, DormBuilding managedBuilding) {
        if (managedBuilding == null || buildingName == null) {
            return false;
        }
        return buildingName.equalsIgnoreCase(managedBuilding.getName())
                || (managedBuilding.getCode() != null && buildingName.equalsIgnoreCase(managedBuilding.getCode()));
    }

    private String resolveRepairBuildingScope(Long operatorId, String operatorRole, String building) {
        String normalizedBuilding = building == null ? null : building.trim();
        if (normalizedBuilding != null && normalizedBuilding.isEmpty()) {
            normalizedBuilding = null;
        }

        if ("ADMIN".equals(operatorRole)) {
            return normalizedBuilding;
        }

        if (!"DORM_MANAGER".equals(operatorRole)) {
            throw new IllegalArgumentException("无权限查看报修列表");
        }

        DormBuilding managedBuilding = getRequiredManagedBuilding(operatorId);
        if (normalizedBuilding != null && !matchesManagedBuilding(normalizedBuilding, managedBuilding)) {
            return null;
        }
        return managedBuilding.getName();
    }

    private void ensureRepairManagePermission(Long operatorId, String operatorRole, DormitoryRepair repair) {
        if ("ADMIN".equals(operatorRole)) {
            return;
        }

        if (!"DORM_MANAGER".equals(operatorRole)) {
            throw new IllegalArgumentException("无权限处理报修");
        }

        DormBuilding managedBuilding = getRequiredManagedBuilding(operatorId);
        String repairBuilding = resolveRepairBuildingName(repair);
        if (repairBuilding == null || !matchesManagedBuilding(repairBuilding, managedBuilding)) {
            throw new IllegalArgumentException("只能处理自己负责楼栋的报修申请");
        }
    }

    private String resolveRepairBuildingName(DormitoryRepair repair) {
        if (repair == null) {
            return null;
        }

        if (repair.getDormitoryId() != null) {
            Dormitory dormitory = dormitoryMapper.findById(repair.getDormitoryId());
            if (dormitory != null && dormitory.getBuilding() != null && !dormitory.getBuilding().isBlank()) {
                return dormitory.getBuilding();
            }
        }

        if (repair.getBuilding() != null && !repair.getBuilding().isBlank()) {
            return repair.getBuilding();
        }

        if (repair.getUserId() != null) {
            UserDormitory userDormitory = userDormitoryMapper.findByUserId(repair.getUserId());
            if (userDormitory != null && userDormitory.getBuilding() != null && !userDormitory.getBuilding().isBlank()) {
                return userDormitory.getBuilding();
            }
        }

        return null;
    }

    private String buildDormBuildingCode(String campus, String name) {
        return (campus + "-" + name).replaceAll("\\s+", "");
    }

    private String normalizeBuildingGender(String gender) {
        if (gender == null) {
            return null;
        }
        String normalized = gender.trim().toUpperCase();
        if (normalized.isEmpty()) {
            return null;
        }
        if (!"MALE".equals(normalized) && !"FEMALE".equals(normalized)) {
            throw new IllegalArgumentException("楼栋性别限制仅支持 MALE/FEMALE");
        }
        return normalized;
    }

    private void syncBuildingRelatedRecords(String oldName,
                                            String oldCode,
                                            String newName,
                                            String newCampus,
                                            String newGender,
                                            Integer newCapacity,
                                            String roomType) {
        if (oldName != null && !oldName.isBlank()) {
            dormitoryMapper.updateByBuilding(oldName, newName, newGender, newCapacity, roomType);
            userDormitoryMapper.updateBuildingAndCampus(oldName, newName, newCampus);
            repairMapper.updateBuildingAndCampus(oldName, newName, newCampus);
        }

        if (oldCode != null && !oldCode.isBlank() && !oldCode.equals(oldName)) {
            dormitoryMapper.updateByBuilding(oldCode, newName, newGender, newCapacity, roomType);
            userDormitoryMapper.updateBuildingAndCampus(oldCode, newName, newCampus);
            repairMapper.updateBuildingAndCampus(oldCode, newName, newCampus);
        }
    }

    private void createDormitoriesForBuilding(DormBuilding building) {
        for (int floor = 1; floor <= building.getFloors(); floor++) {
            for (int roomIndex = 1; roomIndex <= building.getRoomsPerFloor(); roomIndex++) {
                Dormitory dormitory = new Dormitory();
                dormitory.setBuilding(building.getName());
                dormitory.setFloor(floor);
                dormitory.setRoomNo(String.format("%d%02d", floor, roomIndex));
                dormitory.setType(building.getCapacityPerRoom() + "人间");
                dormitory.setCapacity(building.getCapacityPerRoom());
                dormitory.setCurrentCount(0);
                dormitory.setGender(building.getGender());
                dormitoryMapper.insert(dormitory);
            }
        }
    }

    private DormBuilding findBuildingByDormitory(Dormitory dormitory) {
        if (dormitory == null || dormitory.getBuilding() == null) {
            return null;
        }
        DormBuilding building = buildingMapper.findByName(dormitory.getBuilding());
        if (building == null) {
            building = buildingMapper.findByCode(dormitory.getBuilding());
        }
        return building;
    }

    @Override
    @Transactional
    public boolean startRepair(Long id, Long handlerId, String handlerRole) {
        DormitoryRepair repair = repairMapper.findById(id);
        if (repair == null) {
            throw new IllegalArgumentException("报修记录不存在");
        }
        ensureRepairManagePermission(handlerId, handlerRole, repair);
        if (!"PENDING".equals(repair.getStatus())) {
            throw new IllegalArgumentException("只有待处理的报修可以开始处理");
        }
        
        int result = repairMapper.startRepair(id, handlerId);
        if (result == 0) {
            throw new IllegalStateException("状态已变更，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional
    public boolean completeRepair(Long id, Long handlerId, String handlerRole, String remark, String handleImages) {
        DormitoryRepair repair = repairMapper.findById(id);
        if (repair == null) {
            throw new IllegalArgumentException("报修记录不存在");
        }
        ensureRepairManagePermission(handlerId, handlerRole, repair);
        if (!"PROCESSING".equals(repair.getStatus())) {
            throw new IllegalArgumentException("只有处理中的报修可以标记完成");
        }
        if ((remark == null || remark.trim().isEmpty()) && (handleImages == null || handleImages.trim().isEmpty())) {
            throw new IllegalArgumentException("请填写处理备注或上传处理照片");
        }
        
        int result = repairMapper.completeRepair(id, handlerId, remark, handleImages);
        if (result == 0) {
            throw new IllegalStateException("状态已变更，请刷新后重试");
        }
        return true;
    }

    private boolean isValidStatus(String status) {
        return "PENDING".equals(status) || "PROCESSING".equals(status) || "COMPLETED".equals(status);
    }
}
