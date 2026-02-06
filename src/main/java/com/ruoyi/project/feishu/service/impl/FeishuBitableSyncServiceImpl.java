package com.ruoyi.project.feishu.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.project.feishu.domain.dto.DomainCertRecordDto;
import com.ruoyi.project.feishu.domain.dto.FeishuBitablePageResponseDto;
import com.ruoyi.project.feishu.domain.dto.FeishuBitableRecordDto;
import com.ruoyi.project.feishu.service.ICompanyFeishuService;
import com.ruoyi.project.feishu.service.IFeishuBitableSyncService;
import com.ruoyi.project.monitor.domain.DomainCertMonitor;
import com.ruoyi.project.monitor.service.IDomainCertMonitorService;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 飞书多维表格数据同步服务实现类
 * 
 * @author ruoyi
 * @date 2026-02-05
 */
@Slf4j
@Service
public class FeishuBitableSyncServiceImpl implements IFeishuBitableSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(FeishuBitableSyncServiceImpl.class);
    
    @Autowired
    private ICompanyFeishuService companyFeishuService;
    
    @Autowired
    private IDomainCertMonitorService domainCertMonitorService;
    
    // 固定配置参数
    private static final String APP_TOKEN = "T1O7blsfNanfqosMWBvcIWgwnzb";
    private static final String TABLE_ID = "tblrCnUgBgzSMpNq";
    private static final String VIEW_ID = "vewEYjlKYX";
    private static final Integer DEFAULT_PAGE_SIZE = 50;
    
    @Override
    public String syncBitableDataToLocal(String appToken, String tableId, String viewId, Integer pageSize) {
        try {
            logger.info("开始同步飞书多维表格数据到本地数据库");
            long startTime = System.currentTimeMillis();
            
            // 使用固定参数
            appToken = StrUtil.isNotEmpty(appToken) ? appToken : APP_TOKEN;
            tableId = StrUtil.isNotEmpty(tableId) ? tableId : TABLE_ID;
            viewId = StrUtil.isNotEmpty(viewId) ? viewId : VIEW_ID;
            pageSize = pageSize != null ? pageSize : DEFAULT_PAGE_SIZE;
            
            // 1. 获取飞书多维表格数据
            logger.info("正在获取飞书多维表格数据...");
            List<FeishuBitableRecordDto> bitableRecords = getAllBitableRecords(appToken, tableId, viewId, pageSize);
            logger.info("获取到飞书多维表格记录 {} 条", bitableRecords.size());
            
            // 2. 获取本地数据库数据
            logger.info("正在获取本地数据库数据...");
            List<DomainCertMonitor> localRecords = domainCertMonitorService.selectList(null);
            logger.info("获取到本地数据库记录 {} 条", localRecords.size());
            
            // 3. 数据对比和同步
            SyncResult syncResult = compareAndSync(bitableRecords, localRecords, appToken, tableId);
            
            long endTime = System.currentTimeMillis();
            String result = String.format(
                "飞书多维表格数据同步到本地完成！\n" +
                "总耗时: %d ms\n" +
                "新增记录: %d 条\n" +
                "更新记录: %d 条\n" +
                "飞书新增记录: %d 条\n" +
                "处理详情:\n%s",
                (endTime - startTime),
                syncResult.getLocalAdded(),
                syncResult.getLocalUpdated(),
                syncResult.getBitableAdded(),
                String.join("\n", syncResult.getDetails())
            );
            
            logger.info("同步完成: {}", result);
            return result;
            
        } catch (Exception e) {
            logger.error("同步飞书多维表格数据到本地数据库异常", e);
            throw new RuntimeException("同步失败: " + e.getMessage());
        }
    }
    
    @Override
    public String syncLocalDataToBitable(String appToken, String tableId) {
        try {
            logger.info("开始同步本地数据库数据到飞书多维表格");
            long startTime = System.currentTimeMillis();
            
            // 使用固定参数
            appToken = StrUtil.isNotEmpty(appToken) ? appToken : APP_TOKEN;
            tableId = StrUtil.isNotEmpty(tableId) ? tableId : TABLE_ID;
            
            // 1. 获取飞书多维表格数据（使用VIEW_ID确保获取完整数据）
            logger.info("正在获取飞书多维表格数据...");
            List<FeishuBitableRecordDto> bitableRecords = getAllBitableRecords(appToken, tableId, VIEW_ID, DEFAULT_PAGE_SIZE);
            logger.info("获取到飞书多维表格记录 {} 条", bitableRecords.size());
            
            // 2. 获取本地数据库数据
            logger.info("正在获取本地数据库数据...");
            List<DomainCertMonitor> localRecords = domainCertMonitorService.selectList(null);
            logger.info("获取到本地数据库记录 {} 条", localRecords.size());
            
            // 3. 数据对比和同步
            SyncResult syncResult = compareAndSyncForLocalToBitable(bitableRecords, localRecords, appToken, tableId);
            
            long endTime = System.currentTimeMillis();
            String result = String.format(
                "本地数据同步到飞书多维表格完成！\n" +
                "总耗时: %d ms\n" +
                "新增记录: %d 条\n" +
                "更新记录: %d 条\n" +
                "处理详情:\n%s",
                (endTime - startTime),
                syncResult.getBitableAdded(),
                syncResult.getLocalUpdated(),
                String.join("\n", syncResult.getDetails())
            );
            
            logger.info("同步完成: {}", result);
            return result;
            
        } catch (Exception e) {
            logger.error("同步本地数据到飞书多维表格异常", e);
            throw new RuntimeException("同步失败: " + e.getMessage());
        }
    }
    
    @Override
    public String syncBidirectional(String appToken, String tableId, String viewId, Integer pageSize) {
        try {
            logger.info("开始双向同步数据");
            long startTime = System.currentTimeMillis();
            
            // 使用固定参数
            appToken = StrUtil.isNotEmpty(appToken) ? appToken : APP_TOKEN;
            tableId = StrUtil.isNotEmpty(tableId) ? tableId : TABLE_ID;
            viewId = StrUtil.isNotEmpty(viewId) ? viewId : VIEW_ID;
            pageSize = pageSize != null ? pageSize : DEFAULT_PAGE_SIZE;
            
            // 1. 先执行飞书到本地的同步
            String localSyncResult = syncBitableDataToLocal(appToken, tableId, viewId, pageSize);
            
            // 2. 再执行本地到飞书的同步
            String bitableSyncResult = syncLocalDataToBitable(appToken, tableId);
            
            long endTime = System.currentTimeMillis();
            String result = String.format(
                "双向数据同步完成！\n" +
                "总耗时: %d ms\n\n" +
                "=== 飞书到本地同步结果 ===\n%s\n\n" +
                "=== 本地到飞书同步结果 ===\n%s",
                (endTime - startTime),
                localSyncResult,
                bitableSyncResult
            );
            
            logger.info("双向同步完成");
            return result;
            
        } catch (Exception e) {
            logger.error("双向数据同步异常", e);
            throw new RuntimeException("双向同步失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有飞书多维表格记录（处理分页）
     */
    private List<FeishuBitableRecordDto> getAllBitableRecords(String appToken, String tableId, String viewId, Integer pageSize) {
        List<FeishuBitableRecordDto> allRecords = new ArrayList<>();
        boolean hasMore = true;
        
        while (hasMore) {
            try {
                // 查询一页数据
                Object response = companyFeishuService.searchAppTableRecord(
                    appToken, tableId, viewId, pageSize, null);
                
                if (response instanceof FeishuBitablePageResponseDto) {
                    FeishuBitablePageResponseDto pageResponse = (FeishuBitablePageResponseDto) response;
                    
                    if (CollUtil.isNotEmpty(pageResponse.getItems())) {
                        allRecords.addAll(pageResponse.getItems());
                    }
                    
                    hasMore = pageResponse.getHasMore() != null && pageResponse.getHasMore();
                } else {
                    hasMore = false;
                }
                
                // 控制请求频率
                if (hasMore) {
                    Thread.sleep(200);
                }
                
            } catch (Exception e) {
                logger.error("获取飞书多维表格数据失败", e);
                throw new RuntimeException("获取飞书数据失败: " + e.getMessage());
            }
        }
        
        return allRecords;
    }
    
    /**
     * 数据对比和同步（本地到飞书方向）
     */
    private SyncResult compareAndSyncForLocalToBitable(List<FeishuBitableRecordDto> bitableRecords, 
                                                     List<DomainCertMonitor> localRecords,
                                                     String appToken, String tableId) {
        SyncResult result = new SyncResult();
        
        // 打印飞书数据用于调试
        logger.info("===== 开始打印飞书数据 =====");
        for (int i = 0; i < Math.min(3, bitableRecords.size()); i++) {
            FeishuBitableRecordDto record = bitableRecords.get(i);
            logger.info("飞书记录 [{}]: recordId={}", i, record.getRecordId());
            if (record.getFields() != null) {
                logger.info("  fields keys: {}", record.getFields().keySet());
                for (Map.Entry<String, Object> entry : record.getFields().entrySet()) {
                    Object value = entry.getValue();
                    String valueType = value != null ? value.getClass().getSimpleName() : "null";
                    logger.info("  field[{}] = {} (type: {})", entry.getKey(), value, valueType);
                }
            } else {
                logger.info("  fields is null");
            }
        }
        logger.info("===== 飞书数据打印完成 =====");
        
        // 构建飞书域名映射（域名 -> 记录ID）
        Map<String, String> bitableDomainIdMap = bitableRecords.stream()
            .filter(record -> {
                if (record.getFields() == null || !record.getFields().containsKey("域名")) {
                    logger.warn("飞书记录缺少域名字段: recordId={}", record.getRecordId());
                    return false;
                }
                return true;
            })
            .map(record -> {
                Object domainObj = record.getFields().get("域名");
                String domain = extractDomainFromField(domainObj);
                logger.debug("提取域名: domainObj={} (type={}), extracted={}", 
                    domainObj, 
                    domainObj != null ? domainObj.getClass().getSimpleName() : "null",
                    domain);
                if (StrUtil.isBlank(domain)) {
                    logger.warn("飞书记录域名为空: recordId={}, domainObj={}", 
                        record.getRecordId(), domainObj);
                    return null;
                }
                return new Object[] { domain, record.getRecordId() };
            })
            .filter(pair -> pair != null)
            .collect(Collectors.toMap(
                pair -> (String) pair[0],
                pair -> (String) pair[1],
                (existing, replacement) -> {
                    logger.warn("发现重复域名，保留第一个记录ID: {}", existing);
                    return existing;
                }
            ));
        
        logger.info("飞书域名映射构建完成，共 {} 个域名", bitableDomainIdMap.size());
        if (logger.isDebugEnabled()) {
            logger.debug("飞书已有域名: {}", bitableDomainIdMap.keySet());
        }
        
        // 处理本地记录同步到飞书
        int processedCount = 0;
        for (DomainCertMonitor localRecord : localRecords) {
            try {
                String domain = localRecord.getDomain();
                if (StrUtil.isBlank(domain)) {
                    logger.warn("本地记录域名为空，跳过: id={}", localRecord.getId());
                    continue;
                }
                
                DomainCertRecordDto bitableRecord = convertToBitableRecord(localRecord);
                
                if (bitableDomainIdMap.containsKey(domain)) {
                    // 飞书存在该域名记录，更新
                    String recordId = bitableDomainIdMap.get(domain);
                    logger.info("更新飞书记录: {} (recordId: {})", domain, recordId);
                    companyFeishuService.updateAppTableRecord(appToken, tableId, recordId, bitableRecord);
                    result.incrementLocalUpdated();
                    result.addDetail(String.format("更新飞书记录: %s", domain));
                } else {
                    // 飞书不存在该域名记录，新增
                    logger.info("新增飞书记录: {}", domain);
                    companyFeishuService.createAppTableRecord(appToken, tableId, bitableRecord);
                    result.incrementBitableAdded();
                    result.addDetail(String.format("新增飞书记录: %s", domain));
                }
                
                processedCount++;
                // 控制请求频率
                Thread.sleep(100);
            } catch (Exception e) {
                logger.error("同步单条记录失败: {}", localRecord.getDomain(), e);
                result.addDetail(String.format("同步失败: %s - %s", localRecord.getDomain(), e.getMessage()));
            }
        }
        
        logger.info("本地到飞书同步完成，处理 {} 条记录", processedCount);
        return result;
    }
    
    /**
     * 从飞书字段中提取域名字符串
     * 处理可能的数据类型：String、List、Array等
     */
    private String extractDomainFromField(Object domainObj) {
        return extractTextFromField(domainObj);
    }
    
    /**
     * 从飞书字段中提取文本内容
     * 支持：普通文本（String）、多行文本（富文本格式）
     */
    private String extractTextFromField(Object fieldObj) {
        if (fieldObj == null) {
            return null;
        }
        
        // 如果是字符串，直接返回
        if (fieldObj instanceof String) {
            return (String) fieldObj;
        }
        
        // 如果是List（多行文本/富文本格式）
        if (fieldObj instanceof List) {
            List<?> fieldList = (List<?>) fieldObj;
            if (!fieldList.isEmpty()) {
                Object firstElement = fieldList.get(0);
                if (firstElement instanceof String) {
                    return (String) firstElement;
                }
                // 如果是Map类型（飞书富文本格式：[{text: "content", type: "text"}]）
                if (firstElement instanceof Map) {
                    Map<?, ?> elementMap = (Map<?, ?>) firstElement;
                    if (elementMap.containsKey("text")) {
                        Object textValue = elementMap.get("text");
                        return textValue != null ? String.valueOf(textValue) : null;
                    }
                }
            }
        }
        
        // 其他类型转字符串
        return String.valueOf(fieldObj);
    }
    
    /**
     * 数据对比和同步（仅飞书到本地方向 - 只新增不更新）
     */
    private SyncResult compareAndSync(List<FeishuBitableRecordDto> bitableRecords, 
                                    List<DomainCertMonitor> localRecords,
                                    String appToken, String tableId) {
        SyncResult result = new SyncResult();
        
        // 构建域名映射（使用域名+端口作为唯一键）
        Map<String, DomainCertMonitor> localDomainMap = localRecords.stream()
            .filter(record -> StrUtil.isNotBlank(record.getDomain()))
            .collect(Collectors.toMap(
                record -> record.getDomain() + "_" + (record.getPort() != null ? record.getPort() : 443), 
                record -> record, 
                (existing, replacement) -> existing
            ));
        
        Map<String, FeishuBitableRecordDto> bitableDomainMap = bitableRecords.stream()
            .filter(record -> {
                if (record.getFields() == null || !record.getFields().containsKey("域名")) {
                    return false;
                }
                String domain = extractDomainFromField(record.getFields().get("域名"));
                return StrUtil.isNotBlank(domain);
            })
            .collect(Collectors.toMap(
                record -> extractDomainFromField(record.getFields().get("域名")),
                record -> record,
                (existing, replacement) -> existing
            ));
        
        logger.info("飞书到本地同步 - 飞书记录: {} 条, 本地记录: {} 条", 
            bitableDomainMap.size(), localDomainMap.size());
        
        // 处理飞书有但本地没有的记录（新增到本地）
        int addedCount = 0;
        int skippedCount = 0;
        for (Map.Entry<String, FeishuBitableRecordDto> entry : bitableDomainMap.entrySet()) {
            String domain = entry.getKey();
            FeishuBitableRecordDto bitableRecord = entry.getValue();
            
            // 使用域名+默认端口443作为键进行比较
            String localKey = domain + "_443";
            if (!localDomainMap.containsKey(localKey)) {
                try {
                    // 新增到本地
                    DomainCertMonitor localRecord = convertToDomainCertMonitor(bitableRecord);
                    localRecord.setCreateBy("system");
                    localRecord.setDelFlag("0");
                    localRecord.setStatus("0");
                    localRecord.setPort(443); // 默认端口
                    
                    domainCertMonitorService.save(localRecord);
                    result.incrementLocalAdded();
                    result.addDetail(String.format("新增本地记录: %s", domain));
                    addedCount++;
                } catch (Exception e) {
                    logger.error("新增本地记录失败: {}", domain, e);
                    result.addDetail(String.format("新增失败: %s - %s", domain, e.getMessage()));
                }
            } else {
                // 本地已存在，跳过（不更新）
                skippedCount++;
                logger.debug("本地已存在，跳过: {}", domain);
            }
        }
        
        logger.info("飞书到本地同步完成 - 新增: {} 条, 跳过: {} 条", addedCount, skippedCount);
        return result;
    }
    
    /**
     * 将飞书记录转换为本地域名监控记录
     */
    private DomainCertMonitor convertToDomainCertMonitor(FeishuBitableRecordDto bitableRecord) {
        DomainCertMonitor monitor = new DomainCertMonitor();
        
        Map<String, Object> fields = bitableRecord.getFields();
        if (fields != null) {
            // 域名 - 使用统一的提取方法
            if (fields.containsKey("域名")) {
                String domain = extractDomainFromField(fields.get("域名"));
                monitor.setDomain(domain);
            }
            
            // 剩余天数
            if (fields.containsKey("剩余天数")) {
                Object remainingDays = fields.get("剩余天数");
                if (remainingDays instanceof Number) {
                    monitor.setDaysRemaining(((Number) remainingDays).intValue());
                }
            }
            
            // 备注 - 可能是文本或多行文本，使用统一的提取方法
            if (fields.containsKey("备注")) {
                String remark = extractTextFromField(fields.get("备注"));
                monitor.setRemark(remark);
            }
            
            // 过期时间
            if (fields.containsKey("过期时间")) {
                Object expireTime = fields.get("过期时间");
                if (expireTime instanceof Number) {
                    long timestamp = ((Number) expireTime).longValue();
                    monitor.setExpireTime(new Date(timestamp));
                }
            }
        }
        
        monitor.setCreateTime(new Date());
        monitor.setUpdateTime(new Date());
        
        return monitor;
    }
    
    /**
     * 将本地记录转换为飞书记录
     */
    private DomainCertRecordDto convertToBitableRecord(DomainCertMonitor localRecord) {
        DomainCertRecordDto record = new DomainCertRecordDto();
        
        record.setDomain(localRecord.getDomain());
        
        // 备注字段 - 清理可能的错误格式
        String remark = localRecord.getRemark();
        if (remark != null) {
            // 如果备注是错误的富文本格式字符串，提取出纯文本
            // 例如: "[{text=新超阳, type=text}]" -> "新超阳"
            if (remark.startsWith("[{text=") && remark.contains(", type=text}]")) {
                try {
                    // 提取 text= 和 , type 之间的内容
                    int startIndex = remark.indexOf("text=") + 5;
                    int endIndex = remark.indexOf(", type=");
                    if (startIndex > 5 && endIndex > startIndex) {
                        remark = remark.substring(startIndex, endIndex);
                        logger.debug("清理备注格式: domain={}, 原始={}, 清理后={}", 
                            localRecord.getDomain(), localRecord.getRemark(), remark);
                    }
                } catch (Exception e) {
                    logger.warn("清理备注格式失败: domain={}, remark={}", 
                        localRecord.getDomain(), remark, e);
                }
            }
        }
        
        logger.debug("本地记录备注: domain={}, remark={}", 
            localRecord.getDomain(), remark);
        record.setRemark(remark);
        
        if (localRecord.getDaysRemaining() != null) {
            record.setRemainingDays(localRecord.getDaysRemaining().doubleValue());
        }
        
        if (localRecord.getExpireTime() != null) {
            record.setExpireTime(localRecord.getExpireTime().getTime());
        }
        
        return record;
    }
    
    /**
     * 从飞书记录更新本地记录
     */
    private void updateLocalRecordFromBitable(DomainCertMonitor localRecord, FeishuBitableRecordDto bitableRecord) {
        Map<String, Object> fields = bitableRecord.getFields();
        if (fields != null) {
            // 更新备注 - 可能是文本或多行文本，使用统一的提取方法
            if (fields.containsKey("备注")) {
                String remark = extractTextFromField(fields.get("备注"));
                localRecord.setRemark(remark);
            }
            
            // 更新剩余天数
            if (fields.containsKey("剩余天数")) {
                Object remainingDays = fields.get("剩余天数");
                if (remainingDays instanceof Number) {
                    localRecord.setDaysRemaining(((Number) remainingDays).intValue());
                }
            }
            
            // 更新过期时间
            if (fields.containsKey("过期时间")) {
                Object expireTime = fields.get("过期时间");
                if (expireTime instanceof Number) {
                    long timestamp = ((Number) expireTime).longValue();
                    localRecord.setExpireTime(new Date(timestamp));
                }
            }
        }
        
        localRecord.setUpdateTime(new Date());
    }
    
    /**
     * 同步结果统计类
     */
    private static class SyncResult {
        private int localAdded = 0;
        private int localUpdated = 0;
        private int bitableAdded = 0;
        private List<String> details = new ArrayList<>();
        
        public void incrementLocalAdded() {
            localAdded++;
        }
        
        public void incrementLocalUpdated() {
            localUpdated++;
        }
        
        public void incrementBitableAdded() {
            bitableAdded++;
        }
        
        public void addDetail(String detail) {
            details.add(detail);
        }
        
        public int getLocalAdded() {
            return localAdded;
        }
        
        public int getLocalUpdated() {
            return localUpdated;
        }
        
        public int getBitableAdded() {
            return bitableAdded;
        }
        
        public List<String> getDetails() {
            return details;
        }
    }
}