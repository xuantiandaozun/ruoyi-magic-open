package com.ruoyi.project.monitor.service.impl;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.feishu.domain.FeishuUsers;
import com.ruoyi.project.feishu.domain.dto.FeishuMessageDto;
import com.ruoyi.project.feishu.service.IFeishuService;
import com.ruoyi.project.feishu.service.IFeishuUsersService;
import com.ruoyi.project.monitor.domain.DomainCertMonitor;
import com.ruoyi.project.monitor.mapper.DomainCertMonitorMapper;
import com.ruoyi.project.monitor.service.IDomainCertMonitorService;

import cn.hutool.core.util.StrUtil;

/**
 * 域名证书监控Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-12-04
 */
@Service
public class DomainCertMonitorServiceImpl extends ServiceImpl<DomainCertMonitorMapper, DomainCertMonitor>
        implements IDomainCertMonitorService {

    private static final Logger log = LoggerFactory.getLogger(DomainCertMonitorServiceImpl.class);

    /** SSL连接超时时间（毫秒） */
    private static final int SSL_TIMEOUT = 10000;

    @Autowired
    private IFeishuService feishuService;

    @Autowired
    private IFeishuUsersService feishuUsersService;

    @Override
    public List<DomainCertMonitor> selectList(DomainCertMonitor domainCertMonitor) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("del_flag", "0");
        
        if (domainCertMonitor != null) {
            if (StrUtil.isNotBlank(domainCertMonitor.getDomain())) {
                queryWrapper.like("domain", domainCertMonitor.getDomain());
            }
            if (StrUtil.isNotBlank(domainCertMonitor.getStatus())) {
                queryWrapper.eq("status", domainCertMonitor.getStatus());
            }
            if (StrUtil.isNotBlank(domainCertMonitor.getNotifyEnabled())) {
                queryWrapper.eq("notify_enabled", domainCertMonitor.getNotifyEnabled());
            }
        }
        
        queryWrapper.orderBy("create_time", false);
        return list(queryWrapper);
    }

    @Override
    public boolean checkCert(Long id) {
        DomainCertMonitor monitor = getById(id);
        if (monitor == null) {
            log.warn("域名证书监控记录不存在，ID: {}", id);
            return false;
        }
        return checkAndUpdateCert(monitor);
    }

    @Override
    public int checkAllCerts() {
        List<DomainCertMonitor> monitors = selectList(null);
        int successCount = 0;
        
        for (DomainCertMonitor monitor : monitors) {
            try {
                if (checkAndUpdateCert(monitor)) {
                    successCount++;
                }
                // 每次检测间隔100ms，避免请求过快
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        log.info("证书检测完成，总数: {}, 成功: {}", monitors.size(), successCount);
        return successCount;
    }

    @Override
    public int sendExpiringNotifications() {
        // 查询需要通知的域名（状态为即将过期或已过期，且开启了通知）
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("del_flag", "0")
                .eq("notify_enabled", "1")
                .in("status", DomainCertMonitor.STATUS_EXPIRING, DomainCertMonitor.STATUS_EXPIRED);
        
        List<DomainCertMonitor> expiringList = list(queryWrapper);
        
        if (expiringList.isEmpty()) {
            log.info("没有需要通知的即将过期证书");
            return 0;
        }
        
        int notifyCount = 0;
        Date now = new Date();
        
        for (DomainCertMonitor monitor : expiringList) {
            // 检查今天是否已经通知过
            if (monitor.getLastNotifyTime() != null) {
                long diffDays = TimeUnit.MILLISECONDS.toDays(now.getTime() - monitor.getLastNotifyTime().getTime());
                if (diffDays < 1) {
                    log.debug("域名 {} 今天已通知，跳过", monitor.getDomain());
                    continue;
                }
            }
            
            // 发送飞书通知
            if (sendFeishuNotification(monitor)) {
                // 更新最后通知时间
                monitor.setLastNotifyTime(now);
                updateById(monitor);
                notifyCount++;
            }
        }
        
        log.info("证书过期通知发送完成，发送数量: {}", notifyCount);
        return notifyCount;
    }

    @Override
    public boolean sendNotificationById(Long id) {
        DomainCertMonitor monitor = getById(id);
        if (monitor == null) {
            log.error("未找到ID为 {} 的域名证书监控记录", id);
            return false;
        }
        
        // 直接发送通知，不考虑状态
        boolean result = sendFeishuNotification(monitor);
        
        if (result) {
            // 更新最后通知时间
            monitor.setLastNotifyTime(new Date());
            updateById(monitor);
        }
        
        return result;
    }

    @Override
    public DomainCertMonitor selectByDomainAndPort(String domain, Integer port) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("del_flag", "0")
                .eq("domain", domain)
                .eq("port", port != null ? port : 443);
        return getOne(queryWrapper);
    }

    /**
     * 检测并更新证书信息
     */
    private boolean checkAndUpdateCert(DomainCertMonitor monitor) {
        String domain = monitor.getDomain();
        int port = monitor.getPort() != null ? monitor.getPort() : 443;
        
        log.info("开始检测域名证书: {}:{}", domain, port);
        
        try {
            // 获取SSL证书信息
            X509Certificate cert = getSSLCertificate(domain, port);
            
            if (cert != null) {
                // 解析证书信息
                Date expireTime = cert.getNotAfter();
                String issuer = cert.getIssuerX500Principal().getName();
                String subject = cert.getSubjectX500Principal().getName();
                
                // 计算剩余天数
                long diffMs = expireTime.getTime() - System.currentTimeMillis();
                int daysRemaining = (int) TimeUnit.MILLISECONDS.toDays(diffMs);
                
                // 确定状态
                String status;
                if (daysRemaining < 0) {
                    status = DomainCertMonitor.STATUS_EXPIRED;
                } else if (daysRemaining <= (monitor.getNotifyDays() != null ? monitor.getNotifyDays() : 3)) {
                    status = DomainCertMonitor.STATUS_EXPIRING;
                } else {
                    status = DomainCertMonitor.STATUS_NORMAL;
                }
                
                // 更新记录
                monitor.setExpireTime(expireTime);
                monitor.setIssuer(extractCN(issuer));
                monitor.setSubject(extractCN(subject));
                monitor.setDaysRemaining(daysRemaining);
                monitor.setStatus(status);
                monitor.setLastCheckTime(new Date());
                monitor.setErrorMessage(null);
                
                updateById(monitor);
                
                log.info("域名 {} 证书检测成功，过期时间: {}, 剩余天数: {}", domain, expireTime, daysRemaining);
                return true;
            } else {
                throw new Exception("无法获取证书信息");
            }
        } catch (Exception e) {
            log.error("域名 {} 证书检测失败: {}", domain, e.getMessage());
            
            // 更新检测失败状态
            monitor.setStatus(DomainCertMonitor.STATUS_CHECK_FAILED);
            monitor.setLastCheckTime(new Date());
            monitor.setErrorMessage(e.getMessage());
            updateById(monitor);
            
            return false;
        }
    }

    /**
     * 获取SSL证书
     */
    private X509Certificate getSSLCertificate(String domain, int port) throws Exception {
        // 创建信任所有证书的TrustManager
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };
        
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        SSLSocketFactory factory = sslContext.getSocketFactory();
        
        try (SSLSocket socket = (SSLSocket) factory.createSocket(domain, port)) {
            socket.setSoTimeout(SSL_TIMEOUT);
            socket.startHandshake();
            
            java.security.cert.Certificate[] certs = socket.getSession().getPeerCertificates();
            if (certs != null && certs.length > 0 && certs[0] instanceof X509Certificate) {
                return (X509Certificate) certs[0];
            }
        }
        
        return null;
    }

    /**
     * 从DN中提取CN（通用名称）
     */
    private String extractCN(String dn) {
        if (StrUtil.isBlank(dn)) {
            return "";
        }
        
        String[] parts = dn.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.toUpperCase().startsWith("CN=")) {
                return part.substring(3);
            }
        }
        return dn.length() > 100 ? dn.substring(0, 100) : dn;
    }

    /**
     * 发送飞书通知
     */
    private boolean sendFeishuNotification(DomainCertMonitor monitor) {
        try {
            // 获取默认飞书用户（参考云函数发送飞书消息的方式）
            FeishuUsers user = feishuUsersService.getOne(QueryWrapper.create().limit(1));
            if (user == null) {
                log.error("未找到飞书用户，无法发送通知");
                return false;
            }
            
            // 构建消息内容
            String title = "🔔 域名证书过期提醒";
            StringBuilder content = new StringBuilder();
            content.append("域名: ").append(monitor.getDomain());
            if (monitor.getPort() != null && monitor.getPort() != 443) {
                content.append(":").append(monitor.getPort());
            }
            content.append("\n");
            content.append("状态: ").append(DomainCertMonitor.STATUS_EXPIRED.equals(monitor.getStatus()) ? "已过期" : "即将过期").append("\n");
            // 格式化过期时间
            String expireTimeStr = monitor.getExpireTime() != null 
                    ? cn.hutool.core.date.DateUtil.format(monitor.getExpireTime(), "yyyy-MM-dd HH:mm:ss") 
                    : "未知";
            content.append("过期时间: ").append(expireTimeStr).append("\n");
            content.append("剩余天数: ").append(monitor.getDaysRemaining()).append("天\n");
            if (StrUtil.isNotBlank(monitor.getIssuer())) {
                content.append("证书颁发者: ").append(monitor.getIssuer()).append("\n");
            }
            if (StrUtil.isNotBlank(monitor.getRemark())) {
                content.append("备注: ").append(monitor.getRemark());
            }
            
            // 构建飞书消息
            String receiveId = StrUtil.isNotBlank(user.getUserId()) ? user.getUserId() : user.getOpenId();
            String receiveIdType = StrUtil.isNotBlank(user.getUserId()) ? "user_id" : "open_id";
            
            FeishuMessageDto messageDto = FeishuMessageDto.createPostMessage(
                    receiveId, receiveIdType, title, content.toString());
            
            // 发送消息
            boolean success = feishuService.sendMessage(messageDto);
            
            if (success) {
                log.info("域名 {} 证书过期通知发送成功", monitor.getDomain());
            } else {
                log.error("域名 {} 证书过期通知发送失败", monitor.getDomain());
            }
            
            return success;
        } catch (Exception e) {
            log.error("发送飞书通知异常: {}", e.getMessage(), e);
            return false;
        }
    }
}
