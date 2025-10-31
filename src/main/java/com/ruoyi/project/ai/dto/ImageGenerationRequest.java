package com.ruoyi.project.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 图片生成请求DTO
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Data
@Schema(description = "图片生成请求")
public class ImageGenerationRequest {
    
    @Schema(description = "提示词", required = true, example = "一副典雅庄重的对联悬挂于厅堂之中")
    private String prompt;
    
    @Schema(description = "模型名称", example = "qwen-image-plus")
    private String model;
    
    @Schema(description = "图片尺寸", example = "1328*1328")
    private String size;
    
    @Schema(description = "生成数量", example = "1", minimum = "1", maximum = "4")
    private Integer n;
    
    @Schema(description = "是否扩展提示词", example = "true")
    private Boolean promptExtend;
    
    @Schema(description = "是否添加水印", example = "true")
    private Boolean watermark;
}