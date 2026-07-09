package com.ruoyi.project.mediaassistant.domain.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Reddit 帖子采集请求。
 */
@Data
public class RedditSourceCaptureRequest {

    @NotBlank(message = "帖子URL不能为空")
    private String sourceUrl;

    private String redditPostId;

    private String title;

    private String author;

    private String subreddit;

    private String content;

    private Integer score;

    private Integer commentCount;

    private String createdAt;

    private List<Map<String, Object>> topComments;

    private Map<String, Object> rawPayload;

    private Boolean analyzeNow = true;
}
