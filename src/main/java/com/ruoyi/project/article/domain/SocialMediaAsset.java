package com.ruoyi.project.article.domain;

import java.time.LocalDateTime;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自媒体素材对象 social_media_asset
 * 
 * @author ruoyi
 * @date 2025-12-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("social_media_asset")
public class SocialMediaAsset extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 平台(toutiao/douyin/bilibili/weibo/twitter/medium/other) */
    @Excel(name = "平台")
    private String platform;

    /** 内容类型(article/video/image_set/audio/short_post/live_replay/other) */
    @Excel(name = "内容类型")
    private String contentType;

    /** 原始链接，唯一去重 */
    @Excel(name = "原始链接")
    private String sourceUrl;

    /** 平台内容ID，可空，配合平台去重 */
    private String externalId;

    /** 作者/账号名 */
    @Excel(name = "作者")
    private String author;

    /** 作者在平台的ID */
    private String authorId;

    /** 平台发布时间 */
    @Excel(name = "发布时间")
    private LocalDateTime publishTime;

    /** 标题/首句 */
    @Excel(name = "标题")
    private String title;

    /** 摘要/笔记 */
    @Excel(name = "摘要")
    private String summary;

    /** 正文/口播稿/文本快照（内容核心） */
    @Excel(name = "内容快照")
    private String contentSnapshot;

    /** 封面/首图 */
    @Excel(name = "封面")
    private String coverUrl;

    /** 媒体链接(JSON: 视频/图集/音频直链或存储Key) */
    private String mediaUrls;

    /** 时长(秒，视频/音频可填) */
    private Integer durationSeconds;

    /** 标签(逗号或JSON字符串，可选) */
    @Excel(name = "标签")
    private String tags;

    /** 质量等级(high/medium/low，可选) */
    @Excel(name = "质量等级", readConverterExp = "high=优,medium=中,low=低")
    private String qualityLevel;

    /** 状态(active/archived/invalid) */
    @Excel(name = "状态", readConverterExp = "active=可用,archived=归档,invalid=失效")
    private String status;

    /** 版权/授权备注 */
    private String licenseNote;

    /** 运营备注 */
    private String note;

    /** 采集方式(manual/spider/api) */
    @Excel(name = "采集方式", readConverterExp = "manual=手工,spider=爬虫,api=接口")
    private String captureMethod;

    /** 采集时间 */
    @Excel(name = "采集时间")
    private LocalDateTime captureTime;

    /** 指标快照(JSON: read/like/comment/share等) */
    private String metricsSnapshot;

    /** 最近一次校验/补数时间 */
    private LocalDateTime lastCheckTime;
}
