package com.ruoyi.project.bill.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户注册DTO
 * 
 * @author ruoyi
 */
@Data
@Schema(description = "用户注册对象")
public class RegisterDTO {

    /**
     * 手机号（手机号注册时必填）
     */
    @Schema(description = "手机号")
    private String phone;

    /**
     * 邮箱（邮箱注册时必填）
     */
    @Schema(description = "邮箱")
    private String email;

    /**
     * 昵称
     */
    @Schema(description = "昵称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickName;

    /**
     * 密码
     */
    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    /**
     * 验证码
     */
    @Schema(description = "验证码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String verifyCode;
}
