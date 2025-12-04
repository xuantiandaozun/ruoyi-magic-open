package com.ruoyi.project.ai.tool.impl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.project.ai.tool.LangChain4jTool;
import com.ruoyi.project.ai.tool.ToolExecutionResult;
import com.ruoyi.project.feishu.domain.FeishuUsers;
import com.ruoyi.project.feishu.domain.dto.FeishuMessageDto;
import com.ruoyi.project.feishu.service.IFeishuService;
import com.ruoyi.project.feishu.service.IFeishuUsersService;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

/**
 * LangChain4j兼容的飞书消息发送工具
 * 支持发送文本消息和富文本消息到飞书用户或群组
 * 
 * @author ruoyi-magic
 * @date 2025-12-04
 */
@Component
public class FeishuMessageLangChain4jTool implements LangChain4jTool {
    
    private static final Logger log = LoggerFactory.getLogger(FeishuMessageLangChain4jTool.class);
    
    @Autowired
    private IFeishuService feishuService;
    
    @Autowired
    private IFeishuUsersService feishuUsersService;
    
    @Override
    public String getToolName() {
        return "feishu_send_message";
    }
    
    @Override
    public String getToolDescription() {
        return "发送消息到飞书用户或群组。支持发送文本消息和富文本消息。接收者可选，如果不指定则自动从系统配置的默认飞书用户发送。也可以通过user_id、open_id、email或chat_id等方式指定接收者。";
    }
    
    @Override
    public ToolSpecification getToolSpecification() {
        // 创建飞书消息发送工具规范
        JsonObjectSchema parametersSchema = JsonObjectSchema.builder()
            .addStringProperty("content", "消息内容，必填。对于text类型直接填写文本内容；对于post类型填写富文本内容")
            .addStringProperty("title", "消息标题，可选。如果提供则发送富文本消息，否则发送普通文本消息")
            .addStringProperty("receiveId", "接收者ID，可选。如果不指定则自动使用系统配置的默认飞书用户。可以是用户ID(user_id)、开放ID(open_id)、邮箱(email)、联合ID(union_id)或群聊ID(chat_id)")
            .addStringProperty("receiveIdType", "接收者ID类型，可选值：user_id(用户ID)、open_id(开放ID)、email(邮箱)、union_id(联合ID)、chat_id(群聊ID)。默认自动判断")
            .addStringProperty("keyName", "飞书应用密钥名称，可选。用于指定使用哪个飞书应用发送消息，不指定则使用默认配置")
            .required("content")
            .build();
        
        return ToolSpecification.builder()
            .name(getToolName())
            .description(getToolDescription())
            .parameters(parametersSchema)
            .build();
    }
    
