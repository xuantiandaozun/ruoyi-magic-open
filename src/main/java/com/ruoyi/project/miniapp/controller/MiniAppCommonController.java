package com.ruoyi.project.miniapp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.miniapp.domain.vo.MiniSubscribeTemplateVo;
import com.ruoyi.project.miniapp.service.IMiniAppClientRegionService;
import com.ruoyi.project.miniapp.service.IMiniSubscribeTemplateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "小程序通用能力")
@RestController
@RequestMapping("/miniapp/common")
public class MiniAppCommonController {

    private final IMiniAppClientRegionService clientRegionService;
    private final IMiniSubscribeTemplateService subscribeTemplateService;

    public MiniAppCommonController(IMiniAppClientRegionService clientRegionService,
            IMiniSubscribeTemplateService subscribeTemplateService) {
        this.clientRegionService = clientRegionService;
        this.subscribeTemplateService = subscribeTemplateService;
    }

    @Operation(summary = "获取客户端地域信息")
    @GetMapping("/client-region")
    public AjaxResult clientRegion(HttpServletRequest request) {
        Map<String, Object> region = clientRegionService.resolveClientRegion(request);
        return AjaxResult.success(region);
    }

    @Operation(summary = "获取订阅消息模板配置")
    @GetMapping("/subscribe-templates")
    public AjaxResult subscribeTemplates(@RequestParam String appCode,
            @RequestParam(required = false) String sceneCode) {
        List<MiniSubscribeTemplateVo> templates = subscribeTemplateService.listEnabledVoByAppCodeAndScene(appCode,
                sceneCode);
        return AjaxResult.success(templates);
    }
}
