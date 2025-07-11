package com.ruoyi.common.storage;

import com.ruoyi.framework.config.CloudStorageConfig;
import com.ruoyi.project.system.domain.StorageConfig;
import com.ruoyi.project.system.domain.FileUploadRecord;
import com.ruoyi.project.system.service.IStorageConfigService;
import com.ruoyi.project.system.service.IFileUploadRecordService;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.enums.FileType;

import java.util.Map;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;

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
    
    @Autowired
    private IStorageConfigService storageConfigService;
    
    @Autowired
    private IFileUploadRecordService fileUploadRecordService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取当前配置的存储策略
     * 优先使用数据库配置，如果数据库没有配置则使用YML配置
     */
    private FileStorageStrategy getCurrentStrategy() {
        try {
            // 1. 优先查询数据库中的默认配置
            QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("is_default", "Y")
                .eq("status", "0")
                .eq("del_flag", "0");
            
            StorageConfig dbConfig = storageConfigService.getOne(queryWrapper);
            
            if (dbConfig != null && StrUtil.isNotEmpty(dbConfig.getStorageType())) {
                // 使用数据库配置
                return getStrategyByType(dbConfig.getStorageType());
            }
        } catch (Exception e) {
            // 数据库查询失败，记录日志但不影响功能
            System.err.println("查询数据库存储配置失败，使用YML配置: " + e.getMessage());
        }
        
        // 2. 回退到YML配置
        String storageType = cloudStorageConfig.getType();
        return getStrategyByType(storageType);
    }
    
    /**
     * 根据存储类型获取对应的存储策略
     */
    private FileStorageStrategy getStrategyByType(String storageType) {
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
        String storageType = strategy.getStorageType();
        
        // 创建上传记录
        FileUploadRecord record = createUploadRecord(file, fileName, storageType);
        
        try {
            // 执行文件上传
            String fileUrl = strategy.upload(file, fileName);
            
            // 更新上传记录为成功
            record.setFileUrl(fileUrl);
            record.setUploadStatus("1"); // 成功
            
            return fileUrl;
        } catch (Exception e) {
            // 更新上传记录为失败
            record.setUploadStatus("0"); // 失败
            record.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            // 保存上传记录
            try {
                fileUploadRecordService.save(record);
            } catch (Exception e) {
                // 记录保存失败不影响文件上传功能
                System.err.println("保存文件上传记录失败: " + e.getMessage());
            }
        }
    }

    /**
     * 本地文件上传（专门用于FileUploadUtils调用）
     * 使用当前配置的存储策略，而不是强制本地存储
     *
     * @param file 上传的文件
     * @param fileName 文件名（包含路径）
     * @param baseDir 基础目录
     * @return 文件访问URL
     * @throws Exception 上传异常
     */
    public String uploadLocal(MultipartFile file, String fileName, String baseDir) throws Exception {
        // 使用当前配置的存储策略，而不是强制本地存储
        FileStorageStrategy strategy = getCurrentStrategy();
        String storageType = strategy.getStorageType();
        
        // 创建上传记录
        FileUploadRecord record = createUploadRecord(file, fileName, storageType);
        
        try {
            // 执行文件上传
            String fileUrl = strategy.upload(file, fileName);
            
            // 更新上传记录为成功
            record.setFileUrl(fileUrl);
            record.setUploadStatus("1"); // 成功
            
            return fileUrl;
        } catch (Exception e) {
            // 更新上传记录为失败
            record.setUploadStatus("0"); // 失败
            record.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            // 保存上传记录
            try {
                fileUploadRecordService.save(record);
            } catch (Exception e) {
                // 记录保存失败不影响文件上传功能
                System.err.println("保存文件上传记录失败: " + e.getMessage());
            }
        }
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
    
    /**
     * 创建文件上传记录
     */
    private FileUploadRecord createUploadRecord(MultipartFile file, String fileName, String storageType) {
        FileUploadRecord record = new FileUploadRecord();
        
        // 基本文件信息
        record.setOriginalFilename(file.getOriginalFilename());
        record.setStoredFilename(fileName);
        record.setFilePath(fileName);
        record.setFileSize(file.getSize());
        record.setMimeType(file.getContentType());
        record.setStorageType(storageType);
        
        // 文件扩展名和类型
        String originalFilename = file.getOriginalFilename();
        if (StrUtil.isNotEmpty(originalFilename)) {
            int lastDotIndex = originalFilename.lastIndexOf(".");
            if (lastDotIndex > 0) {
                record.setFileExtension(originalFilename.substring(lastDotIndex + 1));
            }
        }
        
        // 根据扩展名确定文件类型
        record.setFileType(getFileTypeByExtension(record.getFileExtension()));
        
        // 获取当前用户和请求信息
        try {
            record.setCreateBy(SecurityUtils.getUsername());
        } catch (Exception e) {
            record.setCreateBy("system");
        }
        
        // 获取请求IP和User-Agent
        try {
            HttpServletRequest request = ServletUtils.getRequest();
            if (request != null) {
                record.setUploadIp(IpUtils.getIpAddr(request));
                record.setUserAgent(request.getHeader("User-Agent"));
            }
        } catch (Exception e) {
            // 获取请求信息失败不影响功能
        }
        
        record.setCreateTime(new Date());
        record.setDelFlag("0");
        
        return record;
    }
    
    /**
     * 根据文件扩展名获取文件类型
     */
    private FileType getFileTypeByExtension(String extension) {
        return FileType.getByExtension(extension);
    }
    
    /**
     * 获取当前存储配置信息（包含数据库配置）
     */
    public Map<String, Object> getCurrentStorageConfig() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 查询数据库默认配置
            QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("is_default", "Y")
                .eq("status", "0")
                .eq("del_flag", "0");
            
            StorageConfig dbConfig = storageConfigService.getOne(queryWrapper);
            
            if (dbConfig != null) {
                result.put("source", "database");
                result.put("configId", dbConfig.getConfigId());
                result.put("configName", dbConfig.getConfigName());
                result.put("storageType", dbConfig.getStorageType());
                
                // 解析配置数据
                if (StrUtil.isNotEmpty(dbConfig.getConfigData())) {
                    try {
                        Map<String, Object> configData = JSONUtil.toBean(dbConfig.getConfigData(), Map.class);
                        result.put("configData", configData);
                    } catch (Exception e) {
                        result.put("configData", dbConfig.getConfigData());
                    }
                }
            } else {
                result.put("source", "yml");
                result.put("storageType", cloudStorageConfig.getType());
            }
        } catch (Exception e) {
            result.put("source", "yml");
            result.put("storageType", cloudStorageConfig.getType());
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}