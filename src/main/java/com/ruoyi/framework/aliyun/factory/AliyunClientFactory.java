package com.ruoyi.framework.aliyun.factory;

import com.ruoyi.framework.aliyun.config.AliyunCredential;

/**
 * 阿里云客户端工厂接口
 * 
 * @author ruoyi
 * @param <T> 客户端类型
 */
public interface AliyunClientFactory<T> {
    
    /**
     * 创建客户端
     * 
     * @param credential 阿里云凭证
     * @return 客户端实例
     */
    T createClient(AliyunCredential credential);
    
    /**
     * 获取服务类型
     * 
     * @return 服务类型标识
     */
    String getServiceType();
    
    /**
     * 关闭客户端
     * 
     * @param client 客户端实例
     */
    default void closeClient(T client) {
        if (client instanceof AutoCloseable) {
            try {
                ((AutoCloseable) client).close();
            } catch (Exception e) {
                // 忽略关闭异常
            }
        }
    }
}