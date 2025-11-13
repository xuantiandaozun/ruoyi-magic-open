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
 * 博客评论对象 blog_comment
 * 
 * @author ruoyi
 * @date 2025-11-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("blog_comment")
public class BlogComment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 评论ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 博客ID */
    @Excel(name = "博客ID")
    private Long blogId;

    /** 父评论ID（回复时使用） */
    @Excel(name = "父评论ID")
    private Long parentId;

    /** 用户ID（游客为空） */
    @Excel(name = "用户ID")
    private Long userId;

    /** 用户IP地址 */
    @Excel(name = "用户IP地址")
    private String userIp;

    /** 昵称 */
    @Excel(name = "昵称")
    private String nickname;

    /** 邮箱 */
    @Excel(name = "邮箱")
    private String email;

    /** 网站 */
    @Excel(name = "网站")
    private String website;

    /** 评论内容 */
    @Excel(name = "评论内容")
    private String content;

    /** 状态（0待审核 1已通过 2已拒绝） */
    @Excel(name = "状态", readConverterExp = "0=待审核,1=已通过,2=已拒绝")
    private String status;

    /** 回复数量 */
    @Excel(name = "回复数量")
    private Integer replyCount;

    /** 点赞数量 */
    @Excel(name = "点赞数量")
    private Integer likeCount;

    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete = true)
    private String delFlag;

    /** 博客标题（冗余字段） */
    @Column(ignore = true)
    private String blogTitle;
}
