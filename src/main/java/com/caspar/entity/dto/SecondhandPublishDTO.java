package com.caspar.entity.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * 二手商品发布请求DTO
 */
@Data
public class SecondhandPublishDTO {
    /**
     * 商品标题
     */
    @NotBlank(message = "商品标题不能为空")
    @Size(max = 100, message = "商品标题长度不能超过100个字符")
    private String title;

    /**
     * 商品描述
     */
    @Size(max = 2000, message = "商品描述长度不能超过2000个字符")
    private String description;

    /**
     * 商品价格
     */
    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    private BigDecimal price;

    /**
     * 分类: ELECTRONICS, BOOKS, CLOTHING, SPORTS, DAILY, OTHER
     */
    @NotBlank(message = "商品分类不能为空")
    @Size(max = 32, message = "商品分类长度不能超过32个字符")
    private String category;

    /**
     * 图片文件列表（用于上传到OSS）
     */
    private List<MultipartFile> imageFiles;

    /**
     * 图片URL列表（可选，用于已上传的图片）
     */
    private List<String> images;

    /**
     * 新旧程度: NEW, LIKE_NEW, GOOD, FAIR
     */
    @Size(max = 32, message = "商品成色长度不能超过32个字符")
    @NotBlank(message = "商品成色不能为空")
    private String condition;
}
