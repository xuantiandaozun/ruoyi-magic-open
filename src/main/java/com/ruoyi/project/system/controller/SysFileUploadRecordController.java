package com.ruoyi.project.system.controller;

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
import com.ruoyi.project.system.domain.SysFileUploadRecord;
import com.ruoyi.project.system.service.ISysFileUploadRecordService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 文件上传记录Controller
 * 
 * @author ruoyi
 * @date 2025-07-11 12:01:15
 */
@RestController
@RequestMapping("/system/record")
public class SysFileUploadRecordController extends BaseController {
    @Autowired
    private ISysFileUploadRecordService sysFileUploadRecordService;

    /**
     * 查询文件上传记录列表
     */
    @SaCheckPermission("system:record:list")
    @GetMapping("/list")
    public TableDataInfo list(SysFileUploadRecord sysFileUploadRecord) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = buildFlexQueryWrapper(sysFileUploadRecord);

        // 使用 MyBatisFlex 的分页方法
        Page<SysFileUploadRecord> page = sysFileUploadRecordService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 导出文件上传记录列表
     */
    @SaCheckPermission("system:record:export")
    @Log(title = "文件上传记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysFileUploadRecord sysFileUploadRecord) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        // 这里需要根据实际业务编写查询条件

        List<SysFileUploadRecord> list = sysFileUploadRecordService.list(queryWrapper);
        MagicExcelUtil<SysFileUploadRecord> util = new MagicExcelUtil<>(SysFileUploadRecord.class);
        util.exportExcel(response, list, "文件上传记录数据");
    }

    /**
     * 获取文件上传记录详细信息
     */
    @SaCheckPermission("system:record:query")
    @GetMapping(value = "/{recordId}")
    public AjaxResult getInfo(@PathVariable("recordId") String recordId) {
        return success(sysFileUploadRecordService.getById(recordId));
    }

    /**
     * 新增文件上传记录
     */
    @SaCheckPermission("system:record:add")
    @Log(title = "文件上传记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysFileUploadRecord sysFileUploadRecord) {
        return toAjax(sysFileUploadRecordService.save(sysFileUploadRecord) ? 1 : 0);
    }

    /**
     * 修改文件上传记录
     */
    @SaCheckPermission("system:record:edit")
    @Log(title = "文件上传记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysFileUploadRecord sysFileUploadRecord) {
        return toAjax(sysFileUploadRecordService.updateById(sysFileUploadRecord) ? 1 : 0);
    }

    /**
     * 删除文件上传记录
     */
    @SaCheckPermission("system:record:remove")
    @Log(title = "文件上传记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable String[] recordIds) {
        return toAjax(sysFileUploadRecordService.removeByIds(Arrays.asList(recordIds)) ? recordIds.length : 0);
    }
}
