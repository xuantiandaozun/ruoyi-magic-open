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
import com.ruoyi.project.feishu.service.IFeishuBitableSyncService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * 飞书多维表格数据同步Controller
 * 
 * @author ruoyi
 * @date 2026-02-05
 */
@Slf4j
@Tag(name = "飞书多维表格数据同步", description = "飞书多维表格与本地数据同步相关接口")
@RestController
@RequestMapping("/feishu/bitable-sync")
public class FeishuBitableSyncController extends BaseController {
    
    @Autowired
    private IFeishuBitableSyncService feishuBitableSyncService;
    
    // 固定配置参数
    private static final String APP_TOKEN = "T1O7blsfNanfqosMWBvcIWgwnzb";
    private static final String TABLE_ID = "tblrCnUgBgzSMpNq";
    private static final String VIEW_ID = "vewEYjlKYX";
    private static final Integer DEFAULT_PAGE_SIZE = 50;
    
    /**
     * 同步飞书多维表格数据到本地数据库
     */
    @Operation(summary = "同步飞书到本地", description = "将飞书多维表格中的数据同步到本地数据库")
    @SaCheckPermission("feishu:bitable-sync:tolocal")
    @Log(title = "飞书数据同步", businessType = BusinessType.OTHER)
    @PostMapping("/to-local")
    public AjaxResult syncBitableDataToLocal(
            @Parameter(description = "多维表格应用token", required = false) @RequestParam(required = false) String appToken,
            @Parameter(description = "数据表ID", required = false) @RequestParam(required = false) String tableId,
            @Parameter(description = "视图ID", required = false) @RequestParam(required = false) String viewId,
            @Parameter(description = "分页大小", required = false) @RequestParam(required = false) Integer pageSize) {
        
        try {
            // 使用固定参数
            appToken = appToken != null ? appToken : APP_TOKEN;
            tableId = tableId != null ? tableId : TABLE_ID;
            viewId = viewId != null ? viewId : VIEW_ID;
            pageSize = pageSize != null ? pageSize : DEFAULT_PAGE_SIZE;
            
            String result = feishuBitableSyncService.syncBitableDataToLocal(
                appToken, tableId, viewId, pageSize);
            
            return success(result);
        } catch (Exception e) {
            log.error("同步飞书数据到本地失败", e);
            return error("同步失败: " + e.getMessage());
        }
    }
    
    /**
     * 同步本地数据库数据到飞书多维表格
     */
    @Operation(summary = "同步本地到飞书", description = "将本地数据库中的数据同步到飞书多维表格")
    @SaCheckPermission("feishu:bitable-sync:tobitable")
    @Log(title = "本地数据同步", businessType = BusinessType.OTHER)
    @PostMapping("/to-bitable")
    public AjaxResult syncLocalDataToBitable(
            @Parameter(description = "多维表格应用token", required = false) @RequestParam(required = false) String appToken,
            @Parameter(description = "数据表ID", required = false) @RequestParam(required = false) String tableId) {
        
        try {
            // 使用固定参数
            appToken = appToken != null ? appToken : APP_TOKEN;
            tableId = tableId != null ? tableId : TABLE_ID;
            
            String result = feishuBitableSyncService.syncLocalDataToBitable(appToken, tableId);
            
            return success(result);
        } catch (Exception e) {
            log.error("同步本地数据到飞书失败", e);
            return error("同步失败: " + e.getMessage());
        }
    }
    
    /**
     * 双向同步数据
     */
    @Operation(summary = "双向数据同步", description = "双向同步飞书多维表格与本地数据库数据")
    @SaCheckPermission("feishu:bitable-sync:bidirectional")
    @Log(title = "双向数据同步", businessType = BusinessType.OTHER)
    @PostMapping("/bidirectional")
    public AjaxResult syncBidirectional(
            @Parameter(description = "多维表格应用token", required = false) @RequestParam(required = false) String appToken,
            @Parameter(description = "数据表ID", required = false) @RequestParam(required = false) String tableId,
            @Parameter(description = "视图ID", required = false) @RequestParam(required = false) String viewId,
            @Parameter(description = "分页大小", required = false) @RequestParam(required = false) Integer pageSize) {
        
        try {
            // 使用固定参数
            appToken = appToken != null ? appToken : APP_TOKEN;
            tableId = tableId != null ? tableId : TABLE_ID;
            viewId = viewId != null ? viewId : VIEW_ID;
            pageSize = pageSize != null ? pageSize : DEFAULT_PAGE_SIZE;
            
            String result = feishuBitableSyncService.syncBidirectional(
                appToken, tableId, viewId, pageSize);
            
            return success(result);
        } catch (Exception e) {
            log.error("双向数据同步失败", e);
            return error("同步失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取同步配置信息
     */
    @Operation(summary = "获取同步配置", description = "获取当前的同步配置参数")
    @SaCheckPermission("feishu:bitable-sync:config")
    @GetMapping("/config")
    public AjaxResult getConfig() {
        try {
            return success(new SyncConfigDto()
                .setAppToken(APP_TOKEN)
                .setTableId(TABLE_ID)
                .setViewId(VIEW_ID)
                .setPageSize(DEFAULT_PAGE_SIZE));
        } catch (Exception e) {
            log.error("获取同步配置失败", e);
            return error("获取配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 同步配置DTO
     */
    public static class SyncConfigDto {
        private String appToken;
        private String tableId;
        private String viewId;
        private Integer pageSize;
        
        public String getAppToken() {
            return appToken;
        }
        
        public SyncConfigDto setAppToken(String appToken) {
            this.appToken = appToken;
            return this;
        }
        
        public String getTableId() {
            return tableId;
        }
        
        public SyncConfigDto setTableId(String tableId) {
            this.tableId = tableId;
            return this;
        }
        
        public String getViewId() {
            return viewId;
        }
        
        public SyncConfigDto setViewId(String viewId) {
            this.viewId = viewId;
            return this;
        }
        
        public Integer getPageSize() {
            return pageSize;
        }
        
        public SyncConfigDto setPageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }
    }
}