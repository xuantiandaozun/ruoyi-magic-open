package com.ruoyi.project.monitor.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ruoyi.project.monitor.service.IDomainCertMonitorService;

/**
 * 域名证书检测定时任务
 * 功能：
 * 1. 每天上午8点检测所有域名的证书过期时间
 * 2. 检测完成后，对即将过期（3天内）的证书发送飞书通知
 * 
 * @author ruoyi
 * @date 2025-12-04
 */
@Component
public class DomainCertCheckTask {

    private static final Logger log = LoggerFactory.getLogger(DomainCertCheckTask.class);

    @Autowired
    private IDomainCertMonitorService domainCertMonitorService;

    /**
     * 执行证书检测任务（每天上午8点执行）
     * cron表达式：0 0 8 * * ?
     * 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void execute() {
        log.info("========== 开始执行域名证书检测定时任务 ==========");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 检测所有域名的证书
            log.info("【步骤1】开始检测所有域名证书...");
            int checkCount = domainCertMonitorService.checkAllCerts();
            log.info("【步骤1完成】证书检测完成，成功检测 {} 个域名", checkCount);

            // 2. 发送即将过期的证书通知
            log.info("【步骤2】开始发送过期通知...");
            int notifyCount = domainCertMonitorService.sendExpiringNotifications();
            log.info("【步骤2完成】通知发送完成，发送 {} 条通知", notifyCount);

        } catch (Exception e) {
            log.error("域名证书检测定时任务执行异常", e);
        }

        long endTime = System.currentTimeMillis();
        log.info("========== 域名证书检测定时任务执行完成，耗时: {} ms ==========", (endTime - startTime));
    }
}
