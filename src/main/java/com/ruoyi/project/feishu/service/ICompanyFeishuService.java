package com.ruoyi.project.feishu.service;

/**
 * 公司飞书Service接口
 * 
 * @author ruoyi
 * @date 2026-02-05
 */
public interface ICompanyFeishuService {
    
    /**
     * 获取部门直属用户列表（使用"公司飞书机器人"密钥）
     * 
     * @param departmentId 部门ID
     * @param userIdType 用户ID类型
     * @param departmentIdType 部门ID类型
     * @param pageSize 分页大小
     * @return 用户列表数据
     */
    Object getDepartmentUsers(String departmentId, String userIdType, String departmentIdType, Integer pageSize);
    
    /**
     * 同步部门用户到本地数据库
     * 
     * @param departmentId 部门ID
     * @param userIdType 用户ID类型
     * @param departmentIdType 部门ID类型
     * @param pageSize 分页大小
     * @return 同步结果信息
     */
    String syncDepartmentUsers(String departmentId, String userIdType, String departmentIdType, Integer pageSize);
}