package com.ruoyi.project.common.service;

import java.util.List;
import java.util.Map;

import com.mybatisflex.core.paginate.Page;
import com.ruoyi.project.common.domain.dto.DictQueryRequest;
import com.ruoyi.project.common.domain.vo.DictOption;

/**
 * 字典查询服务接口
 * 
 * @author ruoyi
 */
public interface IDictionaryService {

    /**
     * 获取表字典数据
     * 
     * @param tableName 表名
     * @param labelField 显示字段名
     * @param valueField 值字段名
     * @param status 状态过滤
     * @return 字典选项列表
     */
    List<DictOption> getTableDict(String tableName, String labelField, String valueField, String status);

    /**
     * 获取自定义表字典数据
     * 
     * @param tableName 表名
     * @param request 查询请求参数
     * @return 字典数据列表
     */
    List<Map<String, Object>> getCustomTableDict(String tableName, DictQueryRequest request);

    /**
     * 分页获取表字典数据
     * 
     * @param tableName 表名
     * @param request 查询请求参数
     * @return 分页字典数据
     */
    Page<Map<String, Object>> getTableDictPage(String tableName, DictQueryRequest request);

    /**
     * 验证表是否存在
     * 
     * @param tableName 表名
     * @return 是否存在
     */
    boolean validateTableExists(String tableName);

    /**
     * 获取表的所有列名
     * 
     * @param tableName 表名
     * @return 列名列表
     */
    List<String> getTableColumns(String tableName);

    /**
     * 获取默认的显示字段
     * 
     * @param tableName 表名
     * @param columns 表列名列表
     * @return 默认显示字段名
     */
    String getDefaultLabelField(String tableName, List<String> columns);

    /**
     * 获取默认的值字段
     * 
     * @param tableName 表名
     * @param columns 表列名列表
     * @return 默认值字段名
     */
    String getDefaultValueField(String tableName, List<String> columns);
}