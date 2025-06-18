package com.ruoyi.project.system.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.druid.pool.DruidDataSource;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.config.properties.DruidProperties;
import com.ruoyi.framework.datasource.DataSourceUtils;
import com.ruoyi.framework.datasource.DynamicDataSourceManager;
import com.ruoyi.project.system.domain.SysDataSource;
import com.ruoyi.project.system.mapper.SysDataSourceMapper;
import com.ruoyi.project.system.service.ISysDataSourceService;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 数据源配置Service业务层处理
 * 
 * @author ruoyi-magic
 */
@Service
public class SysDataSourceServiceImpl extends ServiceImpl<SysDataSourceMapper, SysDataSource> implements ISysDataSourceService {    @Autowired
    
    private DruidProperties druidProperties;

    
    @Autowired
    private DynamicDataSourceManager dataSourceManager;

    @Autowired
    private DataSourceUtils dataSourceUtils;

    /**
     * 查询数据源配置
     * 
     * @param dataSourceId 数据源配置ID
     * @return 数据源配置
     */
    @Override
    public SysDataSource selectSysDataSourceById(Long dataSourceId) {
        return getById(dataSourceId);
    }
    
    /**
     * 根据数据源名称查询数据源配置
     * 
     * @param name 数据源名称
     * @return 数据源配置
     */
    @Override
    public SysDataSource selectSysDataSourceByName(String name) {
        // 如果是主数据源，直接返回
        if ("MASTER".equals(name)) {
            SysDataSource masterDataSource = new SysDataSource();
            masterDataSource.setName("MASTER");
            masterDataSource.setDatabaseName("主数据源");
            masterDataSource.setDescription("默认主数据源");
            masterDataSource.setStatus("0");
            return masterDataSource;
        }
        
        // 使用主数据源查询
        return dataSourceUtils.executeWithMaster(() -> {
            QueryWrapper queryWrapper = QueryWrapper.create()
                .from("sys_data_source")
                .where(new QueryColumn("name").eq(name));
            return getOne(queryWrapper);
        });
    }

    /**
     * 查询数据源配置列表
     * 
     * @param sysDataSource 数据源配置
     * @return 数据源配置
     */
    @Override
    public List<SysDataSource> selectSysDataSourceList(SysDataSource sysDataSource) {
        // 使用主数据源查询
        List<SysDataSource> list = dataSourceUtils.executeWithMaster(() -> {
            QueryWrapper queryWrapper = QueryWrapper.create()
                .from("sys_data_source")
                .where(new QueryColumn("name").like(sysDataSource.getName(), StrUtil.isNotBlank(sysDataSource.getName())))
                .and(new QueryColumn("database_name").like(sysDataSource.getDatabaseName(), StrUtil.isNotBlank(sysDataSource.getDatabaseName())))
                .and(new QueryColumn("description").like(sysDataSource.getDescription(), StrUtil.isNotBlank(sysDataSource.getDescription())))
                .and(new QueryColumn("status").eq(sysDataSource.getStatus(), StrUtil.isNotBlank(sysDataSource.getStatus())))
                .orderBy(new QueryColumn("name").asc());
            return list(queryWrapper);
        });
        
        
        return list;
    }

    /**
     * 新增数据源配置
     * 
     * @param sysDataSource 数据源配置
     * @return 结果
     */
    @Override
    @Transactional
    public int insertSysDataSource(SysDataSource sysDataSource) {
        // 检查数据源名称是否已存在
        SysDataSource existingDataSource = selectSysDataSourceByName(sysDataSource.getName());
        if (existingDataSource != null) {
            throw new ServiceException("新增数据源'" + sysDataSource.getName() + "'失败，数据源名称已存在");
        }
        
        // 设置默认状态为正常
        if (StrUtil.isEmpty(sysDataSource.getStatus())) {
            sysDataSource.setStatus("0");
        }
        
        sysDataSource.setCreateBy(SecurityUtils.getUsername());
        sysDataSource.setCreateTime(DateUtil.date());
        
        // 测试连接
        if (!testConnection(sysDataSource)) {
            throw new ServiceException("新增数据源失败，无法连接到数据库，请检查连接信息");
        }
        
        // 保存到数据库
        boolean success = save(sysDataSource);
        
        // 如果数据源状态为正常，则将其添加到动态数据源管理器中
        if ("0".equals(sysDataSource.getStatus())) {
            addToDataSourceManager(sysDataSource);
        }
        
        return success ? 1 : 0;
    }

