package com.caspar.controller;

import com.caspar.common.PageResult;
import com.caspar.common.Result;
import com.caspar.entity.dto.SecondhandGoodsVO;
import com.caspar.entity.dto.SecondhandPublishDTO;
import com.caspar.service.SecondhandGoodsService;
import com.caspar.util.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/secondhand")
public class SecondhandAdminController {

    @Autowired
    private SecondhandGoodsService secondhandGoodsService;

    @GetMapping("/pending")
    public Result<PageResult<SecondhandGoodsVO>> getPendingList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {

        String role = SecurityUtils.getCurrentUserRole();
        if (!isAdmin(role)) {
            return Result.forbidden();
        }

        try {
            PageResult<SecondhandGoodsVO> pageResult = secondhandGoodsService.getPendingList(page, size, category, keyword);
            return Result.success(pageResult);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取待审核列表失败");
        }
    }

    @GetMapping("/all")
    public Result<PageResult<SecondhandGoodsVO>> getAllList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {

        String role = SecurityUtils.getCurrentUserRole();
        if (!isAdmin(role)) {
            return Result.forbidden();
        }

        try {
            PageResult<SecondhandGoodsVO> pageResult = secondhandGoodsService.getAllList(page, size, category, keyword, status);
            return Result.success(pageResult);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取列表失败");
        }
    }

    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        String role = SecurityUtils.getCurrentUserRole();
        if (!isAdmin(role)) {
            return Result.forbidden();
        }

        try {
            boolean success = secondhandGoodsService.approve(id, userId);
            if (success) {
                return Result.success("审核通过", null);
            } else {
                return Result.error("审核失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("审核失败");
        }
    }

    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody RejectRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        String role = SecurityUtils.getCurrentUserRole();
        if (!isAdmin(role)) {
            return Result.forbidden();
        }

        try {
            boolean success = secondhandGoodsService.reject(id, userId, request.getReason());
            if (success) {
                return Result.success("已拒绝", null);
            } else {
                return Result.error("操作失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("操作失败");
        }
    }

    @PutMapping("/{id}/remove")
    public Result<Void> remove(@PathVariable Long id, @Valid @RequestBody RemoveRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        String role = SecurityUtils.getCurrentUserRole();
        if (!isAdmin(role)) {
            return Result.forbidden();
        }

        try {
            boolean success = secondhandGoodsService.remove(id, userId, request.getReason());
            if (success) {
                return Result.success("已下架", null);
            } else {
                return Result.error("操作失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error("操作失败");
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("操作失败");
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @Valid @RequestBody DeleteRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        String role = SecurityUtils.getCurrentUserRole();
        if (!isAdmin(role)) {
            return Result.forbidden();
        }

        try {
            boolean success = secondhandGoodsService.adminDelete(id, userId, request.getReason());
            if (success) {
                return Result.success("删除成功", null);
            } else {
                return Result.error("删除失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error("操作失败");
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("删除失败");
        }
    }

    // 二手商品管理仅系统管理员可访问
    private boolean isAdmin(String role) {
        return "ADMIN".equals(role);
    }

    // Refactor: 用 @Data 替代重复的样板代码
    @Data
    public static class RejectRequest {
        @NotBlank(message = "拒绝原因不能为空")
        @Size(max = 200, message = "拒绝原因长度不能超过200个字符")
        private String reason;
    }

    @Data
    public static class RemoveRequest {
        @NotBlank(message = "下架原因不能为空")
        @Size(max = 200, message = "下架原因长度不能超过200个字符")
        private String reason;
    }

    @Data
    public static class DeleteRequest {
        @Size(max = 200, message = "删除原因长度不能超过200个字符")
        private String reason;
    }
}
