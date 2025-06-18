package com.ruoyi.project.system.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.DbChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.system.domain.SysDictData;
import com.ruoyi.project.system.mapper.SysDictDataMapper;
import com.ruoyi.project.system.service.ISysDictDataService;

import cn.hutool.core.util.ObjectUtil;

/**
 * 字典 业务层处理
 * 
 * @author ruoyi
 */
@Service
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> implements ISysDictDataService
{
    /**
     * 查询字典数据
     * 
     * @param dictType 字典类型
     * @return 字典数据
     */
    @Override
    public int countDictDataByType(String dictType)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_dict_data")
            .where(new QueryColumn("dict_type").eq(dictType));
        return (int) count(queryWrapper);
    }

    /**
     * 同步修改字典类型
     * 
     * @param oldDictType 旧字典类型
     * @param newDictType 新旧字典类型
     * @return 结果
     */
    @Override
    public int updateDictDataType(String oldDictType, String newDictType)
    {
        boolean success = DbChain.table("sys_dict_data")
            .where(new QueryColumn("dict_type").eq(oldDictType))
            .set("dict_type", newDictType)
            .update();
        return success ? 1 : 0;
    }

    /**
     * 根据条件分页查询字典数据
     * 
     * @param dictData 字典数据信息
     * @return 字典数据集合信息
     */
    @Override
    public List<SysDictData> selectDictDataList(SysDictData dictData)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_dict_data")
            .where(new QueryColumn("dict_type").eq(dictData.getDictType(), ObjectUtil.isNotNull(dictData.getDictType())))
            .and(new QueryColumn("dict_label").like(dictData.getDictLabel(), ObjectUtil.isNotEmpty(dictData.getDictLabel())))
            .and(new QueryColumn("status").eq(dictData.getStatus(), ObjectUtil.isNotNull(dictData.getStatus())))
            .orderBy(new QueryColumn("dict_sort").asc());
        return list(queryWrapper);
    }

    /**
     * 根据字典类型和字典键值查询字典数据信息
     * 
     * @param dictType 字典类型
     * @param dictValue 字典键值
     * @return 字典标签
     */
    @Override
    public String selectDictLabel(String dictType, String dictValue)
    {
        SysDictData dictData = getOne(QueryWrapper.create()
            .select("dict_label")
            .from("sys_dict_data")
            .where(new QueryColumn("dict_type").eq(dictType))
            .and(new QueryColumn("dict_value").eq(dictValue)));
        return dictData != null ? dictData.getDictLabel() : "";
    }
}
