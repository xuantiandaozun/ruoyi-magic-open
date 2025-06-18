package com.ruoyi.project.system.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.DbChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.framework.web.domain.TreeSelect;
import com.ruoyi.project.system.domain.SysDept;
import com.ruoyi.project.system.domain.SysUser;
import com.ruoyi.project.system.mapper.SysDeptMapper;
import com.ruoyi.project.system.mapper.SysUserMapper;
import com.ruoyi.project.system.service.ISysDeptService;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;


/**
 * 部门管理 服务实现
 * 
 * @author ruoyi
 */
@Service
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements ISysDeptService
{

    @Autowired
    private SysUserMapper userMapper;

    /**
     * 修改所在部门正常状态
     * 
     * @param deptIds 部门ID组
     */
    @Override
    public void updateDeptStatusNormal(Long[] deptIds)
    {
        DbChain.table("sys_dept")
            .where(new QueryColumn("dept_id").in(Arrays.asList(deptIds)))
            .set("status", "0")
            .update();
    }

    /**
     * 查询部门管理数据
     * 
     * @param dept 部门信息
     * @return 部门信息集合
     */
    @Override
    public List<SysDept> selectDeptList(SysDept dept)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_dept")
            .where(new QueryColumn("dept_name").like(dept.getDeptName(), ObjectUtil.isNotEmpty(dept.getDeptName())))
            .and(new QueryColumn("status").eq(dept.getStatus(), ObjectUtil.isNotNull(dept.getStatus())))
            .and(new QueryColumn("parent_id").eq(dept.getParentId(), ObjectUtil.isNotNull(dept.getParentId())))
            .orderBy(new QueryColumn("parent_id").asc())
            .orderBy(new QueryColumn("order_num").asc());
               
        return this.list(queryWrapper);
    }

    /**
     * 查询部门树结构信息
     * 
     * @param dept 部门信息
     * @return 部门树信息集合
     */
    @Override
    public List<TreeSelect> selectDeptTreeList(SysDept dept)
    {
        List<SysDept> depts = selectDeptList(dept);
        return buildDeptTreeSelect(depts);
    }

    /**
     * 构建前端所需要树结构
     * 
     * @param depts 部门列表
     * @return 树结构列表
     */
    @Override
    public List<SysDept> buildDeptTree(List<SysDept> depts)
    {
        List<SysDept> returnList = new ArrayList<SysDept>();
        List<Long> tempList = depts.stream().map(SysDept::getDeptId).collect(Collectors.toList());
        for (SysDept dept : depts)
        {
            // 如果是顶级节点, 遍历该父节点的所有子节点
            if (!tempList.contains(dept.getParentId()))
            {
                recursionFn(depts, dept);
                returnList.add(dept);
            }
        }
        if (returnList.isEmpty())
        {
            returnList = depts;
        }
        return returnList;
    }

    /**
     * 构建前端所需要下拉树结构
     * 
     * @param depts 部门列表
     * @return 下拉树结构列表
     */
    @Override
    public List<TreeSelect> buildDeptTreeSelect(List<SysDept> depts)
    {
        List<SysDept> deptTrees = buildDeptTree(depts);
        return deptTrees.stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    /**
     * 根据角色ID查询部门树信息
     * 
     * @param roleId 角色ID
     * @return 选中部门列表
     */
    @Override
    public List<Long> selectDeptListByRoleId(Long roleId, boolean deptCheckStrictly) {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .select("dept_id")
            .from("sys_dept")
            .where("dept_id IN (SELECT dept_id FROM sys_role_dept WHERE role_id = " + roleId + ")");
        
        if (deptCheckStrictly) {
            queryWrapper.and("dept_id NOT IN (SELECT d.parent_id FROM sys_dept d INNER JOIN sys_role_dept rd ON d.dept_id = rd.dept_id AND rd.role_id = " + roleId + ")");
        }
        
        queryWrapper.orderBy(new QueryColumn("parent_id").asc())
                   .orderBy(new QueryColumn("order_num").asc());
                   
        return list(queryWrapper).stream().map(SysDept::getDeptId).collect(Collectors.toList());
    }

    /**
     * 根据ID查询所有子部门（正常状态）
     * 
     * @param deptId 部门ID
     * @return 子部门数
     */
    @Override
    public int selectNormalChildrenDeptById(Long deptId)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_dept")
            .where(new QueryColumn("parent_id").eq(deptId))
            .and(new QueryColumn("status").eq("0"));
        return Math.toIntExact(this.count(queryWrapper));
    }

    /**
     * 是否存在子节点
     * 
     * @param deptId 部门ID
     * @return 结果
     */
    @Override
    public boolean hasChildByDeptId(Long deptId)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_dept")
            .where(new QueryColumn("parent_id").eq(deptId));
        return this.count(queryWrapper) > 0;
    }

    /**
     * 查询部门是否存在用户
     * 
     * @param deptId 部门ID
     * @return 结果 true 存在 false 不存在
     */
    @Override
    public boolean checkDeptExistUser(Long deptId)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_user")
            .where(new QueryColumn("dept_id").eq(deptId));
        return userMapper.selectCountByQuery(queryWrapper) > 0;
    }

    /**
     * 校验部门名称是否唯一
     * 
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    public boolean checkDeptNameUnique(SysDept dept)
    {
        Long deptId = ObjectUtil.isNull(dept.getDeptId()) ? -1L : dept.getDeptId();
        
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_dept")
            .where(new QueryColumn("dept_name").eq(dept.getDeptName()))
            .and(new QueryColumn("parent_id").eq(dept.getParentId()));
               
        SysDept info = this.getOne(queryWrapper);
        
        if (ObjectUtil.isNotNull(info) && info.getDeptId().longValue() != deptId.longValue())
        {
            return false;
        }
        return true;
    }

    /**
     * 校验部门是否有数据权限
     * 
     * @param deptId 部门id
     */
    @Override
    public void checkDeptDataScope(Long deptId)
    {
        if (!SysUser.isAdmin(SecurityUtils.getUserId()))
        {
            SysDept dept = new SysDept();
            dept.setDeptId(deptId);
            List<SysDept> depts = SpringUtils.getAopProxy(this).selectDeptList(dept);
            if (CollUtil.isEmpty(depts))
            {
                throw new ServiceException("没有权限访问部门数据！");
            }
        }
    }

    /**
     * 递归列表
     */
    private void recursionFn(List<SysDept> list, SysDept t)
    {
        // 得到子节点列表
        List<SysDept> childList = getChildList(list, t);
        t.setChildren(childList);
        for (SysDept tChild : childList)
        {
            if (hasChild(list, tChild))
            {
                recursionFn(list, tChild);
            }
        }
    }

    /**
     * 得到子节点列表
     */
    private List<SysDept> getChildList(List<SysDept> list, SysDept t)
    {
        List<SysDept> tlist = new ArrayList<SysDept>();
        Iterator<SysDept> it = list.iterator();
        while (it.hasNext())
        {
            SysDept n = (SysDept) it.next();
            if (ObjectUtil.isNotNull(n.getParentId()) && n.getParentId().longValue() == t.getDeptId().longValue())
            {
                tlist.add(n);
            }
        }
        return tlist;
    }

    /**
     * 判断是否有子节点
     */
    private boolean hasChild(List<SysDept> list, SysDept t)
    {
        return getChildList(list, t).size() > 0;
    }

    @Override
    public List<SysDept> selectChildrenDeptById(Long deptId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_dept")
            .where(new QueryColumn("ancestors").like(deptId.toString()));
        return list(queryWrapper);
    }

    @Override
    public int updateDeptChildren(List<SysDept> depts) {
        int result = 0;
        for (SysDept dept : depts) {
            boolean success = DbChain.table("sys_dept")
                .where(new QueryColumn("dept_id").eq(dept.getDeptId()))
                .set("ancestors", dept.getAncestors())
                .update();
            if (success) {
                result++;
            }
        }
        return result;
    }
}
