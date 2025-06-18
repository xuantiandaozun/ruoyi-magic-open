package com.ruoyi.project.system.service;

import java.util.List;
import com.ruoyi.project.system.domain.SysDictData;
import com.mybatisflex.core.service.IService;

/**
 * 字典 业务层
 * 
 * @author ruoyi
 */
public interface ISysDictDataService extends IService<SysDictData>
{
    /**
     * 查询字典数据
     * 
     * @param dictType 字典类型
     * @return 字典数据
     */
    public int countDictDataByType(String dictType);

    /**
     * 同步修改字典类型
     * 
     * @param oldDictType 旧字典类型
     * @param newDictType 新旧字典类型
     * @return 结果
     */
    public int updateDictDataType(String oldDictType, String newDictType);

    /**
     * 根据条件分页查询字典数据
     * 
     * @param dictData 字典数据信息
     * @return 字典数据集合信息
     */
    public List<SysDictData> selectDictDataList(SysDictData dictData);

    /**
     * 根据字典类型和字典键值查询字典数据信息
     * 
     * @param dictType 字典类型
     * @param dictValue 字典键值
     * @return 字典标签
     */
    public String selectDictLabel(String dictType, String dictValue);
}
