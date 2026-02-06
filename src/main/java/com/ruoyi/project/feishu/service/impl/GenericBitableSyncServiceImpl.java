package com.ruoyi.project.feishu.service.impl;

import com.ruoyi.project.feishu.config.BitableConfig;
import com.ruoyi.project.feishu.core.BitableEntityConverter;
import com.ruoyi.project.feishu.domain.dto.FeishuBitablePageResponseDto;
import com.ruoyi.project.feishu.domain.dto.FeishuBitableRecordDto;
import com.ruoyi.project.feishu.service.ICompanyFeishuService;
import com.ruoyi.project.feishu.service.IGenericBitableSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通用飞书多维表格同步服务实现
 * 
 * @author ruoyi
 * @date 2026-02-06
 */
@Slf4j
@Service
public class GenericBitableSyncServiceImpl implements IGenericBitableSyncService {
    
    @Autowired
    private ICompanyFeishuService companyFeishuService;
    
    @Override
    public <T> SyncResult<T> syncFromFeishu(BitableConfig config, Class<T> entityClass,
                                            Function<T, Boolean> saveFunction,
                                            Function<T, T> existCheckFunction) {
        SyncResult<T> result = new SyncResult<>();
        
        try {
            log.info("开始从飞书同步数据：config={}, entity={}", config.getName(), entityClass.getSimpleName());
            
            // 1. 获取飞书所有记录
            List<FeishuBitableRecordDto> feishuRecords = fetchAllFeishuRecords(config);
            log.info("获取到飞书记录 {} 条", feishuRecords.size());
            
            // 2. 转换为实体并处理
            for (FeishuBitableRecordDto record : feishuRecords) {
                try {
                    // 转换为实体
                    T entity = BitableEntityConverter.convertToEntity(record.getFields(), entityClass, config);

                    if (entity == null) {
                        result.incrementFailed();
                        result.addDetail("转换失败: recordId=" + record.getRecordId());
                        continue;
                    }

                    // 设置飞书recordId到实体（用于后续更新）
                    setFeishuRecordId(entity, record.getRecordId());

                    // 检查是否已存在
                    T existing = existCheckFunction.apply(entity);
                    
                    if (existing == null) {
                        // 新增
                        Boolean saved = saveFunction.apply(entity);
                        if (saved != null && saved) {
                            result.incrementAdded();
                            result.addSuccessEntity(entity);
                            result.addDetail("新增: " + extractKeyValue(entity, config));
                        } else {
                            result.incrementFailed();
                            result.addFailedEntity(entity);
                            result.addDetail("新增失败: " + extractKeyValue(entity, config));
                        }
                    } else {
                        // 已存在，跳过或更新（根据需求）
                        result.incrementSkipped();
                        result.addDetail("跳过（已存在）: " + extractKeyValue(entity, config));
                    }
                    
                    // 控制请求频率
                    Thread.sleep(config.getRequestInterval());
                    
                } catch (Exception e) {
                    log.error("处理飞书记录失败: recordId={}", record.getRecordId(), e);
                    result.incrementFailed();
                    result.addDetail("处理失败: recordId=" + record.getRecordId() + " - " + e.getMessage());
                }
            }
            
            log.info("飞书到本地同步完成: {}", result);
            
        } catch (Exception e) {
            log.error("同步飞书数据到本地异常", e);
            throw new RuntimeException("同步失败: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public <T> SyncResult<T> syncToFeishu(BitableConfig config, List<T> localEntities,
                                          Function<T, String> recordIdExtractor) {
        SyncResult<T> result = new SyncResult<>();
        
        try {
            log.info("开始同步本地数据到飞书：config={}, entities={}", 
                config.getName(), localEntities.size());
            
            // 1. 获取飞书现有记录
            List<FeishuBitableRecordDto> feishuRecords = fetchAllFeishuRecords(config);
            
            // 2. 构建主键映射
            Map<Object, String> feishuKeyMap = buildPrimaryKeyMap(feishuRecords, config);
            
            // 3. 处理本地实体
            for (T entity : localEntities) {
                try {
                    Object primaryKey = BitableEntityConverter.extractPrimaryKey(entity, config);
                    
                    if (primaryKey == null) {
                        log.warn("实体主键为空，跳过: {}", entity);
                        result.incrementFailed();
                        result.addFailedEntity(entity);
                        continue;
                    }
                    
                    String existingRecordId = feishuKeyMap.get(primaryKey);
                    
                    if (existingRecordId != null) {
                        // 更新
                        boolean success = updateRecord(config, existingRecordId, entity);
                        if (success) {
                            result.incrementUpdated();
                            result.addSuccessEntity(entity);
                            result.addDetail("更新: " + primaryKey);
                        } else {
                            result.incrementFailed();
                            result.addFailedEntity(entity);
                            result.addDetail("更新失败: " + primaryKey);
                        }
                    } else {
                        // 新增
                        String recordId = createRecord(config, entity);
                        if (recordId != null) {
                            // 设置recordId到实体
                            setFeishuRecordId(entity, recordId);
                            result.incrementAdded();
                            result.addSuccessEntity(entity);
                            result.addDetail("新增: " + primaryKey);
                        } else {
                            result.incrementFailed();
                            result.addFailedEntity(entity);
                            result.addDetail("新增失败: " + primaryKey);
                        }
                    }
                    
                    // 控制频率
                    Thread.sleep(config.getRequestInterval());
                    
                } catch (Exception e) {
                    log.error("同步实体到飞书失败", e);
                    result.incrementFailed();
                    result.addFailedEntity(entity);
                    result.addDetail("同步失败: " + e.getMessage());
                }
            }
            
            log.info("本地到飞书同步完成: {}", result);
            
        } catch (Exception e) {
            log.error("同步本地数据到飞书异常", e);
            throw new RuntimeException("同步失败: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public <T> SyncResult<T> syncBidirectional(BitableConfig config, Class<T> entityClass,
                                               List<T> localEntities,
                                               Function<T, Boolean> saveFunction,
                                               Function<T, T> existCheckFunction,
                                               Function<T, String> recordIdExtractor) {
        log.info("开始双向同步: {}", config.getName());
        
        // 先飞书到本地
        SyncResult<T> fromFeishuResult = syncFromFeishu(config, entityClass, saveFunction, existCheckFunction);
        
        // 再本地到飞书
        SyncResult<T> toFeishuResult = syncToFeishu(config, localEntities, recordIdExtractor);
        
        // 合并结果
        SyncResult<T> combined = new SyncResult<>();
        // 简化处理，实际可以合并更多统计
        
        log.info("双向同步完成");
        return fromFeishuResult;
    }
    
    @Override
    public <T> T queryByPrimaryKey(BitableConfig config, Object primaryKeyValue, Class<T> entityClass) {
        try {
            // 构建过滤条件查询
            Object response = companyFeishuService.searchAppTableRecord(
                config.getAppToken(),
                config.getTableId(),
                config.getViewId(),
                1,
                String.valueOf(primaryKeyValue)
            );
            
            if (response instanceof FeishuBitablePageResponseDto) {
                FeishuBitablePageResponseDto pageResponse = (FeishuBitablePageResponseDto) response;
                if (pageResponse.getItems() != null && !pageResponse.getItems().isEmpty()) {
                    FeishuBitableRecordDto record = pageResponse.getItems().get(0);
                    return BitableEntityConverter.convertToEntity(record.getFields(), entityClass, config);
                }
            }
            
        } catch (Exception e) {
            log.error("根据主键查询飞书记录失败", e);
        }
        
        return null;
    }
    
    @Override
    public <T> String createRecord(BitableConfig config, T entity) {
        try {
            Map<String, Object> fields = BitableEntityConverter.convertToFeishuFields(entity, config);
            
            log.info("创建飞书记录，字段: {}", fields.keySet());
            
            Object response = companyFeishuService.createAppTableRecord(
                config.getAppToken(),
                config.getTableId(),
                fields
            );
            
            // 从响应中提取recordId
            if (response != null) {
                log.info("创建飞书记录成功");
                return extractRecordIdFromResponse(response);
            }
            
        } catch (Exception e) {
            log.error("创建飞书记录失败", e);
        }
        
        return null;
    }
    
    @Override
    public <T> boolean updateRecord(BitableConfig config, String recordId, T entity) {
        try {
            Map<String, Object> fields = BitableEntityConverter.convertToFeishuFields(entity, config);
            
            log.info("更新飞书记录，recordId={}, 字段: {}", recordId, fields.keySet());
            
            Object response = companyFeishuService.updateAppTableRecord(
                config.getAppToken(),
                config.getTableId(),
                recordId,
                fields
            );
            
            return response != null;
            
        } catch (Exception e) {
            log.error("更新飞书记录失败: recordId={}", recordId, e);
            return false;
        }
    }
    
    @Override
    public <T> List<String> batchCreateRecords(BitableConfig config, List<T> entities) {
        List<String> recordIds = new ArrayList<>();
        for (T entity : entities) {
            String recordId = createRecord(config, entity);
            if (recordId != null) {
                recordIds.add(recordId);
            }
            try {
                Thread.sleep(config.getRequestInterval());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return recordIds;
    }
    
    @Override
    public <T> Map<String, Boolean> batchUpdateRecords(BitableConfig config, Map<String, T> recordIdEntityMap) {
        Map<String, Boolean> results = new HashMap<>();
        for (Map.Entry<String, T> entry : recordIdEntityMap.entrySet()) {
            boolean success = updateRecord(config, entry.getKey(), entry.getValue());
            results.put(entry.getKey(), success);
            try {
                Thread.sleep(config.getRequestInterval());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return results;
    }
    
    @Override
    public boolean deleteRecord(BitableConfig config, String recordId) {
        // TODO: 需要实现删除API
        log.warn("删除飞书记录功能暂未实现");
        return false;
    }
    
    /**
     * 获取所有飞书记录
     */
    private List<FeishuBitableRecordDto> fetchAllFeishuRecords(BitableConfig config) {
        List<FeishuBitableRecordDto> allRecords = new ArrayList<>();
        boolean hasMore = true;
        String pageToken = null;
        
        while (hasMore) {
            try {
                Object response = companyFeishuService.searchAppTableRecord(
                    config.getAppToken(),
                    config.getTableId(),
                    config.getViewId(),
                    config.getPageSize(),
                    null
                );
                
                if (response instanceof FeishuBitablePageResponseDto) {
                    FeishuBitablePageResponseDto pageResponse = (FeishuBitablePageResponseDto) response;
                    
                    if (pageResponse.getItems() != null) {
                        allRecords.addAll(pageResponse.getItems());
                    }
                    
                    hasMore = pageResponse.getHasMore() != null && pageResponse.getHasMore();
                    pageToken = pageResponse.getPageToken();
                    
                    // 控制频率
                    if (hasMore) {
                        Thread.sleep(200);
                    }
                } else {
                    hasMore = false;
                }
                
            } catch (Exception e) {
                log.error("获取飞书记录失败", e);
                throw new RuntimeException("获取飞书数据失败: " + e.getMessage());
            }
        }
        
        return allRecords;
    }
    
    /**
     * 构建主键映射（主键值 -> recordId）
     */
    private Map<Object, String> buildPrimaryKeyMap(List<FeishuBitableRecordDto> records, BitableConfig config) {
        Map<Object, String> map = new HashMap<>();
        
        for (FeishuBitableRecordDto record : records) {
            try {
                Object primaryKey = extractPrimaryKeyFromRecord(record, config);
                if (primaryKey != null) {
                    map.put(primaryKey, record.getRecordId());
                }
            } catch (Exception e) {
                log.warn("提取主键失败: recordId={}", record.getRecordId());
            }
        }
        
        return map;
    }
    
    /**
     * 从飞书记录提取主键值
     */
    private Object extractPrimaryKeyFromRecord(FeishuBitableRecordDto record, BitableConfig config) {
        if (record.getFields() == null) {
            return null;
        }
        
        // 从配置获取主键字段名
        if (config != null && config.getPrimaryMapping() != null) {
            String feishuFieldName = config.getPrimaryMapping().getFeishuFieldName();
            Object value = record.getFields().get(feishuFieldName);
            if (value != null) {
                // 处理富文本格式
                if (value instanceof List && !((List<?>) value).isEmpty()) {
                    Object first = ((List<?>) value).get(0);
                    if (first instanceof Map) {
                        return ((Map<?, ?>) first).get("text");
                    }
                    return first;
                }
                return value;
            }
        }
        
        return null;
    }
    
    /**
     * 从实体提取键值（用于日志）
     */
    private <T> String extractKeyValue(T entity, BitableConfig config) {
        Object primaryKey = BitableEntityConverter.extractPrimaryKey(entity, config);
        return primaryKey != null ? String.valueOf(primaryKey) : entity.toString();
    }
    
    /**
     * 从响应中提取recordId
     */
    private String extractRecordIdFromResponse(Object response) {
        // 使用反射获取recordId
        try {
            java.lang.reflect.Method method = response.getClass().getMethod("getRecordId");
            return (String) method.invoke(response);
        } catch (Exception e) {
            log.warn("从响应提取recordId失败", e);
            return null;
        }
    }

    /**
     * 设置实体的飞书recordId（通过反射设置feishuRecordId字段）
     */
    private <T> void setFeishuRecordId(T entity, String recordId) {
        if (entity == null || recordId == null) {
            return;
        }
        try {
            java.lang.reflect.Field field = findField(entity.getClass(), "feishuRecordId");
            if (field != null) {
                field.setAccessible(true);
                field.set(entity, recordId);
            }
        } catch (Exception e) {
            log.debug("设置feishuRecordId失败（实体可能没有该字段）", e);
        }
    }

    /**
     * 查找字段（包括父类）
     */
    private java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}
