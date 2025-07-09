package com.ruoyi.project.system.mapper;

import java.util.List;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.project.system.domain.SysDataSource;

/**
 * 数据源配置Mapper接口
 * 
 * @author ruoyi-magic
 */
public interface SysDataSourceMapper extends BaseMapper<SysDataSource> {

    /**
     * 查询所有启用的数据源（排除主数据源）
     * 
     * @return 启用的数据源列表
     */
    default List<SysDataSource> selectActiveDataSources() {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .where("status = '0'")
            .and("name != 'MASTER'");
        return selectListByQuery(queryWrapper);
    }
}
