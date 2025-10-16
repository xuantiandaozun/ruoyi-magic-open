package com.ruoyi.project.ai.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.ai.domain.AiWorkflowStep;
import com.ruoyi.project.ai.mapper.AiWorkflowStepMapper;
import com.ruoyi.project.ai.service.IAiWorkflowStepService;

/**
 * AI工作流步骤Service业务层处理
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Service
@UseDataSource("MASTER")
public class AiWorkflowStepServiceImpl extends ServiceImpl<AiWorkflowStepMapper, AiWorkflowStep>
        implements IAiWorkflowStepService {

    @Override
    public List<AiWorkflowStep> selectByWorkflowId(Long workflowId) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_workflow_step")
            .where(new QueryColumn("workflow_id").eq(workflowId))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy(new QueryColumn("step_order").asc());
        return list(qw);
    }

    @Override
    public List<AiWorkflowStep> selectByWorkflowIdAndEnabled(Long workflowId, String enabled) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_workflow_step")
            .where(new QueryColumn("workflow_id").eq(workflowId))
            .and(new QueryColumn("enabled").eq(enabled))
            .and(new QueryColumn("del_flag").eq("0"))
            .orderBy(new QueryColumn("step_order").asc());
        return list(qw);
    }
}