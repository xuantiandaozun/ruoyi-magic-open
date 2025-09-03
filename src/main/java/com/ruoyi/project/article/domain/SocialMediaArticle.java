package com.ruoyi.project.article.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.annotation.Column;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 自媒体文章对象 social_media_article
 * 
 * @author ruoyi
 * @date 2025-09-02 16:42:31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("social_media_article")
public class SocialMediaArticle extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 文章ID */
    @Id(keyType = KeyType.Auto)
    private String articleId;

    /** 中文标题 */
    @Excel(name = "中文标题")
    private String titleZh;

    /** 英文标题 */
    @Excel(name = "英文标题")
    private String titleEn;

    /** 中文摘要/微头条内容 */
    @Excel(name = "中文摘要/微头条内容")
    private String summaryZh;

    /** 英文摘要/Twitter内容 */
    private String summaryEn;

    /** 中文完整内容 */
    private String contentZh;

    /** 英文完整内容 */
    private String contentEn;

    /** 中文关键词 */
    private String keywordsZh;

    /** 英文关键词 */
    private String keywordsEn;

    /** 文章类型(GITHUB_RANKING-GitHub排行榜,PROJECT_ANALYSIS-项目分析等) */
    @Excel(name = "文章类型(GITHUB_RANKING-GitHub排行榜,PROJECT_ANALYSIS-项目分析等)")
    private String articleType;

    /** 内容角度 */
    private String contentAngle;

    /** 目标平台(toutiao-今日头条,twitter-推特,medium-Medium等) */
    private String targetPlatform;

    /** 发布状态(0-草稿,1-已发布,2-已下线) */
    @Excel(name = "发布状态(0-草稿,1-已发布,2-已下线)")
    private String publishStatus;

    /** 来源GitHub仓库信息(JSON格式) */
    private String sourceRepos;

    /** 关联的博客文章ID列表(逗号分隔) */
    private String relatedBlogIds;

    /** 生成日期 */
    private Date generationDate;

    /** 博客名称 */
    @Excel(name = "博客名称")
    private String blogName;

    /** 阅读量 */
    private String viewCount;

    /** 点赞数 */
    private String likeCount;

    /** 分享数 */
    private String shareCount;

    /** 评论数 */
    private String commentCount;

    /** 删除标志(0代表存在 2代表删除) */
    @Column(isLogicDelete = true)
    private String delFlag;

}
