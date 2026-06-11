package com.caspar.controller;

import com.caspar.common.PageResult;
import com.caspar.common.Result;
import com.caspar.entity.User;
import com.caspar.entity.UserDormitory;
import com.caspar.entity.dto.PasswordChangeDTO;
import com.caspar.entity.dto.UserDormitoryDTO;
import com.caspar.entity.dto.UserLoginDTO;
import com.caspar.entity.dto.UserLoginVO;
import com.caspar.entity.dto.UserRegisterDTO;
import com.caspar.service.UserService;
import com.caspar.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Set<String> PUBLIC_REGISTER_ROLES = Set.of("STUDENT", "TEACHER");
    private static final Set<String> PRIVILEGED_REGISTER_ROLES = Set.of("ADMIN", "DORM_MANAGER");

    @Autowired
    private UserService userService;

    @Value("${app.admin-register.key:}")
    private String adminRegisterKey;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Map<String, Long>> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        String role = registerDTO.getRole();
        if (role == null || role.isEmpty()) {
            role = "STUDENT";
        } else {
            role = role.trim().toUpperCase();
        }

        if (!PUBLIC_REGISTER_ROLES.contains(role) && !PRIVILEGED_REGISTER_ROLES.contains(role)) {
            return Result.badRequest("不支持的角色类型");
        }

        if (PRIVILEGED_REGISTER_ROLES.contains(role)) {
            if (adminRegisterKey == null || adminRegisterKey.isBlank()) {
                return Result.error(403, "当前环境未开放管理角色注册");
            }
            String registerKey = registerDTO.getRegisterKey();
            if (registerKey == null || !adminRegisterKey.equals(registerKey.trim())) {
                return Result.error(403, "管理员注册密钥错误");
            }
        }

        registerDTO.setRole(role);

        try {
            Long userId = userService.register(
                    registerDTO.getUsername(),
                    registerDTO.getPassword(),
                    registerDTO.getRealName(),
                    registerDTO.getPhone(),
                    registerDTO.getEmail(),
                    registerDTO.getRole()
            );

            Map<String, Long> data = new HashMap<>();
            data.put("userId", userId);
            return Result.success("注册成功", data);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("注册失败");
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<UserLoginVO> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        try {
            String token = userService.login(loginDTO.getUsername(), loginDTO.getPassword());
            User user = userService.findByUsername(loginDTO.getUsername());

            UserLoginVO loginVO = new UserLoginVO();
            loginVO.setToken(token);
            loginVO.setUserId(user.getUserId());
            loginVO.setUsername(user.getUsername());
            loginVO.setRealName(user.getRealName());
            loginVO.setStudentId(user.getStudentId());
            loginVO.setPhone(user.getPhone());
            loginVO.setEmail(user.getEmail());
            loginVO.setRole(user.getRole());
            loginVO.setAvatar(user.getAvatar());
            loginVO.setBio(user.getBio());

            return Result.success("登录成功", loginVO);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("登录失败");
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<User> getUserInfo() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            User user = userService.getUserInfo(userId);
            return Result.success(user);
        } catch (IllegalArgumentException e) {
            return Result.notFound(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取用户信息失败");
        }
    }

    /**
     * 根据ID获取用户信息（用于消息中心获取卖家信息）
     */
    /**
     * 根据ID获取用户信息（用于消息中心获取卖家信息）
     */
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        if (id == null) {
            return Result.badRequest("用户ID不能为空");
        }

        try {
            User user = userService.getUserInfo(id);
            if (user == null) {
                return Result.notFound("用户不存在");
            }
            // 清除敏感信息
            user.setPassword(null);
            return Result.success(user);
        } catch (IllegalArgumentException e) {
            return Result.notFound(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取用户信息失败");
        }
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordChangeDTO passwordChangeDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            boolean success = userService.changePassword(
                    userId,
                    passwordChangeDTO.getOldPassword(),
                    passwordChangeDTO.getNewPassword()
            );
            if (success) {
                return Result.success("密码修改成功", null);
            } else {
                return Result.error("密码修改失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("修改密码失败");
        }
    }

    /**
     * 更新个人资料
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody User user) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            user.setUserId(userId);
            boolean success = userService.updateProfile(userId, user);
            if (success) {
                return Result.success("资料更新成功", null);
            } else {
                return Result.error("资料更新失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("更新资料失败");
        }
    }

    /**
     * 用户列表（管理员）
     */
    @GetMapping("/list")
    public Result<PageResult<User>> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword) {

        // 仅管理员可访问
        String currentRole = SecurityUtils.getCurrentUserRole();
        if (currentRole == null || !currentRole.equals("ADMIN")) {
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

    /**
     * 获取用户宿舍信息
     */
    @GetMapping("/dormitory")
    public Result<UserDormitoryDTO> getUserDormitory() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            UserDormitory dormitory = userService.getUserDormitory(userId);
            if (dormitory != null) {
                UserDormitoryDTO dto = new UserDormitoryDTO();
                dto.setCampus(dormitory.getCampus());
                dto.setBuilding(dormitory.getBuilding());
                dto.setRoom(dormitory.getRoom());
                dto.setBed(dormitory.getBed());
                dto.setCheckInDate(dormitory.getCheckInDate() != null ? dormitory.getCheckInDate().toLocalDate() : null);
                return Result.success(dto);
            }
            return Result.success("暂无宿舍信息", null);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("获取宿舍信息失败");
        }
    }

    /**
     * 保存用户宿舍信息
     */
    @PostMapping("/dormitory")
    public Result<Void> saveUserDormitory(@Valid @RequestBody UserDormitoryDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            boolean success = userService.saveUserDormitory(userId, dto);
            if (success) {
                return Result.success("宿舍信息保存成功", null);
            }
            return Result.error("宿舍信息保存失败");
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            return Result.error("宿舍信息保存失败");
        }
    }

}
