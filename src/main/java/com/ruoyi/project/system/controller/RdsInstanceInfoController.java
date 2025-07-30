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
import com.ruoyi.project.system.domain.RdsInstanceInfo;
import com.ruoyi.project.system.service.IRdsInstanceInfoService;
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
 * RDS实例管理Controller
 * 
 * @author ruoyi
 * @date 2025-07-11 17:49:40
 */
@RestController
@RequestMapping("/system/rdsInstance")
public class RdsInstanceInfoController extends BaseController
{
    @Autowired
    private IRdsInstanceInfoService rdsInstanceInfoService;

    /**
     * 查询RDS实例管理列表
     */
    @SaCheckPermission("system:rdsInstance:list")
    @GetMapping("/list")
    public TableDataInfo list(RdsInstanceInfo rdsInstanceInfo)
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        
        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = buildFlexQueryWrapper(rdsInstanceInfo);
        
        // 使用 MyBatisFlex 的分页方法
        Page<RdsInstanceInfo> page = rdsInstanceInfoService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 导出RDS实例管理列表
     */
    @SaCheckPermission("system:rdsInstance:export")
    @Log(title = "RDS实例管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, RdsInstanceInfo rdsInstanceInfo)
    {
        QueryWrapper queryWrapper = QueryWrapper.create();
        // 这里需要根据实际业务编写查询条件
        
        List<RdsInstanceInfo> list = rdsInstanceInfoService.list(queryWrapper);
        MagicExcelUtil<RdsInstanceInfo> util = new MagicExcelUtil<>(RdsInstanceInfo.class);
        util.exportExcel(response, list, "RDS实例管理数据");
    }

    /**
     * 获取RDS实例管理详细信息
     */
    @SaCheckPermission("system:rdsInstance:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(rdsInstanceInfoService.getById(id));
    }

    /**
     * 新增RDS实例管理
     */
    @SaCheckPermission("system:rdsInstance:add")
    @Log(title = "RDS实例管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody RdsInstanceInfo rdsInstanceInfo)
    {
        return toAjax(rdsInstanceInfoService.save(rdsInstanceInfo) ? 1 : 0);
    }

    /**
     * 修改RDS实例管理
     */
    @SaCheckPermission("system:rdsInstance:edit")
    @Log(title = "RDS实例管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody RdsInstanceInfo rdsInstanceInfo)
    {
        return toAjax(rdsInstanceInfoService.updateById(rdsInstanceInfo) ? 1 : 0);
    }

    /**
     * 删除RDS实例管理
     */
    @SaCheckPermission("system:rdsInstance:remove")
    @Log(title = "RDS实例管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(rdsInstanceInfoService.removeByIds(Arrays.asList(ids)) ? ids.length : 0);
    }

    /**
     * 同步阿里云RDS实例数据
     */
    @SaCheckPermission("system:rdsInstance:sync")
    @Log(title = "同步阿里云RDS实例", businessType = BusinessType.OTHER)
    @PostMapping("/syncAliyun")
    public AjaxResult syncAliyunRdsInstances()
    {
        return rdsInstanceInfoService.syncAliyunRdsInstances();
    }
}
