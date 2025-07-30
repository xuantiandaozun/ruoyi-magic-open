package com.ruoyi.project.common.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字典选项值对象
 * 
 * @author ruoyi
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "字典选项")
public class DictOption {

    /**
     * 显示文本
     */
    @Schema(description = "显示文本")
    private String label;

    /**
     * 选项值
     */
    @Schema(description = "选项值")
    private String value;

    /**
     * 完整数据对象(可选)
     */
    @Schema(description = "完整数据对象，当includeFullData为true时返回")
    private Object data;

    /**
     * 构造函数 - 只包含label和value
     */
    public DictOption(String label, String value) {
        this.label = label;
        this.value = value;
    }
}