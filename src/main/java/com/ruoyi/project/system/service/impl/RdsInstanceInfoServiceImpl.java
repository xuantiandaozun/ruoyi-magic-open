package com.ruoyi.project.system.service.impl;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// 阿里云RDS SDK相关导入
import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.rds20140815.AsyncClient;
import com.aliyun.sdk.service.rds20140815.models.DescribeDBInstancesRequest;
import com.aliyun.sdk.service.rds20140815.models.DescribeDBInstancesResponse;
import com.aliyun.sdk.service.rds20140815.models.DescribeDBInstancesResponseBody.DBInstance;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.system.domain.RdsInstanceInfo;
import com.ruoyi.project.system.domain.SysSecretKey;
import com.ruoyi.project.system.mapper.RdsInstanceInfoMapper;
import com.ruoyi.project.system.service.IRdsInstanceInfoService;
import com.ruoyi.project.system.service.ISysSecretKeyService;

import darabonba.core.client.ClientOverrideConfiguration;

/**
 * RDS实例管理Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-07-11 17:49:40
 */
@Service
public class RdsInstanceInfoServiceImpl extends ServiceImpl<RdsInstanceInfoMapper, RdsInstanceInfo> implements IRdsInstanceInfoService
{
    private static final Logger log = LoggerFactory.getLogger(RdsInstanceInfoServiceImpl.class);
    
    @Autowired
    private ISysSecretKeyService sysSecretKeyService;
    
    /**
     * 同步阿里云RDS实例数据
     */
    @Override
    public AjaxResult syncAliyunRdsInstances() {
        try {
            // 查询所有阿里云密钥
            QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("provider_brand", "aliyun")
                .eq("status", "0")
                .eq("del_flag", "0");
            List<SysSecretKey> aliyunKeys = sysSecretKeyService.list(queryWrapper);
            
            if (aliyunKeys.isEmpty()) {
                return AjaxResult.error("未找到可用的阿里云密钥");
            }
            
            int totalSynced = 0;
            int successCount = 0;
            int errorCount = 0;
            
            // 遍历每个阿里云密钥，同步对应的RDS实例
            for (SysSecretKey secretKey : aliyunKeys) {
                try {
                    int synced = syncRdsInstancesByKey(secretKey);
                    totalSynced += synced;
                    successCount++;
                    log.info("密钥ID: {} 同步成功，同步实例数: {}", secretKey.getId(), synced);
                } catch (Exception e) {
                    errorCount++;
                    log.error("密钥ID: {} 同步失败: {}", secretKey.getId(), e.getMessage(), e);
                }
            }
            
            String message = String.format("同步完成！总计同步 %d 个RDS实例，成功密钥数: %d，失败密钥数: %d", 
                totalSynced, successCount, errorCount);
            
            return AjaxResult.success(message);
            
        } catch (Exception e) {
            log.error("同步阿里云RDS实例失败", e);
            return AjaxResult.error("同步失败: " + e.getMessage());
        }
    }
    
    /**
     * 使用指定密钥同步RDS实例
     */
    private int syncRdsInstancesByKey(SysSecretKey secretKey) throws Exception {
        // 配置阿里云客户端
        StaticCredentialProvider provider = StaticCredentialProvider.create(Credential.builder()
            .accessKeyId(secretKey.getAccessKey())
            .accessKeySecret(secretKey.getSecretKey())
            .build());
        
        String region = secretKey.getRegion() != null ? secretKey.getRegion() : "cn-hangzhou";
        
        AsyncClient client = AsyncClient.builder()
            .region(region)
            .credentialsProvider(provider)
            .overrideConfiguration(
                ClientOverrideConfiguration.create()
                    .setEndpointOverride("rds." + region + ".aliyuncs.com")
            )
            .build();
        
        try {
            // 构建请求参数
            DescribeDBInstancesRequest request = DescribeDBInstancesRequest.builder()
                .regionId(region)
                .build();
            
            // 同步获取RDS实例列表
            CompletableFuture<DescribeDBInstancesResponse> future = client.describeDBInstances(request);
            DescribeDBInstancesResponse response = future.get();
            
            int syncedCount = 0;
            
            if (response.getBody() != null && response.getBody().getItems() != null) {
                // 修正：使用正确的方法名 getDBInstance()
                List<DBInstance> dbInstance = response.getBody().getItems().getDBInstance();
                
                for (DBInstance instance : dbInstance) {
                    try {
                        saveOrUpdateRdsInstance(instance, secretKey);
                        syncedCount++;
                    } catch (Exception e) {
                        log.error("保存RDS实例失败: {}", instance.getDBInstanceId(), e);
                    }
                }
            }
            
            return syncedCount;
            
        } finally {
            client.close();
        }
    }
    
