package com.ruoyi.project.ai.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.ai.domain.AiOpenApiKey;
import com.ruoyi.project.ai.mapper.AiOpenApiKeyMapper;
import com.ruoyi.project.ai.service.IAiOpenApiKeyService;

import cn.hutool.core.util.StrUtil;

@Service
@UseDataSource("MASTER")
public class AiOpenApiKeyServiceImpl extends ServiceImpl<AiOpenApiKeyMapper, AiOpenApiKey>
        implements IAiOpenApiKeyService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String KEY_PREFIX = "omk";

    @Override
    @Transactional
    public Map<String, Object> createKey(AiOpenApiKey entity) {
        String publicId = randomUrlSafe(10);
        String secret = randomUrlSafe(32);
        String rawKey = KEY_PREFIX + "_" + publicId + "_" + secret;
        String salt = randomUrlSafe(16);

        AiOpenApiKey openApiKey = new AiOpenApiKey();
        openApiKey.setName(entity.getName());
        openApiKey.setKeyPrefix(KEY_PREFIX + "_" + publicId);
        openApiKey.setKeyHash(hash(rawKey, salt));
        openApiKey.setSalt(salt);
        openApiKey.setStatus(StrUtil.blankToDefault(entity.getStatus(), "0"));
        openApiKey.setEnabled(StrUtil.blankToDefault(entity.getEnabled(), "Y"));
        openApiKey.setExpiresAt(entity.getExpiresAt());
        openApiKey.setAllowedModels(entity.getAllowedModels());
        openApiKey.setRequestCount(0L);
        openApiKey.setSuccessCount(0L);
        openApiKey.setFailedCount(0L);
        openApiKey.setInputTokens(0L);
        openApiKey.setOutputTokens(0L);
        openApiKey.setTotalTokens(0L);
        openApiKey.setCreateBy(StrUtil.blankToDefault(entity.getCreateBy(), "system"));
        openApiKey.setUpdateBy(StrUtil.blankToDefault(entity.getUpdateBy(), "system"));
        openApiKey.setRemark(entity.getRemark());
        openApiKey.setDelFlag("0");
        save(openApiKey);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", openApiKey.getId());
        result.put("name", openApiKey.getName());
        result.put("keyPrefix", openApiKey.getKeyPrefix());
        result.put("apiKey", rawKey);
        result.put("createdAt", new Date());
        return result;
    }

    @Override
    public AiOpenApiKey validateKey(String rawKey) {
        if (StrUtil.isBlank(rawKey) || !rawKey.startsWith(KEY_PREFIX + "_")) {
            return null;
        }
        String[] parts = rawKey.split("_", 3);
        if (parts.length != 3) {
            return null;
        }
        String keyPrefix = parts[0] + "_" + parts[1];
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_open_api_key")
            .where(new QueryColumn("key_prefix").eq(keyPrefix))
            .and(new QueryColumn("enabled").eq("Y"))
            .and(new QueryColumn("status").eq("0"))
            .and(new QueryColumn("del_flag").eq("0"));
        AiOpenApiKey entity = getOne(qw);
        if (entity == null) {
            return null;
        }
        if (entity.getExpiresAt() != null && entity.getExpiresAt().toInstant().isBefore(Instant.now())) {
            return null;
        }
        String expectedHash = hash(rawKey, entity.getSalt());
        return StrUtil.equals(expectedHash, entity.getKeyHash()) ? entity : null;
    }

    @Override
    @Transactional
    public void recordUsage(Long keyId, boolean success, Integer inputTokens, Integer outputTokens, Integer totalTokens, String clientIp) {
        AiOpenApiKey entity = getById(keyId);
        if (entity == null) {
            return;
        }
        entity.setLastUsedAt(new Date());
        entity.setLastUsedIp(clientIp);
        entity.setRequestCount(defaultLong(entity.getRequestCount()) + 1);
        entity.setSuccessCount(defaultLong(entity.getSuccessCount()) + (success ? 1 : 0));
        entity.setFailedCount(defaultLong(entity.getFailedCount()) + (success ? 0 : 1));
        entity.setInputTokens(defaultLong(entity.getInputTokens()) + defaultInt(inputTokens));
        entity.setOutputTokens(defaultLong(entity.getOutputTokens()) + defaultInt(outputTokens));
        entity.setTotalTokens(defaultLong(entity.getTotalTokens()) + defaultInt(totalTokens));
        entity.setUpdateBy("system");
        updateById(entity);
    }

    private String randomUrlSafe(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawKey, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((salt + ":" + rawKey).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("生成API Key哈希失败", e);
        }
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private long defaultInt(Integer value) {
        return value == null ? 0L : value.longValue();
    }
}
