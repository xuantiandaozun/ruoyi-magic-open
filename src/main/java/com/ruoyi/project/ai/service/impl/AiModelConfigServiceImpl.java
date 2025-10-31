package com.ruoyi.project.ai.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public boolean setDefault(Long configId) {
        AiModelConfig target = getById(configId);
        if (target == null) {
            return false;
        }
        
        // 如果目标配置已经是默认配置，直接返回成功
        if ("Y".equals(target.getIsDefault())) {
            return true;
        }
        
        // 将所有其他默认配置置为N（排除当前配置）
        QueryWrapper others = QueryWrapper.create()
            .from("ai_model_config")
            .where(new QueryColumn("is_default").eq("Y"))
            .and(new QueryColumn("del_flag").eq("0"))
            .and(new QueryColumn("id").ne(configId)); // 排除当前配置
            
        list(others).forEach(cfg -> {
            cfg.setIsDefault("N");
            updateById(cfg);
        });
        
        // 设置目标配置为默认
        target.setIsDefault("Y");
        return updateById(target);
    }
}