package com.ruoyi.framework.aliyun.factory.impl;

import org.springframework.stereotype.Component;

import com.aliyun.ocr_api20210707.Client;
import com.aliyun.teaopenapi.models.Config;
import com.ruoyi.framework.aliyun.config.AliyunCredential;
import com.ruoyi.framework.aliyun.factory.AliyunClientFactory;

/**
 * 阿里云 OCR 客户端工厂
 */
@Component
public class OcrClientFactory implements AliyunClientFactory<Client> {

    private static final String DEFAULT_ENDPOINT = "ocr-api.cn-hangzhou.aliyuncs.com";

    @Override
    public Client createClient(AliyunCredential credential) {
        try {
            Config config = new Config()
                    .setAccessKeyId(credential.getAccessKeyId())
                    .setAccessKeySecret(credential.getAccessKeySecret())
                    .setEndpoint(DEFAULT_ENDPOINT);
            return new Client(config);
        } catch (Exception e) {
            throw new IllegalStateException("创建阿里云 OCR 客户端失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getServiceType() {
        return "OCR";
    }

    @Override
    public void closeClient(Client client) {
        // Tea SDK 客户端无需显式关闭
    }
}
