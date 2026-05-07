package com.ruoyi.project.ai.service;

import java.util.Map;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.ai.domain.AiOpenApiKey;

public interface IAiOpenApiKeyService extends IService<AiOpenApiKey> {

    Map<String, Object> createKey(AiOpenApiKey entity);

    AiOpenApiKey validateKey(String rawKey);

    void recordUsage(Long keyId, boolean success, Integer inputTokens, Integer outputTokens, Integer totalTokens, String clientIp);
}
