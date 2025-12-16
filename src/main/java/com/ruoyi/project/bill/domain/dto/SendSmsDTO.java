package com.ruoyi.project.bill.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发送短信/邮件验证码DTO
 * 
 * @author ruoyi
 */
@Data
@Schema(description = "发送验证码对象")
public class SendSmsDTO {

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱")
    private String email;

    /**
     * 验证码类型（register-注册，resetPwd-重置密码）
     */
    @Schema(description = "验证码类型")
    private String type;
}
