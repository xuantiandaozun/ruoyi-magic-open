package com.ruoyi.project.ai.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.ai.domain.AiWorkflow;
import com.ruoyi.project.ai.mapper.AiWorkflowMapper;
import com.ruoyi.project.ai.service.IAiWorkflowService;

/**
 * AI工作流Service业务层处理
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Service
@UseDataSource("MASTER")
public class AiWorkflowServiceImpl extends ServiceImpl<AiWorkflowMapper, AiWorkflow>
        implements IAiWorkflowService {

    @Override
    public List<AiWorkflow> listByEnabled(String enabled) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_workflow")
            .where(new QueryColumn("enabled").eq(enabled))
            .and(new QueryColumn("del_flag").eq("0"));
        return list(qw);
    }

    @Override
    public AiWorkflow getByName(String name) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_workflow")
            .where(new QueryColumn("workflow_name").eq(name))
            .and(new QueryColumn("del_flag").eq("0"));
        return getOne(qw);
    }
}