package com.ruoyi.project.gen.service;

import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.gen.domain.GenTableColumn;

/**
 * 业务字段 服务层
 * 
 * @author ruoyi
 */
public interface IGenTableColumnService extends IService<GenTableColumn>
{
    /**
     * 查询业务字段列表
     * 
     * @param tableId 业务字段编号
     * @return 业务字段集合
     */
    public List<GenTableColumn> selectGenTableColumnListByTableId(Long tableId);

    /**
     * 新增业务字段
     * 
     * @param genTableColumn 业务字段信息
     * @return 结果
     */
    public boolean insertGenTableColumn(GenTableColumn genTableColumn);

    /**
     * 修改业务字段
     * 
     * @param genTableColumn 业务字段信息
     * @return 结果
     */
    public boolean updateGenTableColumn(GenTableColumn genTableColumn);

    /**
     * 删除业务字段信息
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public boolean deleteGenTableColumnByIds(String ids);    /**
     * 删除业务字段信息
     * 
     * @param genTableColumns 需要删除的列数据
     * @return 结果
     */
    public boolean deleteGenTableColumns(List<GenTableColumn> genTableColumns);
    
    /**
     * 根据表名称查询列信息
     * 
     * @param tableName 表名称
     * @return 列信息
     */
    public List<GenTableColumn> selectDbTableColumnsByName(String tableName);
    
    /**
     * 根据表名称和数据库名称查询列信息
     * 
     * @param tableName 表名称
     * @param dbName 数据库名称
     * @return 列信息
     */
    public List<GenTableColumn> selectDbTableColumnsByNameAndDataSource(String tableName, String dbName);
    
    /**
     * 根据字段ID查询字段信息
     * 
     * @param columnId 字段ID
     * @return 字段信息
     */
    public GenTableColumn selectGenTableColumnById(Long columnId);
}
