package com.caspar.service.impl;

import com.caspar.common.PageResult;
import com.caspar.mapper.DormBuildingMapper;
import com.caspar.entity.User;
import com.caspar.entity.UserDormitory;
import com.caspar.entity.dto.UserDormitoryDTO;
import com.caspar.mapper.UserDormitoryMapper;
import com.caspar.mapper.UserMapper;
import com.caspar.service.UserService;
import com.caspar.util.JwtUtil;
import com.caspar.util.PaginationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserDormitoryMapper userDormitoryMapper;

    @Autowired
    private DormBuildingMapper dormBuildingMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Long register(String username, String password, String realName, String phone, String email, String role) {
        // 检查用户名是否存在
        if (userMapper.countByUsername(username) > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 检查邮箱是否存在
        if (email != null && !email.isEmpty() && userMapper.countByEmail(email) > 0) {
            throw new IllegalArgumentException("邮箱已被使用");
        }

        // 检查手机号是否存在
        if (phone != null && !phone.isEmpty() && userMapper.countByPhone(phone) > 0) {
            throw new IllegalArgumentException("手机号已被使用");
        }

        // 创建用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setRole(role != null ? role : "STUDENT");
        user.setCreateTime(LocalDateTime.now());

        userMapper.insert(user);

        return user.getUserId();
    }

    @Override
    public String login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        if ("DISABLED".equals(user.getStatus())) {
            throw new IllegalArgumentException("账号已被禁用，请联系管理员");
        }

        return jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole());
    }

    @Override
    @Cacheable(cacheNames = "userInfo", key = "#userId", unless = "#result == null")
    public User getUserInfo(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        // 不返回密码
        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "userInfo", key = "#userId")
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("原密码错误");
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        return userMapper.updatePassword(userId, encodedPassword) > 0;
    }

    @Override
    public PageResult<User> getUserList(Integer page, Integer size, String role, String keyword) {
        int safePage = PaginationUtils.safePage(page);
        int safeSize = PaginationUtils.safeSize(size);
        int offset = PaginationUtils.offset(safePage, safeSize);

        List<User> records = userMapper.selectList(role, keyword, offset, safeSize);
        Long total = userMapper.count(role, keyword);

        // 不返回密码
        records.forEach(user -> user.setPassword(null));

        return new PageResult<>(records, total, safePage, safeSize);
    }

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public User findById(Long userId) {
        return userMapper.findById(userId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "userInfo", key = "#userId")
    public boolean updateProfile(Long userId, User user) {
        User existingUser = userMapper.findById(userId);
        if (existingUser == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 只更新允许修改的字段
        existingUser.setRealName(user.getRealName());
        existingUser.setStudentId(user.getStudentId());
        existingUser.setPhone(user.getPhone());
        existingUser.setEmail(user.getEmail());
        existingUser.setAvatar(user.getAvatar());
        existingUser.setBio(user.getBio());

        return userMapper.update(existingUser) > 0;
    }

    @Override
    @Cacheable(cacheNames = "userDormitory", key = "#userId", unless = "#result == null")
    public UserDormitory getUserDormitory(Long userId) {
        return userDormitoryMapper.findByUserId(userId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "userDormitory", key = "#userId")
    public boolean saveUserDormitory(Long userId, UserDormitoryDTO dto) {
        UserDormitory existing = userDormitoryMapper.findByUserId(userId);

        UserDormitory userDormitory = new UserDormitory();
        userDormitory.setUserId(userId);
        userDormitory.setCampus(dto.getCampus());
        userDormitory.setBuilding(dto.getBuilding());
        userDormitory.setRoom(dto.getRoom());
        userDormitory.setBed(dto.getBed());
        userDormitory.setCheckInDate(dto.getCheckInDate() != null ? dto.getCheckInDate().atStartOfDay() : LocalDateTime.now());

        if (existing != null) {
            userDormitory.setId(existing.getId());
            return userDormitoryMapper.update(userDormitory) > 0;
        } else {
            return userDormitoryMapper.insert(userDormitory) > 0;
        }
    }

    @Override
    @Transactional
    public void createUser(User user) {
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        User existing = userMapper.findByUsername(user.getUsername());
        if (existing != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        userMapper.insert(user);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "userInfo", key = "#userId")
    public void disableUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setStatus("DISABLED");
        userMapper.update(user);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "userInfo", key = "#userId")
    public void enableUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setStatus("ACTIVE");
        userMapper.update(user);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "userInfo", key = "#userId")
    public void resetPassword(Long userId, String newPassword) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.update(user);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "userInfo", key = "#userId"),
            @CacheEvict(cacheNames = "userDormitory", key = "#userId")
    })
    public void updateUserRole(Long userId, String role) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setRole(role);
        userMapper.update(user);
        if (!"DORM_MANAGER".equals(role)) {
            dormBuildingMapper.clearManagerBinding(userId);
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "userInfo", key = "#userId"),
            @CacheEvict(cacheNames = "userDormitory", key = "#userId")
    })
    public void deleteUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        userMapper.deleteById(userId);
    }
}
