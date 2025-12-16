package com.ruoyi.project.bill.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.bill.domain.BillCategory;
import com.ruoyi.project.bill.mapper.BillCategoryMapper;
import com.ruoyi.project.bill.service.IBillCategoryService;

/**
 * 账单分类Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Service
public class BillCategoryServiceImpl extends ServiceImpl<BillCategoryMapper, BillCategory>
        implements IBillCategoryService {
    @Override
    public List<BillCategory> selectCategoryTree(String categoryType) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("category_type", categoryType)
                .eq("status", "0")
                .orderBy("parent_id ASC")
                .orderBy("sort_order ASC");

        return this.list(queryWrapper);
    }

    @Override
    public List<BillCategory> selectTopCategoryList(String categoryType) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("category_type", categoryType)
                .eq("parent_id", 0)
                .eq("status", "0")
                .orderBy("sort_order ASC");

        return this.list(queryWrapper);
    }

    @Override
    public List<BillCategory> selectChildrenByParentId(Long parentId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("parent_id", parentId)
                .eq("status", "0")
                .orderBy("sort_order ASC");

        return this.list(queryWrapper);
    }
}
