package com.ruoyi.project.miniapp.domain.dto;

import lombok.Data;

@Data
public class SubscribeTemplateFieldConfig {
    private String fieldName;

    private String valueExpr;

    private String defaultValue;

    private Integer maxLength;
}
