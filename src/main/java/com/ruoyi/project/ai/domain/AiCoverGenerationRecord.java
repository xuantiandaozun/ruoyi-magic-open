package com.ruoyi.project.ai.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI生图记录对象 ai_cover_generation_record
 * 用于记录AI生成封面图片的过程
 * 
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_cover_generation_record")
public class AiCoverGenerationRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 关联的博客ID */
    @Excel(name = "博客ID")
    private Long blogId;

    /** 封面类型（0-通用封面 1-个性化封面） */
    @Excel(name = "封面类型", readConverterExp = "0=通用封面,1=个性化封面")
    private String coverType;

    /** 博客分类 */
    @Excel(name = "博客分类")
    private String category;

    /** AI生图提示词 */
    @Excel(name = "生图提示词", width = 50)
    private String prompt;

    /** 生成的图片URL */
    private String imageUrl;

    /** 使用的AI模型 */
    @Excel(name = "AI模型")
    private String aiModel;

    /** 生成状态（0-生成中 1-成功 2-失败 3-仅生成提示词） */
    @Excel(name = "生成状态", readConverterExp = "0=生成中,1=成功,2=失败,3=仅生成提示词")
    private String generationStatus;

    /** 是否被使用（0-未使用 1-已使用） */
    @Excel(name = "是否使用", readConverterExp = "0=未使用,1=已使用")
    private String isUsed;

    /** 生成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "生成时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date generationTime;

    /** 错误信息 */
    private String errorMessage;

    /** 与现有提示词的相似度得分 */
    @Excel(name = "相似度得分")
    private BigDecimal similarityScore;

    /** 复用的记录ID（通用封面复用时） */
    @Excel(name = "复用记录ID")
    private Long reuseFromId;

    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete = true)
    private String delFlag;
}
