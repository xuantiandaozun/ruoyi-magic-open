package com.ruoyi.project.mediaassistant.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.project.ai.workflow.AiGateway;
import com.ruoyi.project.article.service.AiBlogPublishService;
import com.ruoyi.project.blogapi.domain.dto.AiBlogPublishRequest;
import com.ruoyi.project.mediaassistant.domain.MediaAiAnalysis;
import com.ruoyi.project.mediaassistant.domain.MediaContentDraft;
import com.ruoyi.project.mediaassistant.domain.MediaSource;
import com.ruoyi.project.mediaassistant.domain.dto.RedditSourceCaptureRequest;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 自媒体助手采集、分析和草稿编排服务。
 */
@Slf4j
@Service
public class MediaAssistantService {

    private final IMediaSourceService mediaSourceService;
    private final IMediaAiAnalysisService mediaAiAnalysisService;
    private final IMediaContentDraftService mediaContentDraftService;
    private final AiBlogPublishService aiBlogPublishService;
    private final AiGateway aiGateway;

    @Value("${media-assistant.ai.model-config-id:0}")
    private Long modelConfigId;

    public MediaAssistantService(IMediaSourceService mediaSourceService,
            IMediaAiAnalysisService mediaAiAnalysisService,
            IMediaContentDraftService mediaContentDraftService,
            AiBlogPublishService aiBlogPublishService,
            AiGateway aiGateway) {
        this.mediaSourceService = mediaSourceService;
        this.mediaAiAnalysisService = mediaAiAnalysisService;
        this.mediaContentDraftService = mediaContentDraftService;
        this.aiBlogPublishService = aiBlogPublishService;
        this.aiGateway = aiGateway;
    }

    public Map<String, Object> captureReddit(RedditSourceCaptureRequest request) {
        String sourceUrl = normalizeUrl(request.getSourceUrl());
        String contentHash = DigestUtil.sha256Hex((StrUtil.blankToDefault(request.getTitle(), "") + "\n"
                + StrUtil.blankToDefault(request.getContent(), "")).getBytes(StandardCharsets.UTF_8));

        MediaSource existing = findExistingSource(sourceUrl, contentHash);
        if (existing != null) {
            Map<String, Object> result = buildSourceResult(existing, true);
            if (Boolean.TRUE.equals(request.getAnalyzeNow())) {
                result.put("analysis", analyze(existing.getSourceId()));
            }
            return result;
        }

        MediaSource source = new MediaSource();
        source.setSourcePlatform("reddit");
        source.setSourceUrl(sourceUrl);
        source.setSourceExternalId(trimToNull(request.getRedditPostId()));
        source.setTitle(trimToNull(request.getTitle()));
        source.setAuthor(trimToNull(request.getAuthor()));
        source.setCommunity(trimToNull(request.getSubreddit()));
        source.setRawContent(trimToNull(request.getContent()));
        source.setRawComments(JSONUtil.toJsonStr(request.getTopComments()));
        source.setRawPayload(JSONUtil.toJsonStr(request.getRawPayload()));
        source.setContentHash(contentHash);
        source.setCollectStatus("pending");
        source.setCollectedAt(new Date());
        mediaSourceService.save(source);

        Map<String, Object> result = buildSourceResult(source, false);
        if (Boolean.TRUE.equals(request.getAnalyzeNow())) {
            result.put("analysis", analyze(source.getSourceId()));
        }
        return result;
    }

