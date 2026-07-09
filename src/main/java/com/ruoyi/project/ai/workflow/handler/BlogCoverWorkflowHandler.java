package com.ruoyi.project.ai.workflow.handler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.ai.tool.impl.AliImageGenerationLangChain4jTool;
import com.ruoyi.project.ai.tool.impl.BlogCoverUpdateLangChain4jTool;
import com.ruoyi.project.article.domain.Blog;
import com.ruoyi.project.article.service.IBlogService;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 工作流封面步骤的确定性执行器：直接调用生图与封面更新工具，不依赖 LLM 是否发起 tool_calls。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlogCoverWorkflowHandler {

    private static final String COVER_SIZE = "1664*928";
    private static final String COVER_MODEL = "qwen-image-plus";
    private static final String NEGATIVE_PROMPT =
            "people, portrait, face, watermark, logo, long text, blurry, low quality";

    private static final Pattern BLOG_ID_JSON = Pattern.compile(
            "\"blogId\"\\s*:\\s*\"?(\\d+)\"?", Pattern.CASE_INSENSITIVE);
    private static final Pattern BLOG_ID_TEXT = Pattern.compile(
            "(?:Blog|Article)\\s*ID[:：\\s]+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ZH_BLOG_ID_JSON = Pattern.compile(
            "\"zhBlogId\"\\s*:\\s*\"?(\\d+)\"?", Pattern.CASE_INSENSITIVE);
    private static final Pattern ZH_BLOG_ID_TEXT = Pattern.compile(
            "zhBlogId[:：\\s=]+(\\d+)", Pattern.CASE_INSENSITIVE);

    private final IBlogService blogService;
    private final AliImageGenerationLangChain4jTool aliImageGenerationTool;
    private final BlogCoverUpdateLangChain4jTool blogCoverUpdateTool;

    public String execute(Map<String, Object> context) {
        String chineseArticle = asText(context.get("chinese_article"));
        String englishArticle = asText(context.get("english_article"));

        String zhBlogId = extractBlogId(chineseArticle);
        if (StrUtil.isBlank(zhBlogId)) {
            zhBlogId = extractZhBlogId(englishArticle);
        }
        String enBlogId = extractBlogId(englishArticle);

        if (StrUtil.isBlank(zhBlogId) && StrUtil.isBlank(enBlogId)) {
            throw new ServiceException("封面步骤无法从上下文解析博客ID，请检查 save-cn / translate-en 输出");
        }

        Blog blog = null;
        if (StrUtil.isNotBlank(zhBlogId)) {
            blog = blogService.getById(zhBlogId);
            if (blog == null) {
                throw new ServiceException("中文博客不存在，ID: " + zhBlogId);
            }
        }

        String prompt = buildCoverPrompt(blog, chineseArticle, asText(context.get("github_analysis")));
        log.info("确定性封面生成开始: zhBlogId={}, enBlogId={}, size={}", zhBlogId, enBlogId, COVER_SIZE);

        Map<String, Object> imageParams = new LinkedHashMap<>();
        imageParams.put("prompt", prompt);
        imageParams.put("model", COVER_MODEL);
        imageParams.put("size", COVER_SIZE);
        imageParams.put("negativePrompt", NEGATIVE_PROMPT);
        imageParams.put("promptExtend", true);
        imageParams.put("watermark", false);

        String imageResult = aliImageGenerationTool.execute(imageParams);
        JSONObject imageJson = JSONUtil.parseObj(imageResult);
        if (!imageJson.getBool("success", false)) {
            throw new ServiceException(imageJson.getStr("message", "AI封面生成失败"));
        }

        JSONObject imageData = imageJson.getJSONObject("data");
        String coverImageUrl = imageData != null ? imageData.getStr("imageUrl") : null;
        if (StrUtil.isBlank(coverImageUrl)) {
            throw new ServiceException("AI封面生成成功，但未返回图片地址");
        }
        if (isForbiddenExternalUrl(coverImageUrl)) {
            throw new ServiceException("拒绝使用外部临时图床URL: " + coverImageUrl);
        }

        Map<String, Object> updateParams = new LinkedHashMap<>();
        updateParams.put("coverImageUrl", coverImageUrl);
        if (StrUtil.isNotBlank(zhBlogId)) {
            updateParams.put("zhBlogId", zhBlogId);
        }
        if (StrUtil.isNotBlank(enBlogId)) {
            updateParams.put("enBlogId", enBlogId);
        }

        String updateResult = blogCoverUpdateTool.execute(updateParams);
        JSONObject updateJson = JSONUtil.parseObj(updateResult);
        if (!updateJson.getBool("success", false)) {
            throw new ServiceException(updateJson.getStr("message", "博客封面更新失败"));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("coverImageUrl", coverImageUrl);
        result.put("prompt", prompt);
        result.put("zhBlogId", zhBlogId);
        result.put("enBlogId", enBlogId);
        result.put("status", "success");
        result.put("message", "封面已通过确定性步骤生成并更新");
        result.put("imageResult", imageJson.get("data"));
        result.put("updateResult", updateJson.get("data"));
        result.put("handler", "blog_cover");

        log.info("确定性封面生成完成: zhBlogId={}, enBlogId={}, url={}", zhBlogId, enBlogId, coverImageUrl);
        return JSONUtil.toJsonStr(result);
    }

    private String buildCoverPrompt(Blog blog, String chineseArticle, String githubAnalysis) {
        String title = blog != null ? StrUtil.blankToDefault(blog.getTitle(), "技术博客") : "技术博客";
        String summary = blog != null ? StrUtil.blankToDefault(blog.getSummary(), "") : "";
        String category = blog != null ? StrUtil.blankToDefault(blog.getCategory(), "technology") : "technology";
        String tags = blog != null ? StrUtil.blankToDefault(blog.getTags(), "") : "";
        String content = blog != null ? StrUtil.blankToDefault(blog.getContent(), "") : chineseArticle;
        content = content.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ");
        if (content.length() > 1200) {
            content = content.substring(0, 1200);
        }
        String analysis = StrUtil.blankToDefault(githubAnalysis, "").replaceAll("\\s+", " ");
        if (analysis.length() > 600) {
            analysis = analysis.substring(0, 600);
        }

        return """
                Create a modern 16:9 technical blog cover image.
                Style: clean, premium, high contrast, editorial technology illustration, no people, no faces, no watermark, no long text.
                Theme must match this tutorial blog:
                Title: %s
                Summary: %s
                Category: %s
                Tags: %s
                Content excerpt: %s
                Project context: %s
                Use abstract product/architecture/code/data visuals related to the topic. Leave enough negative space for blog card cropping.
                """.formatted(title, summary, category, tags, content, analysis);
    }

    static String extractBlogId(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        Matcher jsonMatcher = BLOG_ID_JSON.matcher(text);
        if (jsonMatcher.find()) {
            return jsonMatcher.group(1);
        }
        Matcher textMatcher = BLOG_ID_TEXT.matcher(text);
        if (textMatcher.find()) {
            return textMatcher.group(1);
        }
        return null;
    }

    static String extractZhBlogId(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        Matcher matcher = ZH_BLOG_ID_JSON.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        Matcher textMatcher = ZH_BLOG_ID_TEXT.matcher(text);
        if (textMatcher.find()) {
            return textMatcher.group(1);
        }
        return null;
    }

    private static boolean isForbiddenExternalUrl(String url) {
        String lower = url.toLowerCase();
        return lower.contains("pollinations.ai")
                || lower.contains("placehold")
                || lower.contains("dummyimage")
                || lower.contains("via.placeholder");
    }

    private static String asText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
