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

    /** 客户端选择的原始文件名，用于补全上传时丢失的文件名 */
    private String fileName;
}
