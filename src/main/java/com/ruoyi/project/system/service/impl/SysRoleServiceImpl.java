package com.ruoyi.project.system.service.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.DbChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.project.system.domain.SysRole;
import com.ruoyi.project.system.domain.SysRoleDept;
import com.ruoyi.project.system.domain.SysRoleMenu;
import com.ruoyi.project.system.domain.SysUserRole;
import com.ruoyi.project.system.mapper.SysRoleDeptMapper;
import com.ruoyi.project.system.mapper.SysRoleMapper;
import com.ruoyi.project.system.mapper.SysRoleMenuMapper;
import com.ruoyi.project.system.mapper.SysUserRoleMapper;
import com.ruoyi.project.system.service.ISysRoleService;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;

/**
 * 角色 业务层处理
 * 
 * @author ruoyi
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService
{
    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private SysRoleDeptMapper roleDeptMapper;

    /**
     * 根据条件分页查询角色数据
     * 
     * @param role 角色信息
     * @return 角色数据集合信息
     */
    @Override
    @Cacheable(value = "role", key = "#role.toString()")
    public List<SysRole> selectRoleList(SysRole role)
    {
        QueryWrapper queryWrapper = QueryWrapper.create()
            .from("sys_role")
            .where(new QueryColumn("role_name").like(role.getRoleName(), ObjectUtil.isNotEmpty(role.getRoleName())))
            .and(new QueryColumn("role_key").eq(role.getRoleKey(), ObjectUtil.isNotEmpty(role.getRoleKey())))
            .and(new QueryColumn("status").eq(role.getStatus(), ObjectUtil.isNotNull(role.getStatus())))
            .and(new QueryColumn("create_time").between(role.getParams().get("beginTime"), role.getParams().get("endTime"),
                    ObjectUtil.isNotNull(role.getParams().get("beginTime"))))
            .orderBy(new QueryColumn("role_sort").asc());
        return list(queryWrapper);
    }

    /**
     * 根据用户ID查询角色
     * 
     * @param userId 用户ID
     * @return 角色列表
     */
    @Override
    @Cacheable(value = "role", key = "'user:' + #userId")
    public List<SysRole> selectRolesByUserId(Long userId)
    {
        return mapper.selectRolePermissionByUserId(userId);
    }

    /**
     * 根据用户ID查询权限
     * 
     * @param userId 用户ID
     * @return 权限列表
     */
    @Override
    @Cacheable(value = "role", key = "'perms:' + #userId")
    public Set<String> selectRolePermissionByUserId(Long userId)
    {
        List<SysRole> perms = selectRolesByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (SysRole perm : perms)
        {
            if (ObjectUtil.isNotNull(perm))
            {
                permsSet.addAll(Arrays.asList(perm.getRoleKey().trim().split(",")));
            }
        }
        return permsSet;
    }

    /**
     * 查询所有角色
     * 
     * @return 角色列表
     */
    @Override
    @Cacheable(value = "role", key = "'all'")
    public List<SysRole> selectRoleAll()
    {
        return SpringUtils.getAopProxy(this).selectRoleList(new SysRole());
    }

    /**
     * 根据用户ID获取角色选择框列表
     * 
     * @param userId 用户ID
     * @return 选中角色ID列表
     */
    @Override
    public List<Long> selectRoleListByUserId(Long userId)
    {
        return mapper.selectRoleListByUserId(userId);
    }

    /**
     * 校验角色名称是否唯一
     * 
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public boolean checkRoleNameUnique(SysRole role)
    {
        Long roleId = ObjectUtil.isNull(role.getRoleId()) ? -1L : role.getRoleId();
        
        SysRole info = getOne(QueryWrapper.create()
            .from("sys_role")
            .where(new QueryColumn("role_name").eq(role.getRoleName()))
            .limit(1));
            
        if (ObjectUtil.isNotNull(info) && info.getRoleId().longValue() != roleId.longValue())
        {
            return false;
        }
        return true;
    }

    /**
     * 校验角色权限是否唯一
     * 
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public boolean checkRoleKeyUnique(SysRole role)
    {
        Long roleId = ObjectUtil.isNull(role.getRoleId()) ? -1L : role.getRoleId();
        
        SysRole info = getOne(QueryWrapper.create()
            .from("sys_role")
            .where(new QueryColumn("role_key").eq(role.getRoleKey()))
            .limit(1));
            
        if (ObjectUtil.isNotNull(info) && info.getRoleId().longValue() != roleId.longValue())
        {
            return false;
        }
        return true;
    }

    /**
     * 校验角色是否允许操作
     * 
     * @param role 角色信息
     */
    @Override
    public void checkRoleAllowed(SysRole role)
    {
        if (ObjectUtil.isNotNull(role.getRoleId()) && role.isAdmin())
        {
            throw new ServiceException("不允许操作超级管理员角色");
        }
    }

    /**
     * 校验角色是否有数据权限
     * 
     * @param roleIds 角色id
     */
    @Override
    public void checkRoleDataScope(Long... roleIds)
    {
        if (!SysRole.isAdmin(SecurityUtils.getUserId()))
        {
            for (Long roleId : roleIds)
            {
                SysRole role = new SysRole();
                role.setRoleId(roleId);
                List<SysRole> roles = SpringUtils.getAopProxy(this).selectRoleList(role);
                if (CollUtil.isEmpty(roles))
                {
                    throw new ServiceException("没有权限访问角色数据！");
                }
            }
        }
    }

    /**
     * 通过角色ID查询角色使用数量
     * 
     * @param roleId 角色ID
     * @return 结果
     */
    @Override
    public int countUserRoleByRoleId(Long roleId)
    {
        return Math.toIntExact(userRoleMapper.selectCountByQuery(
            QueryWrapper.create()
                .from("sys_user_role")
                .where(new QueryColumn("role_id").eq(roleId))
        ));
    }

    /**
     * 修改角色状态
     * 
     * @param role 角色信息
     * @return 结果
     */
    @Override
    @CacheEvict(value = "role", allEntries = true)
    public boolean updateRoleStatus(SysRole role)
    {
        return DbChain.table("sys_role")
            .where("role_id = ?", role.getRoleId())
            .set("status", role.getStatus())
            .update();
    }

    /**
     * 修改数据权限信息
     * 
     * @param role 角色信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "role", allEntries = true)
    public boolean authDataScope(SysRole role)
    {
        // 修改角色信息
        DbChain.table("sys_role")
            .where("role_id = ?", role.getRoleId())
            .set("data_scope", role.getDataScope())
            .update();
            
        // 删除角色与部门关联
        DbChain.table("sys_role_dept")
            .where("role_id = ?", role.getRoleId())
            .remove();
            
        // 新增角色和部门信息（数据权限）
        return insertRoleDept(role);
    }

    /**
     * 新增角色菜单信息
     * 
     * @param role 角色对象
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "role", allEntries = true)
    public boolean insertRoleMenu(SysRole role)
    {
        if (role.getMenuIds() == null || role.getMenuIds().length == 0)
        {
            return true;
        }
        
        List<SysRoleMenu> list = Arrays.stream(role.getMenuIds())
            .map(menuId -> {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(role.getRoleId());
                rm.setMenuId(menuId);
                return rm;
            })
            .collect(Collectors.toList());
            
        return roleMenuMapper.insertBatch(list) > 0;
    }

    /**
     * 删除角色菜单关联信息
     * 
     * @param roleId 角色ID
     * @return 结果
     */
    @Override
    @CacheEvict(value = "role", allEntries = true)
    public boolean deleteRoleMenuByRoleId(Long roleId)
    {
        return DbChain.table("sys_role_menu")
            .where("role_id = ?", roleId)
            .remove();
    }

    /**
     * 新增角色部门信息(数据权限)
     *
     * @param role 角色对象
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "role", allEntries = true)
    public boolean insertRoleDept(SysRole role)
    {
        if (role.getDeptIds() == null || role.getDeptIds().length == 0)
        {
            return true;
        }
        
        List<SysRoleDept> list = Arrays.stream(role.getDeptIds())
            .map(deptId -> {
                SysRoleDept rd = new SysRoleDept();
                rd.setRoleId(role.getRoleId());
                rd.setDeptId(deptId);
                return rd;
            })
            .collect(Collectors.toList());
            
        return roleDeptMapper.insertBatch(list) > 0;
    }

    /**
     * 取消授权用户角色
     * 
     * @param userRole 用户和角色关联信息
     * @return 结果
     */
    @Override
    @CacheEvict(value = "role", allEntries = true)
    public boolean deleteAuthUser(SysUserRole userRole)
    {
        return DbChain.table("sys_user_role")
            .where("user_id = ?", userRole.getUserId())
            .and("role_id = ?", userRole.getRoleId())
            .remove();
    }

    /**
     * 批量取消授权用户角色
     * 
     * @param roleId 角色ID
     * @param userIds 需要取消授权的用户数据ID
     * @return 结果
     */
    @Override
    @CacheEvict(value = "role", allEntries = true)
    public boolean deleteAuthUsers(Long roleId, Long[] userIds)
    {
        return DbChain.table("sys_user_role")
            .where("role_id = ?", roleId)
            .where("user_id IN (" + String.join(",", Arrays.stream(userIds).map(String::valueOf).collect(Collectors.toList())) + ")")
            .remove();
    }

    /**
     * 批量选择授权用户角色
     * 
     * @param roleId 角色ID
     * @param userIds 需要删除的用户数据ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "role", allEntries = true)
    public boolean insertAuthUsers(Long roleId, Long[] userIds)
    {
        if (userIds == null || userIds.length == 0)
        {
            return true;
        }
        
        List<SysUserRole> list = Arrays.stream(userIds)
            .map(userId -> {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                return ur;
            })
            .collect(Collectors.toList());
            
        return userRoleMapper.insertBatch(list) > 0;
    }
}
