package com.ruoyi.project.miniapp.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTranslateTaskRequest {
    @NotNull(message = "documentId不能为空")
    private Long documentId;

    private String sourceLanguage;

    @NotBlank(message = "targetLanguage不能为空")
    private String targetLanguage;

    private String outputFormat;
}
