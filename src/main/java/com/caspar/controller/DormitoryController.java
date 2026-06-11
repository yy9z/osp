package com.caspar.controller;

import com.caspar.common.PageResult;
import com.caspar.common.Result;
import com.caspar.common.UserRole;
import com.caspar.entity.Dormitory;
import com.caspar.entity.DormBuilding;
import com.caspar.entity.dto.*;
import com.caspar.service.DormitoryService;
import com.caspar.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 宿舍管理控制器
 * 提供宿舍信息查询、报修申请及审批等功能
 */
@RestController
@RequestMapping("/api/dormitory")
public class DormitoryController {

    @Autowired
    private DormitoryService dormitoryService;

    @GetMapping("/list")
    public Result<PageResult<Dormitory>> getDormitoryList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) Integer floor,
            @RequestParam(required = false) String roomNo) {

        Long operatorId = SecurityUtils.getCurrentUserId();
        if (operatorId == null) {
            return Result.unauthorized();
        }

        String currentRole = SecurityUtils.getCurrentUserRole();

        try {
            PageResult<Dormitory> pageResult = dormitoryService.getDormitoryList(operatorId, currentRole, page, size, building, floor, roomNo);
            return Result.success(pageResult);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取宿舍列表失败");
        }
    }

    @GetMapping("/{id}")
    public Result<DormitoryVO> getDormitoryDetail(@PathVariable Long id) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        if (operatorId == null) {
            return Result.unauthorized();
        }

        String currentRole = SecurityUtils.getCurrentUserRole();

        try {
            DormitoryVO vo = dormitoryService.getDormitoryDetail(operatorId, currentRole, id);
            return Result.success(vo);
        } catch (IllegalArgumentException e) {
            return Result.notFound(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取宿舍详情失败");
        }
    }

    @GetMapping("/my")
    public Result<DormitoryVO> getMyDormitory() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            DormitoryVO vo = dormitoryService.getMyDormitory(userId);
            if (vo == null) {
                return Result.success("您还未分配宿舍", null);
            }
            return Result.success(vo);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取我的宿舍失败");
        }
    }

    @GetMapping("/{id}/members")
    public Result<List<DormitoryVO.MemberVO>> getDormitoryMembers(@PathVariable Long id) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        if (operatorId == null) {
            return Result.unauthorized();
        }

        String currentRole = SecurityUtils.getCurrentUserRole();

        try {
            List<DormitoryVO.MemberVO> members = dormitoryService.getDormitoryMembers(operatorId, currentRole, id);
            return Result.success(members);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取宿舍成员失败");
        }
    }

    @GetMapping("/buildings")
    public Result<List<DormBuilding>> getAllBuildings() {
        try {
            List<DormBuilding> buildings = dormitoryService.getAllBuildings();
            return Result.success(buildings);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取楼栋列表失败");
        }
    }

    @PostMapping("/buildings")
    public Result<DormBuilding> createBuilding(@Valid @RequestBody DormBuildingCreateDTO createDTO) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        if (operatorId == null) {
            return Result.unauthorized();
        }

        String currentRole = SecurityUtils.getCurrentUserRole();
        if (!isAdmin(currentRole)) {
            return Result.forbidden();
        }

        try {
            DormBuilding building = dormitoryService.createBuilding(operatorId, currentRole, createDTO);
            return Result.success("楼栋创建成功", building);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("创建楼栋失败");
        }
    }

    @PutMapping("/buildings/{id}")
    public Result<DormBuilding> updateBuilding(@PathVariable Long id, @Valid @RequestBody DormBuildingUpdateDTO updateDTO) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        if (operatorId == null) {
            return Result.unauthorized();
        }

        String currentRole = SecurityUtils.getCurrentUserRole();
        if (!isAdmin(currentRole)) {
            return Result.forbidden();
        }

        try {
            DormBuilding building = dormitoryService.updateBuilding(operatorId, currentRole, id, updateDTO);
            return Result.success("楼栋信息更新成功", building);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("更新楼栋失败");
        }
    }

    @PutMapping("/buildings/{id}/manager")
    public Result<DormBuilding> assignBuildingManager(@PathVariable Long id, @Valid @RequestBody DormBuildingManagerAssignDTO assignDTO) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        if (operatorId == null) {
            return Result.unauthorized();
        }

        String currentRole = SecurityUtils.getCurrentUserRole();
        if (!isAdmin(currentRole)) {
            return Result.forbidden();
        }

        try {
            DormBuilding building = dormitoryService.assignBuildingManager(operatorId, id, assignDTO.getManagerId());
            return Result.success("楼栋负责人已更新", building);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("更新楼栋负责人失败");
        }
    }

    @GetMapping("/building/my")
    public Result<DormBuilding> getMyBuilding() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            DormBuilding building = dormitoryService.getBuildingByManager(userId);
            if (building == null) {
                return Result.success("您不是宿管员", null);
            }
            return Result.success(building);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取管理楼栋失败");
        }
    }

    @GetMapping("/assignable-users")
    public Result<PageResult<DormitoryAssignableUserVO>> getAssignableUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean assigned) {

        Long operatorId = SecurityUtils.getCurrentUserId();
        if (operatorId == null) {
            return Result.unauthorized();
        }

        String currentRole = SecurityUtils.getCurrentUserRole();
        if (!isAdminOrDormManager(currentRole)) {
            return Result.forbidden();
        }

        try {
            PageResult<DormitoryAssignableUserVO> pageResult = dormitoryService.getAssignableUsers(operatorId, currentRole, page, size, keyword, assigned);
            return Result.success(pageResult);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取可分配用户失败");
        }
    }

    @PostMapping("/assign")
    public Result<Void> assignDormitory(@Valid @RequestBody DormitoryAssignDTO assignDTO) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        if (operatorId == null) {
            return Result.unauthorized();
        }

        String currentRole = SecurityUtils.getCurrentUserRole();
        if (!isAdminOrDormManager(currentRole)) {
            return Result.forbidden();
        }

        try {
            dormitoryService.assignDormitory(operatorId, currentRole, assignDTO);
            return Result.success("宿舍分配成功", null);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("宿舍分配失败");
        }
    }

    @PutMapping("/checkout/{userId}")
    public Result<Void> checkoutDormitory(@PathVariable Long userId) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        if (operatorId == null) {
            return Result.unauthorized();
        }

        String currentRole = SecurityUtils.getCurrentUserRole();
        if (!isAdminOrDormManager(currentRole)) {
            return Result.forbidden();
        }

        try {
            dormitoryService.checkoutDormitory(operatorId, currentRole, userId);
            return Result.success("退宿成功", null);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("退宿失败");
        }
    }

    @PostMapping("/repair")
    public Result<Map<String, Long>> createRepair(@Valid @RequestBody RepairCreateDTO createDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        boolean hasDormitoryId = createDTO.getDormitoryId() != null;
        boolean hasUserDormitory = createDTO.getCampus() != null && 
                                    createDTO.getBuilding() != null && 
                                    createDTO.getRoomNo() != null;
        
        if (!hasDormitoryId && !hasUserDormitory) {
            return Result.badRequest("请提供宿舍信息");
        }
        if (createDTO.getDescription() == null || createDTO.getDescription().isEmpty()) {
            return Result.badRequest("报修描述不能为空");
        }

        try {
            Long repairId = dormitoryService.createRepair(userId, createDTO);
            Map<String, Long> data = new HashMap<>();
            data.put("repairId", repairId);
            return Result.success("报修提交成功", data);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("提交报修失败");
        }
    }

    @GetMapping("/repair/my")
    public Result<PageResult<RepairVO>> getMyRepairs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            PageResult<RepairVO> pageResult = dormitoryService.getMyRepairs(userId, page, size);
            return Result.success(pageResult);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取报修列表失败");
        }
    }

    @GetMapping("/repair/list")
    public Result<PageResult<RepairVO>> getRepairList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String urgency) {

        Long operatorId = SecurityUtils.getCurrentUserId();
        String currentRole = SecurityUtils.getCurrentUserRole();
        if (operatorId == null) {
            return Result.unauthorized();
        }
        if (!isAdminOrDormManager(currentRole)) {
            return Result.forbidden();
        }

        try {
            PageResult<RepairVO> pageResult = dormitoryService.getRepairList(operatorId, currentRole, page, size, status, building, urgency);
            return Result.success(pageResult);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取报修列表失败");
        }
    }

    @GetMapping("/repair/stats")
    public Result<Map<String, Integer>> getRepairStats(@RequestParam(required = false) String building) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        if (operatorId == null) {
            return Result.unauthorized();
        }

        String currentRole = SecurityUtils.getCurrentUserRole();
        if (!isAdminOrDormManager(currentRole)) {
            return Result.forbidden();
        }

        try {
            Map<String, Integer> stats = dormitoryService.getRepairStats(operatorId, currentRole, building);
            return Result.success(stats);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取统计数据失败");
        }
    }

    @PutMapping("/repair/{id}/handle")
    public Result<Void> handleRepair(
            @PathVariable Long id,
            @Valid @RequestBody RepairHandleDTO handleDTO) {

        Long handlerId = SecurityUtils.getCurrentUserId();
        if (handlerId == null) {
            return Result.unauthorized();
        }

        String currentRole = SecurityUtils.getCurrentUserRole();
        if (!isAdminOrDormManager(currentRole)) {
            return Result.forbidden();
        }

        if (handleDTO.getStatus() == null || handleDTO.getStatus().isEmpty()) {
            return Result.badRequest("状态不能为空");
        }

        try {
            boolean success = dormitoryService.handleRepair(id, handleDTO, handlerId, currentRole);
            if (success) {
                return Result.success("处理成功", null);
            } else {
                return Result.error("处理失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("处理报修失败");
        }
    }

    @PutMapping("/repair/{id}/close")
    public Result<Void> closeRepair(@PathVariable Long id) {
        Long handlerId = SecurityUtils.getCurrentUserId();
        if (handlerId == null) {
            return Result.unauthorized();
        }

        String currentRole = SecurityUtils.getCurrentUserRole();
        if (!isAdminOrDormManager(currentRole)) {
            return Result.forbidden();
        }

        try {
            boolean success = dormitoryService.closeRepair(id, handlerId, currentRole);
            if (success) {
                return Result.success("报修已关闭", null);
            } else {
                return Result.error("关闭失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("关闭报修失败");
        }
    }

    @PutMapping("/repair/{id}/cancel")
    public Result<Void> cancelRepair(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            boolean success = dormitoryService.cancelRepairByUser(id, userId);
            if (success) {
                return Result.success("撤销成功", null);
            } else {
                return Result.badRequest("撤销失败，请刷新后重试");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("撤销失败");
        }
    }

    @PutMapping("/repair/{id}/reapply")
    public Result<Void> reapplyRepair(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            boolean success = dormitoryService.reapplyRepairByUser(id, userId);
            if (success) {
                return Result.success("重新申请成功", null);
            } else {
                return Result.badRequest("重新申请失败，请刷新后重试");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("重新申请失败");
        }
    }

    @DeleteMapping("/repair/{id}")
    public Result<Void> deleteRepair(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            boolean success = dormitoryService.deleteRepairByUser(id, userId);
            if (success) {
                return Result.success("删除成功", null);
            } else {
                return Result.badRequest("删除失败，请刷新后重试");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("删除失败");
        }
    }

    @PutMapping("/repair/{id}")
    public Result<Void> updateRepair(
            @PathVariable Long id,
            @Valid @RequestBody RepairCreateDTO updateDTO) {
        
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            boolean success = dormitoryService.updateRepairByUser(
                id, 
                userId, 
                updateDTO.getDescription(), 
                updateDTO.getUrgency(), 
                updateDTO.getImages()
            );
            if (success) {
                return Result.success("修改成功", null);
            } else {
                return Result.badRequest("修改失败，请刷新后重试");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("修改失败");
        }
    }

    @PutMapping("/repair/{id}/admin")
    public Result<Void> processRepair(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String remark) {

        String currentRole = SecurityUtils.getCurrentUserRole();
        if (!isAdmin(currentRole)) {
            return Result.forbidden();
        }

        if (status == null || status.isEmpty()) {
            return Result.badRequest("状态不能为空");
        }

        try {
            boolean success = dormitoryService.processRepair(id, status, remark, null, SecurityUtils.getCurrentUserId(), currentRole);
            if (success) {
                return Result.success("处理成功", null);
            } else {
                return Result.error("处理失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("处理报修失败");
        }
    }

    @PostMapping("/repair/{id}/start")
    public Result<Void> startRepair(@PathVariable Long id) {
        Long handlerId = SecurityUtils.getCurrentUserId();
        if (handlerId == null) {
            return Result.unauthorized();
        }

        String currentRole = SecurityUtils.getCurrentUserRole();
        if (!isAdminOrDormManager(currentRole)) {
            return Result.forbidden();
        }

        try {
            boolean success = dormitoryService.startRepair(id, handlerId, currentRole);
            if (success) {
                return Result.success("已开始处理", null);
            } else {
                return Result.error("操作失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error("状态已变更，请刷新后重试");
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("操作失败");
        }
    }

    @PostMapping("/repair/{id}/complete")
    public Result<Void> completeRepair(
            @PathVariable Long id,
            @Valid @RequestBody CompleteRepairRequest request) {

        Long handlerId = SecurityUtils.getCurrentUserId();
        if (handlerId == null) {
            return Result.unauthorized();
        }

        String currentRole = SecurityUtils.getCurrentUserRole();
        if (!isAdminOrDormManager(currentRole)) {
            return Result.forbidden();
        }

        try {
            boolean success = dormitoryService.completeRepair(id, handlerId, currentRole, request.getRemark(), request.getHandleImages());
            if (success) {
                return Result.success("处理完成", null);
            } else {
                return Result.error("操作失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error("状态已变更，请刷新后重试");
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("操作失败");
        }
    }

    private boolean isAdmin(String role) {
        return UserRole.ADMIN.name().equals(role);
    }

    private boolean isAdminOrDormManager(String role) {
        return UserRole.ADMIN.name().equals(role) || UserRole.DORM_MANAGER.name().equals(role);
    }

}
