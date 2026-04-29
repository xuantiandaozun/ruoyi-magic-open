package com.ruoyi.project.ai.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.ai.tool.impl.AliImageGenerationLangChain4jTool;
import com.ruoyi.project.ai.tool.impl.BlogCoverUpdateLangChain4jTool;
import com.ruoyi.project.article.domain.Blog;
import com.ruoyi.project.article.service.IBlogService;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * AI工作流API封装Controller
 * 将工作流封装成固定的API接口，供前端调用
 * 
 * @author ruoyi-magic
 * @date 2025-12-05
 */
@Tag(name = "AI工作流API")
@RestController
@RequestMapping("/ai/workflow/api")
public class AiWorkflowApiController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(AiWorkflowApiController.class);

    @Autowired
    private IBlogService blogService;

    @Autowired
    private AliImageGenerationLangChain4jTool aliImageGenerationTool;

    @Autowired
    private BlogCoverUpdateLangChain4jTool blogCoverUpdateTool;

    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;

    /**
     * AI生成博客封面
     * 
     * @param blogId 博客ID
     * @return 执行结果
     */
    @Operation(summary = "AI生成博客封面")
    @PostMapping("/blog/cover/{blogId}")
    public AjaxResult generateBlogCover(@PathVariable("blogId") Long blogId) {
        if (blogService.getById(blogId) == null) {
            return AjaxResult.error("博客不存在，ID: " + blogId);
        }

        taskExecutor.execute(() -> {
            try {
                generateBlogCoverSync(blogId);
            } catch (Exception e) {
                log.error("异步生成博客封面失败，blogId={}", blogId, e);
            }
        });

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("blogId", blogId);
        data.put("async", true);
        return AjaxResult.success("AI封面生成任务已提交，后台生成完成后会自动更新博客封面", data);
    }

    private Map<String, Object> generateBlogCoverSync(Long blogId) {
        Blog blog = blogService.getById(blogId);
        if (blog == null) {
            throw new IllegalArgumentException("博客不存在，ID: " + blogId);
        }

        Map<String, Object> imageParams = new LinkedHashMap<>();
        imageParams.put("prompt", buildCoverPrompt(blog));
        imageParams.put("model", "qwen-image-plus");
        imageParams.put("size", "1664*928");
        imageParams.put("negativePrompt", "people, portrait, face, watermark, logo, long text, blurry, low quality");
        imageParams.put("promptExtend", true);
        imageParams.put("watermark", false);

        String imageResult = aliImageGenerationTool.execute(imageParams);
        JSONObject imageJson = JSONUtil.parseObj(imageResult);
        if (!imageJson.getBool("success", false)) {
            throw new IllegalStateException(imageJson.getStr("message", "AI封面生成失败"));
        }

        JSONObject imageData = imageJson.getJSONObject("data");
        String coverImageUrl = imageData != null ? imageData.getStr("imageUrl") : null;
        if (StrUtil.isBlank(coverImageUrl)) {
            throw new IllegalStateException("AI封面生成成功，但未返回图片地址");
        }

        Map<String, Object> updateParams = new LinkedHashMap<>();
        updateParams.put("coverImageUrl", coverImageUrl);
        updateParams.put("zhBlogId", String.valueOf(blogId));
        String updateResult = blogCoverUpdateTool.execute(updateParams);
        JSONObject updateJson = JSONUtil.parseObj(updateResult);
        if (!updateJson.getBool("success", false)) {
            throw new IllegalStateException(updateJson.getStr("message", "AI封面更新失败"));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("blogId", blogId);
        data.put("coverImageUrl", coverImageUrl);
        data.put("imageResult", imageJson.get("data"));
        data.put("updateResult", updateJson.get("data"));
        return data;
    }

    /**
     * 批量AI生成博客封面
     * 
     * @param blogIds 博客ID数组
     * @return 执行结果
     */
    @Operation(summary = "批量AI生成博客封面")
    @PostMapping("/blog/cover/batch")
    public AjaxResult batchGenerateBlogCover(@RequestBody Long[] blogIds) {
        if (blogIds == null || blogIds.length == 0) {
            return AjaxResult.error("请选择要生成封面的博客");
        }

        ArrayList<Object> results = new ArrayList<>();
        int successCount = 0;
        for (Long blogId : blogIds) {
            AjaxResult result = generateBlogCover(blogId);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("blogId", blogId);
            item.put("code", result.get("code"));
            item.put("msg", result.get("msg"));
            item.put("data", result.get("data"));
            results.add(item);
            if (Integer.valueOf(200).equals(result.get("code"))) {
                successCount++;
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", blogIds.length);
        data.put("successCount", successCount);
        data.put("failCount", blogIds.length - successCount);
        data.put("results", results);
        return AjaxResult.success("批量AI封面生成完成，成功 " + successCount + " / " + blogIds.length, data);
    }

    private String buildCoverPrompt(Blog blog) {
        String title = StrUtil.blankToDefault(blog.getTitle(), "技术博客");
        String summary = StrUtil.blankToDefault(blog.getSummary(), "");
        String category = StrUtil.blankToDefault(blog.getCategory(), "technology");
        String tags = StrUtil.blankToDefault(blog.getTags(), "");
        String content = StrUtil.blankToDefault(blog.getContent(), "")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ");
        if (content.length() > 1200) {
            content = content.substring(0, 1200);
        }

        return """
                Create a modern 16:9 technical blog cover image.
                Style: clean, premium, high contrast, editorial technology illustration, no people, no faces, no watermark, no long text.
                Theme must match this blog:
                Title: %s
                Summary: %s
                Category: %s
                Tags: %s
                Content excerpt: %s
                Use abstract product/architecture/code/data visuals related to the topic. Leave enough negative space for blog card cropping.
                """.formatted(title, summary, category, tags, content);
    }
}
