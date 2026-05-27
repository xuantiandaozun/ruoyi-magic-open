package com.ruoyi.project.miniapp.controller;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.miniapp.domain.dto.CreateTranslateTaskRequest;
import com.ruoyi.project.miniapp.domain.vo.MiniAppLoginUser;
import com.ruoyi.project.miniapp.service.ITranslateTaskService;
import com.ruoyi.project.miniapp.util.MiniAppSecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "小程序翻译任务")
@RestController
@RequestMapping("/miniapp/translate/tasks")
public class MiniAppTranslateTaskController {

    private final ITranslateTaskService translateTaskService;

    public MiniAppTranslateTaskController(ITranslateTaskService translateTaskService) {
        this.translateTaskService = translateTaskService;
    }

    @Operation(summary = "创建翻译任务")
    @PostMapping
    public AjaxResult create(@Validated @RequestBody CreateTranslateTaskRequest request) {
        MiniAppLoginUser loginUser = MiniAppSecurityUtils.getLoginUser();
        return AjaxResult.success(translateTaskService.createTask(request, loginUser));
    }

    @Operation(summary = "查询翻译任务详情")
    @GetMapping("/{taskId}")
    public AjaxResult detail(@PathVariable Long taskId) {
        MiniAppLoginUser loginUser = MiniAppSecurityUtils.getLoginUser();
        return AjaxResult.success(translateTaskService.getOwnedTask(taskId, loginUser));
    }

    @Operation(summary = "查询当前用户翻译任务列表")
    @GetMapping
    public AjaxResult list() {
        MiniAppLoginUser loginUser = MiniAppSecurityUtils.getLoginUser();
        return AjaxResult.success(translateTaskService.listByOwner(loginUser));
    }

    @Operation(summary = "下载翻译结果")
    @GetMapping("/{taskId}/download")
    public void download(@PathVariable Long taskId, HttpServletResponse response) throws Exception {
        MiniAppLoginUser loginUser = MiniAppSecurityUtils.getLoginUser();
        translateTaskService.downloadResult(taskId, loginUser, response);
    }
}
