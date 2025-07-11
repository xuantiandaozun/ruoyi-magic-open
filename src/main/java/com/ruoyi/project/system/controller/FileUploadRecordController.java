package com.ruoyi.project.system.controller;

import java.util.List;
import java.util.Arrays;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.project.system.domain.FileUploadRecord;
import com.ruoyi.project.system.service.IFileUploadRecordService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.MagicExcelUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.paginate.Page;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableSupport;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.framework.web.page.TableDataInfo;

/**
 * 文件上传记录Controller
 * 
 * @author ruoyi
 * @date 2025-07-11 12:01:15
 */
@RestController
@RequestMapping("/system/record")
public class FileUploadRecordController extends BaseController
{
    @Autowired
    private IFileUploadRecordService fileUploadRecordService;

    /**
     * 查询文件上传记录列表
     */
    @SaCheckPermission("system:record:list")
    @GetMapping("/list")
    public TableDataInfo list(FileUploadRecord fileUploadRecord)
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        
        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = buildFlexQueryWrapper(fileUploadRecord);
        
        // 使用 MyBatisFlex 的分页方法
        Page<FileUploadRecord> page = fileUploadRecordService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 导出文件上传记录列表
     */
    @SaCheckPermission("system:record:export")
    @Log(title = "文件上传记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, FileUploadRecord fileUploadRecord)
    {
        QueryWrapper queryWrapper = QueryWrapper.create();
        // 这里需要根据实际业务编写查询条件
        
        List<FileUploadRecord> list = fileUploadRecordService.list(queryWrapper);
        MagicExcelUtil<FileUploadRecord> util = new MagicExcelUtil<>(FileUploadRecord.class);
        util.exportExcel(response, list, "文件上传记录数据");
    }

    /**
     * 获取文件上传记录详细信息
     */
    @SaCheckPermission("system:record:query")
    @GetMapping(value = "/{recordId}")
    public AjaxResult getInfo(@PathVariable("recordId") String recordId)
    {
        return success(fileUploadRecordService.getById(recordId));
    }

    /**
     * 新增文件上传记录
     */
    @SaCheckPermission("system:record:add")
    @Log(title = "文件上传记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody FileUploadRecord fileUploadRecord)
    {
        return toAjax(fileUploadRecordService.save(fileUploadRecord) ? 1 : 0);
    }

    /**
     * 修改文件上传记录
     */
    @SaCheckPermission("system:record:edit")
    @Log(title = "文件上传记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody FileUploadRecord fileUploadRecord)
    {
        return toAjax(fileUploadRecordService.updateById(fileUploadRecord) ? 1 : 0);
    }

    /**
     * 删除文件上传记录
     */
    @SaCheckPermission("system:record:remove")
    @Log(title = "文件上传记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable String[] recordIds)
    {
        return toAjax(fileUploadRecordService.removeByIds(Arrays.asList(recordIds)) ? recordIds.length : 0);
    }
}