    @Override
    public String execute(Map<String, Object> parameters) {
        // 获取必填参数
        String content = (String) parameters.get("content");
        
        // 获取可选参数
        String receiveId = (String) parameters.get("receiveId");
        String receiveIdType = (String) parameters.get("receiveIdType");
        String title = (String) parameters.get("title");
        String keyName = (String) parameters.get("keyName");
        
        log.info("[FeishuMessageTool] 开始发送飞书消息, receiveId={}, receiveIdType={}, title={}, keyName={}",
                receiveId, receiveIdType, title, keyName);
        
        // 参数验证
        if (StrUtil.isBlank(content)) {
            return ToolExecutionResult.failure("operation", "消息内容(content)不能为空");
        }
        
        // 如果没有指定接收者，从数据库获取默认飞书用户
        if (StrUtil.isBlank(receiveId)) {
            FeishuUsers user = feishuUsersService.getOne(QueryWrapper.create().limit(1));
            if (user == null) {
                return ToolExecutionResult.failure("operation", "未指定接收者且系统中未配置默认飞书用户");
            }
            // 优先使用user_id，其次使用open_id
            receiveId = StrUtil.isNotBlank(user.getUserId()) ? user.getUserId() : user.getOpenId();
            receiveIdType = StrUtil.isNotBlank(user.getUserId()) ? "user_id" : "open_id";
            log.info("[FeishuMessageTool] 使用默认飞书用户, name={}, receiveId={}, receiveIdType={}", 
                    user.getName(), receiveId, receiveIdType);
        } else {
            // 如果指定了接收者但没有指定类型，默认使用user_id
            if (StrUtil.isBlank(receiveIdType)) {
                receiveIdType = "user_id";
            }
        }
        
        // 检查飞书配置是否可用
        if (!feishuService.isConfigAvailable()) {
            log.error("[FeishuMessageTool] 飞书配置不可用");
            return ToolExecutionResult.failure("operation", "飞书配置不可用，请检查系统配置");
        }
        
        try {
            boolean success;
            FeishuMessageDto messageDto;
            
            // 根据是否有标题决定消息类型
            if (StrUtil.isNotBlank(title)) {
                // 有标题则发送富文本消息
                messageDto = FeishuMessageDto.createPostMessage(receiveId, receiveIdType, title, content);
            } else {
                // 无标题则发送文本消息
                messageDto = FeishuMessageDto.createTextMessage(receiveId, receiveIdType, content);
            }
            
            // 发送消息
            if (StrUtil.isNotBlank(keyName)) {
                success = feishuService.sendMessage(messageDto, keyName);
            } else {
                success = feishuService.sendMessage(messageDto);
            }
            
            if (success) {
                log.info("[FeishuMessageTool] 飞书消息发送成功, receiveId={}", receiveId);
                
                // 构建返回信息
                StringBuilder result = new StringBuilder();
                result.append("飞书消息发送成功！\n");
                result.append("接收者: ").append(receiveId).append("\n");
                result.append("接收者类型: ").append(getReceiveIdTypeDesc(receiveIdType)).append("\n");
                result.append("消息类型: ").append(StrUtil.isNotBlank(title) ? "富文本消息" : "文本消息").append("\n");
                if (StrUtil.isNotBlank(title)) {
                    result.append("消息标题: ").append(title).append("\n");
                }
                result.append("消息内容: ").append(content.length() > 100 ? content.substring(0, 100) + "..." : content);
                
                return ToolExecutionResult.operationSuccess(result.toString(), "飞书消息发送成功");
            } else {
                log.error("[FeishuMessageTool] 飞书消息发送失败, receiveId={}", receiveId);
                return ToolExecutionResult.failure("operation", "飞书消息发送失败，请检查接收者ID是否正确或查看系统日志");
            }
            
        } catch (Exception e) {
            log.error("[FeishuMessageTool] 发送飞书消息异常", e);
            return ToolExecutionResult.failure("operation", "发送飞书消息异常: " + e.getMessage());
        }
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return false;
        }
        
        // 验证必填参数：content
        String content = (String) parameters.get("content");
        if (StrUtil.isBlank(content)) {
            return false;
        }
        
        // 验证receiveIdType参数（如果提供）
        if (parameters.containsKey("receiveIdType") && parameters.get("receiveIdType") != null) {
            String receiveIdType = parameters.get("receiveIdType").toString().toLowerCase();
            if (!receiveIdType.equals("user_id") && !receiveIdType.equals("open_id") && 
                !receiveIdType.equals("email") && !receiveIdType.equals("union_id") &&
                !receiveIdType.equals("chat_id")) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String getUsageExample() {
        return """
        示例用法：
        1. 发送简单文本消息（使用默认接收者）：
           {"content": "你好，这是一条测试消息"}
        
        2. 发送带标题的富文本消息（使用默认接收者）：
           {"title": "重要通知", "content": "这是一条重要的富文本消息内容"}
        
        3. 发送消息给指定用户（通过user_id）：
           {"receiveId": "user123", "content": "你好，这是发给指定用户的消息"}
        
        4. 发送消息给指定用户（通过邮箱）：
           {"receiveId": "user@example.com", "receiveIdType": "email", "content": "你好，这是通过邮箱发送的消息"}
        
        5. 发送消息到群聊：
           {"receiveId": "oc_xxx", "receiveIdType": "chat_id", "content": "这是发送到群聊的消息"}
        
        6. 使用指定的飞书应用发送消息：
           {"content": "使用指定应用发送的消息", "keyName": "feishu_app_1"}
        
        7. 发送富文本消息到群聊：
           {"receiveId": "oc_xxx", "receiveIdType": "chat_id", "title": "日报通知", "content": "今日工作内容汇总..."}
        """;
    }
    
    /**
     * 获取接收者ID类型描述
     */
    private String getReceiveIdTypeDesc(String receiveIdType) {
        switch (receiveIdType.toLowerCase()) {
            case "user_id":
                return "用户ID";
            case "open_id":
                return "开放ID";
            case "email":
                return "邮箱";
            case "union_id":
                return "联合ID";
            case "chat_id":
                return "群聊ID";
            default:
                return receiveIdType;
        }
    }
}
