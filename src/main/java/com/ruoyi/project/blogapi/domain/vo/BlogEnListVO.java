package com.ruoyi.project.blogapi.domain.vo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 英文博客列表VO对象
 * 用于博客网页API返回
 * 
 * @author ruoyi
 * @date 2025-11-13
 */
@Data
public class BlogEnListVO
{
    /** 博客ID */
    private String blogId;

    /** 英文博客标题 */
    private String title;

    /** 英文博客摘要 */
    private String summary;

    /** 封面图片URL */
    private String coverImage;

    /** 博客分类（英文） */
    private String category;

    /** 博客标签，多个用逗号分隔 */
    private String tags;

    /** 状态（0草稿 1已发布 2已下线） */
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date publishTime;

    /** 关联的中文博客ID */
    private String zhBlogId;

    /** 关联的飞书文档token */
    private String feishuDocToken;

    /** 关联的飞书文档名称 */
    private String feishuDocName;

    /** 飞书同步状态（0未同步 1已同步 2同步失败） */
    private String feishuSyncStatus;

    /** 飞书最后同步时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date feishuLastSyncTime;

    /** 排序字段 */
    private String sortOrder;

    /** 上次阅读时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastReadTime;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /** 备注 */
    private String remark;
}
