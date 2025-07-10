package com.ruoyi.project.gen.tools.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.system.domain.SysDictData;
import com.ruoyi.project.system.domain.SysDictType;
import com.ruoyi.project.system.service.ISysDictDataService;
import com.ruoyi.project.system.service.ISysDictTypeService;

import cn.hutool.core.util.StrUtil;

/**
 * 字典管理工具
 * 提供字典类型和字典数据的管理操作
 */
@Service
public class DictTool {
    private static final Logger logger = LoggerFactory.getLogger(DictTool.class);

    @Autowired
    private ISysDictDataService sysDictDataService;

    @Autowired
    private ISysDictTypeService sysDictTypeService;

    /**
     * 查询字典类型列表
     */
    @Tool(name = "getDictTypeList", description = "查询字典类型列表")
    public Map<String, Object> getDictTypeList(String dictName, String dictType, String status, Integer pageNum, Integer pageSize) {
        try {
            logger.info("getDictTypeList查询字典类型列表");
            
            // 限制每页最大500条记录
            if (pageSize == null || pageSize > 500) {
                pageSize = 500;
            }
            if (pageNum == null || pageNum < 1) {
                pageNum = 1;
            }
            
            // 构建查询条件
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .select()
                    .from("sys_dict_type");
            
            // 添加字典名称条件
            if (StrUtil.isNotBlank(dictName)) {
                queryWrapper.and(new QueryColumn("dict_name").like(dictName));
            }
            
            // 添加字典类型条件
            if (StrUtil.isNotBlank(dictType)) {
                queryWrapper.and(new QueryColumn("dict_type").like(dictType));
            }
            
            // 添加状态条件
            if (StrUtil.isNotBlank(status)) {
                queryWrapper.and(new QueryColumn("status").eq(status));
            }
            
            // 添加排序
            queryWrapper.orderBy(new QueryColumn("dict_id").asc());

            // 创建分页对象
            Page<SysDictType> pageObj = Page.of(pageNum, pageSize);
            
            // 执行分页查询
            Page<SysDictType> page = sysDictTypeService.page(pageObj, queryWrapper);
            
            Map<String, Object> result = new HashMap<>();
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            result.put("totalCount", page != null ? page.getTotalRow() : 0);
            result.put("totalPage", page != null ? page.getTotalPage() : 0);
            
            if (page == null || page.getRecords().isEmpty()) {
                result.put("message", "没有找到匹配的字典类型");
                result.put("dictTypeList", new ArrayList<>());
                return result;
            }
            
            List<SysDictType> dictTypeList = new ArrayList<>(page.getRecords());
            result.put("dictTypeList", dictTypeList);
            result.put("message", "查询字典类型列表成功");
            
            return result;
        } catch (Exception e) {
            logger.error("查询字典类型列表失败", e);
            throw new ServiceException("查询字典类型列表失败：" + e.getMessage());
        }
    }

    /**
     * 新增字典类型
     */
    @Tool(name = "addDictType", description = "新增字典类型")
    public Map<String, Object> addDictType(SysDictType dictType) {
        try {
            logger.info("addDictType新增字典类型: {}", dictType);
            boolean save = sysDictTypeService.save(dictType);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", save);
            result.put("dictType", dictType);
            result.put("message", save ? "字典类型新增成功" : "字典类型新增失败");
            
            return result;
        } catch (Exception e) {
            logger.error("新增字典类型失败", e);
            throw new ServiceException("新增字典类型失败：" + e.getMessage());
        }
    }

    /**
     * 修改字典类型
     */
    @Tool(name = "updateDictType", description = "修改字典类型")
    public Map<String, Object> updateDictType(SysDictType dictType, String dictId) {
        try {
            logger.info("updateDictType修改字典类型: {}, dictId: {}", dictType, dictId);

            // 如果提供了字符串ID，设置到实体类中
            if (StrUtil.isNotBlank(dictId)) {
                try {
                    dictType.setDictId(Long.parseLong(dictId));
                } catch (NumberFormatException e) {
                    logger.error("字典类型ID格式错误: {}", dictId);
                    throw new ServiceException("字典类型ID格式错误");
                }
            }

            boolean update = sysDictTypeService.updateById(dictType);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", update);
            result.put("dictType", dictType);
            result.put("dictId", dictId);
            result.put("message", update ? "字典类型修改成功" : "字典类型修改失败");
            
            return result;
        } catch (Exception e) {
            logger.error("修改字典类型失败", e);
            throw new ServiceException("修改字典类型失败：" + e.getMessage());
        }
    }

