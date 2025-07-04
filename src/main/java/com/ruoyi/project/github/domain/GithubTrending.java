package com.ruoyi.project.github.domain;

import java.util.Date;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * github流行榜单对象 github_trending
 * 
 * @author ruoyi
 * @date 2025-07-03 11:47:11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table(value = "github_trending",dataSource="爬虫" ,mapperGenerateEnable=false)
public class GithubTrending extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /**  */
    @Excel(name = "")
    @Id(keyType = KeyType.Auto)
    private String id;

    /** 仓库标题 */
    @Excel(name = "仓库标题")
    private String title;

    /** 仓库作者 */
    @Excel(name = "仓库作者")
    private String owner;

    /** 仓库描述 */
    private String description;

    /** 仓库地址 */
    @Excel(name = "仓库地址")
    private String url;

    /** 仓库语言 */
    @Excel(name = "仓库语言")
    private String language;

    /** 总上榜天数 */
    private String trendingDays;

    /** 连续上榜天数 */
    private String continuousTrendingDays;

    /** 首次上榜日期 */
    private Date firstTrendingDate;

    /** 最后一次上榜日期 */
    private Date lastTrendingDate;

    /** 跟新时间 */
    private Date updatedAt;

    /** 是否翻译描述 */
    private String isTranDes;

    /** 项目的 star 数量 */
    private String starsCount;

    /** 项目的 fork 数量 */
    private String forksCount;

    /** 开放问题数量 */
    private String openIssuesCount;

    /** 仓库创建时间 */
    private Date githubCreatedAt;

    /** 仓库最后更新时间 */
    private Date githubUpdatedAt;

    /** readme 文件路径 */
    private String readmePath;

    /** readme 文件跟新日期 */
    private Date readmeUpdatedAt;

    /** 创建时间 */
    private Date createdAt;

    /** 修改时间 */
    private Date updateAt;

    /** 是否需要跟新项目 */
    private String isNeedUpdate;

    /** ai翻译后的readme文件 */
    private String aiReadmePath;

    /** 仓库价值:普通/值得关注/值得收藏/商业价值 */
    private String repValue;

    /** 仓库推广文章 */
    private String promotionArticle;

}
