package com.ruoyi.project.gen.tools.request;

import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 批量更新表字段请求类
 * 用于AI调用时避免Long类型数据溢出问题
 * 
 * @author ruoyi
 */
@Data
@Accessors(chain = true)
public class BatchUpdateGenTableColumnRequest {
    
    /** 字段信息列表 */
    private List<UpdateGenTableColumnRequest> columns;
    
    /** 任务ID */
    private String taskId;
}