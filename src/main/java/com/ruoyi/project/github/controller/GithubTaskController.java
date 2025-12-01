package com.ruoyi.project.github.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.github.task.GithubReadmeUploadTask;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * GitHub定时任务测试Controller
 * 
 * @author ruoyi-magic
 * @date 2025-12-01
 */
@Tag(name = "GitHub定时任务测试")
@RestController
@RequestMapping("/github/task")
public class GithubTaskController extends BaseController {

    @Autowired
    private GithubReadmeUploadTask readmeUploadTask;

    /**
     * 手动触发README上传任务（用于测试）
     */
    @Operation(summary = "手动触发README上传任务")
    @SaIgnore
    @Log(title = "GitHub任务", businessType = BusinessType.OTHER)
    @PostMapping("/readme/upload")
    public AjaxResult executeReadmeUpload() {
        try {
            // 异步执行任务，避免接口超时
            new Thread(() -> {
                readmeUploadTask.execute();
            }).start();
            
            return success("README上传任务已触发执行，请查看日志");
        } catch (Exception e) {
            return error("任务执行失败: " + e.getMessage());
        }
    }

    /**
     * 同步执行README上传任务（用于测试，可能会超时）
     */
    @Operation(summary = "同步执行README上传任务")
    @SaCheckPermission("github:task:execute")
    @Log(title = "GitHub任务", businessType = BusinessType.OTHER)
    @PostMapping("/readme/upload/sync")
    public AjaxResult executeReadmeUploadSync() {
        try {
            readmeUploadTask.execute();
            return success("README上传任务执行完成，请查看日志");
        } catch (Exception e) {
            return error("任务执行失败: " + e.getMessage());
        }
    }
}
