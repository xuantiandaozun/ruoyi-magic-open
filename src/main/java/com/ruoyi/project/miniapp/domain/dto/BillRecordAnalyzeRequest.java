package com.ruoyi.project.miniapp.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BillRecordAnalyzeRequest {

    @NotBlank(message = "记账描述不能为空")
    @Size(max = 500, message = "记账描述不能超过500字符")
    private String text;
}
