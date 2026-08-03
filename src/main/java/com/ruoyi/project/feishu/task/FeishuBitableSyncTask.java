package com.ruoyi.project.feishu.task;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${feishu.bitable-sync.max-process-rss-mb:650}")
    private long maxProcessRssMb;
    
    /** 默认每两个小时整点执行飞书到本地同步，可通过环境变量调整。 */
    @Scheduled(cron = "${feishu.bitable-sync.from-feishu-cron:0 0 0/2 * * ?}")
    public void syncFromFeishu() {
        log.info("========== 开始执行飞书到本地同步任务 ==========");
        long startTime = System.currentTimeMillis();
        MemorySnapshot before = logMemory("飞书到本地任务开始前");

        if (isMemoryPressureHigh(before)) {
            log.warn("跳过飞书到本地同步：当前进程 RSS {} MB 已达到安全阈值 {} MB",
                before.rssMb(), maxProcessRssMb);
            return;
        }

        try {
            String syncResult = feishuBitableSyncService.syncBitableDataToLocal(
                APP_TOKEN, TABLE_ID, VIEW_ID, DEFAULT_PAGE_SIZE);
            log.info("飞书到本地同步结果:\n{}", syncResult);
        } catch (Exception e) {
            log.error("飞书到本地同步任务执行异常", e);
        }

        log.info("========== 飞书到本地同步任务完成，耗时: {} ms ==========",
            System.currentTimeMillis() - startTime);
        logMemory("飞书到本地任务结束后");
    }

    /** 默认每 15 分钟轮转同步一条，避开整点任务，单轮最多一次查询和一次写请求。 */
    @Scheduled(cron = "${feishu.bitable-sync.to-feishu-cron:0 7/15 * * * ?}")
    public void syncToFeishu() {
        log.info("========== 开始执行本地到飞书单条增量同步任务 ==========");
        long startTime = System.currentTimeMillis();
        MemorySnapshot before = logMemory("本地到飞书任务开始前");

        if (isMemoryPressureHigh(before)) {
            log.warn("跳过本地到飞书同步：当前进程 RSS {} MB 已达到安全阈值 {} MB",
                before.rssMb(), maxProcessRssMb);
            return;
        }

        try {
            String syncResult = feishuBitableSyncService.syncLocalDataToBitable(APP_TOKEN, TABLE_ID);
            log.info("本地到飞书同步结果:\n{}", syncResult);
        } catch (Exception e) {
            log.error("本地到飞书同步任务执行异常", e);
        }

        log.info("========== 本地到飞书单条增量同步任务完成，耗时: {} ms ==========",
            System.currentTimeMillis() - startTime);
        logMemory("本地到飞书任务结束后");
    }

    private boolean isMemoryPressureHigh(MemorySnapshot snapshot) {
        return snapshot.rssMb() >= 0 && snapshot.rssMb() >= maxProcessRssMb;
    }

    private MemorySnapshot logMemory(String stage) {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        MemoryUsage nonHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        long rssMb = readLinuxRssMb();
        int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();
        MemorySnapshot snapshot = new MemorySnapshot(
            toMb(heap.getUsed()),
            toMb(heap.getCommitted()),
            toMb(heap.getMax()),
            toMb(nonHeap.getUsed()),
            rssMb,
            threadCount
        );
        log.info("{}内存快照: heapUsed={}MB, heapCommitted={}MB, heapMax={}MB, " +
                "nonHeapUsed={}MB, rss={}MB, threads={}",
            stage,
            snapshot.heapUsedMb(),
            snapshot.heapCommittedMb(),
            snapshot.heapMaxMb(),
            snapshot.nonHeapUsedMb(),
            snapshot.rssMb() >= 0 ? snapshot.rssMb() : "N/A",
            snapshot.threadCount());
        return snapshot;
    }

    private long readLinuxRssMb() {
        Path statusPath = Path.of("/proc/self/status");
        if (!Files.isReadable(statusPath)) {
            return -1;
        }
        try {
            List<String> lines = Files.readAllLines(statusPath);
            for (String line : lines) {
                if (line.startsWith("VmRSS:")) {
                    String numericKb = line.substring("VmRSS:".length())
                        .replace("kB", "")
                        .trim();
                    return Long.parseLong(numericKb) / 1024;
                }
            }
        } catch (IOException | NumberFormatException e) {
            log.debug("读取 Linux RSS 失败", e);
        }
        return -1;
    }

    private long toMb(long bytes) {
        return bytes < 0 ? -1 : bytes / 1024 / 1024;
    }

    private record MemorySnapshot(
        long heapUsedMb,
        long heapCommittedMb,
        long heapMaxMb,
        long nonHeapUsedMb,
        long rssMb,
        int threadCount) {
    }
}
