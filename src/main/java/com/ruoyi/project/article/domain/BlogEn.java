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
 * 英文博客对象 blog_en
 * 
 * @author ruoyi
 * @date 2025-08-26 15:05:51
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("blog_en")
public class BlogEn extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 英文博客ID */
    @Id(keyType = KeyType.Auto)
    private String blogId;

    /** 英文博客标题 */
    @Excel(name = "英文博客标题")
    private String title;

    /** 英文博客摘要 */
    private String summary;

    /** 英文博客内容 */
    private String content;

    /** 封面图片URL */
    private String coverImage;

    /** 博客分类（英文） */
    @Excel(name = "博客分类", readConverterExp = "英=文")
    private String category;

    /** 博客标签，多个用逗号分隔 */
    private String tags;

    /** 状态（0草稿 1已发布 2已下线） */
    @Excel(name = "状态", readConverterExp = "0=草稿,1=已发布,2=已下线")
    private String status;

    /** 是否置顶（0否 1是） */
    private String isTop;

    /** 是否原创（0转载 1原创） */
    private String isOriginal;

    /** 浏览次数 */
    private String viewCount;

    /** 点赞次数 */
    private String likeCount;

    /** 评论次数 */
    private String commentCount;

    /** 发布时间 */
    private Date publishTime;

    /** 关联的中文博客ID */
    private String zhBlogId;

    /** 关联的飞书文档token */
    private String feishuDocToken;

    /** 关联的飞书文档名称（冗余字段） */
    @Excel(name = "关联的飞书文档名称", readConverterExp = "冗=余字段")
    private String feishuDocName;

    /** 飞书同步状态（0未同步 1已同步 2同步失败） */
    @Excel(name = "飞书同步状态", readConverterExp = "0=未同步,1=已同步,2=同步失败")
    private String feishuSyncStatus;

    /** 飞书最后同步时间 */
    private Date feishuLastSyncTime;

    /** 排序字段 */
    @Excel(name = "排序字段")
    private String sortOrder;

    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete = true)
    private String delFlag;

}
