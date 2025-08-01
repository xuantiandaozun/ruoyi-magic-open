package com.ruoyi.project.system.domain.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 飞书OAuth授权请求DTO
 * 
 * @author ruoyi
 */
@Data
public class FeishuOAuthRequest {
    
    /** 授权码 */
    @NotBlank(message = "授权码不能为空")
    private String code;
    
    /** 状态参数 */
    private String state;
    
    /** 重定向地址 */
    private String redirectUri;
}