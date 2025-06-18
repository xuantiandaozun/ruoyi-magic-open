package com.ruoyi.project.system.service;

import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.system.domain.SysDataSource;

/**
 * 数据源配置Service接口
 * 
 * @author ruoyi-magic
 */
public interface ISysDataSourceService extends IService<SysDataSource> {
    /**
     * 查询数据源配置
     * 
     * @param dataSourceId 数据源配置ID
     * @return 数据源配置
     */
    public SysDataSource selectSysDataSourceById(Long dataSourceId);
    
    /**
     * 根据数据源名称查询数据源配置
     * 
     * @param name 数据源名称
     * @return 数据源配置
     */
    public SysDataSource selectSysDataSourceByName(String name);
    
    /**
     * 查询数据源配置列表
     * 
     * @param sysDataSource 数据源配置
     * @return 数据源配置集合
     */
    public List<SysDataSource> selectSysDataSourceList(SysDataSource sysDataSource);
    
    /**
     * 新增数据源配置
     * 
     * @param sysDataSource 数据源配置
     * @return 结果
     */
    public int insertSysDataSource(SysDataSource sysDataSource);
    
    /**
     * 修改数据源配置
     * 
     * @param sysDataSource 数据源配置
     * @return 结果
     */
    public int updateSysDataSource(SysDataSource sysDataSource);
    
    /**
     * 批量删除数据源配置
     * 
     * @param dataSourceIds 需要删除的数据源配置ID
     * @return 结果
     */
    public int deleteSysDataSourceByIds(Long[] dataSourceIds);
    
    /**
     * 删除数据源配置信息
     * 
     * @param dataSourceId 数据源配置ID
     * @return 结果
     */
    public int deleteSysDataSourceById(Long dataSourceId);
    
    /**
     * 测试数据源连接
     * 
     * @param sysDataSource 数据源配置
     * @return 结果
     */
    public boolean testConnection(SysDataSource sysDataSource);
    
    /**
     * 刷新数据源管理器中的数据源
     * 
     * @return 结果
     */
    public boolean refreshDataSources();
}