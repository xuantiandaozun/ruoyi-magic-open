package com.ruoyi.project.feishu.service;

/**
 * 飞书多维表格数据同步服务接口
 * 
 * @author ruoyi
 * @date 2026-02-05
 */
public interface IFeishuBitableSyncService {
    
    /**
     * 同步飞书多维表格数据到本地数据库
     * 
     * @param appToken 多维表格应用token
     * @param tableId 数据表ID
     * @param viewId 视图ID（可选）
     * @param pageSize 分页大小，默认50
     * @return 同步结果信息
     */
    String syncBitableDataToLocal(String appToken, String tableId, String viewId, Integer pageSize);
    
    /**
     * 同步本地数据库数据到飞书多维表格
     * 
     * @param appToken 多维表格应用token
     * @param tableId 数据表ID
     * @return 同步结果信息
     */
    String syncLocalDataToBitable(String appToken, String tableId);
    
    /**
     * 双向同步：本地与飞书多维表格数据同步
     * 
     * @param appToken 多维表格应用token
     * @param tableId 数据表ID
     * @param viewId 视图ID（可选）
     * @param pageSize 分页大小，默认50
     * @return 同步结果信息
     */
    String syncBidirectional(String appToken, String tableId, String viewId, Integer pageSize);
}