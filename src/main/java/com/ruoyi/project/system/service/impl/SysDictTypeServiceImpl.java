package com.ruoyi.project.system.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.core.annotation.Order;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.project.system.domain.SysDictData;
import com.ruoyi.project.system.domain.SysDictType;
import com.ruoyi.project.system.mapper.SysDictDataMapper;
import com.ruoyi.project.system.mapper.SysDictTypeMapper;
import com.ruoyi.project.system.service.ISysDictTypeService;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Async;
/**
 * 字典 业务层处理
 * 
 * @author ruoyi
 */
@Service
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType> implements ISysDictTypeService
{


    @Autowired
    private SysDictDataMapper dictDataMapper;

    /**
     * 项目启动时，初始化字典到缓存
     */
    @PostConstruct
    public void init()
    {
        initializeDictCacheAsync();
    }

    /**
     * 异步初始化字典缓存
     */
    @Async("threadPoolTaskExecutor")
    @Order(2)
    public void initializeDictCacheAsync()
    {
        loadingDictCache();
    }

    /**
     * 根据条件分页查询字典类型
     * 
     * @param dictType 字典类型信息
     * @return 字典类型集合信息
     */
    @Override
    public List<SysDictType> selectDictTypeList(SysDictType dictType)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_dict_type")
            .where(new QueryColumn("dict_name").like(dictType.getDictName(), ObjectUtil.isNotEmpty(dictType.getDictName())))
            .and(new QueryColumn("dict_type").like(dictType.getDictType(), ObjectUtil.isNotEmpty(dictType.getDictType())))
            .and(new QueryColumn("status").eq(dictType.getStatus(), ObjectUtil.isNotNull(dictType.getStatus())))
            .orderBy(new QueryColumn("dict_id").asc());
        return list(queryWrapper);
    }

    /**
     * 根据所有字典类型
     * 
     * @return 字典类型集合信息
     */
    @Override
    public List<SysDictType> selectDictTypeAll()
    {
        return list(QueryWrapper.create()
            .from("sys_dict_type")
            .orderBy(new QueryColumn("dict_id").asc()));
    }

    /**
     * 根据字典类型查询字典数据
     * 
     * @param dictType 字典类型
     * @return 字典数据集合信息
     */
    @Override
    public List<SysDictData> selectDictDataByType(String dictType)
    {
        List<SysDictData> dictDatas = DictUtils.getDictCache(dictType);
        if (CollUtil.isNotEmpty(dictDatas))
        {
            return dictDatas;
        }
        
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_dict_data")
            .where(new QueryColumn("dict_type").eq(dictType))
            .and(new QueryColumn("status").eq("0"))
            .orderBy(new QueryColumn("dict_sort").asc());
               
        dictDatas = dictDataMapper.selectListByQuery(queryWrapper);
        if (CollUtil.isNotEmpty(dictDatas))
        {
            DictUtils.setDictCache(dictType, dictDatas);
            return dictDatas;
        }
        return null;
    }

    /**
     * 根据字典类型查询信息
     * 
     * @param dictType 字典类型
     * @return 字典类型
     */
    @Override
    public SysDictType selectDictTypeByType(String dictType)
    {
        return getOne(QueryWrapper.create()
            .from("sys_dict_type")
            .where(new QueryColumn("dict_type").eq(dictType)));
    }

    /**
     * 加载字典缓存数据
     */
    @Override
    public void loadingDictCache()
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_dict_data")
            .where(new QueryColumn("status").eq("0"))
            .orderBy(new QueryColumn("dict_sort").asc());
               
        Map<String, List<SysDictData>> dictDataMap = dictDataMapper.selectListByQuery(queryWrapper)
                .stream().collect(Collectors.groupingBy(SysDictData::getDictType));
                
        for (Map.Entry<String, List<SysDictData>> entry : dictDataMap.entrySet())
        {
            DictUtils.setDictCache(entry.getKey(), entry.getValue().stream()
                    .sorted(Comparator.comparing(SysDictData::getDictSort))
                    .collect(Collectors.toList()));
        }
    }

    /**
     * 清空字典缓存数据
     */
    @Override
    public void clearDictCache()
    {
        DictUtils.clearDictCache();
    }

    /**
     * 重置字典缓存数据
     */
    @Override
    public void resetDictCache()
    {
        clearDictCache();
        loadingDictCache();
    }

    /**
     * 校验字典类型称是否唯一
     * 
     * @param dict 字典类型
     * @return 结果
     */
    @Override
    public boolean checkDictTypeUnique(SysDictType dict)
    {
        Long dictId = ObjectUtil.isNull(dict.getDictId()) ? -1L : dict.getDictId();
        SysDictType dictType = getOne(QueryWrapper.create()
            .from("sys_dict_type")
            .where(new QueryColumn("dict_type").eq(dict.getDictType())));
            
        if (ObjectUtil.isNotNull(dictType) && dictType.getDictId().longValue() != dictId.longValue())
        {
            return false;
        }
        return true;
    }
}