    /**
     * 保存或更新RDS实例信息
     */
    private void saveOrUpdateRdsInstance(
        DBInstance instance, 
        SysSecretKey secretKey) {
        
        // 查询是否已存在该实例
        QueryWrapper queryWrapper = QueryWrapper.create()
            .eq("db_instance_id", instance.getDBInstanceId())
            .eq("secret_key_id", secretKey.getId());
        
        RdsInstanceInfo existingInstance = this.getOne(queryWrapper);
        
        RdsInstanceInfo rdsInstance;
        if (existingInstance != null) {
            rdsInstance = existingInstance;
        } else {
            rdsInstance = new RdsInstanceInfo();
            rdsInstance.setCreateTime(new Date());
        }
        
        // 设置实例信息
        rdsInstance.setDbInstanceId(instance.getDBInstanceId());
        rdsInstance.setDbInstanceDescription(instance.getDBInstanceDescription());
        rdsInstance.setEngine(instance.getEngine());
        rdsInstance.setEngineVersion(instance.getEngineVersion());
        rdsInstance.setDbInstanceClass(instance.getDBInstanceClass());
        rdsInstance.setDbInstanceCpu(instance.getDBInstanceCPU());
        rdsInstance.setDbInstanceMemory(instance.getDBInstanceMemory());
        rdsInstance.setDbInstanceStorageType(instance.getDBInstanceStorageType());
        rdsInstance.setCategory(instance.getCategory());
        rdsInstance.setDbInstanceStatus(instance.getDBInstanceStatus());
        rdsInstance.setDbInstanceType(instance.getDBInstanceType());
        rdsInstance.setConnectionMode(instance.getConnectionMode());
        rdsInstance.setConnectionString(instance.getConnectionString());
        rdsInstance.setPayType(instance.getPayType());
        rdsInstance.setLockMode(instance.getLockMode());
        rdsInstance.setLockReason(instance.getLockReason());
        rdsInstance.setDeletionProtection(String.valueOf(instance.getDeletionProtection()));
        rdsInstance.setInstanceCreateTime(parseAliyunDateTime(instance.getCreateTime()));
        rdsInstance.setExpireTime(parseAliyunDateTime(instance.getExpireTime()));
        rdsInstance.setDestroyTime(parseAliyunDateTime(instance.getDestroyTime()));
        rdsInstance.setMasterInstanceId(instance.getMasterInstanceId());
        rdsInstance.setGuardDbInstanceId(instance.getGuardDBInstanceId());
        rdsInstance.setTempDbInstanceId(instance.getTempDBInstanceId());
        
        // 设置密钥相关信息
        rdsInstance.setSecretKeyId(secretKey.getId());
        rdsInstance.setAccessKey(secretKey.getAccessKey());
        rdsInstance.setSecretKey(secretKey.getSecretKey());
        rdsInstance.setKeyRegion(secretKey.getRegion());
        rdsInstance.setKeyStatus(secretKey.getStatus());
        
        rdsInstance.setUpdateTime(new Date());
        rdsInstance.setDelFlag("0");
        
        // 保存或更新
        this.saveOrUpdate(rdsInstance);
    }
    
    /**
     * 解析阿里云返回的日期时间字符串
     * 支持ISO 8601格式（如：2024-12-09T01:21:43Z）和时间戳格式
     */
    private Date parseAliyunDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 尝试解析ISO 8601格式（如：2024-12-09T01:21:43Z）
            if (dateTimeStr.contains("T") && dateTimeStr.endsWith("Z")) {
                Instant instant = Instant.parse(dateTimeStr);
                return Date.from(instant);
            }
            // 尝试解析时间戳格式（秒）
            else {
                long timestamp = Long.parseLong(dateTimeStr);
                return new Date(timestamp * 1000);
            }
        } catch (DateTimeParseException | NumberFormatException e) {
            log.warn("无法解析日期时间字符串: {}, 错误: {}", dateTimeStr, e.getMessage());
            return null;
        }
    }
}