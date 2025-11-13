package com.ruoyi.project.article.domain;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 博客点赞记录对象 blog_like_record
 * 
 * @author ruoyi
 * @date 2025-11-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("blog_like_record")
public class BlogLikeRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 博客ID */
    @Excel(name = "博客ID")
    private Long blogId;

    /** 用户ID（游客为空） */
    @Excel(name = "用户ID")
    private Long userId;

    /** 用户IP地址 */
    @Excel(name = "用户IP地址")
    private String userIp;

    /** 点赞状态（0取消点赞 1点赞） */
    @Excel(name = "点赞状态", readConverterExp = "0=取消点赞,1=点赞")
    private String likeStatus;

    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete = true)
    private String delFlag;

    /** 博客标题（冗余字段） */
    @Column(ignore = true)
    private String blogTitle;
}
