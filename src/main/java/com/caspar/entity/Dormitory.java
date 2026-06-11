package com.caspar.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 宿舍实体类
 */
@Data
public class Dormitory {
    /**
     * 宿舍ID
     */
    private Long id;

    /**
     * 楼栋号，如"A栋"
     */
    private String building;

    /**
     * 楼层
     */
    private Integer floor;

    /**
     * 房间号，如"101"
     */
    private String roomNo;

    /**
     * 房间类型，如"4人间"
     */
    private String type;

    /**
     * 容量
     */
    private Integer capacity;

    /**
     * 当前人数
     */
    private Integer currentCount;

    /**
     * 性别: MALE(男), FEMALE(女)
     */
    private String gender;

    /**
     * 宿舍长ID
     */
    private Long headId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
