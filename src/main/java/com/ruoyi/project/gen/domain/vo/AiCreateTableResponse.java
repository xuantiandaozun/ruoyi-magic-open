package com.ruoyi.project.gen.domain.vo;

import java.util.List;

import com.ruoyi.project.gen.domain.GenTable;
import com.ruoyi.project.gen.domain.GenTableColumn;

import lombok.Data;

/**
 * 表结构信息返回VO
 */
@Data
public class AiCreateTableResponse {
    
    /**
     * 表信息
     */
    private GenTable info;

    /**
     * 列信息
     */
    private List<GenTableColumn> rows;
} 