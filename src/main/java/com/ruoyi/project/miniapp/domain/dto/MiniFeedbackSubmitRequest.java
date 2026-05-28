package com.ruoyi.project.miniapp.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MiniFeedbackSubmitRequest {
    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 2000, message = "反馈内容不能超过2000个字符")
    private String content;

    @Size(max = 128, message = "联系方式长度不能超过128个字符")
    private String contact;

    @Size(max = 32, message = "反馈类型长度不能超过32个字符")
    private String feedbackType;
}
