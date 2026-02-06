package com.ruoyi.project.feishu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.feishu.domain.dto.DomainCertRecordDto;
import com.ruoyi.project.feishu.service.ICompanyFeishuService;
import com.ruoyi.project.feishu.service.IFeishuBitableSyncService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 公司飞书Controller
 * 
 * @author ruoyi
 * @date 2026-02-05
 */
@Tag(name = "公司飞书管理", description = "公司飞书相关接口")
@RestController
@RequestMapping("/system/company-feishu")
public class CompanyFeishuController extends BaseController {
    
    @Autowired
    private ICompanyFeishuService companyFeishuService;
    
    @Autowired
    private IFeishuBitableSyncService feishuBitableSyncService;
    
    /**
     * 获取部门直属用户列表
     */
    @Operation(summary = "获取部门直属用户列表", description = "获取指定部门的直属用户列表")
    @SaCheckPermission("system:company-feishu:list")
    @Log(title = "部门用户", businessType = BusinessType.OTHER)
    @GetMapping("/department/users")
    public AjaxResult getDepartmentUsers(
            @Parameter(description = "分页大小", required = false) @RequestParam(defaultValue = "10") Integer pageSize) {
        
        // 固定参数配置
        String departmentId = "od-1c599c2f1ded9663044bad0561ff5fa1";
        String userIdType = "user_id";
        String departmentIdType = "open_department_id";
        
        try {
            // 获取部门用户列表，使用固定配置
            Object result = companyFeishuService.getDepartmentUsers(departmentId, userIdType, departmentIdType, pageSize);
            
            return success(result);
        } catch (Exception e) {
            return error("获取部门用户列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 同步部门用户到本地数据库
     */
    @Operation(summary = "同步部门用户", description = "同步飞书部门用户到本地数据库，自动新增或更新")
    @SaCheckPermission("system:company-feishu:sync")
    @Log(title = "同步部门用户", businessType = BusinessType.OTHER)
    @PostMapping("/department/users/sync")
    public AjaxResult syncDepartmentUsers(
            @Parameter(description = "分页大小", required = false) @RequestParam(defaultValue = "50") Integer pageSize) {
        
        // 固定参数配置
        String departmentId = "od-1c599c2f1ded9663044bad0561ff5fa1";
        String userIdType = "user_id";
        String departmentIdType = "open_department_id";
        
        try {
            // 同步部门用户到本地数据库
            String result = companyFeishuService.syncDepartmentUsers(departmentId, userIdType, departmentIdType, pageSize);
            
            return success(result);
        } catch (Exception e) {
            return error("同步部门用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询多维表格数据
     */
    @Operation(summary = "查询多维表格数据", description = "查询飞书多维表格中的记录数据，支持按域名等条件过滤")
    @SaCheckPermission("system:company-feishu:bitable")
    @Log(title = "查询多维表格", businessType = BusinessType.OTHER)
    @GetMapping("/bitable/records")
    public AjaxResult searchAppTableRecord(
            @Parameter(description = "多维表格应用token", required = true) @RequestParam String appToken,
            @Parameter(description = "数据表ID", required = true) @RequestParam String tableId,
            @Parameter(description = "视图ID", required = false) @RequestParam(required = false) String viewId,
            @Parameter(description = "分页大小", required = false) @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "域名过滤条件", required = false) @RequestParam(required = false) String domain) {
        
        try {
            // 查询多维表格数据，支持过滤条件
            Object result = companyFeishuService.searchAppTableRecord(
                    appToken, tableId, viewId, pageSize, domain);
            
            return success(result);
        } catch (Exception e) {
            return error("查询多维表格数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 新增多维表格记录
     */
    @Operation(summary = "新增多维表格记录", description = "向飞书多维表格中新增一条记录")
    @SaCheckPermission("system:company-feishu:bitable")
    @Log(title = "新增多维表格记录", businessType = BusinessType.INSERT)
    @PostMapping("/bitable/records")
    public AjaxResult createAppTableRecord(
            @Parameter(description = "多维表格应用token", required = true) @RequestParam String appToken,
            @Parameter(description = "数据表ID", required = true) @RequestParam String tableId,
            @Parameter(description = "域名证书记录数据", required = true) @RequestBody DomainCertRecordDto record) {
        
        try {
            // 新增多维表格记录
            Object result = companyFeishuService.createAppTableRecord(
                    appToken, tableId, record);
            
            return success(result);
        } catch (Exception e) {
            return error("新增多维表格记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新多维表格记录
     */
    @Operation(summary = "更新多维表格记录", description = "更新飞书多维表格中的指定记录")
    @SaCheckPermission("system:company-feishu:bitable")
    @Log(title = "更新多维表格记录", businessType = BusinessType.UPDATE)
    @PostMapping("/bitable/records/{recordId}")
    public AjaxResult updateAppTableRecord(
            @Parameter(description = "多维表格应用token", required = true) @RequestParam String appToken,
            @Parameter(description = "数据表ID", required = true) @RequestParam String tableId,
            @Parameter(description = "记录ID", required = true) @PathVariable String recordId,
            @Parameter(description = "域名证书记录数据", required = true) @RequestBody DomainCertRecordDto record) {
        
        try {
            // 更新多维表格记录
            Object result = companyFeishuService.updateAppTableRecord(
                    appToken, tableId, recordId, record);
            
            return success(result);
        } catch (Exception e) {
            return error("更新多维表格记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动触发飞书多维表格数据同步
     */
    @Operation(summary = "手动同步数据", description = "手动触发飞书多维表格与本地数据库的数据同步")
    @SaCheckPermission("system:company-feishu:sync")
    @Log(title = "手动数据同步", businessType = BusinessType.OTHER)
    @PostMapping("/bitable/sync/manual")
    public AjaxResult manualSyncBitableData(
            @Parameter(description = "同步类型: tolocal(飞书到本地), tobitable(本地到飞书), bidirectional(双向)", required = false) 
            @RequestParam(defaultValue = "bidirectional") String syncType,
            @Parameter(description = "分页大小", required = false) @RequestParam(required = false) Integer pageSize) {
        
        // 固定配置参数
        String appToken = "T1O7blsfNanfqosMWBvcIWgwnzb";
        String tableId = "tblrCnUgBgzSMpNq";
        String viewId = "vewEYjlKYX";
        pageSize = pageSize != null ? pageSize : 50;
        
        try {
            String result;
            switch (syncType.toLowerCase()) {
                case "tolocal":
                    result = feishuBitableSyncService.syncBitableDataToLocal(appToken, tableId, viewId, pageSize);
                    break;
                case "tobitable":
                    result = feishuBitableSyncService.syncLocalDataToBitable(appToken, tableId);
                    break;
                case "bidirectional":
                default:
                    result = feishuBitableSyncService.syncBidirectional(appToken, tableId, viewId, pageSize);
                    break;
            }
            
            return success(result);
        } catch (Exception e) {
            return error("手动同步失败: " + e.getMessage());
        }
    }
}