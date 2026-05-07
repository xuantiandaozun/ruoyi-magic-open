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
 * 大模型配置对象 ai_model_config
 * 管理不同厂商与模型的接入参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_model_config")
public class AiModelConfig extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 配置ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 厂商类型（doubao、openai、deepseek、qianwen、glm、operouter等） */
    @Excel(name = "厂商类型")
    private String provider;

    /** 模型类型（chat、vision、embedding、image等） */
    @Excel(name = "模型类型")
    private String capability;

    /** 模型名称/ID */
    @Excel(name = "模型名称/ID")
    private String model;

    /** API密钥 */
    private String apiKey;

    /** 密钥引用（环境变量或密钥管理Key，优先于明文api_key） */
    private String apiKeyRef;

    /** API端点 */
    private String endpoint;

    /** 额外参数（JSON） */
    private String extraParams;

    /** 上下文窗口Token数量 */
    private Integer contextWindow;

    /** 最大输出Token数量 */
    private Integer maxOutputTokens;

    /** 是否支持流式输出 */
    private String supportsStream;

    /** 是否支持视觉输入 */
    private String supportsVision;

    /** 是否支持厂商侧缓存 */
    private String supportsCache;

    /** 是否可用于免费用户 */
    private String freeAvailable;

    /** 工具调用后延时（毫秒），防止频率限制 */
    @Excel(name = "工具调用延时")
    private Integer toolCallDelay;

    /** 是否启用 */
    @Excel(name = "是否启用", readConverterExp = "Y=是,N=否")
    private String enabled;

    /** 是否默认 */
    @Excel(name = "是否默认", readConverterExp = "Y=是,N=否")
    private String isDefault;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete = true)
    private String delFlag;
}
