package com.ruoyi.project.gen.service;

import java.util.List;
import java.util.Map;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.ruoyi.project.gen.domain.GenTable;

/**
 * 业务 服务层
 * 
 * @author ruoyi
 */
public interface IGenTableService extends IService<GenTable> {
    /**
     * 查询业务列表
     * 
     * @param genTable 业务信息
     * @return 业务集合
     */
    public List<GenTable> selectGenTableList(GenTable genTable);

    /**
     * 查询据库列表
     * 
     * @param genTable 业务信息
     * @return 数据库表集合
     */
    public Page<GenTable> selectDbTableList(GenTable genTable);

    /**
     * 根据指定数据源查询数据库列表
     * 
     * @param genTable       业务信息
     * @param dataSourceName 数据源名称
     * @return 数据库表集合
     */
    public Page<GenTable> selectDbTableListByDataSource(GenTable genTable, String dataSourceName);

    /**
     * * 查询据库列表
     * 
     * @param tableNames 表名称组
     * @return 数据库表集合
     */
    public List<GenTable> selectDbTableListByNames(String[] tableNames);

    /**
     * 根据指定数据源查询据库列表
     * 
     * @param tableNames     表名称组
     * @param dataSourceName 数据源名称
     * @return 数据库表集合
     */
    public List<GenTable> selectDbTableListByNamesAndDataSource(String[] tableNames, String dataSourceName);

    /**
     * 查询所有表信息
     * 
     * @return 表信息集合
     */
    public List<GenTable> selectGenTableAll();

    /**
     * 查询业务信息
     * 
     * @param id 业务ID
     * @return 业务信息
     */
    public GenTable selectGenTableById(Long id);

    /**
     * 修改业务
     * 
     * @param genTable 业务信息
     * @return 结果
     */
    public void updateGenTable(GenTable genTable);

    /**
     * 删除业务信息
     * 
     * @param tableIds 需要删除的表数据ID
     * @return 结果
     */
    public void deleteGenTableByIds(Long[] tableIds);

    /**
     * 创建表
     *
     * @param sql 创建表语句
     * @return 结果
     */
    public boolean createTable(String sql);

    /**
     * 导入表结构
     *
     * @param tableList 导入表列表
     * @param operName  操作人员
     */
    public void importGenTable(List<GenTable> tableList, String operName);

    /**
     * 预览代码
     * 
     * @param tableId 表编号
     * @return 预览数据列表
     */
    public Map<String, String> previewCode(Long tableId);

    /**
     * 生成代码（下载方式）
     * 
     * @param tableName 表名称
     * @return 数据
     */
    public byte[] downloadCode(String tableName);

    /**
     * 生成代码（自定义路径）
     * 
     * @param tableName 表名称
     * @return 数据
     */
    public void generatorCode(String tableName);

    /**
     * 同步数据库
     * 将当前表结构信息同步到数据库中，如果表不存在则创建，如果表存在则更新结构
     * 
     * @param tableName 表名称
     */
    public void synchDb(String tableName);

    /**
     * 根据指定数据源同步数据库
     * 将当前表结构信息同步到指定数据源的数据库中
     * 
     * @param tableName      表名称
     * @param dataSourceName 数据源名称
     */
    public void synchDbWithDataSource(String tableName, String dataSourceName);

    /**
     * 批量生成代码（下载方式）
     * 
     * @param tableNames 表数组
     * @return 数据
     */
    public byte[] downloadCode(String[] tableNames);

    /**
     * 插入业务表信息
     *
     * @param genTable 业务表信息
     * @return 结果
     */
    public void insertGenTable(GenTable genTable);

    /**
     * 修改保存参数校验
     * 
     * @param genTable 业务信息
     */
    public void validateEdit(GenTable genTable);

    /**
     * 获取当前数据库名称
     * 
     * @return 当前数据库名称
     */
    public String selectCurrentDatabase();

    /**
     * 获取所有已生成的表名列表
     * 
     * @return 已生成表名列表
     */
    public List<String> selectTableNameList();

    /**
     * 检查表是否有主键
     *
     * @param tableName 表名
     * @return 如果有主键返回1，否则返回0
     */
    public int checkTablePrimaryKey(String tableName);

    /**
     * 查询表的主键列名
     * 
     * @param tableName 表名
     * @return 主键列名列表
     */
    public List<String> getTablePrimaryKeyColumns(String tableName);

    /**
     * 查询表名称业务信息
     * 
     * @param tableName 表名称
     * @return 业务信息
     */
    public GenTable selectGenTableByName(String tableName);
}
