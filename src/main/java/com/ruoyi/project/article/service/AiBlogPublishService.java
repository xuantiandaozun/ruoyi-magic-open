package com.ruoyi.project.article.service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ruoyi.project.ai.domain.AiBlogProductionRecord;
import com.ruoyi.project.ai.service.IAiBlogProductionRecordService;
import com.ruoyi.project.article.domain.Blog;
import com.ruoyi.project.blogapi.domain.dto.AiBlogPublishRequest;
import com.ruoyi.project.wechatmp.service.IWechatMpDraftService;

import cn.hutool.core.util.StrUtil;

/**
 * AI 博客发布编排服务
 */
@Service
public class AiBlogPublishService {

    private final IBlogService blogService;
    private final IAiBlogProductionRecordService aiBlogProductionRecordService;
    private final IWechatMpDraftService wechatMpDraftService;

    public AiBlogPublishService(IBlogService blogService,
            IAiBlogProductionRecordService aiBlogProductionRecordService,
            IWechatMpDraftService wechatMpDraftService) {
        this.blogService = blogService;
        this.aiBlogProductionRecordService = aiBlogProductionRecordService;
        this.wechatMpDraftService = wechatMpDraftService;
    }

    public Map<String, Object> publish(AiBlogPublishRequest request, String defaultStatus, String productionType) {
        String normalizedRepoUrl = normalizeRepoUrl(request.getRepoUrl());
        Map<String, Object> duplicateResult = tryBuildDuplicateResult(normalizedRepoUrl);
        if (duplicateResult != null) {
            return duplicateResult;
        }

        Blog blog = new Blog();
        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());
        blog.setSummary(trimToNull(request.getSummary()));
        blog.setCoverImage(trimToNull(request.getCoverImage()));
        blog.setCategory(trimToNull(request.getCategory()));
        blog.setTags(trimToNull(request.getTags()));
        blog.setStatus(resolveStatus(request.getStatus(), defaultStatus));
        blog.setIsTop(resolveBinaryValue(request.getIsTop(), "0"));
        blog.setIsOriginal(resolveBinaryValue(request.getIsOriginal(), "1"));
        blog.setFeishuDocToken(trimToNull(request.getFeishuDocToken()));
        blog.setFeishuDocName(trimToNull(request.getFeishuDocName()));
        blog.setViewCount("0");
        blog.setLikeCount("0");
        blog.setCommentCount(0L);
        blog.setFeishuSyncStatus("0");

        if ("1".equals(blog.getStatus())) {
            blog.setPublishTime(LocalDateTime.now());
        }

        boolean saved = blogService.save(blog);
        if (!saved) {
            throw new IllegalStateException("博客文章保存失败，请检查数据库连接或参数是否正确");
        }

        saveProductionRecord(blog, normalizedRepoUrl, request.getRepoName(), productionType);

        Map<String, Object> result = buildResult(blog, false);
        result.put("detailApiPath", "/api/blog/detail/" + blog.getBlogId());

        if (Boolean.TRUE.equals(request.getSyncWechatDraft())) {
            try {
                result.put("wechatDraft", wechatMpDraftService.syncBlogToDraft(blog.getBlogId()));
                result.put("wechatDraftSynced", true);
            } catch (Exception e) {
                result.put("wechatDraftSynced", false);
                result.put("wechatDraftError", e.getMessage());
            }
        }

        return result;
    }

    private Map<String, Object> tryBuildDuplicateResult(String repoUrl) {
        if (StrUtil.isBlank(repoUrl)) {
            return null;
        }
        AiBlogProductionRecord existingRecord = aiBlogProductionRecordService.findTodaySuccessByRepoUrl(repoUrl);
        if (existingRecord == null || existingRecord.getBlogId() == null) {
            return null;
        }
        Blog existingBlog = blogService.getById(String.valueOf(existingRecord.getBlogId()));
        if (existingBlog == null) {
            return null;
        }
        Map<String, Object> result = buildResult(existingBlog, true);
        result.put("detailApiPath", "/api/blog/detail/" + existingBlog.getBlogId());
        return result;
    }

    private void saveProductionRecord(Blog blog, String repoUrl, String repoName, String productionType) {
        AiBlogProductionRecord productionRecord = new AiBlogProductionRecord();
        productionRecord.setRepoUrl(StrUtil.blankToDefault(repoUrl, ""));

        if (StrUtil.isNotBlank(repoName)) {
            if (repoName.contains("/")) {
                String[] parts = repoName.split("/", 2);
                productionRecord.setRepoOwner(parts[0]);
                productionRecord.setRepoTitle(parts.length > 1 ? parts[1] : repoName);
            } else {
                productionRecord.setRepoTitle(repoName);
            }
        }

        if (StrUtil.isNotBlank(blog.getBlogId())) {
            productionRecord.setBlogId(Long.parseLong(blog.getBlogId()));
        }
        productionRecord.setProductionType(StrUtil.blankToDefault(productionType, "blog_generation"));
        productionRecord.setStatus("1");
        productionRecord.setProductionTime(new Date());
        productionRecord.setCompletionTime(new Date());
        aiBlogProductionRecordService.save(productionRecord);
    }

    private Map<String, Object> buildResult(Blog blog, boolean duplicateSkipped) {
        Map<String, Object> result = new HashMap<>();
        result.put("blogId", blog.getBlogId());
        result.put("title", blog.getTitle());
        result.put("status", getStatusText(blog.getStatus()));
        result.put("statusCode", blog.getStatus());
        result.put("category", StrUtil.blankToDefault(blog.getCategory(), "未分类"));
        result.put("tags", StrUtil.blankToDefault(blog.getTags(), "无标签"));
        result.put("duplicateSkipped", duplicateSkipped);
        return result;
    }

    private String resolveStatus(String status, String defaultStatus) {
        if (StrUtil.isBlank(status)) {
            return StrUtil.blankToDefault(defaultStatus, "1");
        }
        if ("0".equals(status) || "1".equals(status) || "2".equals(status)) {
            return status;
        }
        throw new IllegalArgumentException("状态参数不正确，应为 0、1 或 2");
    }

    private String resolveBinaryValue(String value, String defaultValue) {
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        if ("0".equals(value) || "1".equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("布尔状态参数不正确，应为 0 或 1");
    }

    private String getStatusText(String status) {
        return switch (status) {
            case "0" -> "草稿";
            case "1" -> "已发布";
            case "2" -> "已下线";
            default -> "未知状态";
        };
    }

    private String normalizeRepoUrl(String repoUrl) {
        if (StrUtil.isBlank(repoUrl)) {
            return null;
        }
        String normalized = repoUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }
}
