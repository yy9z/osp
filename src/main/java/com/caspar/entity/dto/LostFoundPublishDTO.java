package com.caspar.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 失物招领发布请求DTO
 */
@Data
public class LostFoundPublishDTO {
    /**
     * 类型: LOST, FOUND
     */
    @NotBlank(message = "类型不能为空")
    @Size(max = 16, message = "类型长度不能超过16个字符")
    private String type;

    /**
     * 标题
     */
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100个字符")
    private String title;

    /**
     * 描述
     */
    @Size(max = 1000, message = "描述长度不能超过1000个字符")
    private String description;

    /**
     * 分类: ELECTRONICS, DOCUMENTS, KEY, CLOTHING, OTHER
     */
    @NotBlank(message = "分类不能为空")
    @Size(max = 32, message = "分类长度不能超过32个字符")
    private String category;

    /**
     * 图片URL列表
     */
    private List<String> images;

    /**
     * 丢失/拾取地点
     */
    @Size(max = 255, message = "地点长度不能超过255个字符")
    private String location;

    /**
     * 悬赏金额
     */
    private BigDecimal reward;
}
