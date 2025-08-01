package com.ruoyi.project.feishu.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonParser;
import com.lark.oapi.Client;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.service.im.v1.model.CreateMessageReq;
import com.lark.oapi.service.im.v1.model.CreateMessageReqBody;
import com.lark.oapi.service.im.v1.model.CreateMessageResp;
import com.ruoyi.common.utils.FeishuConfigUtils;
import com.ruoyi.project.feishu.domain.dto.FeishuMessageDto;
import com.ruoyi.project.feishu.service.IFeishuService;
import com.ruoyi.project.system.config.FeishuConfig;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 飞书服务实现类
 * 
 * @author ruoyi
 * @date 2025-01-30
 */
@Slf4j
@Service
public class FeishuServiceImpl implements IFeishuService {
    

    
    private FeishuConfig feishuConfig;
    private Client feishuClient;
    
    /**
     * 获取飞书配置
     * 
     * @return 飞书配置
     */
    private FeishuConfig getFeishuConfig() {
        if (feishuConfig == null || !feishuConfig.isValid()) {
            loadFeishuConfig();
        }
        return feishuConfig;
    }
    
    /**
     * 从数据库加载飞书配置
     */
    private void loadFeishuConfig() {
        loadFeishuConfig(null);
    }
    
    /**
     * 从数据库加载飞书配置
     * @param keyName 指定的密钥名称，如果为空则使用第一个找到的密钥
     */
    private void loadFeishuConfig(String keyName) {
        try {
            feishuConfig = FeishuConfigUtils.getFeishuConfig(keyName);
            
            if (feishuConfig != null && feishuConfig.isValid()) {
                // 重新创建客户端
                feishuClient = Client.newBuilder(feishuConfig.getAppId(), feishuConfig.getAppSecret()).build();
            } else {
                feishuConfig = new FeishuConfig();
                feishuClient = null;
                log.warn("未找到有效的飞书配置，请在sys_secret_key表中配置飞书应用密钥");
            }
        } catch (Exception e) {
            log.error("加载飞书配置失败", e);
            feishuConfig = new FeishuConfig();
            feishuClient = null;
        }
    }
    
    /**
     * 获取飞书客户端
     * 
     * @return 飞书客户端
     */
    private Client getFeishuClient() {
        if (feishuClient == null) {
            getFeishuConfig();
        }
        return feishuClient;
    }
    
    @Override
    public boolean sendTextMessage(String receiveId, String receiveIdType, String content) {
        return sendTextMessage(receiveId, receiveIdType, content, null);
    }
    
    /**
     * 发送文本消息
     * @param receiveId 接收者ID
     * @param receiveIdType 接收者ID类型
     * @param content 消息内容
     * @param keyName 指定的密钥名称
     * @return 发送结果
     */
    public boolean sendTextMessage(String receiveId, String receiveIdType, String content, String keyName) {
        // 如果指定了keyName，重新加载配置
        if (StrUtil.isNotBlank(keyName)) {
            loadFeishuConfig(keyName);
        }
        
        FeishuMessageDto messageDto = FeishuMessageDto.createTextMessage(receiveId, receiveIdType, content);
        return sendMessage(messageDto);
    }
    
    @Override
    public boolean sendMessage(FeishuMessageDto messageDto) {
        return sendMessage(messageDto, null);
    }
    
    /**
     * 发送通用消息
     * @param messageDto 消息DTO
     * @param keyName 指定的密钥名称
     * @return 发送结果
     */
    public boolean sendMessage(FeishuMessageDto messageDto, String keyName) {
        // 如果指定了keyName，重新加载配置
        if (StrUtil.isNotBlank(keyName)) {
            loadFeishuConfig(keyName);
        }
        
        try {
            Client client = getFeishuClient();
            if (client == null) {
                log.error("飞书客户端未初始化，请检查配置");
                return false;
            }
            
            // 生成UUID（如果未提供）
            String uuid = StrUtil.isNotEmpty(messageDto.getUuid()) ? 
                messageDto.getUuid() : UUID.randomUUID().toString();
            
            // 创建请求对象
            CreateMessageReq req = CreateMessageReq.newBuilder()
                .receiveIdType(messageDto.getReceiveIdType())
                .createMessageReqBody(CreateMessageReqBody.newBuilder()
                    .receiveId(messageDto.getReceiveId())
                    .msgType(messageDto.getMsgType())
                    .content(messageDto.getContent())
                    .uuid(uuid)
                    .build())
                .build();
            
            // 发起请求
            CreateMessageResp resp = client.im().v1().message().create(req);
            
            // 处理响应
            if (resp.success()) {
                log.info("飞书消息发送成功，接收者: {}, 消息类型: {}", 
                    messageDto.getReceiveId(), messageDto.getMsgType());
                return true;
            } else {
                log.error("飞书消息发送失败，错误码: {}, 错误信息: {}, 请求ID: {}", 
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
                
                // 记录详细错误信息
                if (resp.getRawResponse() != null && resp.getRawResponse().getBody() != null) {
                    String errorDetail = Jsons.createGSON(true, false).toJson(
                        JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)));
                    log.error("飞书API详细错误信息: {}", errorDetail);
                }
                return false;
            }
        } catch (Exception e) {
            log.error("发送飞书消息异常", e);
            return false;
        }
    }
    
    @Override
    public boolean isConfigAvailable() {
        FeishuConfig config = getFeishuConfig();
        return config != null && config.isValid();
    }
    
    @Override
    public void reloadConfig() {
        log.info("重新加载飞书配置");
        feishuConfig = null;
        feishuClient = null;
        loadFeishuConfig();
    }
    
    /**
     * 重新加载指定密钥的配置
     * @param keyName 指定的密钥名称
     */
    public void reloadConfig(String keyName) {
        log.info("重新加载飞书配置，密钥名称: {}", keyName);
        feishuConfig = null;
        feishuClient = null;
        loadFeishuConfig(keyName);
    }
}