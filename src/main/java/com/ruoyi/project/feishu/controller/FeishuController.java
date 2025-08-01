package com.ruoyi.project.feishu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.feishu.domain.dto.FeishuMessageDto;
import com.ruoyi.project.feishu.service.IFeishuService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 飞书消息Controller
 * 
 * @author ruoyi
 * @date 2025-01-30
 */
@Tag(name = "飞书消息管理", description = "飞书消息发送相关接口")
@RestController
@RequestMapping("/system/feishu")
public class FeishuController extends BaseController {
    
    @Autowired
    private IFeishuService feishuService;
    
    /**
     * 发送文本消息
     */
    @Operation(summary = "发送文本消息", description = "向指定用户发送文本消息")
    @SaCheckPermission("system:feishu:send")
    @Log(title = "飞书消息", businessType = BusinessType.INSERT)
    @PostMapping("/sendText")
    public AjaxResult sendTextMessage(
            @Parameter(description = "接收者ID", required = true) @RequestParam String receiveId,
            @Parameter(description = "接收者ID类型", required = false) @RequestParam(defaultValue = "user_id") String receiveIdType,
            @Parameter(description = "消息内容", required = true) @RequestParam String content,
            @Parameter(description = "指定的密钥名称", required = false) @RequestParam(required = false) String keyName) {
        
        // 参数校验
        if (StrUtil.isEmpty(receiveId)) {
            return error("接收者ID不能为空");
        }
        if (StrUtil.isEmpty(content)) {
            return error("消息内容不能为空");
        }
        
        // 检查配置
        if (!feishuService.isConfigAvailable()) {
            return error("飞书配置不可用，请检查sys_secret_key表中的飞书配置");
        }
        
        // 发送消息
        boolean success;
        if (StrUtil.isNotBlank(keyName)) {
            success = feishuService.sendTextMessage(receiveId, receiveIdType, content, keyName);
        } else {
            success = feishuService.sendTextMessage(receiveId, receiveIdType, content);
        }
        
        if (success) {
            return success("消息发送成功");
        } else {
            return error("消息发送失败，请查看日志获取详细信息");
        }
    }
    
    /**
     * 发送消息（通用接口）
     */
    @Operation(summary = "发送消息", description = "发送各种类型的消息")
    @SaCheckPermission("system:feishu:send")
    @Log(title = "飞书消息", businessType = BusinessType.INSERT)
    @PostMapping("/send")
    public AjaxResult sendMessage(
            @RequestBody FeishuMessageDto messageDto,
            @Parameter(description = "指定的密钥名称", required = false) @RequestParam(required = false) String keyName) {
        
        // 参数校验
        if (messageDto == null) {
            return error("消息参数不能为空");
        }
        if (StrUtil.isEmpty(messageDto.getReceiveId())) {
            return error("接收者ID不能为空");
        }
        if (StrUtil.isEmpty(messageDto.getContent())) {
            return error("消息内容不能为空");
        }
        
        // 设置默认值
        if (StrUtil.isEmpty(messageDto.getReceiveIdType())) {
            messageDto.setReceiveIdType("user_id");
        }
        if (StrUtil.isEmpty(messageDto.getMsgType())) {
            messageDto.setMsgType("text");
        }
        
        // 检查配置
        if (!feishuService.isConfigAvailable()) {
            return error("飞书配置不可用，请检查sys_secret_key表中的飞书配置");
        }
        
        // 发送消息
        boolean success;
        if (StrUtil.isNotBlank(keyName)) {
            success = feishuService.sendMessage(messageDto, keyName);
        } else {
            success = feishuService.sendMessage(messageDto);
        }
        
        if (success) {
            return success("消息发送成功");
        } else {
            return error("消息发送失败，请查看日志获取详细信息");
        }
    }
    
    /**
     * 发送富文本消息
     */
    @Operation(summary = "发送富文本消息", description = "向指定用户发送富文本消息")
    @SaCheckPermission("system:feishu:send")
    @Log(title = "飞书消息", businessType = BusinessType.INSERT)
    @PostMapping("/sendPost")
    public AjaxResult sendPostMessage(
            @Parameter(description = "接收者ID", required = true) @RequestParam String receiveId,
            @Parameter(description = "接收者ID类型", required = false) @RequestParam(defaultValue = "user_id") String receiveIdType,
            @Parameter(description = "消息标题", required = true) @RequestParam String title,
            @Parameter(description = "消息内容", required = true) @RequestParam String content,
            @Parameter(description = "指定的密钥名称", required = false) @RequestParam(required = false) String keyName) {
        
        // 参数校验
        if (StrUtil.isEmpty(receiveId)) {
            return error("接收者ID不能为空");
        }
        if (StrUtil.isEmpty(title)) {
            return error("消息标题不能为空");
        }
        if (StrUtil.isEmpty(content)) {
            return error("消息内容不能为空");
        }
        
        // 检查配置
        if (!feishuService.isConfigAvailable()) {
            return error("飞书配置不可用，请检查sys_secret_key表中的飞书配置");
        }
        
        // 创建富文本消息
        FeishuMessageDto messageDto = FeishuMessageDto.createPostMessage(receiveId, receiveIdType, title, content);
        
        // 发送消息
        boolean success;
        if (StrUtil.isNotBlank(keyName)) {
            success = feishuService.sendMessage(messageDto, keyName);
        } else {
            success = feishuService.sendMessage(messageDto);
        }
        
        if (success) {
            return success("富文本消息发送成功");
        } else {
            return error("富文本消息发送失败，请查看日志获取详细信息");
        }
    }
    
    /**
     * 检查飞书配置状态
     */
    @Operation(summary = "检查配置状态", description = "检查飞书配置是否可用")
    @SaCheckPermission("system:feishu:config")
    @GetMapping("/status")
    public AjaxResult checkStatus() {
        boolean available = feishuService.isConfigAvailable();
        
        if (available) {
            return success("飞书配置正常");
        } else {
            return error("飞书配置不可用，请检查sys_secret_key表中的飞书配置");
        }
    }
    
    /**
     * 重新加载飞书配置
     */
    @Operation(summary = "重新加载配置", description = "重新从数据库加载飞书配置")
    @SaCheckPermission("system:feishu:config")
    @Log(title = "飞书配置", businessType = BusinessType.UPDATE)
    @PostMapping("/reload")
    public AjaxResult reloadConfig(
            @Parameter(description = "指定的密钥名称", required = false) @RequestParam(required = false) String keyName) {
        try {
            if (StrUtil.isNotBlank(keyName)) {
                feishuService.reloadConfig(keyName);
            } else {
                feishuService.reloadConfig();
            }
            
            if (feishuService.isConfigAvailable()) {
                return success("飞书配置重新加载成功");
            } else {
                return error("飞书配置重新加载失败，请检查sys_secret_key表中的飞书配置");
            }
        } catch (Exception e) {
            return error("重新加载配置时发生异常: " + e.getMessage());
        }
    }
}