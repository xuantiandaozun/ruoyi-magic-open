package com.ruoyi.project.feishu.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.project.feishu.annotation.FieldType;
import com.ruoyi.project.feishu.config.BitableConfig;
import com.ruoyi.project.feishu.config.BitableFieldMapping;
import com.ruoyi.project.feishu.service.IGenericBitableSyncService;
import com.ruoyi.project.feishu.service.IFeishuBitableSyncService;
import com.ruoyi.project.monitor.domain.DomainCertMonitor;
import com.ruoyi.project.monitor.service.IDomainCertMonitorService;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 飞书多维表格数据同步服务实现类（使用通用化框架重构）
 * 
 * @author ruoyi
 * @date 2026-02-06
 */
@Slf4j
@Service
public class FeishuBitableSyncServiceImpl implements IFeishuBitableSyncService {

    @Autowired
    private IGenericBitableSyncService genericBitableSyncService;

    @Autowired
    private IDomainCertMonitorService domainCertMonitorService;

    // 固定配置参数
    private static final String APP_TOKEN = "T1O7blsfNanfqosMWBvcIWgwnzb";
    private static final String TABLE_ID = "tblrCnUgBgzSMpNq";
    private static final String VIEW_ID = "vewEYjlKYX";
    private static final Integer DEFAULT_PAGE_SIZE = 50;

    /**
     * 获取域名证书监控配置
     * 注意：只配置飞书表格中实际存在的字段
     */
    private BitableConfig getDomainCertConfig(String appToken, String tableId, String viewId) {
        return new BitableConfig()
            .setName("域名证书监控")
            .setAppToken(StrUtil.isNotEmpty(appToken) ? appToken : APP_TOKEN)
            .setTableId(StrUtil.isNotEmpty(tableId) ? tableId : TABLE_ID)
            .setViewId(StrUtil.isNotEmpty(viewId) ? viewId : VIEW_ID)
            .setPageSize(DEFAULT_PAGE_SIZE)
            .setRequestInterval(100L)
            // 主键字段配置
            .setPrimaryField("domain")
            // 字段映射配置 - 只配置飞书表格中实际存在的字段
            .addFieldMapping(new BitableFieldMapping("域名", "domain", FieldType.TEXT).setPrimary(true))
            .addFieldMapping(new BitableFieldMapping("备注", "remark", FieldType.TEXT))
            .addFieldMapping(new BitableFieldMapping("剩余天数", "daysRemaining", FieldType.NUMBER))
            .addFieldMapping(new BitableFieldMapping("过期时间", "expireTime", FieldType.DATE));
            // 注意：如果飞书表格还有其他字段（如端口），需要在这里添加
    }

    @Override
    public String syncBitableDataToLocal(String appToken, String tableId, String viewId, Integer pageSize) {
        try {
            log.info("开始同步飞书多维表格数据到本地数据库（使用通用化框架）");
            long startTime = System.currentTimeMillis();

            BitableConfig config = getDomainCertConfig(appToken, tableId, viewId);
            if (pageSize != null) {
                config.setPageSize(pageSize);
            }

            // 使用通用化服务从飞书同步到本地
            IGenericBitableSyncService.SyncResult<DomainCertMonitor> result = 
                genericBitableSyncService.syncFromFeishu(
                    config,
                    DomainCertMonitor.class,
                    this::saveDomainCertMonitor,
                    this::checkDomainCertExists
                );

            long endTime = System.currentTimeMillis();
            return String.format(
                "飞书多维表格数据同步到本地完成（通用化框架）！\n" +
                "总耗时: %d ms\n" +
                "新增记录: %d 条\n" +
                "更新记录: %d 条\n" +
                "跳过记录: %d 条\n" +
                "失败记录: %d 条\n" +
                "处理详情:\n%s",
                (endTime - startTime),
                result.getAdded(),
                result.getUpdated(),
                result.getSkipped(),
                result.getFailed(),
                String.join("\n", result.getDetails())
            );

        } catch (Exception e) {
            log.error("同步飞书多维表格数据到本地数据库异常", e);
            throw new RuntimeException("同步失败: " + e.getMessage());
        }
    }

    @Override
    public String syncLocalDataToBitable(String appToken, String tableId) {
        try {
            log.info("开始同步本地数据库数据到飞书多维表格（使用通用化框架）");
            long startTime = System.currentTimeMillis();

            BitableConfig config = getDomainCertConfig(appToken, tableId, VIEW_ID);

            // 获取本地所有记录
            List<DomainCertMonitor> localRecords = domainCertMonitorService.selectList(null);
            log.info("获取到本地数据库记录 {} 条", localRecords.size());

            // 使用通用化服务同步到飞书
            IGenericBitableSyncService.SyncResult<DomainCertMonitor> result = 
                genericBitableSyncService.syncToFeishu(
                    config,
                    localRecords,
                    this::extractRecordId
                );

            long endTime = System.currentTimeMillis();
            return String.format(
                "本地数据同步到飞书多维表格完成（通用化框架）！\n" +
                "总耗时: %d ms\n" +
                "新增记录: %d 条\n" +
                "更新记录: %d 条\n" +
                "失败记录: %d 条\n" +
                "处理详情:\n%s",
                (endTime - startTime),
                result.getAdded(),
                result.getUpdated(),
                result.getFailed(),
                String.join("\n", result.getDetails())
            );

        } catch (Exception e) {
            log.error("同步本地数据到飞书多维表格异常", e);
            throw new RuntimeException("同步失败: " + e.getMessage());
        }
    }

