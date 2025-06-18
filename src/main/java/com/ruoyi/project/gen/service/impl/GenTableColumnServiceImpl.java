package com.ruoyi.project.gen.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.gen.domain.GenTableColumn;
import com.ruoyi.project.gen.mapper.GenTableColumnMapper;
import com.ruoyi.project.gen.service.IGenTableColumnService;


/**
 * 业务字段 服务层实现
 * 
 * @author ruoyi
 */
@Service
public class GenTableColumnServiceImpl extends ServiceImpl<GenTableColumnMapper, GenTableColumn> implements IGenTableColumnService
{


    /**
     * 查询业务字段列表
     * 
     * @param tableId 业务字段编号
     * @return 业务字段集合
     */
    @Override
    public List<GenTableColumn> selectGenTableColumnListByTableId(Long tableId)
    {
        QueryWrapper query = QueryWrapper.create()
            .from("gen_table_column")
            .where(new QueryColumn("table_id").eq(tableId))
            .orderBy("sort asc");
        return this.list(query);
    }

    /**
     * 新增业务字段
     * 
     * @param genTableColumn 业务字段信息
     * @return 结果
     */
    @Override
    public boolean insertGenTableColumn(GenTableColumn genTableColumn)
    {
        return this.save(genTableColumn);
    }

    /**
     * 修改业务字段
     * 
     * @param genTableColumn 业务字段信息
     * @return 结果
     */
    @Override
    public boolean updateGenTableColumn(GenTableColumn genTableColumn)
    {
        return this.updateById(genTableColumn);
    }

    /**
     * 删除业务字段信息
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    public boolean deleteGenTableColumnByIds(String ids)
    {
        return this.removeByIds(Arrays.asList(ids.split(",")));
    }

    /**
     * 删除业务字段信息
     * 
     * @param genTableColumns 需要删除的列数据
     * @return 结果
     */
    @Override
    public boolean deleteGenTableColumns(List<GenTableColumn> genTableColumns)
    {
        List<Long> ids = genTableColumns.stream().map(GenTableColumn::getColumnId).collect(Collectors.toList());
        return this.removeByIds(ids);
    }    /**
     * 根据表名称查询列信息
     * 
     * @param tableName 表名称
     * @return 列信息
     */
    @Override
    public List<GenTableColumn> selectDbTableColumnsByName(String tableName)
    {
        String sql = "SELECT column_name, " +
                "(CASE WHEN (is_nullable = 'no' AND column_key != 'PRI') THEN '1' ELSE '0' END) AS is_required, " +
                "(CASE WHEN column_key = 'PRI' THEN '1' ELSE '0' END) AS is_pk, " +
                "ordinal_position AS sort, column_comment, " +
                "(CASE WHEN extra = 'auto_increment' THEN '1' ELSE '0' END) AS is_increment, " +
                "column_type " +
                "FROM information_schema.columns " +
                "WHERE table_schema = (SELECT DATABASE()) AND table_name = ? " +
                "ORDER BY ordinal_position";
        
        List<Row> rows = Db.selectListBySql(sql, tableName);
        return rows.stream().map(row -> row.toEntity(GenTableColumn.class)).collect(Collectors.toList());
    }
      /**
     * 根据表名称和数据库名称查询列信息
     * 
     * @param tableName 表名称
     * @param dbName 数据库名称
     * @return 列信息
     */
    @Override
    public List<GenTableColumn> selectDbTableColumnsByNameAndDataSource(String tableName, String dbName)
    {
        String sql = "SELECT column_name, " +
                "(CASE WHEN (is_nullable = 'no' AND column_key != 'PRI') THEN '1' ELSE '0' END) AS is_required, " +
                "(CASE WHEN column_key = 'PRI' THEN '1' ELSE '0' END) AS is_pk, " +
                "ordinal_position AS sort, column_comment, " +
                "(CASE WHEN extra = 'auto_increment' THEN '1' ELSE '0' END) AS is_increment, " +
                "column_type " +
                "FROM information_schema.columns " +
                "WHERE table_schema = ? AND table_name = ? " +
                "ORDER BY ordinal_position";
        
        List<Row> rows = Db.selectListBySql(sql, dbName, tableName);
        return rows.stream().map(row -> row.toEntity(GenTableColumn.class)).collect(Collectors.toList());
    }
}