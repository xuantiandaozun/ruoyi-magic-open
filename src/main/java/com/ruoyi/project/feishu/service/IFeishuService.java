package com.ruoyi.project.feishu.service;

import com.ruoyi.project.feishu.domain.dto.FeishuMessageDto;

/**
 * 飞书服务接口
 * 
 * @author ruoyi
 * @date 2025-01-30
 */
public interface IFeishuService {
    
    /**
     * 发送文本消息
     * @param receiveId 接收者ID
     * @param receiveIdType 接收者ID类型（user_id、email、open_id等）
     * @param content 消息内容
     * @return 发送结果
     */
    boolean sendTextMessage(String receiveId, String receiveIdType, String content);

    /**
     * 发送文本消息（指定密钥）
     * @param receiveId 接收者ID
     * @param receiveIdType 接收者ID类型（user_id、email、open_id等）
     * @param content 消息内容
     * @param keyName 指定的密钥名称
     * @return 发送结果
     */
    boolean sendTextMessage(String receiveId, String receiveIdType, String content, String keyName);
    
    /**
     * 发送通用消息
     * @param messageDto 消息DTO
     * @return 发送结果
     */
    boolean sendMessage(FeishuMessageDto messageDto);

    /**
     * 发送通用消息（指定密钥）
     * @param messageDto 消息DTO
     * @param keyName 指定的密钥名称
     * @return 发送结果
     */
    boolean sendMessage(FeishuMessageDto messageDto, String keyName);
    
    /**
     * 检查飞书配置是否可用
     * 
     * @return 是否可用
     */
    boolean isConfigAvailable();
    
    /**
     * 重新加载配置
     */
    void reloadConfig();

    /**
     * 重新加载指定密钥的配置
     * @param keyName 指定的密钥名称
     */
    void reloadConfig(String keyName);
}