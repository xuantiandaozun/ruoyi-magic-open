package com.ruoyi.framework.aliyun.factory.impl;

import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.rds20140815.AsyncClient;
import com.ruoyi.framework.aliyun.config.AliyunCredential;
import com.ruoyi.framework.aliyun.factory.AliyunClientFactory;
import darabonba.core.client.ClientOverrideConfiguration;
import org.springframework.stereotype.Component;

/**
 * RDS客户端工厂实现
 * 
 * @author ruoyi
 */
@Component
public class RdsClientFactory implements AliyunClientFactory<AsyncClient> {
    
    @Override
    public AsyncClient createClient(AliyunCredential credential) {
        // 创建凭证提供者
        StaticCredentialProvider provider = StaticCredentialProvider.create(
            Credential.builder()
                .accessKeyId(credential.getAccessKeyId())
                .accessKeySecret(credential.getAccessKeySecret())
                .build()
        );
        
        // 获取区域，默认为杭州
        String region = credential.getRegion() != null ? credential.getRegion() : "cn-hangzhou";
        
        // 构建客户端
        return AsyncClient.builder()
                .region(region)
                .credentialsProvider(provider)
                .overrideConfiguration(
                    ClientOverrideConfiguration.create()
                        .setEndpointOverride("rds." + region + ".aliyuncs.com")
                )
                .build();
    }
    
    @Override
    public String getServiceType() {
        return "RDS";
    }
    
    @Override
    public void closeClient(AsyncClient client) {
        if (client != null) {
            client.close();
        }
    }
}