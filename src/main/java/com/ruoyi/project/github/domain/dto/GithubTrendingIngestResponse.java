package com.ruoyi.project.github.domain.dto;

import lombok.Data;

/**
 * GitHub Trending 数据接入响应
 * 
 * @author ruoyi
 * @date 2025-12-02
 */
@Data
public class GithubTrendingIngestResponse {

    /**
     * 接收总数
     */
    private Integer totalReceived;

    /**
     * 新增数量
     */
    private Integer newInserted;

    /**
     * 更新数量
     */
    private Integer updated;

    /**
     * 跳过数量
     */
    private Integer skipped;

    /**
     * 失败数量
     */
    private Integer failed;

    /**
     * 处理消息
     */
    private String message;

    /**
     * 请求ID(幂等键)
     */
    private String requestId;
}
