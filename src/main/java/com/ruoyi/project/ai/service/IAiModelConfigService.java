package com.ruoyi.project.ai.service;

import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.ai.domain.AiModelConfig;

/**
 * 大模型配置Service接口
 */
public interface IAiModelConfigService extends IService<AiModelConfig> {
    
    /** 根据厂商与能力获取启用的模型配置列表 */
    List<AiModelConfig> listEnabledByProviderAndCapability(String provider, String capability);

    /** 根据能力获取所有启用的模型配置列表 */
    List<AiModelConfig> listEnabledByCapability(String capability);

    /** 获取默认的模型配置（若存在） */
    AiModelConfig getDefaultByProviderAndCapability(String provider, String capability);

    /** 设置默认配置（保证唯一） */
    boolean setDefault(Long configId);
}