    public Map<String, Object> analyze(Long sourceId) {
        MediaSource source = mediaSourceService.getById(sourceId);
        if (source == null) {
            throw new IllegalArgumentException("素材不存在: " + sourceId);
        }

        source.setCollectStatus("analyzing");
        source.setFailReason(null);
        mediaSourceService.updateById(source);

        JSONObject analysisJson;
        String rawAiResponse = null;
        try {
            if (modelConfigId != null && modelConfigId > 0) {
                rawAiResponse = aiGateway.chat(modelConfigId, buildSystemPrompt(), buildUserPrompt(source), List.of());
                analysisJson = parseAiJson(rawAiResponse);
            } else {
                analysisJson = buildHeuristicAnalysis(source);
                rawAiResponse = analysisJson.toString();
            }
        } catch (Exception e) {
            log.warn("自媒体助手AI分析失败，改用启发式分析: sourceId={}, error={}", sourceId, e.getMessage());
            analysisJson = buildHeuristicAnalysis(source);
            rawAiResponse = analysisJson.toString();
        }

        MediaAiAnalysis analysis = saveAnalysis(source, analysisJson, rawAiResponse);
        List<MediaContentDraft> drafts = saveDrafts(source, analysis, analysisJson);

        source.setCollectStatus("done");
        mediaSourceService.updateById(source);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", buildSourceResult(source, false));
        result.put("analysisId", analysis.getAnalysisId());
        result.put("recommendation", analysis.getRecommendation());
        result.put("suitablePlatforms", JSONUtil.parseArray(analysis.getSuitablePlatforms()));
        result.put("draftCount", drafts.size());
        result.put("drafts", drafts);
        return result;
    }

    public Map<String, Object> saveDraftToBlog(Long draftId) {
        MediaContentDraft draft = mediaContentDraftService.getById(draftId);
        if (draft == null) {
            throw new IllegalArgumentException("草稿不存在: " + draftId);
        }
        MediaSource source = mediaSourceService.getById(draft.getSourceId());
        if (source == null) {
            throw new IllegalArgumentException("素材不存在: " + draft.getSourceId());
        }

        AiBlogPublishRequest request = new AiBlogPublishRequest();
        request.setTitle(draft.getTitle());
        request.setSummary(draft.getSummary());
        request.setContent(draft.getContent());
        request.setCategory("技术观察");
        request.setTags(StrUtil.blankToDefault(draft.getTags(), "程序员,技术观察,Reddit"));
        request.setStatus("0");
        request.setIsOriginal("1");
        request.setRepoUrl(source.getSourceUrl());
        request.setRepoName("reddit/" + StrUtil.blankToDefault(source.getCommunity(), "unknown"));

        Map<String, Object> blogResult = aiBlogPublishService.publish(request, "0", "reddit_media_assistant");
        Object blogId = blogResult.get("blogId");
        if (blogId != null) {
            draft.setRelatedBlogId(Long.parseLong(String.valueOf(blogId)));
        }
        draft.setStatus("approved");
        draft.setPublishResult(JSONUtil.toJsonStr(blogResult));
        mediaContentDraftService.updateById(draft);
        return blogResult;
    }

    private MediaSource findExistingSource(String sourceUrl, String contentHash) {
        MediaSource byUrl = mediaSourceService.getOne(QueryWrapper.create().eq("source_url", sourceUrl));
        if (byUrl != null) {
            return byUrl;
        }
        if (StrUtil.isNotBlank(contentHash)) {
            return mediaSourceService.getOne(QueryWrapper.create().eq("content_hash", contentHash));
        }
        return null;
    }

    private MediaAiAnalysis saveAnalysis(MediaSource source, JSONObject analysisJson, String rawAiResponse) {
        MediaAiAnalysis analysis = new MediaAiAnalysis();
        analysis.setSourceId(source.getSourceId());
        analysis.setProgrammerRelevanceScore(BigDecimal.valueOf(analysisJson.getDouble("programmerRelevanceScore", 0D)));
        analysis.setValueScore(BigDecimal.valueOf(analysisJson.getDouble("valueScore", 0D)));
        analysis.setOriginalityRisk(analysisJson.getStr("originalityRisk", "medium"));
        analysis.setRecommendation(analysisJson.getStr("recommendation", "rewrite"));
        analysis.setSuitablePlatforms(JSONUtil.toJsonStr(analysisJson.getJSONArray("suitablePlatforms")));
        analysis.setTopicAngle(analysisJson.getStr("topicAngle"));
        analysis.setReason(analysisJson.getStr("reason"));
        analysis.setAiModel(modelConfigId != null && modelConfigId > 0 ? String.valueOf(modelConfigId) : "heuristic");
        analysis.setAiRawResponse(rawAiResponse);
        mediaAiAnalysisService.save(analysis);
        return analysis;
    }

