package com.caspar.mapper;

import com.caspar.entity.User;
import com.caspar.entity.dto.DormitoryAssignableUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    User findByUsername(@Param("username") String username);

    /**
     * 根据用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    User findById(@Param("userId") Long userId);

    /**
     * 新增用户
     *
     * @param user 用户信息
     * @return 影响行数
     */
    int insert(User user);

    /**
     * 更新用户信息
     *
     * @param user 用户信息
     * @return 影响行数
     */
    int update(User user);

    /**
     * 更新密码
     *
     * @param userId      用户ID
     * @param newPassword 新密码
     * @return 影响行数
     */
    int updatePassword(@Param("userId") Long userId, @Param("newPassword") String newPassword);

    int deleteById(@Param("userId") Long userId);

    /**
     * 查询用户列表（分页）
     *
     * @param role     角色过滤
     * @param keyword  关键字过滤
     * @param offset   偏移量
     * @param limit    数量限制
     * @return 用户列表
     */
    List<User> selectList(@Param("role") String role,
                          @Param("keyword") String keyword,
                          @Param("offset") Integer offset,
                          @Param("limit") Integer limit);

    List<DormitoryAssignableUserVO> selectDormitoryAssignableUsers(@Param("operatorId") Long operatorId,
                                                                   @Param("allowSelfDormManager") Boolean allowSelfDormManager,
                                                                   @Param("keyword") String keyword,
                                                                   @Param("assigned") Boolean assigned,
                                                                   @Param("offset") Integer offset,
                                                                   @Param("limit") Integer limit);

    /**
     * 查询用户总数
     *
     * @param role    角色过滤
     * @param keyword 关键字过滤
     * @return 用户总数
     */
    Long count(@Param("role") String role, @Param("keyword") String keyword);

    Long countDormitoryAssignableUsers(@Param("operatorId") Long operatorId,
                                       @Param("allowSelfDormManager") Boolean allowSelfDormManager,
                                       @Param("keyword") String keyword,
                                       @Param("assigned") Boolean assigned);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 数量
     */
    int countByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱
     * @return 数量
     */
    int countByEmail(@Param("email") String email);

    /**
     * 检查手机号是否存在
     *
     * @param phone 手机号
     * @return 数量
     */
    int countByPhone(@Param("phone") String phone);
}
