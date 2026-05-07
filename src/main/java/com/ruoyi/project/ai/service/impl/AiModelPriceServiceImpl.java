package com.ruoyi.project.ai.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.ai.domain.AiModelPrice;
import com.ruoyi.project.ai.mapper.AiModelPriceMapper;
import com.ruoyi.project.ai.service.IAiModelPriceService;

@Service
@UseDataSource("MASTER")
public class AiModelPriceServiceImpl extends ServiceImpl<AiModelPriceMapper, AiModelPrice>
        implements IAiModelPriceService {
}
