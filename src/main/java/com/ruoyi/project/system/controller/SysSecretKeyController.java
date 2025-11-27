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
import com.ruoyi.project.system.domain.SysSecretKey;
import com.ruoyi.project.system.service.ISysSecretKeyService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 密钥管理Controller
 * 
 * @author ruoyi
 * @date 2025-07-11 17:46:46
 */
@RestController
@RequestMapping("/system/secretKey")
public class SysSecretKeyController extends BaseController
{
    @Autowired
    private ISysSecretKeyService sysSecretKeyService;

    /**
     * 查询密钥管理列表
     */
    @SaCheckPermission("system:secretKey:list")
    @GetMapping("/list")
    public TableDataInfo list(SysSecretKey sysSecretKey)
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        
        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = buildFlexQueryWrapper(sysSecretKey);
        
        // 使用 MyBatisFlex 的分页方法
        Page<SysSecretKey> page = sysSecretKeyService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 导出密钥管理列表
     */
    @SaCheckPermission("system:secretKey:export")
    @Log(title = "密钥管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysSecretKey sysSecretKey)
    {
        QueryWrapper queryWrapper = QueryWrapper.create();
        // 这里需要根据实际业务编写查询条件
        
        List<SysSecretKey> list = sysSecretKeyService.list(queryWrapper);
        MagicExcelUtil<SysSecretKey> util = new MagicExcelUtil<>(SysSecretKey.class);
        util.exportExcel(response, list, "密钥管理数据");
    }

    /**
     * 获取密钥管理详细信息
     */
    @SaCheckPermission("system:secretKey:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(sysSecretKeyService.getById(id));
    }

    /**
     * 新增密钥管理
     */
    @SaCheckPermission("system:secretKey:add")
    @Log(title = "密钥管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysSecretKey sysSecretKey)
    {
        return toAjax(sysSecretKeyService.save(sysSecretKey) ? 1 : 0);
    }

    /**
     * 修改密钥管理
     */
    @SaCheckPermission("system:secretKey:edit")
    @Log(title = "密钥管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysSecretKey sysSecretKey)
    {
        return toAjax(sysSecretKeyService.updateById(sysSecretKey) ? 1 : 0);
    }

    /**
     * 删除密钥管理
     */
    @SaCheckPermission("system:secretKey:remove")
    @Log(title = "密钥管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(sysSecretKeyService.removeByIds(Arrays.asList(ids)) ? ids.length : 0);
    }

    /**
     * 获取飞书密钥下拉选项列表
     */
    @GetMapping("/feishu/options")
    public AjaxResult getFeishuKeyOptions()
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .eq("provider_name", "飞书")
            .eq("status", "0")
            .select("id", "key_name", "key_type");
        List<SysSecretKey> list = sysSecretKeyService.list(queryWrapper);
        return success(list);
    }
}
