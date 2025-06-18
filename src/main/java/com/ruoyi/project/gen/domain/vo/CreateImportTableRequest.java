package com.ruoyi.project.gen.domain.vo;

import java.util.List;

import com.ruoyi.project.gen.domain.GenTable;
import com.ruoyi.project.gen.domain.GenTableColumn;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建导入表请求DTO
 */
@Data
public class CreateImportTableRequest {
    
    /**
     * 表名
     */
    @NotBlank(message = "表名不能为空")
    private String tables;
    
    /**
     * 表信息
     */
    @NotNull(message = "表信息不能为空")
    private GenTable info;

    /**
     * 列信息
     */
    @NotNull(message = "列信息不能为空")
    private List<GenTableColumn> rows;
}
