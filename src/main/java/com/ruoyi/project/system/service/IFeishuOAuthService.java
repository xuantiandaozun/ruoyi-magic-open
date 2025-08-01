package com.ruoyi.project.system.service;

import com.ruoyi.project.system.domain.dto.FeishuOAuthRequest;

/**
 * 飞书OAuth服务接口
 * 
 * @author ruoyi
 */
public interface IFeishuOAuthService {
    
    /**
     * 生成飞书授权URL
     * 
     * @param redirectUri 回调地址
     * @param state 状态参数
     * @return 授权URL
     */
    String generateAuthUrl(String redirectUri, String state);
    
    /**
     * 使用授权码获取访问令牌并保存到用户表
     * 
     * @param request 授权请求
     * @return 是否成功
     */
    boolean handleOAuthCallback(FeishuOAuthRequest request);
    
    /**
     * 获取当前用户的飞书访问令牌
     * 
     * @return 访问令牌
     */
    String getCurrentUserFeishuToken();
    
    /**
     * 检查当前用户是否已授权飞书
     * 
     * @return 是否已授权
     */
    boolean isCurrentUserAuthorized();
    
    /**
     * 注销当前用户的飞书授权
     * 
     * @return 是否成功
     */
    boolean revokeCurrentUserAuthorization();
}