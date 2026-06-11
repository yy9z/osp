package com.caspar.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 宿舍成员实体类
 */
@Data
public class DormitoryMember {
    /**
     * ID
     */
    private Long id;

    /**
     * 宿舍ID
     */
    private Long dormitoryId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 加入时间
     */
    private LocalDateTime joinTime;
}
