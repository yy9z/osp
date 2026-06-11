package com.caspar.service;

import com.caspar.common.PageResult;
import com.caspar.entity.User;
import com.caspar.entity.UserDormitory;
import com.caspar.entity.dto.UserDormitoryDTO;

public interface UserService {

    Long register(String username, String password, String realName, String phone, String email, String role);

    String login(String username, String password);

    User getUserInfo(Long userId);

    boolean changePassword(Long userId, String oldPassword, String newPassword);

    PageResult<User> getUserList(Integer page, Integer size, String role, String keyword);

    User findByUsername(String username);

    User findById(Long userId);

    boolean updateProfile(Long userId, User user);

    UserDormitory getUserDormitory(Long userId);

    boolean saveUserDormitory(Long userId, UserDormitoryDTO dto);

    void createUser(User user);

    void disableUser(Long userId);

    void enableUser(Long userId);

    void resetPassword(Long userId, String newPassword);

    void updateUserRole(Long userId, String role);

    void deleteUser(Long userId);
}
