package com.ruoyi.common.storage;

import com.ruoyi.framework.config.CloudStorageConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件存储服务
 * 根据配置自动选择合适的存储策略
 *
 * @author ruoyi
 */
@Service
public class FileStorageService {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CloudStorageConfig cloudStorageConfig;

    /**
     * 获取当前配置的存储策略
     */
    private FileStorageStrategy getCurrentStrategy() {
        String storageType = cloudStorageConfig.getType();
        
        // 根据配置类型获取对应的存储策略实现
        switch (storageType.toLowerCase()) {
            case "aliyun":
                return getStrategyBean("aliyunOssStorageStrategy");
            case "tencent":
                return getStrategyBean("tencentCosStorageStrategy");
            case "amazon":
                return getStrategyBean("amazonS3StorageStrategy");
            case "azure":
                return getStrategyBean("azureStorageStrategy");
            case "local":
            default:
                return getStrategyBean("localFileStorageStrategy");
        }
    }

    /**
     * 获取存储策略Bean
     */
    private FileStorageStrategy getStrategyBean(String beanName) {
        try {
            return applicationContext.getBean(beanName, FileStorageStrategy.class);
        } catch (Exception e) {
            // 如果指定的策略不存在，回退到本地存储
            return applicationContext.getBean("localFileStorageStrategy", FileStorageStrategy.class);
        }
    }

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @param fileName 文件名（包含路径）
     * @return 文件访问URL
     * @throws Exception 上传异常
     */
    public String upload(MultipartFile file, String fileName) throws Exception {
        FileStorageStrategy strategy = getCurrentStrategy();
        return strategy.upload(file, fileName);
    }

    /**
     * 删除文件
     *
     * @param fileName 文件名（包含路径）
     * @return 是否删除成功
     */
    public boolean delete(String fileName) {
        FileStorageStrategy strategy = getCurrentStrategy();
        return strategy.delete(fileName);
    }

    /**
     * 获取文件访问URL
     *
     * @param fileName 文件名（包含路径）
     * @return 文件访问URL
     */
    public String getFileUrl(String fileName) {
        FileStorageStrategy strategy = getCurrentStrategy();
        return strategy.getFileUrl(fileName);
    }

    /**
     * 检查文件是否存在
     *
     * @param fileName 文件名（包含路径）
     * @return 是否存在
     */
    public boolean exists(String fileName) {
        FileStorageStrategy strategy = getCurrentStrategy();
        return strategy.exists(fileName);
    }

    /**
     * 获取当前存储类型
     *
     * @return 存储类型
     */
    public String getCurrentStorageType() {
        FileStorageStrategy strategy = getCurrentStrategy();
        return strategy.getStorageType();
    }

    /**
     * 获取所有可用的存储策略
     *
     * @return 存储策略映射
     */
    public Map<String, FileStorageStrategy> getAllStrategies() {
        return applicationContext.getBeansOfType(FileStorageStrategy.class);
    }
}