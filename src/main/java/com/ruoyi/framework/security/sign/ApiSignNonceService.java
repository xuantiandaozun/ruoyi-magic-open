package com.ruoyi.framework.security.sign;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * 签名请求 nonce 防重放
 */
@Service
public class ApiSignNonceService {

    private final Map<String, Long> nonceExpiryMap = new ConcurrentHashMap<>();

    public boolean isReplay(String clientId, String nonce, long ttlMillis) {
        long now = System.currentTimeMillis();
        nonceExpiryMap.entrySet().removeIf(entry -> entry.getValue() <= now);

        String key = clientId + ':' + nonce;
        Long previous = nonceExpiryMap.putIfAbsent(key, now + Math.max(ttlMillis, 1000L));
        return previous != null && previous > now;
    }
}