    /**
     * 修改数据源配置
     * 
     * @param sysDataSource 数据源配置
     * @return 结果
     */
    @Override
    @Transactional
    public int updateSysDataSource(SysDataSource sysDataSource) {
        // 获取原数据源信息
        SysDataSource oldDataSource = getById(sysDataSource.getDataSourceId());
        if (oldDataSource == null) {
            throw new ServiceException("修改数据源失败，数据源ID不存在");
        }
        
        // 如果修改了数据源名称，则检查新名称是否已存在
        if (!oldDataSource.getName().equals(sysDataSource.getName())) {
            SysDataSource existingDataSource = selectSysDataSourceByName(sysDataSource.getName());
            if (existingDataSource != null) {
                throw new ServiceException("修改数据源'" + sysDataSource.getName() + "'失败，数据源名称已存在");
            }
        }
        
        // 如果修改了连接信息，则测试连接
        if (!oldDataSource.getUrl().equals(sysDataSource.getUrl()) 
                || !oldDataSource.getUsername().equals(sysDataSource.getUsername()) 
                || (!StrUtil.isEmpty(sysDataSource.getPassword()) && !oldDataSource.getPassword().equals(sysDataSource.getPassword()))
                || !oldDataSource.getDriverClassName().equals(sysDataSource.getDriverClassName())) {
            if (!testConnection(sysDataSource)) {
                throw new ServiceException("修改数据源失败，无法连接到数据库，请检查连接信息");
            }
        }
        
        // 如果密码为空，则保留原密码
        if (StrUtil.isEmpty(sysDataSource.getPassword())) {
            sysDataSource.setPassword(oldDataSource.getPassword());
        }
        
        sysDataSource.setUpdateBy(SecurityUtils.getUsername());
        sysDataSource.setUpdateTime(DateUtil.date());
        
        boolean success = updateById(sysDataSource);
        
        // 如果数据源在管理器中存在，则先移除
        if (dataSourceManager.dataSourceExists(oldDataSource.getName())) {
            dataSourceManager.removeDataSource(oldDataSource.getName());
        }
        
        // 如果数据源状态为正常，则将其添加到动态数据源管理器中
        if ("0".equals(sysDataSource.getStatus())) {
            addToDataSourceManager(sysDataSource);
        }
        
        return success ? 1 : 0;
    }

    /**
     * 批量删除数据源配置
     * 
     * @param dataSourceIds 需要删除的数据源配置ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteSysDataSourceByIds(Long[] dataSourceIds) {
        for (Long dataSourceId : dataSourceIds) {
            deleteSysDataSourceById(dataSourceId);
        }
        return dataSourceIds.length;
    }

    /**
     * 删除数据源配置信息
     * 
     * @param dataSourceId 数据源配置ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteSysDataSourceById(Long dataSourceId) {
        SysDataSource dataSource = getById(dataSourceId);
        if (dataSource == null) {
            throw new ServiceException("删除数据源失败，数据源ID不存在");
        }
        
        // 如果是MASTER数据源，不允许删除
        if ("MASTER".equals(dataSource.getName())) {
            throw new ServiceException("不能删除主数据源");
        }
        
        // 先从动态数据源管理器中移除
        if (dataSourceManager.dataSourceExists(dataSource.getName())) {
            dataSourceManager.removeDataSource(dataSource.getName());
        }
        
        return removeById(dataSourceId) ? 1 : 0;
    }
    
    /**
     * 测试数据源连接
     * 
     * @param sysDataSource 数据源配置
     * @return 结果
     */
    @Override
    public boolean testConnection(SysDataSource sysDataSource) {
        SysDataSource dsToTest = sysDataSource;
        // 如果参数中有id，优先用数据库中的配置
        if (sysDataSource.getDataSourceId() != null) {            SysDataSource dbDs = getById(sysDataSource.getDataSourceId());
            if (dbDs != null) {
                dsToTest = dbDs;
            }
        }
        DruidDataSource testDataSource = null;
        try {
            testDataSource = new DruidDataSource();
            testDataSource.setUrl(dsToTest.getUrl());
            testDataSource.setUsername(dsToTest.getUsername());
            testDataSource.setPassword(dsToTest.getPassword());
            testDataSource.setDriverClassName(dsToTest.getDriverClassName());
            druidProperties.dataSource(testDataSource);
            testDataSource.init();
            return testDataSource.isEnable();
        } catch (Exception e) {
            return false;
        } finally {
            if (testDataSource != null) {
                testDataSource.close();
            }
        }
    }
      /**
     * 刷新数据源管理器中的数据源
     * 
     * @return 结果
     */
    @Override
    @Transactional
    public boolean refreshDataSources() {
        try {
            // 通过管理器清除所有自定义数据源（保留MASTER）
            dataSourceManager.clearDataSources();
            
            // 查询所有状态为正常的数据源
            QueryWrapper queryWrapper = QueryWrapper.create()
                .from("sys_data_source")
                .where(new QueryColumn("status").eq("0"));
            List<SysDataSource> dataSources = list(queryWrapper);
            
            // 添加到数据源管理器
            for (SysDataSource dataSource : dataSources) {
                if (!"MASTER".equals(dataSource.getName())) {
                    addToDataSourceManager(dataSource);
                }
            }
            
            return true;
        } catch (Exception e) {
            throw new ServiceException("刷新数据源失败: " + e.getMessage());
        }
    }
    
    /**
     * 将数据源添加到动态数据源管理器中
     * 
     * @param sysDataSource 数据源配置
     */
    private void addToDataSourceManager(SysDataSource sysDataSource) {
        // Directly use sysDataSource with dataSourceManager
        boolean result = dataSourceManager.addOrUpdateDataSource(sysDataSource);
        if (!result) {
            throw new ServiceException("添加数据源到管理器失败，请检查连接信息");
        }
    }
}
