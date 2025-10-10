package com.ruoyi.project.ai.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.ai.domain.AiModelConfig;
import com.ruoyi.project.ai.mapper.AiModelConfigMapper;
import com.ruoyi.project.ai.service.IAiModelConfigService;

@Service
@UseDataSource("MASTER")
public class AiModelConfigServiceImpl extends ServiceImpl<AiModelConfigMapper, AiModelConfig>
        implements IAiModelConfigService {

    @Override
    public List<AiModelConfig> listEnabledByProviderAndCapability(String provider, String capability) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_model_config")
            .where(new QueryColumn("provider").eq(provider))
            .and(new QueryColumn("capability").eq(capability))
            .and(new QueryColumn("enabled").eq("Y"))
            .and(new QueryColumn("del_flag").eq("0"));
        return list(qw);
    }

    @Override
    public List<AiModelConfig> listEnabledByCapability(String capability) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_model_config")
            .where(new QueryColumn("capability").eq(capability))
            .and(new QueryColumn("enabled").eq("Y"))
            .and(new QueryColumn("status").eq("0"))
            .and(new QueryColumn("del_flag").eq("0"));
        return list(qw);
    }

    @Override
    public AiModelConfig getDefaultByProviderAndCapability(String provider, String capability) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_model_config")
            .where(new QueryColumn("provider").eq(provider))
            .and(new QueryColumn("capability").eq(capability))
            .and(new QueryColumn("is_default").eq("Y"))
            .and(new QueryColumn("enabled").eq("Y"))
            .and(new QueryColumn("status").eq("0"))
            .and(new QueryColumn("del_flag").eq("0"));
        return getOne(qw);
    }

    @Override
    public boolean setDefault(Long configId) {
        AiModelConfig target = getById(configId);
        if (target == null) {
            return false;
        }
        // 将同provider+capability的其他默认置为N
        QueryWrapper others = QueryWrapper.create()
            .from("ai_model_config")
            .where(new QueryColumn("provider").eq(target.getProvider()))
            .and(new QueryColumn("capability").eq(target.getCapability()))
            .and(new QueryColumn("is_default").eq("Y"))
            .and(new QueryColumn("del_flag").eq("0"));
        list(others).forEach(cfg -> {
            cfg.setIsDefault("N");
            updateById(cfg);
        });
        target.setIsDefault("Y");
        return updateById(target);
    }
}