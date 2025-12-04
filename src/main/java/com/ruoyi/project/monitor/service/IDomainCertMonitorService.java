package com.ruoyi.project.monitor.service;

import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.monitor.domain.DomainCertMonitor;

/**
 * 域名证书监控Service接口
 * 
 * @author ruoyi
 * @date 2025-12-04
 */
public interface IDomainCertMonitorService extends IService<DomainCertMonitor> {

    /**
     * 查询域名证书监控列表
     * 
     * @param domainCertMonitor 查询条件
     * @return 域名证书监控集合
     */
    List<DomainCertMonitor> selectList(DomainCertMonitor domainCertMonitor);

    /**
     * 检测单个域名的证书信息
     * 
     * @param id 域名证书监控ID
     * @return 检测结果
     */
    boolean checkCert(Long id);

    /**
     * 检测所有域名的证书信息
     * 
     * @return 检测成功数量
     */
    int checkAllCerts();

    /**
     * 发送即将过期的证书通知
     * 
     * @return 发送通知的数量
     */
    int sendExpiringNotifications();

    /**
     * 根据ID发送指定域名的证书通知
     * 
     * @param id 域名证书监控ID
     * @return 发送结果
     */
    boolean sendNotificationById(Long id);

    /**
     * 根据域名查询证书监控信息
     * 
     * @param domain 域名
     * @param port 端口
     * @return 域名证书监控信息
     */
    DomainCertMonitor selectByDomainAndPort(String domain, Integer port);
}
