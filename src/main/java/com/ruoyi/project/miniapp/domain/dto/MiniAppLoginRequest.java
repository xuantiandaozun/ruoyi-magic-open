package com.ruoyi.project.miniapp.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MiniAppLoginRequest {
    @NotBlank(message = "appCode不能为空")
    private String appCode;

    @NotBlank(message = "code不能为空")
    private String code;
}
