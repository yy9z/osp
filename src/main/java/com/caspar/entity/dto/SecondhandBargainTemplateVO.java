package com.caspar.entity.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 一键议价模板响应。
 */
@Data
public class SecondhandBargainTemplateVO {

    private BigDecimal suggestedPrice;

    private String quickCopy;

    private List<String> templates;
}

