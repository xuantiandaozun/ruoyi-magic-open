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
import com.ruoyi.project.system.domain.SysStorageConfig;
import com.ruoyi.project.system.service.ISysStorageConfigService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 存储配置Controller
 * 
 * @author ruoyi
 * @date 2025-07-11 11:32:00
 */
@RestController
@RequestMapping("/system/storageConfig")
public class SysStorageConfigController extends BaseController {
    @Autowired
    private ISysStorageConfigService sysStorageConfigService;

    /**
     * 查询存储配置列表
     */
    @SaCheckPermission("system:storageConfig:list")
    @GetMapping("/list")
    public TableDataInfo list(SysStorageConfig sysStorageConfig) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = buildFlexQueryWrapper(sysStorageConfig);

        // 使用 MyBatisFlex 的分页方法
        Page<SysStorageConfig> page = sysStorageConfigService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 导出存储配置列表
     */
    @SaCheckPermission("system:storageConfig:export")
    @Log(title = "存储配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysStorageConfig sysStorageConfig) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        // 这里需要根据实际业务编写查询条件

        List<SysStorageConfig> list = sysStorageConfigService.list(queryWrapper);
        MagicExcelUtil<SysStorageConfig> util = new MagicExcelUtil<>(SysStorageConfig.class);
        util.exportExcel(response, list, "存储配置数据");
    }

    /**
     * 获取存储配置详细信息
     */
    @SaCheckPermission("system:storageConfig:query")
    @GetMapping(value = "/{configId}")
    public AjaxResult getInfo(@PathVariable("configId") String configId) {
        return success(sysStorageConfigService.getById(configId));
    }

    /**
     * 新增存储配置
     */
    @SaCheckPermission("system:storageConfig:add")
    @Log(title = "存储配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysStorageConfig sysStorageConfig) {
        return toAjax(sysStorageConfigService.saveSysStorageConfig(sysStorageConfig) ? 1 : 0);
    }

    /**
     * 修改存储配置
     */
    @SaCheckPermission("system:storageConfig:edit")
    @Log(title = "存储配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysStorageConfig sysStorageConfig) {
        return toAjax(sysStorageConfigService.updateSysStorageConfig(sysStorageConfig) ? 1 : 0);
    }

    /**
     * 删除存储配置
     */
    @SaCheckPermission("system:storageConfig:remove")
    @Log(title = "存储配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{configIds}")
    public AjaxResult remove(@PathVariable String[] configIds) {
        return toAjax(sysStorageConfigService.removeByIds(Arrays.asList(configIds)) ? configIds.length : 0);
    }
}
