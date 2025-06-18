package com.ruoyi.project.system.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.project.system.domain.SysRole;

/**
 * 角色表 数据层
 * 
 * @author ruoyi
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {
    /**
     * 根据用户ID查询角色
     * 
     * @param userId 用户ID
     * @return 角色列表
     */
    default List<SysRole> selectRolePermissionByUserId(@Param("userId") Long userId) {
        return selectListByQuery(QueryWrapper.create()
                .select("DISTINCT r.role_id, r.role_name, r.role_key, r.role_sort, r.data_scope, " +
                        "r.menu_check_strictly, r.dept_check_strictly, r.status, r.del_flag, " +
                        "r.create_time, r.remark")
                .from("sys_role").as("r")
                .innerJoin("sys_user_role").as("ur").on("ur.role_id = r.role_id")
                .innerJoin("sys_user").as("u").on("u.user_id = ur.user_id")
                .where(new QueryColumn("r", "del_flag").eq("0"))
                .and(new QueryColumn("ur", "user_id").eq(userId)));
    }

    /**
     * 根据用户ID获取角色选择框列表
     * 
     * @param userId 用户ID
     * @return 选中角色ID列表
     */
    default List<Long> selectRoleListByUserId(@Param("userId") Long userId) {
        return selectListByQueryAs(QueryWrapper.create()
                .select("r.role_id")
                .from("sys_role").as("r")
                .leftJoin("sys_user_role").as("ur").on("ur.role_id = r.role_id")
                .leftJoin("sys_user").as("u").on("u.user_id = ur.user_id")
                .where(new QueryColumn("u", "user_id").eq(userId)), Long.class);
    }
}
