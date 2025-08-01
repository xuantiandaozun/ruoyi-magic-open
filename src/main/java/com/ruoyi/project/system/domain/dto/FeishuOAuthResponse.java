package com.ruoyi.project.system.domain.dto;

import lombok.Data;

/**
 * 飞书OAuth授权响应DTO
 * 
 * @author ruoyi
 */
@Data
public class FeishuOAuthResponse {
    
    /** 响应码 */
    private Integer code;
    
    /** 访问令牌 */
    private String access_token;
    
    /** 令牌类型 */
    private String token_type;
    
    /** 过期时间(秒) */
    private Integer expires_in;
    
    /** 刷新令牌 */
    private String refresh_token;
    
    /** 刷新令牌过期时间(秒) */
    private Integer refresh_token_expires_in;
    
    /** 权限范围 */
    private String scope;
    
    /** 错误信息 */
    private String error;
    
    /** 错误描述 */
    private String error_description;
}