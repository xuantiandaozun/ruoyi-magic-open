package com.ruoyi.project.miniapp.controller.admin;

import java.util.Arrays;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.miniapp.domain.MiniFeedback;
import com.ruoyi.project.miniapp.service.IMiniFeedbackService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 用户反馈管理 Controller
 */
@Tag(name = "用户反馈管理")
@RestController
@RequestMapping("/manage/minifeedback")
public class MiniFeedbackManageController extends BaseController {

    @Autowired
    private IMiniFeedbackService miniFeedbackService;

    @Operation(summary = "查询反馈列表")
    @SaCheckPermission("manage:minifeedback:list")
    @GetMapping("/list")
    public TableDataInfo list(MiniFeedback feedback) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        QueryWrapper queryWrapper = QueryWrapper.create()
                .from("mini_feedback")
                .orderBy("create_time desc");
        if (StrUtil.isNotBlank(feedback.getAppCode())) {
            queryWrapper.and("app_code = ?", feedback.getAppCode());
        }
        if (StrUtil.isNotBlank(feedback.getFeedbackType())) {
            queryWrapper.and("feedback_type = ?", feedback.getFeedbackType());
        }
        if (StrUtil.isNotBlank(feedback.getStatus())) {
            queryWrapper.and("status = ?", feedback.getStatus());
        }

        Page<MiniFeedback> page = miniFeedbackService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    @Operation(summary = "获取反馈详情")
    @SaCheckPermission("manage:minifeedback:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(miniFeedbackService.getById(id));
    }

    @Operation(summary = "回复反馈")
    @SaCheckPermission("manage:minifeedback:edit")
    @Log(title = "用户反馈管理", businessType = BusinessType.UPDATE)
    @PutMapping("/reply")
    public AjaxResult reply(@RequestBody MiniFeedback feedback) {
        MiniFeedback existing = miniFeedbackService.getById(feedback.getId());
        if (existing == null) {
            return error("反馈不存在");
        }
        existing.setReplyContent(feedback.getReplyContent());
        existing.setStatus("2");
        existing.setReplyTime(new Date());
        return toAjax(miniFeedbackService.updateById(existing) ? 1 : 0);
    }

    @Operation(summary = "删除反馈")
    @SaCheckPermission("manage:minifeedback:remove")
    @Log(title = "用户反馈管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(miniFeedbackService.removeByIds(Arrays.asList(ids)) ? ids.length : 0);
    }
}
