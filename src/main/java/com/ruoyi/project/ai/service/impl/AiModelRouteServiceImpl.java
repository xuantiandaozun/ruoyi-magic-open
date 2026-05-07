package com.ruoyi.project.ai.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.ai.domain.AiModelRoute;
import com.ruoyi.project.ai.mapper.AiModelRouteMapper;
import com.ruoyi.project.ai.service.IAiModelRouteService;

@Service
@UseDataSource("MASTER")
public class AiModelRouteServiceImpl extends ServiceImpl<AiModelRouteMapper, AiModelRoute>
        implements IAiModelRouteService {
}
