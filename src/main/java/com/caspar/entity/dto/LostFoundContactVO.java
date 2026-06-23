package com.caspar.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录后按需返回的失物招领联系方式。
 */
@Data
@AllArgsConstructor
public class LostFoundContactVO {

    private String contact;
}
