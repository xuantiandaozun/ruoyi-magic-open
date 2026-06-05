package com.ruoyi.framework.aliyun.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AliyunNlsConfig {

    private AliyunCredential credential;

    /** 一句话识别网关地址 */
    private String gatewayUrl;
}
