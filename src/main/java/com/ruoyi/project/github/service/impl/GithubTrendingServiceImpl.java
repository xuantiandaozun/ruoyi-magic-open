package com.ruoyi.project.github.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.framework.redis.RedisCache;
import com.ruoyi.project.github.domain.GithubTrending;
import com.ruoyi.project.github.domain.dto.GithubTrendingIngestRequest;
import com.ruoyi.project.github.domain.dto.GithubTrendingIngestRequest.GithubTrendingRepoDTO;
import com.ruoyi.project.github.domain.dto.GithubTrendingIngestResponse;
import com.ruoyi.project.github.mapper.GithubTrendingMapper;
import com.ruoyi.project.github.service.IGithubTrendingService;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * github流行榜单Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-07-03 11:47:11
 */
@Slf4j
@Service
public class GithubTrendingServiceImpl extends ServiceImpl<GithubTrendingMapper, GithubTrending> implements IGithubTrendingService
{
    private static final String IDEMPOTENCY_KEY_PREFIX = "github:trending:ingest:";
    private static final int IDEMPOTENCY_EXPIRE_HOURS = 24;
    
    @Autowired
    private RedisCache redisCache;

    /**
     * 接收云函数推送的 Trending 数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GithubTrendingIngestResponse ingestTrendingData(GithubTrendingIngestRequest request, String idempotencyKey) {
        GithubTrendingIngestResponse response = new GithubTrendingIngestResponse();
        response.setRequestId(idempotencyKey);
        response.setTotalReceived(request.getRepos().size());
        
        // 幂等性检查
        if (StrUtil.isNotBlank(idempotencyKey)) {
            String cacheKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
            Object cached = redisCache.getCacheObject(cacheKey);
            if (cached != null) {
                log.warn("重复请求被拦截，Idempotency-Key: {}", idempotencyKey);
                response.setMessage("重复请求，已被幂等性拦截");
                response.setSkipped(request.getRepos().size());
                return response;
            }
            // 设置幂等键缓存
            redisCache.setCacheObject(cacheKey, "1", IDEMPOTENCY_EXPIRE_HOURS, TimeUnit.HOURS);
        }
        
        // 统计数据
        int newInserted = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;
        
        // 批量查询已存在的仓库（根据 URL 去重）
        List<String> urls = request.getRepos().stream()
                .map(GithubTrendingRepoDTO::getUrl)
                .collect(Collectors.toList());
        
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where("url IN (?)", urls);
        List<GithubTrending> existingRepos = this.list(queryWrapper);
        Map<String, GithubTrending> existingRepoMap = existingRepos.stream()
                .collect(Collectors.toMap(GithubTrending::getUrl, repo -> repo));
        
        // 处理每个仓库
        for (GithubTrendingRepoDTO repoDTO : request.getRepos()) {
            try {
                // 补充 full_name
                if (StrUtil.isBlank(repoDTO.getFullName())) {
                    repoDTO.setFullName(repoDTO.getOwner() + "/" + repoDTO.getTitle());
                }
                
                GithubTrending existing = existingRepoMap.get(repoDTO.getUrl());
                
                if (existing == null) {
                    // 新增
                    GithubTrending newRepo = convertToEntity(repoDTO);
                    newRepo.setId(IdUtil.fastSimpleUUID());
                    
                    // 设置上榜统计
                    newRepo.setTrendingDays("1");
                    newRepo.setContinuousTrendingDays("1");
                    newRepo.setFirstTrendingDate(parseTrendingDate(repoDTO.getTrendingDate()));
                    newRepo.setLastTrendingDate(parseTrendingDate(repoDTO.getTrendingDate()));
                    newRepo.setCreatedAt(new Date());
                    newRepo.setUpdateAt(new Date());
                    
                    this.save(newRepo);
                    newInserted++;
                    log.info("新增 Trending 仓库: {}", repoDTO.getFullName());
                } else {
                    // 更新
                    Date trendingDate = parseTrendingDate(repoDTO.getTrendingDate());
                    Date lastTrendingDate = existing.getLastTrendingDate();
                    
                    // 更新基础信息
                    existing.setDescription(repoDTO.getDescription());
                    existing.setLanguage(repoDTO.getLanguage());
                    existing.setStarsCount(String.valueOf(repoDTO.getStarsCount()));
                    existing.setForksCount(String.valueOf(repoDTO.getForksCount()));
                    existing.setLastTrendingDate(trendingDate);
                    existing.setUpdateAt(new Date());
                    
                    // 更新上榜天数统计
                    int totalDays = Integer.parseInt(existing.getTrendingDays() == null ? "0" : existing.getTrendingDays());
                    existing.setTrendingDays(String.valueOf(totalDays + 1));
                    
                    // 计算连续上榜天数
                    if (lastTrendingDate != null && isConsecutiveDay(lastTrendingDate, trendingDate)) {
                        int continuousDays = Integer.parseInt(existing.getContinuousTrendingDays() == null ? "0" : existing.getContinuousTrendingDays());
                        existing.setContinuousTrendingDays(String.valueOf(continuousDays + 1));
                    } else {
                        existing.setContinuousTrendingDays("1");
                    }
                    
                    this.updateById(existing);
                    updated++;
                    log.info("更新 Trending 仓库: {}", repoDTO.getFullName());
                }
            } catch (Exception e) {
                failed++;
                log.error("处理仓库失败: {}, 错误: {}", repoDTO.getFullName(), e.getMessage(), e);
            }
        }
        
        // 填充响应
        response.setNewInserted(newInserted);
        response.setUpdated(updated);
        response.setSkipped(skipped);
        response.setFailed(failed);
        response.setMessage(String.format("处理完成: 新增 %d, 更新 %d, 失败 %d", newInserted, updated, failed));
        
        log.info("GitHub Trending 数据接入完成: {}", response.getMessage());
        return response;
    }
    
    /**
     * 将 DTO 转换为实体
     */
    private GithubTrending convertToEntity(GithubTrendingRepoDTO dto) {
        GithubTrending entity = new GithubTrending();
        entity.setTitle(dto.getTitle());
        entity.setOwner(dto.getOwner());
        entity.setDescription(dto.getDescription());
        entity.setUrl(dto.getUrl());
        entity.setLanguage(dto.getLanguage());
        entity.setStarsCount(String.valueOf(dto.getStarsCount()));
        entity.setForksCount(String.valueOf(dto.getForksCount()));
        entity.setIsTranDes("0");
        entity.setIsNeedUpdate("1");
        return entity;
    }
    
    /**
     * 解析上榜日期字符串
     */
    private Date parseTrendingDate(String dateStr) {
        if (StrUtil.isBlank(dateStr)) {
            return new Date();
        }
        try {
            LocalDate localDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return DateUtil.parse(localDate.toString());
        } catch (Exception e) {
            log.warn("日期解析失败: {}, 使用当前时间", dateStr);
            return new Date();
        }
    }
    
    /**
     * 判断是否为连续的天数
     */
    private boolean isConsecutiveDay(Date lastDate, Date currentDate) {
        if (lastDate == null || currentDate == null) {
            return false;
        }
        LocalDate last = DateUtil.toLocalDateTime(lastDate).toLocalDate();
        LocalDate current = DateUtil.toLocalDateTime(currentDate).toLocalDate();
        return last.plusDays(1).isEqual(current);
    }
}
