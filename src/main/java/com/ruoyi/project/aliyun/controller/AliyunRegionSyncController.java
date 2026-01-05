package com.ruoyi.project.aliyun.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.aliyun.service.IAliyunRegionSyncService;

import lombok.extern.slf4j.Slf4j;

/**
 * 阿里云地域同步控制器
 * 
 * @author ruoyi
 */
@Slf4j
@RestController
@RequestMapping("/system/aliyun/region")
public class AliyunRegionSyncController extends BaseController {

    @Autowired
    private IAliyunRegionSyncService aliyunRegionSyncService;

    /**
     * 同步阿里云地域到数据字典
     */
    @PostMapping("/sync")
    public AjaxResult sync() {
        try {
            boolean result = aliyunRegionSyncService.syncRegionsToDict();
            if (result) {
                return AjaxResult.success("同步阿里云地域成功");
            } else {
                return AjaxResult.error("同步阿里云地域失败");
            }
        } catch (Exception e) {
            log.error("同步阿里云地域失败", e);
            return AjaxResult.error("同步阿里云地域失败: " + e.getMessage());
        }
    }
}
