package com.ruoyi.project.miniapp.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.miniapp.domain.TranslateTask;
import com.ruoyi.project.miniapp.service.ITranslateTaskService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 翻译任务管理 Controller
 */
@Tag(name = "翻译任务管理")
@RestController
@RequestMapping("/manage/translateTask")
public class TranslateTaskManageController extends BaseController {

    @Autowired
    private ITranslateTaskService translateTaskService;

    @Operation(summary = "查询翻译任务列表")
    @SaCheckPermission("manage:translateTask:list")
    @GetMapping("/list")
    public TableDataInfo list(TranslateTask task) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        QueryWrapper queryWrapper = QueryWrapper.create()
                .from("translate_task")
                .and("del_flag = '0'")
                .orderBy("create_time desc");
        if (task.getMiniAppId() != null) {
            queryWrapper.and("mini_app_id = ?", task.getMiniAppId());
        }
        if (task.getMiniUserId() != null) {
            queryWrapper.and("mini_user_id = ?", task.getMiniUserId());
        }
        if (StrUtil.isNotBlank(task.getStatus())) {
            queryWrapper.and("status = ?", task.getStatus());
        }
        if (StrUtil.isNotBlank(task.getSourceLanguage())) {
            queryWrapper.and("source_language = ?", task.getSourceLanguage());
        }
        if (StrUtil.isNotBlank(task.getTargetLanguage())) {
            queryWrapper.and("target_language = ?", task.getTargetLanguage());
        }

        Page<TranslateTask> page = translateTaskService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    @Operation(summary = "获取翻译任务详情")
    @SaCheckPermission("manage:translateTask:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(translateTaskService.getById(id));
    }
}
