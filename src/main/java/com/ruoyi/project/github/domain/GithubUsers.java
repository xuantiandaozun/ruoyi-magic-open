package com.ruoyi.project.github.domain;

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
 * GitHub用户/组织监控对象 github_users
 * 
 * @author ruoyi
 * @date 2025-11-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("github_users")
public class GithubUsers extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** GitHub用户/组织ID */
    @Excel(name = "GitHub ID")
    private Long githubId;

    /** 用户名/组织名 */
    @Excel(name = "用户名")
    private String login;

    /** 真实姓名/组织显示名称 */
    @Excel(name = "真实姓名")
    private String name;

    /** 头像URL */
    private String avatarUrl;

    /** GitHub页面URL */
    @Excel(name = "GitHub页面")
    private String htmlUrl;

    /** 类型(User/Organization) */
    @Excel(name = "类型")
    private String type;

    /** 公司信息 */
    @Excel(name = "公司")
    private String company;

    /** 博客地址 */
    @Excel(name = "博客")
    private String blog;

    /** 位置信息 */
    @Excel(name = "位置")
    private String location;

    /** 邮箱 */
    @Excel(name = "邮箱")
    private String email;

    /** 是否可雇佣(仅用户) */
    @Excel(name = "可雇佣", readConverterExp = "0=否,1=是")
    private Integer hireable;

    /** 个人简介(用户)/组织描述(组织) */
    @Excel(name = "简介")
    private String bio;

    /** Twitter用户名 */
    private String twitterUsername;

    /** 公开仓库数量 */
    @Excel(name = "公开仓库数")
    private Integer publicRepos;

    /** 公开Gist数量 */
    @Excel(name = "公开Gist数")
    private Integer publicGists;

    /** 关注者数量 */
    @Excel(name = "关注者数")
    private Integer followers;

    /** 关注数量 */
    @Excel(name = "关注数")
    private Integer following;

    /** GitHub账号创建时间 */
    @Excel(name = "GitHub创建时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime githubCreatedAt;

    /** GitHub账号最后更新时间 */
    @Excel(name = "GitHub更新时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime githubUpdatedAt;

    /** 排名位置 */
    @Excel(name = "排名位置")
    private Integer rankPosition;

    /** 是否为前100(0-否 1-是) */
    @Excel(name = "是否前100", readConverterExp = "0=否,1=是")
    private Integer isTop100;

    /** 是否关注(0-否 1-是) */
    @Excel(name = "是否关注", readConverterExp = "0=否,1=是")
    private Integer isWatched;

    /** 关注优先级(数字越小优先级越高) */
    @Excel(name = "关注优先级")
    private Integer watchPriority;

    /** 最后同步时间 */
    @Excel(name = "最后同步时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSyncTime;

    /** 最后仓库同步时间 */
    @Excel(name = "仓库同步时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastRepoSyncTime;

    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete = true)
    private String delFlag;
}
