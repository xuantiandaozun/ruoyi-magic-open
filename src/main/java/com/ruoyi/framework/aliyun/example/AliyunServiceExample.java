package com.ruoyi.framework.aliyun.example;

import com.aliyun.oss.OSS;
import com.aliyun.sdk.service.rds20140815.AsyncClient;
import com.aliyun.sdk.service.rds20140815.models.DescribeDBInstancesRequest;
import com.aliyun.sdk.service.rds20140815.models.DescribeDBInstancesResponse;
import com.ruoyi.framework.aliyun.config.AliyunCredential;
import com.ruoyi.framework.aliyun.service.AliyunService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 阿里云服务使用示例
 * 展示如何使用封装后的阿里云SDK
 * 
 * @author ruoyi
 */
@Slf4j
@Service
public class AliyunServiceExample {
    
    @Autowired
    private AliyunService aliyunService;
    
    /**
     * 示例1：使用默认凭证获取RDS实例列表
     */
    public void exampleGetRdsInstancesWithDefaultCredential() {
        try {
            List<String> instanceIds = aliyunService.executeWithDefaultCredential("RDS", (AsyncClient client) -> {
                try {
                    DescribeDBInstancesRequest request = DescribeDBInstancesRequest.builder().build();
                    CompletableFuture<DescribeDBInstancesResponse> future = client.describeDBInstances(request);
                    DescribeDBInstancesResponse response = future.get();
                    
                    return response.getBody().getItems().getDBInstance().stream()
                            .map(instance -> instance.getDBInstanceId())
                            .toList();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            
            log.info("获取到RDS实例: {}", instanceIds);
        } catch (Exception e) {
            log.error("获取RDS实例失败", e);
        }
    }
    
    /**
     * 示例2：使用指定密钥ID操作OSS
     */
    public void exampleUseOssWithSecretKeyId(Long secretKeyId, String bucketName) {
        try {
            List<String> objectKeys = aliyunService.executeWithSecretKeyId("OSS", secretKeyId, (OSS client) -> {
                return client.listObjects(bucketName).getObjectSummaries().stream()
                        .map(summary -> summary.getKey())
                        .toList();
            });
            
            log.info("OSS存储桶 {} 中的对象: {}", bucketName, objectKeys);
        } catch (Exception e) {
            log.error("操作OSS失败", e);
        }
    }
    
    /**
     * 示例3：使用指定区域的凭证
     */
    public void exampleUseRdsWithRegion(String region) {
        try {
            Integer instanceCount = aliyunService.executeWithRegionCredential("RDS", region, (AsyncClient client) -> {
                try {
                    DescribeDBInstancesRequest request = DescribeDBInstancesRequest.builder()
                            .regionId(region)
                            .build();
                    CompletableFuture<DescribeDBInstancesResponse> future = client.describeDBInstances(request);
                    DescribeDBInstancesResponse response = future.get();
                    
                    return response.getBody().getItems().getDBInstance().size();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            
            log.info("区域 {} 中的RDS实例数量: {}", region, instanceCount);
        } catch (Exception e) {
            log.error("获取区域RDS实例失败", e);
        }
    }
    
    /**
     * 示例4：使用所有可用凭证批量操作
     */
    public void exampleBatchOperationWithAllCredentials() {
        try {
            List<Integer> instanceCounts = aliyunService.executeWithAllCredentials("RDS", (AsyncClient client) -> {
                try {
                    DescribeDBInstancesRequest request = DescribeDBInstancesRequest.builder().build();
                    CompletableFuture<DescribeDBInstancesResponse> future = client.describeDBInstances(request);
                    DescribeDBInstancesResponse response = future.get();
                    
                    return response.getBody().getItems().getDBInstance().size();
                } catch (Exception e) {
                    log.warn("获取RDS实例失败", e);
                    return 0;
                }
            });
            
            int totalInstances = instanceCounts.stream().mapToInt(Integer::intValue).sum();
            log.info("所有账号的RDS实例总数: {}", totalInstances);
        } catch (Exception e) {
            log.error("批量操作失败", e);
        }
    }
    
    /**
     * 示例5：直接获取客户端进行复杂操作
     */
    public void exampleGetClientDirectly() {
        try {
            List<AliyunCredential> credentials = aliyunService.getAllCredentials();
            if (!credentials.isEmpty()) {
                AliyunCredential credential = credentials.get(0);
                
                // 获取RDS客户端
                AsyncClient rdsClient = aliyunService.getClient("RDS", credential);
                
                // 获取OSS客户端
                OSS ossClient = aliyunService.getClient("OSS", credential);
                
                // 进行复杂的业务操作...
                log.info("获取到客户端，可以进行复杂操作");
            }
        } catch (Exception e) {
            log.error("获取客户端失败", e);
        }
    }
    
    /**
     * 示例6：管理操作
     */
    public void exampleManagementOperations() {
        // 获取支持的服务类型
        List<String> serviceTypes = aliyunService.getSupportedServiceTypes();
        log.info("支持的服务类型: {}", serviceTypes);
        
        // 获取所有可用凭证
        List<AliyunCredential> credentials = aliyunService.getAllCredentials();
        log.info("可用凭证数量: {}", credentials.size());
        
        // 刷新凭证（重新从数据库加载）
        aliyunService.refreshCredentials();
        
        // 清理客户端缓存
        aliyunService.clearClientCache();
    }
}