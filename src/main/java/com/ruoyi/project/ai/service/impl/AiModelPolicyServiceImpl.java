package com.ruoyi.project.ai.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.ai.domain.AiModelPolicy;
import com.ruoyi.project.ai.mapper.AiModelPolicyMapper;
import com.ruoyi.project.ai.service.IAiModelPolicyService;

@Service
@UseDataSource("MASTER")
public class AiModelPolicyServiceImpl extends ServiceImpl<AiModelPolicyMapper, AiModelPolicy>
        implements IAiModelPolicyService {
}