    private List<MediaContentDraft> saveDrafts(MediaSource source, MediaAiAnalysis analysis, JSONObject analysisJson) {
        List<MediaContentDraft> drafts = new ArrayList<>();
        JSONArray draftArray = analysisJson.getJSONArray("drafts");
        if (draftArray == null || draftArray.isEmpty()) {
            return drafts;
        }
        for (Object item : draftArray) {
            JSONObject draftJson = JSONUtil.parseObj(item);
            MediaContentDraft draft = new MediaContentDraft();
            draft.setSourceId(source.getSourceId());
            draft.setAnalysisId(analysis.getAnalysisId());
            draft.setTargetPlatform(draftJson.getStr("targetPlatform", "blog"));
            draft.setContentType(draftJson.getStr("contentType", "long_article"));
            draft.setTitle(trimToNull(draftJson.getStr("title")));
            draft.setSummary(trimToNull(draftJson.getStr("summary")));
            draft.setContent(StrUtil.blankToDefault(draftJson.getStr("content"), ""));
            draft.setTags(trimToNull(draftJson.getStr("tags")));
            draft.setCoverPrompt(trimToNull(draftJson.getStr("coverPrompt")));
            draft.setStatus("draft");
            mediaContentDraftService.save(draft);
            drafts.add(draft);

            if ("blog".equals(draft.getTargetPlatform()) && "long_article".equals(draft.getContentType())) {
                try {
                    saveDraftToBlog(draft.getDraftId());
                } catch (Exception e) {
                    log.warn("自媒体助手博客草稿落库失败: draftId={}, error={}", draft.getDraftId(), e.getMessage());
                }
            }
        }
        return drafts;
    }

    private JSONObject parseAiJson(String response) {
        String text = StrUtil.trim(response);
        if (text.startsWith("```")) {
            text = text.replaceFirst("(?s)^```[a-zA-Z]*\\s*", "").replaceFirst("(?s)\\s*```$", "");
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }
        return JSONUtil.parseObj(text);
    }

    private JSONObject buildHeuristicAnalysis(MediaSource source) {
        String text = (StrUtil.blankToDefault(source.getTitle(), "") + "\n" + StrUtil.blankToDefault(source.getRawContent(), ""))
                .toLowerCase();
        int keywordHits = 0;
        for (String keyword : List.of("developer", "programming", "code", "coding", "software", "engineer",
                "ai", "llm", "api", "database", "javascript", "java", "python", "rust", "devops",
                "architecture", "open source", "github", "debug", "productivity")) {
            if (text.contains(keyword)) {
                keywordHits++;
            }
        }
        double relevance = Math.min(100, 35 + keywordHits * 8);
        double value = Math.min(100, 40 + Math.min(StrUtil.length(source.getRawContent()) / 80, 35) + keywordHits * 4);
        boolean accept = relevance >= 60 && value >= 55;

        JSONArray platforms = JSONUtil.createArray();
        if (accept) {
            platforms.add("blog");
            platforms.add("wechat");
            platforms.add("x");
            if (value < 72) {
                platforms.add("wetoutiao");
            }
        } else {
            platforms.add("x");
            platforms.add("wetoutiao");
        }

        JSONObject result = JSONUtil.createObj();
        result.set("recommendation", accept ? "accept" : "rewrite");
        result.set("programmerRelevanceScore", relevance);
        result.set("valueScore", value);
        result.set("originalityRisk", "medium");
        result.set("suitablePlatforms", platforms);
        result.set("topicAngle", buildTopicAngle(source));
        result.set("reason", accept ? "内容与程序员、技术工具或工程实践有关，可作为技术观点文章素材。"
                : "内容技术相关性或信息密度一般，更适合整理为短观点而不是长文。");
        result.set("drafts", buildHeuristicDrafts(source, platforms));
        return result;
    }

