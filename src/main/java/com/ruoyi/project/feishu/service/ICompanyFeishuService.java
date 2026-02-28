package com.ruoyi.project.feishu.service;

import java.util.Map;

import com.ruoyi.project.feishu.domain.dto.DomainCertRecordDto;

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
     * 获取部门直属用户列表（使用指定密钥名称）
     * 
     * @param departmentId 部门ID
     * @param userIdType 用户ID类型
     * @param departmentIdType 部门ID类型
     * @param pageSize 分页大小
     * @param keyName 密钥名称
     * @return 用户列表数据
     */
    Object getDepartmentUsers(String departmentId, String userIdType, String departmentIdType, Integer pageSize, String keyName);
    
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
    
    /**
     * 同步部门用户到本地数据库（使用指定密钥名称）
     * 
     * @param departmentId 部门ID
     * @param userIdType 用户ID类型
     * @param departmentIdType 部门ID类型
     * @param pageSize 分页大小
     * @param keyName 密钥名称
     * @return 同步结果信息
     */
    String syncDepartmentUsers(String departmentId, String userIdType, String departmentIdType, Integer pageSize, String keyName);
    
    /**
     * 查询多维表格数据
     * 
     * @param appToken 多维表格应用token
     * @param tableId 数据表ID
     * @param viewId 视图ID（可选）
     * @param pageSize 分页大小
     * @param domain 域名过滤条件（可选）
     * @return 多维表格记录数据
     */
    Object searchAppTableRecord(String appToken, String tableId, String viewId, Integer pageSize, String domain);
    
    /**
     * 查询多维表格数据（使用指定密钥名称）
     * 
     * @param appToken 多维表格应用token
     * @param tableId 数据表ID
     * @param viewId 视图ID（可选）
     * @param pageSize 分页大小
     * @param domain 域名过滤条件（可选）
     * @param keyName 密钥名称
     * @return 多维表格记录数据
     */
    Object searchAppTableRecord(String appToken, String tableId, String viewId, Integer pageSize, String domain, String keyName);
    
    /**
     * 新增多维表格记录
     * 
     * @param appToken 多维表格应用token
     * @param tableId 数据表ID
     * @param record 域名证书记录数据DTO
     * @return 新增记录结果
     */
    Object createAppTableRecord(String appToken, String tableId, DomainCertRecordDto record);
    
    /**
     * 新增多维表格记录（使用指定密钥名称）
     * 
     * @param appToken 多维表格应用token
     * @param tableId 数据表ID
     * @param record 域名证书记录数据DTO
     * @param keyName 密钥名称
     * @return 新增记录结果
     */
    Object createAppTableRecord(String appToken, String tableId, DomainCertRecordDto record, String keyName);
    
    /**
     * 更新多维表格记录
     * 
     * @param appToken 多维表格应用token
     * @param tableId 数据表ID
     * @param recordId 记录ID
     * @param record 域名证书记录数据DTO
     * @return 更新记录结果
     */
    Object updateAppTableRecord(String appToken, String tableId, String recordId, DomainCertRecordDto record);
    
    /**
     * 更新多维表格记录（使用指定密钥名称）
     * 
     * @param appToken 多维表格应用token
     * @param tableId 数据表ID
     * @param recordId 记录ID
     * @param record 域名证书记录数据DTO
     * @param keyName 密钥名称
     * @return 更新记录结果
     */
    Object updateAppTableRecord(String appToken, String tableId, String recordId, DomainCertRecordDto record, String keyName);

    /**
     * 新增多维表格记录（通用Map版本）
     *
     * @param appToken 多维表格应用token
     * @param tableId 数据表ID
     * @param fields 字段数据Map
     * @return 新增记录结果
     */
    Object createAppTableRecord(String appToken, String tableId, Map<String, Object> fields);

    /**
     * 新增多维表格记录（通用Map版本，使用指定密钥名称）
     *
     * @param appToken 多维表格应用token
     * @param tableId 数据表ID
     * @param fields 字段数据Map
     * @param keyName 密钥名称
     * @return 新增记录结果
     */
    Object createAppTableRecord(String appToken, String tableId, Map<String, Object> fields, String keyName);

    /**
     * 更新多维表格记录（通用Map版本）
     *
     * @param appToken 多维表格应用token
     * @param tableId 数据表ID
     * @param recordId 记录ID
     * @param fields 字段数据Map
     * @return 更新记录结果
     */
    Object updateAppTableRecord(String appToken, String tableId, String recordId, Map<String, Object> fields);

    /**
     * 更新多维表格记录（通用Map版本，使用指定密钥名称）
     *
     * @param appToken 多维表格应用token
     * @param tableId 数据表ID
     * @param recordId 记录ID
     * @param fields 字段数据Map
     * @param keyName 密钥名称
     * @return 更新记录结果
     */
    Object updateAppTableRecord(String appToken, String tableId, String recordId, Map<String, Object> fields, String keyName);
}