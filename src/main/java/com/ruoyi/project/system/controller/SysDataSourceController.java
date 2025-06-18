package com.ruoyi.project.system.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
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
import com.ruoyi.project.system.domain.SysDataSource;
import com.ruoyi.project.system.service.ISysDataSourceService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 数据源管理 信息操作处理
 * 
 * @author ruoyi-magic
 */
@Tag(name = "数据源管理")
@RestController
@RequestMapping("/system/datasource")
public class SysDataSourceController extends BaseController {
    @Autowired
    private ISysDataSourceService dataSourceService;

    /**
     * 获取数据源列表
     */
    @SaCheckPermission("system:datasource:list")
    @GetMapping("/list")
    public TableDataInfo list(SysDataSource dataSource) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        
        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = buildFlexQueryWrapper(dataSource);
        
        // 使用 MyBatisFlex 的分页方法
        Page<SysDataSource> page = dataSourceService.page(new Page<>(pageNum, pageSize), queryWrapper);
        
        // 移除敏感信息
        page.getRecords().forEach(ds -> ds.setPassword(null));
        
        return getDataTable(page);
    }

    /**
     * 导出数据源
     */
    @SaCheckPermission("system:datasource:export")
    @Log(title = "数据源管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysDataSource dataSource, HttpServletResponse response) {
        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = buildFlexQueryWrapper(dataSource);
        List<SysDataSource> list = dataSourceService.list(queryWrapper);
        
        // 移除敏感信息
        list.forEach(ds -> ds.setPassword(null));
        
        MagicExcelUtil<SysDataSource> util = new MagicExcelUtil<>(SysDataSource.class);
        util.exportExcel(response,list, "数据源数据");
    }

    /**
     * 根据数据源ID获取详细信息
     */
    @SaCheckPermission("system:datasource:query")
    @GetMapping(value = "/{dataSourceId}")
    public AjaxResult getInfo(@PathVariable Long dataSourceId) {
        SysDataSource dataSource = dataSourceService.selectSysDataSourceById(dataSourceId);
        if (dataSource != null) {
            // 移除敏感信息
            dataSource.setPassword(null);
        }
        return success(dataSource);
    }
    
    /**
     * 根据数据源名称获取详细信息
     */
    @SaCheckPermission("system:datasource:query")
    @GetMapping(value = "/name/{name}")
    public AjaxResult getInfoByName(@PathVariable String name) {
        SysDataSource dataSource = dataSourceService.selectSysDataSourceByName(name);
        if (dataSource != null) {
            // 移除敏感信息
            dataSource.setPassword(null);
        }
        return success(dataSource);
    }

    /**
     * 测试数据源连接
     */
    @SaCheckPermission("system:datasource:query")
    @PostMapping("/test")
    public AjaxResult testConnection(@RequestBody SysDataSource dataSource) {
        return toAjax(dataSourceService.testConnection(dataSource));
    }

    /**
     * 新增数据源
     */
    @SaCheckPermission("system:datasource:add")
    @Log(title = "数据源管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysDataSource dataSource) {
        if (dataSourceService.selectSysDataSourceByName(dataSource.getName()) != null) {
            return error("新增数据源'" + dataSource.getName() + "'失败，数据源名称已存在");
        }
        return toAjax(dataSourceService.insertSysDataSource(dataSource));
    }

    /**
     * 修改数据源
     */
    @SaCheckPermission("system:datasource:edit")
    @Log(title = "数据源管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysDataSource dataSource) {
        SysDataSource existingDs = dataSourceService.selectSysDataSourceByName(dataSource.getName());
        if (existingDs != null && !existingDs.getDataSourceId().equals(dataSource.getDataSourceId())) {
            return error("修改数据源'" + dataSource.getName() + "'失败，数据源名称已存在");
        }
        return toAjax(dataSourceService.updateSysDataSource(dataSource));
    }

    /**
     * 删除数据源
     */
    @SaCheckPermission("system:datasource:remove")
    @Log(title = "数据源管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{dataSourceIds}")
    public AjaxResult remove(@PathVariable Long[] dataSourceIds) {
        return toAjax(dataSourceService.deleteSysDataSourceByIds(dataSourceIds));
    }
    
    /**
     * 刷新数据源
     */
    @SaCheckPermission("system:datasource:edit")
    @Log(title = "数据源管理", businessType = BusinessType.UPDATE)
    @PostMapping("/refresh")
    public AjaxResult refresh() {
        return toAjax(dataSourceService.refreshDataSources());
    }
}
