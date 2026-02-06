package com.ruoyi.project.feishu.service;

import com.ruoyi.project.feishu.config.BitableConfig;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 通用飞书多维表格同步服务接口
 * 支持任意实体类的双向同步
 * 
 * @author ruoyi
 * @date 2026-02-06
 */
public interface IGenericBitableSyncService {
    
    /**
     * 从飞书同步数据到本地
     * 
     * @param config 多维表格配置
     * @param entityClass 实体类
     * @param saveFunction 保存实体的函数
     * @param existCheckFunction 检查实体是否存在的函数（根据主键）
     * @param <T> 实体类型
     * @return 同步结果
     */
    <T> SyncResult<T> syncFromFeishu(BitableConfig config, Class<T> entityClass,
                                     Function<T, Boolean> saveFunction,
                                     Function<T, T> existCheckFunction);
    
    /**
     * 从本地同步数据到飞书
     * 
     * @param config 多维表格配置
     * @param localEntities 本地实体列表
     * @param recordIdExtractor 从实体提取飞书recordId的函数
     * @param <T> 实体类型
     * @return 同步结果
     */
    <T> SyncResult<T> syncToFeishu(BitableConfig config, List<T> localEntities,
                                   Function<T, String> recordIdExtractor);
    
    /**
     * 双向同步
     * 
     * @param config 多维表格配置
     * @param entityClass 实体类
     * @param localEntities 本地实体列表
     * @param saveFunction 保存实体函数
     * @param existCheckFunction 检查存在函数
     * @param recordIdExtractor 提取recordId函数
     * @param <T> 实体类型
     * @return 同步结果
     */
    <T> SyncResult<T> syncBidirectional(BitableConfig config, Class<T> entityClass,
                                        List<T> localEntities,
                                        Function<T, Boolean> saveFunction,
                                        Function<T, T> existCheckFunction,
                                        Function<T, String> recordIdExtractor);
    
    /**
     * 根据主键值从飞书查询记录
     * 
     * @param config 配置
     * @param primaryKeyValue 主键值
     * @param entityClass 实体类
     * @return 实体对象
     */
    <T> T queryByPrimaryKey(BitableConfig config, Object primaryKeyValue, Class<T> entityClass);
    
    /**
     * 创建飞书记录
     * 
     * @param config 配置
     * @param entity 实体对象
     * @return 飞书recordId
     */
    <T> String createRecord(BitableConfig config, T entity);
    
    /**
     * 更新飞书记录
     * 
     * @param config 配置
     * @param recordId 飞书记录ID
     * @param entity 实体对象
     * @return 是否成功
     */
    <T> boolean updateRecord(BitableConfig config, String recordId, T entity);
    
    /**
     * 批量创建飞书记录
     * 
     * @param config 配置
     * @param entities 实体列表
     * @return 创建的recordId列表
     */
    <T> List<String> batchCreateRecords(BitableConfig config, List<T> entities);
    
    /**
     * 批量更新飞书记录
     * 
     * @param config 配置
     * @param recordIdEntityMap recordId与实体的映射
     * @return 更新结果
     */
    <T> Map<String, Boolean> batchUpdateRecords(BitableConfig config, Map<String, T> recordIdEntityMap);
    
    /**
     * 删除飞书记录
     * 
     * @param config 配置
     * @param recordId 飞书记录ID
     * @return 是否成功
     */
    boolean deleteRecord(BitableConfig config, String recordId);
    
    /**
     * 同步结果类
     */
    class SyncResult<T> {
        private int added;
        private int updated;
        private int skipped;
        private int failed;
        private List<String> details;
        private List<T> successEntities;
        private List<T> failedEntities;
        
        public SyncResult() {
            this.details = new java.util.ArrayList<>();
            this.successEntities = new java.util.ArrayList<>();
            this.failedEntities = new java.util.ArrayList<>();
        }
        
        public void incrementAdded() {
            this.added++;
        }
        
        public void incrementUpdated() {
            this.updated++;
        }
        
        public void incrementSkipped() {
            this.skipped++;
        }
        
        public void incrementFailed() {
            this.failed++;
        }
        
        public void addDetail(String detail) {
            this.details.add(detail);
        }
        
        public void addSuccessEntity(T entity) {
            this.successEntities.add(entity);
        }
        
        public void addFailedEntity(T entity) {
            this.failedEntities.add(entity);
        }
        
        // Getters
        public int getAdded() { return added; }
        public int getUpdated() { return updated; }
        public int getSkipped() { return skipped; }
        public int getFailed() { return failed; }
        public List<String> getDetails() { return details; }
        public List<T> getSuccessEntities() { return successEntities; }
        public List<T> getFailedEntities() { return failedEntities; }
        
        @Override
        public String toString() {
            return String.format("同步完成：新增 %d, 更新 %d, 跳过 %d, 失败 %d", 
                added, updated, skipped, failed);
        }
    }
}
