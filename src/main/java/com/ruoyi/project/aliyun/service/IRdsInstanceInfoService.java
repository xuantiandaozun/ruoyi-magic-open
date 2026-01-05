package com.ruoyi.project.aliyun.service;

import com.mybatisflex.core.service.IService;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.aliyun.domain.RdsInstanceInfo;

/**
 * RDS实例管理Service接口
 * 
 * @author ruoyi
 * @date 2025-07-11 17:49:40
 */
public interface IRdsInstanceInfoService extends IService<RdsInstanceInfo>
{
    /**
     * 同步阿里云RDS实例数据
     * 
     * @return 同步结果
     */
    AjaxResult syncAliyunRdsInstances();
    
    /**
     * 获取RDS实例连接信息
     * 
     * @param dbInstanceId RDS实例ID
     * @return 连接信息
     */
    AjaxResult getRdsInstanceNetInfo(String dbInstanceId);
    
    /**
     * 获取RDS实例白名单信息
     * 
     * @param dbInstanceId RDS实例ID
     * @return 白名单信息
     */
    AjaxResult getRdsInstanceIPArrayList(String dbInstanceId);
    
    /**
     * 修改RDS实例白名单
     * 
     * @param dbInstanceId RDS实例ID
     * @param securityIps 安全IP列表，多个IP用逗号分隔
     * @param dbInstanceIPArrayName 白名单组名称，可选，默认为"default"
     * @return 修改结果
     */
    AjaxResult modifyRdsInstanceSecurityIps(String dbInstanceId, String securityIps, String dbInstanceIPArrayName);
    
    /**
     * 批量更新所有RDS实例的客户端白名单
     * 获取客户端IP并设置到所有RDS实例的client分组中
     * 
     * @param clientIp 客户端IP地址
     * @return 更新结果
     */
    AjaxResult updateAllRdsClientWhitelist(String clientIp);
}