    @Override
    public String syncBidirectional(String appToken, String tableId, String viewId, Integer pageSize) {
        try {
            log.info("开始双向同步数据（使用通用化框架）");
            long startTime = System.currentTimeMillis();

            BitableConfig config = getDomainCertConfig(appToken, tableId, viewId);
            if (pageSize != null) {
                config.setPageSize(pageSize);
            }

            // 1. 先执行飞书到本地的同步
            log.info("第一步：飞书到本地同步");
            IGenericBitableSyncService.SyncResult<DomainCertMonitor> fromFeishuResult = 
                genericBitableSyncService.syncFromFeishu(
                    config,
                    DomainCertMonitor.class,
                    this::saveDomainCertMonitor,
                    this::checkDomainCertExists
                );
            log.info("飞书到本地同步完成: {}", fromFeishuResult);

            // 2. 再执行本地到飞书的同步
            log.info("第二步：本地到飞书同步");
            List<DomainCertMonitor> localRecords = domainCertMonitorService.selectList(null);
            IGenericBitableSyncService.SyncResult<DomainCertMonitor> toFeishuResult = 
                genericBitableSyncService.syncToFeishu(
                    config,
                    localRecords,
                    this::extractRecordId
                );
            log.info("本地到飞书同步完成: {}", toFeishuResult);

            // 3. 合并统计结果
            int totalAdded = fromFeishuResult.getAdded() + toFeishuResult.getAdded();
            int totalUpdated = fromFeishuResult.getUpdated() + toFeishuResult.getUpdated();
            int totalSkipped = fromFeishuResult.getSkipped() + toFeishuResult.getSkipped();
            int totalFailed = fromFeishuResult.getFailed() + toFeishuResult.getFailed();

            long endTime = System.currentTimeMillis();
            return String.format(
                "双向数据同步完成（通用化框架）！\n" +
                "总耗时: %d ms\n\n" +
                "=== 飞书到本地 ===\n" +
                "新增: %d, 更新: %d, 跳过: %d, 失败: %d\n\n" +
                "=== 本地到飞书 ===\n" +
                "新增: %d, 更新: %d, 跳过: %d, 失败: %d\n\n" +
                "=== 总计 ===\n" +
                "新增: %d 条\n" +
                "更新: %d 条\n" +
                "跳过: %d 条\n" +
                "失败: %d 条",
                (endTime - startTime),
                fromFeishuResult.getAdded(), fromFeishuResult.getUpdated(), 
                fromFeishuResult.getSkipped(), fromFeishuResult.getFailed(),
                toFeishuResult.getAdded(), toFeishuResult.getUpdated(),
                toFeishuResult.getSkipped(), toFeishuResult.getFailed(),
                totalAdded, totalUpdated, totalSkipped, totalFailed
            );

        } catch (Exception e) {
            log.error("双向数据同步异常", e);
            throw new RuntimeException("双向同步失败: " + e.getMessage());
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 保存域名监控记录
     */
    private Boolean saveDomainCertMonitor(DomainCertMonitor monitor) {
        try {
            // 设置默认值
            if (monitor.getCreateBy() == null) {
                monitor.setCreateBy("system");
            }
            if (monitor.getDelFlag() == null) {
                monitor.setDelFlag("0");
            }
            if (monitor.getStatus() == null) {
                monitor.setStatus("0");
            }
            if (monitor.getPort() == null) {
                monitor.setPort(443);
            }
            if (monitor.getCreateTime() == null) {
                monitor.setCreateTime(new Date());
            }
            monitor.setUpdateTime(new Date());

            return domainCertMonitorService.save(monitor);
        } catch (Exception e) {
            log.error("保存域名监控记录失败: {}", monitor.getDomain(), e);
            return false;
        }
    }

    /**
     * 检查域名监控记录是否已存在
     * 返回已存在的记录，如果不存在返回null
     */
    private DomainCertMonitor checkDomainCertExists(DomainCertMonitor monitor) {
        try {
            // 根据域名+端口查询
            String domain = monitor.getDomain();
            Integer port = monitor.getPort() != null ? monitor.getPort() : 443;
            
            // 从本地数据库查询
            List<DomainCertMonitor> existing = domainCertMonitorService.selectList(null);
            return existing.stream()
                .filter(m -> domain.equals(m.getDomain()) && port.equals(m.getPort()))
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            log.error("检查域名监控记录存在性失败: {}", monitor.getDomain(), e);
            return null;
        }
    }

    /**
     * 从实体提取recordId
     */
    private String extractRecordId(DomainCertMonitor monitor) {
        // 从 transient 字段获取飞书 recordId
        return monitor.getFeishuRecordId();
    }
}
