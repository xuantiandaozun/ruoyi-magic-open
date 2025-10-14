package com.ruoyi.project.ai.strategy;

import com.ruoyi.project.ai.domain.AiModelConfig;
import com.ruoyi.project.ai.strategy.impl.LangChainGenericClientStrategy;

/**
 * 策略工厂：根据配置创建具体策略实现
 */
public class AiClientFactory {

    public static AiClientStrategy fromConfig(AiModelConfig cfg) {
        String provider = cfg.getProvider();
        String capability = cfg.getCapability();
        String model = cfg.getModel();
        String apiKey = cfg.getApiKey();
        String endpoint = cfg.getEndpoint();

        // 其他统一走 LangChain4j 通用策略
        return new LangChainGenericClientStrategy(provider, model, endpoint, apiKey);
    }
}