package com.ruoyi.project.ai.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.ai.domain.AiWorkflowStepRun;
import com.ruoyi.project.ai.mapper.AiWorkflowStepRunMapper;
import com.ruoyi.project.ai.service.IAiWorkflowStepRunService;

/**
 * AI工作流步骤执行记录Service实现。
 */
@Service
@UseDataSource("MASTER")
public class AiWorkflowStepRunServiceImpl
        extends ServiceImpl<AiWorkflowStepRunMapper, AiWorkflowStepRun>
        implements IAiWorkflowStepRunService {
}
