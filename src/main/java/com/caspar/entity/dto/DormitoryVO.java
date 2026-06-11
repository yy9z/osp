package com.caspar.entity.dto;

import com.caspar.entity.Dormitory;
import com.caspar.entity.User;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 宿舍详情VO（包含成员信息）
 */
@Data
public class DormitoryVO extends Dormitory {
    /**
     * 宿舍长信息
     */
    private User head;

    /**
     * 成员列表
     */
    private List<MemberVO> members;

    /**
     * 宿舍成员VO
     */
    @Data
    public static class MemberVO {
        private Long id;
        private Long dormitoryId;
        private Long userId;
        private String username;
        private String realName;
        private String phone;
        private String avatar;
        private LocalDateTime joinTime;
    }
}
