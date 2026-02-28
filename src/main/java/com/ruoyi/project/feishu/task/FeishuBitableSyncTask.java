package com.ruoyi.project.feishu.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ruoyi.project.feishu.service.IFeishuBitableSyncService;

/**
 * 飞书多维表格数据同步定时任务
 * 功能：
 * 1. 定期同步飞书多维表格数据到本地数据库
 * 2. 保持数据一致性
 * 
 * @author ruoyi
 * @date 2026-02-05
 */
@Component
public class FeishuBitableSyncTask {
    
    private static final Logger log = LoggerFactory.getLogger(FeishuBitableSyncTask.class);
    
    @Autowired
    private IFeishuBitableSyncService feishuBitableSyncService;
    
    // 固定配置参数
    private static final String APP_TOKEN = "T1O7blsfNanfqosMWBvcIWgwnzb";
    private static final String TABLE_ID = "tblrCnUgBgzSMpNq";
    private static final String VIEW_ID = "vewEYjlKYX";
    private static final Integer DEFAULT_PAGE_SIZE = 50;
    
    /**
     * 执行数据同步任务（每2小时执行一次）
     * cron表达式：0 0 0/2 * * ?
     * 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 0/2 * * ?")
    public void execute() {
        log.info("========== 开始执行飞书多维表格数据同步定时任务 ==========");
        long startTime = System.currentTimeMillis();
        
        try {
            // 执行双向同步
            log.info("【步骤1】开始执行双向数据同步...");
            String syncResult = feishuBitableSyncService.syncBidirectional(
                APP_TOKEN, TABLE_ID, VIEW_ID, DEFAULT_PAGE_SIZE);
            
            log.info("【步骤1完成】数据同步结果:\n{}", syncResult);
            
        } catch (Exception e) {
            log.error("飞书多维表格数据同步定时任务执行异常", e);
        }
        
        long endTime = System.currentTimeMillis();
        log.info("========== 飞书多维表格数据同步定时任务执行完成，耗时: {} ms ==========", (endTime - startTime));
    }
    
}