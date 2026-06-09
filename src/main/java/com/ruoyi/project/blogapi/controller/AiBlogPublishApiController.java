package com.ruoyi.project.blogapi.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.aspectj.lang.annotation.Anonymous;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.article.service.AiBlogPublishService;
import com.ruoyi.project.blogapi.domain.dto.AiBlogPublishRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * AI 直发博客 API
 */
@Tag(name = "AI 直发博客 API")
@Anonymous
@RestController
@RequestMapping("/api/blog/ai")
public class AiBlogPublishApiController extends BaseController {

    private final AiBlogPublishService aiBlogPublishService;

    public AiBlogPublishApiController(AiBlogPublishService aiBlogPublishService) {
        this.aiBlogPublishService = aiBlogPublishService;
    }

    @Operation(summary = "AI 直发博客")
    @Log(title = "AI直发博客", businessType = BusinessType.INSERT)
    @PostMapping("/publish")
    public AjaxResult publish(@Validated @RequestBody AiBlogPublishRequest request) {
        return success(aiBlogPublishService.publish(request, "1", "ai_skill_publish"));
    }
}
