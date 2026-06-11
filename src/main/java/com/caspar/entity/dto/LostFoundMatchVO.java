package com.caspar.entity.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 失物招领智能匹配结果
 */
@Data
public class LostFoundMatchVO {
    private Long id;
    private String type;
    private String title;
    private String category;
    private String location;
    private String status;
    private String publisherName;
    private String publisherPhone;
    private String images;
    private LocalDateTime createTime;

    /**
     * 匹配分（0-100）
     */
    private Integer matchScore;

    /**
     * 匹配依据（可解释）
     */
    private List<String> matchReasons;
}
