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
import com.aliyun.sdk.service.rds20140815.AsyncClient;
import com.aliyun.sdk.service.rds20140815.models.DescribeDBInstancesRequest;
import com.aliyun.sdk.service.rds20140815.models.DescribeDBInstancesResponse;
import com.aliyun.sdk.service.rds20140815.models.DescribeDBInstancesResponseBody.DBInstance;
// 阿里云封装相关导入
import com.ruoyi.framework.aliyun.config.AliyunCredential;
import com.ruoyi.framework.aliyun.service.AliyunService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.system.domain.RdsInstanceInfo;
import com.ruoyi.project.system.mapper.RdsInstanceInfoMapper;
import com.ruoyi.project.system.service.IRdsInstanceInfoService;
import com.ruoyi.project.system.service.ISysSecretKeyService;

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
    
    @Autowired
    private AliyunService aliyunService;
    
    /**
     * 同步阿里云RDS实例数据
     */
    @Override
    public AjaxResult syncAliyunRdsInstances() {
        try {
            // 获取所有可用的阿里云凭证
            List<AliyunCredential> credentials = aliyunService.getAllCredentials();
            
            if (credentials.isEmpty()) {
                return AjaxResult.error("未找到可用的阿里云密钥");
            }
            
            int totalSynced = 0;
            int successCount = 0;
            int errorCount = 0;
            
            // 遍历每个阿里云凭证，同步对应的RDS实例
            for (AliyunCredential credential : credentials) {
                try {
                    int synced = syncRdsInstancesByCredential(credential);
                    totalSynced += synced;
                    successCount++;
                    log.info("密钥: {} 同步成功，同步实例数: {}", credential.getKeyName(), synced);
                } catch (Exception e) {
                    errorCount++;
                    log.error("密钥: {} 同步失败: {}", credential.getKeyName(), e.getMessage(), e);
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
     * 使用指定凭证同步RDS实例
     */
    private int syncRdsInstancesByCredential(AliyunCredential credential) throws Exception {
        // 使用阿里云服务执行RDS操作
        return aliyunService.executeWithCredential("RDS", credential, (AsyncClient client) -> {
            try {
                // 构建请求参数
                DescribeDBInstancesRequest request = DescribeDBInstancesRequest.builder()
                    .regionId(credential.getRegion())
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
                            saveOrUpdateRdsInstance(instance, credential);
                            syncedCount++;
                            log.debug("同步RDS实例: {}", instance.getDBInstanceId());
                        } catch (Exception e) {
                            log.error("保存RDS实例失败: {}, 错误: {}", instance.getDBInstanceId(), e.getMessage());
                        }
                    }
                }
                
                return syncedCount;
                
            } catch (Exception e) {
                log.error("同步RDS实例失败", e);
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * 保存或更新RDS实例信息
     */
    private void saveOrUpdateRdsInstance(
        DBInstance instance, 
        AliyunCredential credential) {
        
        // 查询是否已存在该实例
        QueryWrapper queryWrapper = QueryWrapper.create()
            .eq("db_instance_id", instance.getDBInstanceId())
            .eq("secret_key_id", credential.getSecretKeyId());
        
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
        rdsInstance.setSecretKeyId(credential.getSecretKeyId());
        rdsInstance.setAccessKey(credential.getAccessKeyId());
        rdsInstance.setSecretKey(credential.getAccessKeySecret());
        rdsInstance.setKeyRegion(credential.getRegion());
        rdsInstance.setKeyStatus("0"); // 正常状态
        
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