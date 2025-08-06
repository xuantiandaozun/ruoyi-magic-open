package com.ruoyi.project.article.domain;

import java.time.LocalDateTime;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文章列表对象 blog
 * 
 * @author ruoyi
 * @date 2025-08-05 16:49:23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("blog")
public class Blog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 博客ID */
    @Id(keyType = KeyType.Auto)
    private String blogId;

    /** 博客标题 */
    @Excel(name = "博客标题")
    private String title;

    /** 博客摘要 */
    @Excel(name = "博客摘要")
    private String summary;

    /** 博客内容 */
    @Excel(name = "博客内容")
    private String content;

    /** 封面图片URL */
    @Excel(name = "封面图片URL")
    private String coverImage;

    /** 博客分类 */
    @Excel(name = "博客分类")
    private String category;

    /** 博客标签，多个用逗号分隔 */
    @Excel(name = "博客标签，多个用逗号分隔")
    private String tags;

    /** 状态（0草稿 1已发布 2已下线） */
    @Excel(name = "状态", readConverterExp = "0=草稿,1=已发布,2=已下线")
    private String status;

    /** 是否置顶（0否 1是） */
    @Excel(name = "是否置顶", readConverterExp = "0=否,1=是")
    private String isTop;

    /** 是否原创（0转载 1原创） */
    @Excel(name = "是否原创", readConverterExp = "0=转载,1=原创")
    private String isOriginal;

    /** 浏览次数 */
    @Excel(name = "浏览次数")
    private String viewCount;

    /** 点赞次数 */
    @Excel(name = "点赞次数")
    private String likeCount;

    /** 评论次数 */
    private Long commentCount;

    /** 发布时间 */
    @Excel(name = "发布时间")
    private LocalDateTime publishTime;

    /** 关联的飞书文档token */
    private String feishuDocToken;

    /** 关联的飞书文档名称（冗余字段） */
    private String feishuDocName;

    /** 飞书同步状态（0未同步 1已同步 2同步失败） */
    @Excel(name = "飞书同步状态", readConverterExp = "0=未同步,1=已同步,2=同步失败")
    private String feishuSyncStatus;

    /** 飞书最后同步时间 */
    private LocalDateTime feishuLastSyncTime;

    /** 排序字段 */
    private Integer sortOrder;

    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete = true)
    private String delFlag;

}
