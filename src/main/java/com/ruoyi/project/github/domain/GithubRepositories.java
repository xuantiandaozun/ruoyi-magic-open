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
 * GitHub仓库监控对象 github_repositories
 * 
 * @author ruoyi
 * @date 2025-11-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("github_repositories")
public class GithubRepositories extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** GitHub仓库ID */
    @Excel(name = "GitHub仓库ID")
    private Long githubId;

    /** 仓库名称 */
    @Excel(name = "仓库名称")
    private String name;

    /** 完整仓库名(owner/repo) */
    @Excel(name = "完整仓库名")
    private String fullName;

    /** 所有者登录名 */
    @Excel(name = "所有者登录名")
    private String ownerLogin;

    /** 所有者类型(User/Organization) */
    @Excel(name = "所有者类型")
    private String ownerType;

    /** 所有者GitHub ID */
    @Excel(name = "所有者GitHub ID")
    private Long ownerGithubId;

    /** 仓库描述 */
    @Excel(name = "仓库描述")
    private String description;

    /** 仓库GitHub页面URL */
    @Excel(name = "仓库URL")
    private String htmlUrl;

    /** 克隆URL */
    private String cloneUrl;

    /** SSH URL */
    private String sshUrl;

    /** 项目主页 */
    private String homepage;

    /** 主要编程语言 */
    @Excel(name = "主要编程语言")
    private String language;

    /** 仓库大小(KB) */
    @Excel(name = "仓库大小")
    private Integer size;

    /** Star数量 */
    @Excel(name = "Star数量")
    private Integer stargazersCount;

    /** Watch数量 */
    @Excel(name = "Watch数量")
    private Integer watchersCount;

    /** Fork数量 */
    @Excel(name = "Fork数量")
    private Integer forksCount;

    /** 开放Issue数量 */
    @Excel(name = "开放Issue数量")
    private Integer openIssuesCount;

    /** 是否私有仓库 */
    @Excel(name = "是否私有仓库", readConverterExp = "0=否,1=是")
    private Integer isPrivate;

    /** 是否为Fork仓库 */
    @Excel(name = "是否为Fork仓库", readConverterExp = "0=否,1=是")
    private Integer isFork;

    /** 是否已归档 */
    @Excel(name = "是否已归档", readConverterExp = "0=否,1=是")
    private Integer isArchived;

    /** 是否已禁用 */
    @Excel(name = "是否已禁用", readConverterExp = "0=否,1=是")
    private Integer isDisabled;

    /** 默认分支 */
    private String defaultBranch;

    /** 仓库标签(JSON数组) */
    private String topics;

    /** 许可证名称 */
    private String licenseName;

    /** README原始内容 */
    private String readmeContent;

    /** README下载URL */
    private String readmeUrl;

    /** AI生成的仓库简介 */
    private String aiSummary;

    /** AI简介生成时间 */
    private LocalDateTime aiSummaryTime;

    /** GitHub仓库创建时间 */
    @Excel(name = "GitHub创建时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime githubCreatedAt;

    /** GitHub仓库最后更新时间 */
    @Excel(name = "GitHub更新时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime githubUpdatedAt;

    /** GitHub仓库最后推送时间 */
    @Excel(name = "GitHub推送时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime githubPushedAt;

    /** 最后同步时间 */
    @Excel(name = "最后同步时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSyncTime;

    /** 同步状态(0-待同步 1-同步成功 2-同步失败) */
    @Excel(name = "同步状态", readConverterExp = "0=待同步,1=同步成功,2=同步失败")
    private String syncStatus;

    /** 错误信息 */
    private String errorMessage;

    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete = true)
    private String delFlag;
}
