package com.ruoyi.project.ai.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.common.utils.poi.MagicExcelUtil;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.ai.domain.AiBlogProductionRecord;
import com.ruoyi.project.ai.service.IAiBlogProductionRecordService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

/**
 * AI博客生产记录Controller
 * 
 * @author ruoyi
 */
@Tag(name = "AI博客生产记录")
@RestController
@RequestMapping("/ai/blogProductionRecord")
public class AiBlogProductionRecordController extends BaseController {
    @Autowired
    private IAiBlogProductionRecordService aiBlogProductionRecordService;

    /**
     * 查询AI博客生产记录列表
     */
    @Operation(summary = "查询AI博客生产记录列表")
    @SaCheckPermission("ai:blogProductionRecord:list")
    @GetMapping("/list")
    public TableDataInfo list(AiBlogProductionRecord aiBlogProductionRecord) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        QueryWrapper qw = buildFlexQueryWrapper(aiBlogProductionRecord);
        Page<AiBlogProductionRecord> page = aiBlogProductionRecordService.page(new Page<>(pageNum, pageSize), qw);
        return getDataTable(page);
    }

    /**
     * 导出AI博客生产记录列表
     */
    @Operation(summary = "导出AI博客生产记录列表")
    @SaCheckPermission("ai:blogProductionRecord:export")
    @Log(title = "AI博客生产记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiBlogProductionRecord aiBlogProductionRecord) {
        QueryWrapper qw = buildFlexQueryWrapper(aiBlogProductionRecord);
        List<AiBlogProductionRecord> list = aiBlogProductionRecordService.list(qw);
        MagicExcelUtil<AiBlogProductionRecord> util = new MagicExcelUtil<>(AiBlogProductionRecord.class);
        util.exportExcel(response, list, "AI博客生产记录");
    }

    /**
     * 获取AI博客生产记录详细信息
     */
    @Operation(summary = "获取AI博客生产记录详细信息")
    @SaCheckPermission("ai:blogProductionRecord:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(aiBlogProductionRecordService.getById(id));
    }

    /**
     * 新增AI博客生产记录
     */
    @Operation(summary = "新增AI博客生产记录")
    @SaCheckPermission("ai:blogProductionRecord:add")
    @Log(title = "AI博客生产记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiBlogProductionRecord aiBlogProductionRecord) {
        return toAjax(aiBlogProductionRecordService.save(aiBlogProductionRecord));
    }

    /**
     * 修改AI博客生产记录
     */
    @Operation(summary = "修改AI博客生产记录")
    @SaCheckPermission("ai:blogProductionRecord:edit")
    @Log(title = "AI博客生产记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiBlogProductionRecord aiBlogProductionRecord) {
        return toAjax(aiBlogProductionRecordService.updateById(aiBlogProductionRecord));
    }

    /**
     * 删除AI博客生产记录
     */
    @Operation(summary = "删除AI博客生产记录")
    @SaCheckPermission("ai:blogProductionRecord:remove")
    @Log(title = "AI博客生产记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(aiBlogProductionRecordService.removeByIds(Arrays.asList(ids)));
    }

    /**
     * 根据仓库URL查询生产记录
     */
    @Operation(summary = "根据仓库URL查询生产记录")
    @SaCheckPermission("ai:blogProductionRecord:list")
    @GetMapping("/listByRepoUrl/{repoUrl}")
    public AjaxResult listByRepoUrl(@PathVariable("repoUrl") String repoUrl) {
        return success(aiBlogProductionRecordService.listByRepoUrl(repoUrl));
    }

    /**
     * 根据生产类型查询记录
     */
    @Operation(summary = "根据生产类型查询记录")
    @SaCheckPermission("ai:blogProductionRecord:list")
    @GetMapping("/listByProductionType/{productionType}")
    public AjaxResult listByProductionType(@PathVariable("productionType") String productionType) {
        return success(aiBlogProductionRecordService.listByProductionType(productionType));
    }

    /**
     * 查询失败的生产记录
     */
    @Operation(summary = "查询失败的生产记录")
    @SaCheckPermission("ai:blogProductionRecord:list")
    @GetMapping("/listFailedRecords")
    public AjaxResult listFailedRecords() {
        return success(aiBlogProductionRecordService.listFailedRecords());
    }

    /**
     * 查询进行中的生产记录
     */
    @Operation(summary = "查询进行中的生产记录")
    @SaCheckPermission("ai:blogProductionRecord:list")
    @GetMapping("/listRunningRecords")
    public AjaxResult listRunningRecords() {
        return success(aiBlogProductionRecordService.listRunningRecords());
    }
}