package com.ruoyi.project.common.domain.dto;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 字典查询请求对象
 * 
 * @author ruoyi
 */
@Data
@Schema(description = "字典查询请求参数")
public class DictQueryRequest {

    /**
     * 显示字段名
     */
    @Schema(description = "显示字段名，用作选项的显示文本")
    @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]*$", message = "字段名格式不正确")
    @Size(max = 50, message = "字段名长度不能超过50个字符")
    private String labelField;

    /**
     * 值字段名
     */
    @Schema(description = "值字段名，用作选项的值")
    @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]*$", message = "字段名格式不正确")
    @Size(max = 50, message = "字段名长度不能超过50个字符")
    private String valueField;

    /**
     * 状态过滤
     */
    @Schema(description = "状态过滤，0-正常，1-停用")
    @Pattern(regexp = "^[01]$", message = "状态值只能是0或1")
    private String status;

    /**
     * 自定义查询条件
     */
    @Schema(description = "自定义查询条件，key为字段名，value为字段值")
    private Map<String, Object> conditions;

    /**
     * 排序字段
     */
    @Schema(description = "排序字段名")
    @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]*$", message = "字段名格式不正确")
    @Size(max = 50, message = "字段名长度不能超过50个字符")
    private String orderBy;

    /**
     * 排序方向
     */
    @Schema(description = "排序方向，ASC-升序，DESC-降序")
    @Pattern(regexp = "^(ASC|DESC)$", message = "排序方向只能是ASC或DESC")
    private String orderDirection = "ASC";

    /**
     * 是否返回完整数据
     */
    @Schema(description = "是否返回完整数据对象，true-返回完整对象，false-只返回label和value")
    private Boolean includeFullData = false;

    /**
     * 页码
     */
    @Schema(description = "页码，从1开始")
    private Integer pageNum;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小")
    private Integer pageSize;

    /**
     * 高级查询条件
     */
    @Schema(description = "高级查询条件，支持范围查询、比较查询等")
    private List<AdvancedCondition> advancedConditions;

    /**
     * 高级查询条件内部类
     */
    @Data
    @Schema(description = "高级查询条件")
    public static class AdvancedCondition {
        
        /**
         * 字段名
         */
        @Schema(description = "字段名")
        @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]*$", message = "字段名格式不正确")
        @Size(max = 50, message = "字段名长度不能超过50个字符")
        private String fieldName;
        
        /**
         * 操作符
         */
        @Schema(description = "操作符：=, !=, >, <, >=, <=, BETWEEN, LIKE, IN, NOT_IN")
        @Pattern(regexp = "^(=|!=|>|<|>=|<=|BETWEEN|LIKE|IN|NOT_IN)$", message = "操作符不支持")
        private String operator;
        
        /**
         * 值
         */
        @Schema(description = "查询值")
        private Object value;
        
        /**
         * 第二个值（用于BETWEEN操作）
         */
        @Schema(description = "第二个值（用于BETWEEN操作）")
        private Object value2;
    }
}