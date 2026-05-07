package com.ruoyi.project.ai.service.impl;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.ai.domain.AiProductApp;
import com.ruoyi.project.ai.mapper.AiProductAppMapper;
import com.ruoyi.project.ai.service.IAiProductAppService;

@Service
@UseDataSource("MASTER")
public class AiProductAppServiceImpl extends ServiceImpl<AiProductAppMapper, AiProductApp>
        implements IAiProductAppService {
}
