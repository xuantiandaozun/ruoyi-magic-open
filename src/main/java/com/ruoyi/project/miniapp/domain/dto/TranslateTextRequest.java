package com.ruoyi.project.miniapp.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TranslateTextRequest {

    @NotBlank(message = "text不能为空")
    @Size(max = 2000, message = "文本长度不能超过2000字符")
    private String text;

    private String sourceLanguage;

    @NotBlank(message = "targetLanguage不能为空")
    private String targetLanguage;
}
