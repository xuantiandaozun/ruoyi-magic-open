package com.ruoyi.project.ai.domain;

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
 * AI博客生产记录对象 ai_blog_production_record
 * 用于记录AI生成博客的生产过程（README分析、排行榜、教程、趋势分析等）
 * 
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_blog_production_record")
public class AiBlogProductionRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 仓库地址 */
    @Excel(name = "仓库地址", width = 50)
    private String repoUrl;

    /** 仓库标题 */
    @Excel(name = "仓库标题")
    private String repoTitle;

    /** 仓库所有者 */
    @Excel(name = "仓库所有者")
    private String repoOwner;

    /** 仓库主要编程语言 */
    @Excel(name = "编程语言")
    private String repoLanguage;

    /** 生产类型（readme_analysis-README分析，ranking-排行榜，tutorial-教程，trending_analysis-趋势分析等） */
    @Excel(name = "生产类型")
    private String productionType;

    /** 关联的博客ID */
    @Excel(name = "博客ID")
    private Long blogId;

    /** 生产状态（0-生产中，1-成功，2-失败） */
    @Excel(name = "生产状态", readConverterExp = "0=生产中,1=成功,2=失败")
    private String status;

    /** 生产时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "生产时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date productionTime;

    /** 完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "完成时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date completionTime;

    /** 目标日期（用于按日期生产的场景） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "目标日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date targetDate;

    /** 错误信息（失败时记录） */
    private String errorMessage;

    /** 扩展数据（存储其他相关信息，如仓库stars、forks等） */
    private String extraData;

    /** 使用的AI模型 */
    @Excel(name = "AI模型")
    private String aiModel;

    /** 内容来源（如README文件路径） */
    private String contentSource;

    /** 重试次数 */
    @Excel(name = "重试次数")
    private Integer retryCount;

    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete = true)
    private String delFlag;
}
