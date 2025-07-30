package com.ruoyi.framework.aliyun.factory.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.ruoyi.framework.aliyun.config.AliyunCredential;
import com.ruoyi.framework.aliyun.factory.AliyunClientFactory;
import org.springframework.stereotype.Component;

/**
 * OSS客户端工厂实现
 * 
 * @author ruoyi
 */
@Component
public class OssClientFactory implements AliyunClientFactory<OSS> {
    
    @Override
    public OSS createClient(AliyunCredential credential) {
        // 获取区域，默认为杭州
        String region = credential.getRegion() != null ? credential.getRegion() : "cn-hangzhou";
        
        // 构建endpoint
        String endpoint = "https://oss-" + region + ".aliyuncs.com";
        
        // 创建OSS客户端
        return new OSSClientBuilder().build(
            endpoint,
            credential.getAccessKeyId(),
            credential.getAccessKeySecret()
        );
    }
    
    @Override
    public String getServiceType() {
        return "OSS";
    }
    
    @Override
    public void closeClient(OSS client) {
        if (client != null) {
            client.shutdown();
        }
    }
}