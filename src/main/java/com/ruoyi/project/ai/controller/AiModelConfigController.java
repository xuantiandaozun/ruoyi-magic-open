package com.ruoyi.project.ai.controller;

import java.util.Arrays;
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
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.ai.domain.AiModelConfig;
import com.ruoyi.project.ai.service.IAiModelConfigService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 大模型配置 控制器
 */
@Tag(name = "大模型配置")
@RestController
@RequestMapping("/ai/modelConfig")
public class AiModelConfigController extends BaseController {

    @Autowired
    private IAiModelConfigService modelConfigService;

    /**
     * 分页查询配置列表
     */
    @Operation(summary = "查询配置列表")
    @SaCheckPermission("ai:modelConfig:list")
    @GetMapping("/list")
    public TableDataInfo list(AiModelConfig query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        QueryWrapper qw = buildFlexQueryWrapper(query);
        Page<AiModelConfig> page = modelConfigService.page(new Page<>(pageNum, pageSize), qw);
        return getDataTable(page);
    }

    /**
     * 获取配置详情
     */
    @Operation(summary = "获取配置详情")
    @SaCheckPermission("ai:modelConfig:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        AiModelConfig cfg = modelConfigService.getById(id);
        return success(cfg);
    }

    /**
     * 新增配置
     */
    @Operation(summary = "新增配置")
    @SaCheckPermission("ai:modelConfig:add")
    @Log(title = "大模型配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AiModelConfig config) {
        return toAjax(modelConfigService.save(config));
    }

    /**
     * 修改配置
     */
    @Operation(summary = "修改配置")
    @SaCheckPermission("ai:modelConfig:edit")
    @Log(title = "大模型配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AiModelConfig config) {
        return toAjax(modelConfigService.updateById(config));
    }

    /**
     * 删除配置
     */
    @Operation(summary = "删除配置")
    @SaCheckPermission("ai:modelConfig:remove")
    @Log(title = "大模型配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(modelConfigService.removeByIds(Arrays.asList(ids)));
    }

    /**
     * 设置默认配置（同厂商+能力唯一）
     */
    @Operation(summary = "设置默认配置")
    @SaCheckPermission("ai:modelConfig:config")
    @PutMapping("/setDefault/{id}")
    public AjaxResult setDefault(@PathVariable Long id) {
        boolean ok = modelConfigService.setDefault(id);
        return toAjax(ok);
    }

    /**
     * 获取可用的聊天模型列表
     */
    @Operation(summary = "获取可用的聊天模型列表")
    @SaCheckPermission("ai:chat:query")
    @GetMapping("/chat/available")
    public AjaxResult getAvailableChatModels() {
        try {
            // 获取所有启用的聊天模型配置
            List<AiModelConfig> chatModels = modelConfigService.listEnabledByCapability("chat");
            return success(chatModels);
        } catch (Exception e) {
            return error("获取可用聊天模型失败: " + e.getMessage());
        }
    }
}