package com.ruoyi.project.ai.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.ai.domain.AiToolRun;
import com.ruoyi.project.ai.mapper.AiToolRunMapper;
import com.ruoyi.project.ai.service.IAiToolRunService;

/**
 * AI工具调用记录Service实现。
 */
@Service
@UseDataSource("MASTER")
public class AiToolRunServiceImpl extends ServiceImpl<AiToolRunMapper, AiToolRun>
        implements IAiToolRunService {
}
