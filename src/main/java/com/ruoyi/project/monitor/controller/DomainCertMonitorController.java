package com.ruoyi.project.monitor.controller;

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
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.monitor.domain.DomainCertMonitor;
import com.ruoyi.project.monitor.service.IDomainCertMonitorService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 域名证书监控Controller
 * 
 * @author ruoyi
 * @date 2025-12-04
 */
@Tag(name = "域名证书监控", description = "域名HTTPS证书监控相关接口")
@RestController
@RequestMapping("/monitor/cert")
public class DomainCertMonitorController extends BaseController {

    @Autowired
    private IDomainCertMonitorService domainCertMonitorService;

    /**
     * 查询域名证书监控列表
     */
    @Operation(summary = "查询域名证书监控列表")
    @SaCheckPermission("monitor:cert:list")
    @GetMapping("/list")
    public TableDataInfo list(DomainCertMonitor domainCertMonitor) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        
        // 构建查询条件
        var queryWrapper = buildFlexQueryWrapper(domainCertMonitor);
        // 添加逻辑删除过滤条件
        queryWrapper.eq("del_flag", "0");
        
        Page<DomainCertMonitor> page = domainCertMonitorService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 获取域名证书监控详细信息
     */
    @Operation(summary = "获取域名证书监控详细信息")
    @SaCheckPermission("monitor:cert:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@Parameter(description = "域名证书监控ID") @PathVariable Long id) {
        return success(domainCertMonitorService.getById(id));
    }

    /**
     * 新增域名证书监控
     */
    @Operation(summary = "新增域名证书监控")
    @SaCheckPermission("monitor:cert:add")
    @Log(title = "域名证书监控", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DomainCertMonitor domainCertMonitor) {
        // 校验域名
        if (StrUtil.isBlank(domainCertMonitor.getDomain())) {
            return error("域名不能为空");
        }
        
        // 默认端口443
        if (domainCertMonitor.getPort() == null) {
            domainCertMonitor.setPort(443);
        }
        
        // 默认通知天数3天
        if (domainCertMonitor.getNotifyDays() == null) {
            domainCertMonitor.setNotifyDays(3);
        }
        
        // 默认开启通知
        if (StrUtil.isBlank(domainCertMonitor.getNotifyEnabled())) {
            domainCertMonitor.setNotifyEnabled("1");
        }
        
        // 检查是否已存在
        DomainCertMonitor existing = domainCertMonitorService.selectByDomainAndPort(
                domainCertMonitor.getDomain(), domainCertMonitor.getPort());
        if (existing != null) {
            return error("该域名和端口已存在监控记录");
        }
        
        domainCertMonitor.setCreateBy(SecurityUtils.getUsername());
        domainCertMonitor.setDelFlag("0");
        domainCertMonitor.setStatus("0");
        
        boolean result = domainCertMonitorService.save(domainCertMonitor);
        
        if (result) {
            // 新增后立即检测一次证书
            domainCertMonitorService.checkCert(domainCertMonitor.getId());
        }
        
        return toAjax(result);
    }

    /**
     * 修改域名证书监控
     */
    @Operation(summary = "修改域名证书监控")
    @SaCheckPermission("monitor:cert:edit")
    @Log(title = "域名证书监控", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DomainCertMonitor domainCertMonitor) {
        if (domainCertMonitor.getId() == null) {
            return error("ID不能为空");
        }
        
        domainCertMonitor.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(domainCertMonitorService.updateById(domainCertMonitor));
    }

    /**
     * 删除域名证书监控
     */
    @Operation(summary = "删除域名证书监控")
    @SaCheckPermission("monitor:cert:remove")
    @Log(title = "域名证书监控", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "域名证书监控ID数组") @PathVariable Long[] ids) {
        // 逻辑删除
        for (Long id : ids) {
            DomainCertMonitor monitor = new DomainCertMonitor();
            monitor.setId(id);
            monitor.setDelFlag("2");
            domainCertMonitorService.updateById(monitor);
        }
        return success();
    }

    /**
     * 检测单个域名证书
     */
    @Operation(summary = "检测单个域名证书")
    @SaCheckPermission("monitor:cert:check")
    @Log(title = "域名证书检测", businessType = BusinessType.OTHER)
    @PostMapping("/check/{id}")
    public AjaxResult checkCert(@Parameter(description = "域名证书监控ID") @PathVariable Long id) {
        boolean result = domainCertMonitorService.checkCert(id);
        return result ? success("检测成功") : error("检测失败，请查看日志获取详细信息");
    }

    /**
     * 检测所有域名证书
     */
    @Operation(summary = "检测所有域名证书")
    @SaCheckPermission("monitor:cert:check")
    @Log(title = "域名证书批量检测", businessType = BusinessType.OTHER)
    @PostMapping("/checkAll")
    public AjaxResult checkAllCerts() {
        int count = domainCertMonitorService.checkAllCerts();
        return success("检测完成，成功检测 " + count + " 个域名");
    }

    /**
     * 发送指定域名的过期通知
     */
    @Operation(summary = "发送指定域名的过期通知")
    @SaCheckPermission("monitor:cert:notify")
    @Log(title = "域名证书过期通知", businessType = BusinessType.OTHER)
    @PostMapping("/notify/{id}")
    public AjaxResult sendNotification(@Parameter(description = "域名证书监控ID") @PathVariable Long id) {
        boolean result = domainCertMonitorService.sendNotificationById(id);
        return result ? success("通知发送成功") : error("通知发送失败，请检查域名信息或飞书配置");
    }
}
