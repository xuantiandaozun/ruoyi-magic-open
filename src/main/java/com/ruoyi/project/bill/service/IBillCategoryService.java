package com.ruoyi.project.bill.service;

import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.bill.domain.BillCategory;

/**
 * 账单分类Service接口
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
public interface IBillCategoryService extends IService<BillCategory> {
    /**
     * 查询分类树结构
     * 
     * @param categoryType 分类类型（0支出 1收入）
     * @return 分类树列表
     */
    List<BillCategory> selectCategoryTree(String categoryType);

    /**
     * 根据类型查询一级分类列表
     * 
     * @param categoryType 分类类型（0支出 1收入）
     * @return 一级分类列表
     */
    List<BillCategory> selectTopCategoryList(String categoryType);

    /**
     * 根据父ID查询子分类列表
     * 
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    List<BillCategory> selectChildrenByParentId(Long parentId);
}
