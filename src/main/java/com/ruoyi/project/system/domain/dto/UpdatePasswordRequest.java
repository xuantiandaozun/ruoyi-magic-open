package com.ruoyi.project.system.domain.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 密码更新请求DTO
 * 
 * @author ruoyi
 */
@Data
public class UpdatePasswordRequest {
    
    /** 旧密码 */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;
    
    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度必须在6-20个字符之间")
    private String newPassword;
}