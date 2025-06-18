package com.ruoyi.project.gen.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.gen.domain.AsyncTaskInfo;
import com.ruoyi.project.gen.service.IAsyncTaskService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
/**
 * 异步任务 操作处理
 *
 * @author ruoyi
 */
@Tag(name = "异步任务")
@RestController
@Slf4j
@RequestMapping("/tool/async")
public class AsyncTaskController extends BaseController {
    
    @Autowired
    private IAsyncTaskService asyncTaskService;
    
    /**
     * 获取任务状态
     */
    @GetMapping("/task/{taskId}")
    public AjaxResult getTaskStatus(@PathVariable("taskId") String taskId) {
        AsyncTaskInfo taskInfo = asyncTaskService.getTask(taskId);
        if (taskInfo == null) {
            return error("任务不存在");
        }
        return success(taskInfo);
    }
}