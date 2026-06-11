package com.caspar.controller;

import com.caspar.common.PageResult;
import com.caspar.common.Result;
import com.caspar.entity.dto.LostFoundVO;
import com.caspar.mapper.LostFoundMapper;
import com.caspar.util.PaginationUtils;
import com.caspar.util.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/lostfound")
public class LostFoundAdminController {

    @Autowired
    private LostFoundMapper lostFoundMapper;

    @GetMapping("/all")
    public Result<PageResult<LostFoundVO>> getAllList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {

        String role = SecurityUtils.getCurrentUserRole();
        if (!isAdmin(role)) {
            return Result.forbidden();
        }

        try {
            int safePage = PaginationUtils.safePage(page);
            int safeSize = PaginationUtils.safeSize(size);
            int offset = PaginationUtils.offset(safePage, safeSize);
            var records = lostFoundMapper.selectAllList(type, category, keyword, status, offset, safeSize);
            var total = lostFoundMapper.countAll(type, category, keyword, status);
            return Result.success(new PageResult<>(records, total, safePage, safeSize));
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取列表失败");
        }
    }

    @PutMapping("/{id}/remove")
    public Result<Void> remove(@PathVariable Long id, @Valid @RequestBody RemoveRequest request) {
        String role = SecurityUtils.getCurrentUserRole();
        if (!isAdmin(role)) {
            return Result.forbidden();
        }

        try {
            lostFoundMapper.updateStatus(id, "REMOVED");
            lostFoundMapper.updateRemoveReason(id, request.getReason());
            return Result.success("已下架", null);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("操作失败");
        }
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equals(role);
    }

    @Data
    public static class RemoveRequest {
        @NotBlank(message = "下架原因不能为空")
        @Size(max = 200, message = "下架原因长度不能超过200个字符")
        private String reason;
    }
}
