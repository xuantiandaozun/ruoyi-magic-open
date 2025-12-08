package com.ruoyi.project.ai.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.ai.service.IWorkflowExecutionService;

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

    @Autowired
    private IWorkflowExecutionService workflowExecutionService;

    // 工作流ID常量
    private static final Long WORKFLOW_BLOG_COVER_GENERATION = 100L; // 博客封面生成工作流

    /**
     * AI生成博客封面
     * 
     * @param blogId 博客ID
     * @return 执行结果
     */
    @Operation(summary = "AI生成博客封面")
    @PostMapping("/blog/cover/{blogId}")
    public AjaxResult generateBlogCover(@PathVariable("blogId") Long blogId) {
        if (blogId == null || blogId <= 0) {
            return AjaxResult.error("博客ID无效");
        }

        try {
            // 构建输入参数
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("blogId", blogId);

            // 执行工作流
            Map<String, Object> result = workflowExecutionService.executeWorkflow(
                    WORKFLOW_BLOG_COVER_GENERATION,
                    inputData);

            return AjaxResult.success("AI封面生成任务已提交", result);
        } catch (Exception e) {
            return AjaxResult.error("AI封面生成失败: " + e.getMessage());
        }
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
            return AjaxResult.error("博客ID列表不能为空");
        }

        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMsg = new StringBuilder();

        for (Long blogId : blogIds) {
            try {
                Map<String, Object> inputData = new HashMap<>();
                inputData.put("blogId", blogId);

                workflowExecutionService.executeWorkflow(
                        WORKFLOW_BLOG_COVER_GENERATION,
                        inputData);
                successCount++;
            } catch (Exception e) {
                failCount++;
                errorMsg.append("博客ID ").append(blogId).append(" 失败: ").append(e.getMessage()).append("; ");
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("total", blogIds.length);

        if (failCount == 0) {
            return AjaxResult.success("批量AI封面生成任务已全部提交", result);
        } else if (successCount == 0) {
            return AjaxResult.error("批量AI封面生成全部失败: " + errorMsg);
        } else {
            result.put("errorDetails", errorMsg.toString());
            return AjaxResult.success("部分任务提交成功", result);
        }
    }
}
