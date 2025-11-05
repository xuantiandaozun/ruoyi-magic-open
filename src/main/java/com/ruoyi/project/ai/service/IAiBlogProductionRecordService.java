package com.ruoyi.project.ai.service;

import java.util.Date;
import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.ai.domain.AiBlogProductionRecord;

/**
 * AI博客生产记录Service接口
 * 
 * @author ruoyi
 */
public interface IAiBlogProductionRecordService extends IService<AiBlogProductionRecord> {
    
    /**
     * 根据仓库URL查询生产记录
     * 
     * @param repoUrl 仓库地址
     * @return 生产记录列表
     */
    List<AiBlogProductionRecord> listByRepoUrl(String repoUrl);

    /**
     * 根据生产类型查询记录
     * 
     * @param productionType 生产类型
     * @return 生产记录列表
     */
    List<AiBlogProductionRecord> listByProductionType(String productionType);

    /**
     * 根据博客ID查询生产记录
     * 
     * @param blogId 博客ID
     * @return 生产记录
     */
    AiBlogProductionRecord getByBlogId(Long blogId);

    /**
     * 查询指定日期的生产记录
     * 
     * @param targetDate 目标日期
     * @return 生产记录列表
     */
    List<AiBlogProductionRecord> listByTargetDate(Date targetDate);

    /**
     * 查询失败的生产记录（用于重试）
     * 
     * @return 失败的生产记录列表
     */
    List<AiBlogProductionRecord> listFailedRecords();

    /**
     * 查询进行中的生产记录
     * 
     * @return 进行中的生产记录列表
     */
    List<AiBlogProductionRecord> listRunningRecords();
}
