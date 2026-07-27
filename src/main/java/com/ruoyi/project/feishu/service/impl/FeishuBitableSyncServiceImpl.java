package com.ruoyi.project.feishu.service.impl;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private static final int MAX_FEISHU_PAGE_SIZE = 500;
    private static final long SAFE_WRITE_INTERVAL_MS = 500L;
    private final AtomicBoolean localToFeishuRunning = new AtomicBoolean(false);

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
            .setKeyName("公司飞书机器人")  // 固定使用公司飞书机器人
            .setPageSize(DEFAULT_PAGE_SIZE)
            .setRequestInterval(100L)
            // 主键字段配置
            .setPrimaryField("domain")
            // 字段映射配置 - 只配置飞书表格中实际存在的字段
            .addFieldMapping(new BitableFieldMapping("域名", "domain", com.ruoyi.project.feishu.annotation.FieldType.TEXT).setPrimary(true))
            .addFieldMapping(new BitableFieldMapping("备注", "remark", com.ruoyi.project.feishu.annotation.FieldType.TEXT))
            .addFieldMapping(new BitableFieldMapping("剩余天数", "daysRemaining", com.ruoyi.project.feishu.annotation.FieldType.NUMBER))
            .addFieldMapping(new BitableFieldMapping("过期时间", "expireTime", com.ruoyi.project.feishu.annotation.FieldType.DATE));
            // 注意：如果飞书表格还有其他字段（如端口），需要在这里添加
    }

    @Override
    public String syncBitableDataToLocal(String appToken, String tableId, String viewId, Integer pageSize) {
        try {
            log.info("开始同步飞书多维表格数据到本地数据库（使用通用化框架）");
            long startTime = System.currentTimeMillis();

            BitableConfig config = getDomainCertConfig(appToken, tableId, viewId);
            if (pageSize != null) {
                config.setPageSize(Math.max(1, Math.min(pageSize, MAX_FEISHU_PAGE_SIZE)));
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
        if (!localToFeishuRunning.compareAndSet(false, true)) {
            throw new IllegalStateException("本地到飞书同步任务正在执行，请勿重复触发");
        }

        try {
            log.info("开始逐条同步本地数据库数据到飞书多维表格");
            long startTime = System.currentTimeMillis();

            BitableConfig config = getDomainCertConfig(appToken, tableId, VIEW_ID);

            IGenericBitableSyncService.SyncResult<DomainCertMonitor> result = new IGenericBitableSyncService.SyncResult<>();
            Long lastProcessedId = null;

            while (true) {
                List<DomainCertMonitor> records = loadNextLocalRecord(lastProcessedId);
                if (records.isEmpty()) {
                    break;
                }

                DomainCertMonitor localRecord = records.get(0);
                lastProcessedId = localRecord.getId();
                syncSingleLocalRecord(config, localRecord, result);
                sleepSafely(SAFE_WRITE_INTERVAL_MS);
            }

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
        } finally {
            localToFeishuRunning.set(false);
        }
    }

    @Override
    public String syncBidirectional(String appToken, String tableId, String viewId, Integer pageSize) {
        try {
            log.info("开始依次执行两个方向的同步");
            long startTime = System.currentTimeMillis();

            String fromFeishuResult = syncBitableDataToLocal(appToken, tableId, viewId, pageSize);
            String toFeishuResult = syncLocalDataToBitable(appToken, tableId);

            long endTime = System.currentTimeMillis();
            return String.format(
                "两个方向同步执行完成！\n总耗时: %d ms\n\n=== 飞书到本地 ===\n%s\n\n=== 本地到飞书 ===\n%s",
                (endTime - startTime),
                fromFeishuResult,
                toFeishuResult
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
            
            return domainCertMonitorService.selectByDomainAndPort(domain, port);
        } catch (Exception e) {
            log.error("检查域名监控记录存在性失败: {}", monitor.getDomain(), e);
            return null;
        }
    }

    private List<DomainCertMonitor> loadNextLocalRecord(Long lastProcessedId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .eq("del_flag", "0")
            .orderBy("id", true)
            .limit(1);
        if (lastProcessedId != null) {
            queryWrapper.gt("id", lastProcessedId);
        }
        return domainCertMonitorService.list(queryWrapper);
    }

    private void syncSingleLocalRecord(
            BitableConfig config,
            DomainCertMonitor localRecord,
            IGenericBitableSyncService.SyncResult<DomainCertMonitor> result) {
        String domain = localRecord.getDomain();
        if (StrUtil.isBlank(domain)) {
            result.incrementFailed();
            result.addFailedEntity(localRecord);
            result.addDetail("同步失败: 本地记录 ID=" + localRecord.getId() + " 的域名为空");
            return;
        }

        try {
            DomainCertMonitor feishuRecord = genericBitableSyncService.queryByPrimaryKey(
                config, domain, DomainCertMonitor.class);
            if (feishuRecord == null) {
                String recordId = genericBitableSyncService.createRecord(config, localRecord);
                if (recordId != null) {
                    result.incrementAdded();
                    result.addSuccessEntity(localRecord);
                    result.addDetail("新增: " + domain);
                } else {
                    result.incrementFailed();
                    result.addFailedEntity(localRecord);
                    result.addDetail("新增失败: " + domain);
                }
                return;
            }

            String recordId = feishuRecord.getFeishuRecordId();
            if (StrUtil.isBlank(recordId)) {
                throw new IllegalStateException("飞书记录缺少 recordId");
            }

            if (genericBitableSyncService.updateRecord(config, recordId, localRecord)) {
                result.incrementUpdated();
                result.addSuccessEntity(localRecord);
                result.addDetail("更新: " + domain);
            } else {
                result.incrementFailed();
                result.addFailedEntity(localRecord);
                result.addDetail("更新失败: " + domain);
            }
        } catch (Exception e) {
            log.error("逐条同步本地记录到飞书失败: id={}, domain={}",
                localRecord.getId(), domain, e);
            result.incrementFailed();
            result.addFailedEntity(localRecord);
            result.addDetail("同步失败: " + domain + " - " + e.getMessage());
        }
    }

    private void sleepSafely(long intervalMs) {
        try {
            Thread.sleep(intervalMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("同步任务被中断", e);
        }
    }
}
