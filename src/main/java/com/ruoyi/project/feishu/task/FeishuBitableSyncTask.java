package com.ruoyi.project.feishu.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ruoyi.project.feishu.service.IFeishuBitableSyncService;

/**
 * 飞书多维表格数据同步定时任务。
 * 两个同步方向错峰执行，避免同一任务同时持有两边的数据和连续写飞书。
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
    
    /** 每两个小时整点执行飞书到本地同步。 */
    @Scheduled(cron = "0 0 0/2 * * ?")
    public void syncFromFeishu() {
        log.info("========== 开始执行飞书到本地同步任务 ==========");
        long startTime = System.currentTimeMillis();

        try {
            String syncResult = feishuBitableSyncService.syncBitableDataToLocal(
                APP_TOKEN, TABLE_ID, VIEW_ID, DEFAULT_PAGE_SIZE);
            log.info("飞书到本地同步结果:\n{}", syncResult);
        } catch (Exception e) {
            log.error("飞书到本地同步任务执行异常", e);
        }

        log.info("========== 飞书到本地同步任务完成，耗时: {} ms ==========",
            System.currentTimeMillis() - startTime);
    }

    /** 每两个小时的第 10 分钟执行本地到飞书同步，与读任务错峰。 */
    @Scheduled(cron = "0 10 0/2 * * ?")
    public void syncToFeishu() {
        log.info("========== 开始执行本地到飞书逐条同步任务 ==========");
        long startTime = System.currentTimeMillis();

        try {
            String syncResult = feishuBitableSyncService.syncLocalDataToBitable(APP_TOKEN, TABLE_ID);
            log.info("本地到飞书同步结果:\n{}", syncResult);
        } catch (Exception e) {
            log.error("本地到飞书同步任务执行异常", e);
        }

        log.info("========== 本地到飞书逐条同步任务完成，耗时: {} ms ==========",
            System.currentTimeMillis() - startTime);
    }
}
