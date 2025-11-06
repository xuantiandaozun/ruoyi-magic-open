package com.ruoyi.project.ai.service;

import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.ai.domain.AiCoverGenerationRecord;

/**
 * AI生图记录Service接口
 * 
 * @author ruoyi
 */
public interface IAiCoverGenerationRecordService extends IService<AiCoverGenerationRecord> {
    
    /**
     * 根据博客ID查询生成记录
     * 
     * @param blogId 博客ID
     * @return 生成记录列表
     */
    List<AiCoverGenerationRecord> listByBlogId(Long blogId);

    /**
     * 根据封面类型查询记录
     * 
     * @param coverType 封面类型（0-通用封面 1-个性化封面）
     * @return 生成记录列表
     */
    List<AiCoverGenerationRecord> listByCoverType(String coverType);

    /**
     * 根据分类查询记录
     * 
     * @param category 博客分类
     * @return 生成记录列表
     */
    List<AiCoverGenerationRecord> listByCategory(String category);

    /**
     * 查询失败的生成记录
     * 
     * @return 失败的生成记录列表
     */
    List<AiCoverGenerationRecord> listFailedRecords();

    /**
     * 查询成功且已被使用的记录
     * 
     * @return 已使用的生成记录列表
     */
    List<AiCoverGenerationRecord> listUsedRecords();

    /**
     * 查询成功但未被使用的记录
     * 
     * @return 未使用的生成记录列表
     */
    List<AiCoverGenerationRecord> listUnusedRecords();

    /**
     * 查询可复用的通用封面（成功且未使用）
     * 
     * @param category 分类
     * @return 可复用的通用封面列表
     */
    List<AiCoverGenerationRecord> listReusableGenericCovers(String category);

    /**
     * 根据提示词查询成功且未使用的记录
     * 
     * @param prompt 提示词
     * @return 成功且未使用的生成记录列表
     */
    List<AiCoverGenerationRecord> listByPrompt(String prompt);
}
