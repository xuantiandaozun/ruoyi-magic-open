package com.ruoyi.project.github.domain.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * GitHub Trending 数据接入请求
 * 
 * @author ruoyi
 * @date 2025-12-02
 */
@Data
public class GithubTrendingIngestRequest {

    /**
     * 仓库列表
     */
    @NotEmpty(message = "仓库列表不能为空")
    @Valid
    private List<GithubTrendingRepoDTO> repos;

    /**
     * 元数据信息
     */
    private MetaDTO meta;

    /**
     * 仓库数据传输对象
     */
    @Data
    public static class GithubTrendingRepoDTO {
        
        /**
         * 仓库所有者
         */
        @NotEmpty(message = "仓库所有者不能为空")
        private String owner;

        /**
         * 仓库标题
         */
        @NotEmpty(message = "仓库标题不能为空")
        private String title;

        /**
         * 完整仓库名(owner/title)
         */
        private String fullName;

        /**
         * 仓库地址
         */
        @NotEmpty(message = "仓库地址不能为空")
        private String url;

        /**
         * 仓库描述
         */
        private String description;

        /**
         * 仓库语言
         */
        private String language;

        /**
         * Star 数量
         */
        private Integer starsCount;

        /**
         * Fork 数量
         */
        private Integer forksCount;

        /**
         * 上榜日期 (YYYY-MM-DD)
         */
        private String trendingDate;
    }

    /**
     * 元数据对象
     */
    @Data
    public static class MetaDTO {
        
        /**
         * 抓取时间 (YYYY-MM-DD HH:mm:ss)
         */
        private String fetchedAt;

        /**
         * 数据来源
         */
        private String source;

        /**
         * 时间周期 (daily/weekly/monthly)
         */
        private String period;

        /**
         * 数据版本
         */
        private String version;
    }
}
