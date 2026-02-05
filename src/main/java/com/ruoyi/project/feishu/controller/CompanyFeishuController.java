package com.ruoyi.project.feishu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.feishu.service.ICompanyFeishuService;

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
}