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
 * AI产品应用对象 ai_product_app
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_product_app")
public class AiProductApp extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Excel(name = "应用编码")
    private String appCode;

    @Excel(name = "应用名称")
    private String appName;

    @Excel(name = "产品类型")
    private String productType;

    @Excel(name = "默认模型路由编码")
    private String defaultRouteCode;

    @Excel(name = "是否公开访问", readConverterExp = "Y=是,N=否")
    private String publicAccess;

    @Excel(name = "是否启用RAG", readConverterExp = "Y=是,N=否")
    private String ragEnabled;

    private String ragConfigJson;

    private String configJson;

    @Excel(name = "是否启用", readConverterExp = "Y=是,N=否")
    private String enabled;

    @Column(isLogicDelete = true)
    private String delFlag;
}
