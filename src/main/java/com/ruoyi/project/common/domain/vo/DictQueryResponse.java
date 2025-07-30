package com.ruoyi.project.common.domain.vo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字典查询响应对象
 * 
 * @author ruoyi
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "字典查询响应")
public class DictQueryResponse {

    /**
     * 字典选项列表
     */
    @Schema(description = "字典选项列表")
    private List<DictOption> options;

    /**
     * 总记录数
     */
    @Schema(description = "总记录数")
    private Long total;

    /**
     * 表名
     */
    @Schema(description = "查询的表名")
    private String tableName;

    /**
     * 使用的显示字段
     */
    @Schema(description = "实际使用的显示字段名")
    private String actualLabelField;

    /**
     * 使用的值字段
     */
    @Schema(description = "实际使用的值字段名")
    private String actualValueField;

    /**
     * 构造函数 - 只包含选项列表
     */
    public DictQueryResponse(List<DictOption> options) {
        this.options = options;
    }

    /**
     * 构造函数 - 包含选项列表和总数
     */
    public DictQueryResponse(List<DictOption> options, Long total) {
        this.options = options;
        this.total = total;
    }
}