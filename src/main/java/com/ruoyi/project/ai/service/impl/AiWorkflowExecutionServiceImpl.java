package com.ruoyi.project.ai.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.ai.domain.AiWorkflowExecution;
import com.ruoyi.project.ai.mapper.AiWorkflowExecutionMapper;
import com.ruoyi.project.ai.service.IAiWorkflowExecutionService;

/**
 * AI工作流执行记录Service业务层处理
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Service
@UseDataSource("MASTER")
public class AiWorkflowExecutionServiceImpl extends ServiceImpl<AiWorkflowExecutionMapper, AiWorkflowExecution>
        implements IAiWorkflowExecutionService {

    @Override
    public List<AiWorkflowExecution> listByWorkflowId(Long workflowId) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_workflow_execution")
            .where(new QueryColumn("workflow_id").eq(workflowId))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy(new QueryColumn("create_time").desc());
        return list(qw);
    }

    @Override
    public List<AiWorkflowExecution> listByStatus(String status) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_workflow_execution")
            .where(new QueryColumn("status").eq(status))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy(new QueryColumn("create_time").desc());
        return list(qw);
    }

    @Override
    public List<AiWorkflowExecution> listByWorkflowIdAndStatus(Long workflowId, String status) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_workflow_execution")
            .where(new QueryColumn("workflow_id").eq(workflowId))
            .and(new QueryColumn("status").eq(status))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy(new QueryColumn("create_time").desc());
        return list(qw);
    }
}