    private JSONArray buildHeuristicDrafts(MediaSource source, JSONArray platforms) {
        JSONArray drafts = JSONUtil.createArray();
        String title = StrUtil.blankToDefault(source.getTitle(), "Reddit 技术讨论观察");
        String excerpt = StrUtil.blankToDefault(source.getRawContent(), "");
        if (excerpt.length() > 1200) {
            excerpt = excerpt.substring(0, 1200);
        }
        String sourceLine = "\n\n> 来源：Reddit " + source.getSourceUrl();

        if (platforms.contains("blog")) {
            JSONObject blogDraft = JSONUtil.createObj();
            blogDraft.set("targetPlatform", "blog");
            blogDraft.set("contentType", "long_article");
            blogDraft.set("title", "从 Reddit 讨论看：" + title);
            blogDraft.set("summary", "基于 Reddit 技术讨论整理的程序员视角观察，适合作为后续深度改写草稿。");
            blogDraft.set("tags", "程序员,技术观察,Reddit");
            blogDraft.set("content", """
                    # 从 Reddit 讨论看：%s

                    ## 背景

                    这篇草稿来自一次 Reddit 技术讨论。原帖的核心信息是：

                    %s

                    ## 程序员视角

                    这个话题值得关注，不是因为它本身一定有结论，而是它反映了开发者在工具选择、工程效率、技术判断上的真实困惑。

                    ## 可以展开的文章方向

                    1. 这个问题背后的工程约束是什么。
                    2. 对个人开发者和团队开发分别有什么影响。
                    3. 哪些经验可以沉淀成可复用的方法论。

                    ## 待人工补充

                    - 增加自己的真实项目经验。
                    - 补充中文技术生态里的对照案例。
                    - 删除与原帖表达过近的句子，改成自己的判断。
                    %s
                    """.formatted(title, excerpt, sourceLine));
            drafts.add(blogDraft);
        }

        JSONObject shortDraft = JSONUtil.createObj();
        shortDraft.set("targetPlatform", platforms.contains("x") ? "x" : "wetoutiao");
        shortDraft.set("contentType", platforms.contains("x") ? "thread" : "short_post");
        shortDraft.set("title", title);
        shortDraft.set("tags", "程序员,技术观点");
        shortDraft.set("content", "Reddit 上这个讨论挺适合程序员反思：\n\n" + title
                + "\n\n我的初步判断是：真正有价值的不是结论，而是它暴露出的工程取舍。后续可以展开成一篇博客。"
                + sourceLine);
        drafts.add(shortDraft);
        return drafts;
    }

    private String buildSystemPrompt() {
        return """
                你是程序员博主的自媒体选题助手。只筛选适合程序员、软件工程、AI 编程、工具链、架构、效率、开源生态的内容。
                你必须输出严格 JSON，不要 Markdown，不要解释。
                对原文只能做观点提炼和重新组织，不能近似翻译或洗稿。
                """;
    }

    private String buildUserPrompt(MediaSource source) {
        return """
                请分析下面 Reddit 帖子是否适合我的自媒体内容体系，并生成多平台草稿。

                输出 JSON 字段：
                recommendation: accept/rewrite/ignore
                programmerRelevanceScore: 0-100
                valueScore: 0-100
                originalityRisk: low/medium/high
                suitablePlatforms: blog/wechat/toutiao/wetoutiao/x/wechat_image 数组
                topicAngle: 中文切入角度
                reason: 中文原因
                drafts: 数组，每项包含 targetPlatform、contentType、title、summary、content、tags、coverPrompt

                Reddit URL: %s
                Subreddit: %s
                Title: %s
                Content:
                %s

                Top comments JSON:
                %s
                """.formatted(source.getSourceUrl(), source.getCommunity(), source.getTitle(),
                source.getRawContent(), source.getRawComments());
    }

    private String buildTopicAngle(MediaSource source) {
        return "从 Reddit 讨论切入，结合程序员日常开发经验重新组织成中文技术观点。";
    }

    private Map<String, Object> buildSourceResult(MediaSource source, boolean duplicateSkipped) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sourceId", source.getSourceId());
        result.put("sourceUrl", source.getSourceUrl());
        result.put("title", source.getTitle());
        result.put("status", source.getCollectStatus());
        result.put("duplicateSkipped", duplicateSkipped);
        return result;
    }

    private String normalizeUrl(String url) {
        String normalized = StrUtil.trim(url);
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }
}