    /**
     * 查询字典数据列表
     */
    @Tool(name = "getDictDataList", description = "查询字典数据列表")
    public Map<String, Object> getDictDataList(String dictType, String dictLabel, String status, Integer pageNum, Integer pageSize) {
        try {
            logger.info("getDictDataList查询字典数据列表");
            
            // 限制每页最大500条记录
            if (pageSize == null || pageSize > 500) {
                pageSize = 500;
            }
            if (pageNum == null || pageNum < 1) {
                pageNum = 1;
            }
            
            // 构建查询条件
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .select()
                    .from("sys_dict_data");
            
            // 添加字典类型条件
            if (StrUtil.isNotBlank(dictType)) {
                queryWrapper.and(new QueryColumn("dict_type").like(dictType));
            }
            
            // 添加字典标签条件
            if (StrUtil.isNotBlank(dictLabel)) {
                queryWrapper.and(new QueryColumn("dict_label").like(dictLabel));
            }
            
            // 添加状态条件
            if (StrUtil.isNotBlank(status)) {
                queryWrapper.and(new QueryColumn("status").eq(status));
            }
            
            // 添加排序
            queryWrapper.orderBy(new QueryColumn("dict_sort").asc());

            // 创建分页对象
            Page<SysDictData> pageObj = Page.of(pageNum, pageSize);
            
            // 执行分页查询
            Page<SysDictData> page = sysDictDataService.page(pageObj, queryWrapper);
            
            Map<String, Object> result = new HashMap<>();
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            result.put("totalCount", page != null ? page.getTotalRow() : 0);
            result.put("totalPage", page != null ? page.getTotalPage() : 0);
            
            if (page == null || page.getRecords().isEmpty()) {
                result.put("message", "没有找到匹配的字典数据");
                result.put("dictDataList", new ArrayList<>());
                return result;
            }
            
            List<SysDictData> dictDataList = new ArrayList<>(page.getRecords());
            result.put("dictDataList", dictDataList);
            result.put("message", "查询字典数据列表成功");
            
            return result;
        } catch (Exception e) {
            logger.error("查询字典数据列表失败", e);
            throw new ServiceException("查询字典数据列表失败：" + e.getMessage());
        }
    }

    /**
     * 新增字典数据
     */
    @Tool(name = "addDictData", description = "新增字典数据")
    public Map<String, Object> addDictData(SysDictData dictData) {
        try {
            logger.info("addDictData新增字典数据: {}", dictData);
            boolean success = sysDictDataService.save(dictData);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("dictData", dictData);
            result.put("message", success ? "字典数据新增成功" : "字典数据新增失败");
            
            return result;
        } catch (Exception e) {
            logger.error("新增字典数据失败", e);
            throw new ServiceException("新增字典数据失败：" + e.getMessage());
        }
    }

    /**
     * 修改字典数据
     */
    @Tool(name = "updateDictData", description = "修改字典数据")
    public Map<String, Object> updateDictData(SysDictData dictData, String dictCode) {
        try {
            logger.info("updateDictData修改字典数据: {}, dictCode: {}", dictData, dictCode);

            // 如果提供了字符串ID，设置到实体类中
            if (StrUtil.isNotBlank(dictCode)) {
                try {
                    dictData.setDictCode(Long.parseLong(dictCode));
                } catch (NumberFormatException e) {
                    logger.error("字典数据ID格式错误: {}", dictCode);
                    throw new ServiceException("字典数据ID格式错误");
                }
            }

            boolean success = sysDictDataService.updateById(dictData);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("dictData", dictData);
            result.put("dictCode", dictCode);
            result.put("message", success ? "字典数据修改成功" : "字典数据修改失败");
            
            return result;
        } catch (Exception e) {
            logger.error("修改字典数据失败", e);
            throw new ServiceException("修改字典数据失败：" + e.getMessage());
        }
    }
}