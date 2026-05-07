package com.ruoyi.project.ai.domain;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI模型路由对象 ai_model_route
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_model_route")
public class AiModelRoute extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Excel(name = "路由编码")
    private String routeCode;

    @Excel(name = "产品类型")
    private String productType;

    @Excel(name = "场景编码")
    private String sceneCode;

    @Excel(name = "用户等级")
    private String userTier;

    @Excel(name = "主模型配置ID")
    private Long primaryModelConfigId;

    @Excel(name = "降级模型配置ID")
    private Long fallbackModelConfigId;

    @Excel(name = "是否启用RAG", readConverterExp = "Y=是,N=否")
    private String ragEnabled;

    private String ragConfigJson;

    @Excel(name = "是否启用", readConverterExp = "Y=是,N=否")
    private String enabled;

    @Column(isLogicDelete = true)
    private String delFlag;
}
