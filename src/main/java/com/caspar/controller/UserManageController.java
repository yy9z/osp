package com.caspar.controller;

import com.caspar.common.PageResult;
import com.caspar.common.Result;
import com.caspar.entity.User;
import com.caspar.service.UserService;
import com.caspar.util.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/user")
public class UserManageController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/list")
    public Result<PageResult<User>> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword) {

        String userRole = SecurityUtils.getCurrentUserRole();
        if (!"ADMIN".equals(userRole)) {
            return Result.forbidden();
        }

        try {
            PageResult<User> pageResult = userService.getUserList(page, size, role, keyword);
            return Result.success(pageResult);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取用户列表失败");
        }
    }

    @PostMapping("/create")
    public Result<Void> createUser(@Valid @RequestBody CreateUserRequest request) {
        String userRole = SecurityUtils.getCurrentUserRole();
        if (!"ADMIN".equals(userRole)) {
            return Result.forbidden();
        }

        try {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRealName(request.getRealName());
            user.setPhone(request.getPhone());
            user.setEmail(request.getEmail());
            user.setRole(request.getRole());
            userService.createUser(user);
            return Result.success("创建成功", null);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("创建失败");
        }
    }

    @PutMapping("/{id}/disable")
    public Result<Void> disableUser(@PathVariable Long id) {
        String userRole = SecurityUtils.getCurrentUserRole();
        if (!"ADMIN".equals(userRole)) {
            return Result.forbidden();
        }

        try {
            userService.disableUser(id);
            return Result.success("已禁用", null);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("操作失败");
        }
    }

    @PutMapping("/{id}/enable")
    public Result<Void> enableUser(@PathVariable Long id) {
        String userRole = SecurityUtils.getCurrentUserRole();
        if (!"ADMIN".equals(userRole)) {
            return Result.forbidden();
        }

        try {
            userService.enableUser(id);
            return Result.success("已启用", null);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("操作失败");
        }
    }

    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        String userRole = SecurityUtils.getCurrentUserRole();
        if (!"ADMIN".equals(userRole)) {
            return Result.forbidden();
        }

        try {
            userService.resetPassword(id, request.getPassword());
            return Result.success("密码重置成功", null);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("操作失败");
        }
    }

    @PutMapping("/{id}/roles")
    public Result<Void> updateUserRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        String userRole = SecurityUtils.getCurrentUserRole();
        if (!"ADMIN".equals(userRole)) {
            return Result.forbidden();
        }

        try {
            userService.updateUserRole(id, request.getRole());
            return Result.success("角色已更新", null);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("操作失败");
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        String userRole = SecurityUtils.getCurrentUserRole();
        if (!"ADMIN".equals(userRole)) {
            return Result.forbidden();
        }

        try {
            userService.deleteUser(id);
            return Result.success("用户已注销", null);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("操作失败");
        }
    }

    // Refactor: 用 @Data 替代手写 getter/setter，项目已引入 Lombok
    @Data
    public static class CreateUserRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(max = 32, message = "用户名长度不能超过32个字符")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度需为8-64位")
        private String password;

        @Size(max = 32, message = "姓名长度不能超过32个字符")
        private String realName;

        @Size(max = 32, message = "手机号长度不能超过32个字符")
        private String phone;

        @Email(message = "邮箱格式不正确")
        @Size(max = 64, message = "邮箱长度不能超过64个字符")
        private String email;

        @NotBlank(message = "角色不能为空")
        @Size(max = 20, message = "角色长度不能超过20个字符")
        private String role;
    }

    @Data
    public static class UpdateRoleRequest {
        @NotBlank(message = "角色不能为空")
        @Size(max = 20, message = "角色长度不能超过20个字符")
        private String role;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 64, message = "新密码长度需为8-64位")
        private String password;
    }
}
