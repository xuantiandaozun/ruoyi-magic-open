package com.ruoyi.project.ai.strategy;

import com.ruoyi.project.ai.domain.AiModelConfig;
import com.ruoyi.project.ai.strategy.impl.LangChainGenericClientStrategy;

/**
 * 策略工厂：根据配置创建具体策略实现
 */
public class AiClientFactory {

    public static AiClientStrategy fromConfig(AiModelConfig cfg) {
        // 统一走 LangChain4j 通用策略，传递完整的配置对象
        return new LangChainGenericClientStrategy(cfg);
    }
}