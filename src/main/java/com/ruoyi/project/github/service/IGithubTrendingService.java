package com.ruoyi.project.github.service;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.github.domain.GithubTrending;
import com.ruoyi.project.github.domain.dto.GithubTrendingIngestRequest;
import com.ruoyi.project.github.domain.dto.GithubTrendingIngestResponse;

/**
 * github流行榜单Service接口
 * 
 * @author ruoyi
 * @date 2025-07-03 11:47:11
 */
public interface IGithubTrendingService extends IService<GithubTrending>
{
    /**
     * 接收云函数推送的 Trending 数据
     * 
     * @param request 请求数据
     * @param idempotencyKey 幂等键
     * @return 处理结果
     */
    GithubTrendingIngestResponse ingestTrendingData(GithubTrendingIngestRequest request, String idempotencyKey);
}
