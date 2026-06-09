package com.ruoyi.project.blogapi.domain.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 直发博客请求
 */
@Data
public class AiBlogPublishRequest {

    @NotBlank(message = "博客标题不能为空")
    private String title;

    @NotBlank(message = "博客内容不能为空")
    private String content;

    private String summary;

    private String coverImage;

    private String category;

    private String tags;

    /**
     * 状态（0草稿 1已发布 2已下线）
     */
    @Pattern(regexp = "^[012]$", message = "status 只能为 0、1 或 2")
    private String status;

    @Pattern(regexp = "^[01]$", message = "isTop 只能为 0 或 1")
    private String isTop;

    @Pattern(regexp = "^[01]$", message = "isOriginal 只能为 0 或 1")
    private String isOriginal;

    private String feishuDocToken;

    private String feishuDocName;

    private String repoUrl;

    private String repoName;

    /**
     * 是否在发布后同步到微信公众号草稿箱
     */
    private Boolean syncWechatDraft;
}
