package com.ruoyi.project.ai.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.ai.domain.AiImageTask;
import com.ruoyi.project.ai.mapper.AiImageTaskMapper;
import com.ruoyi.project.ai.service.IAiImageTaskService;

@Service
@UseDataSource("MASTER")
public class AiImageTaskServiceImpl extends ServiceImpl<AiImageTaskMapper, AiImageTask>
        implements IAiImageTaskService {
}
