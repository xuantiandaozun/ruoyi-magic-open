-- 修改RDS实例管理菜单权限,从system模块迁移到aliyun模块
-- 作者: 系统管理员
-- 日期: 2025-12-26
-- 说明: 配合前后端代码重构,将RDS实例管理功能从system模块迁移到aliyun模块

-- 1. 修改RDS实例管理主菜单
UPDATE `sys_menu` 
SET `component` = 'aliyun/rdsInstance/index',
    `perms` = 'aliyun:rdsInstance:list',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` = 2049;

-- 2. 修改RDS实例管理查询权限
UPDATE `sys_menu` 
SET `perms` = 'aliyun:rdsInstance:query',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` = 2050;

-- 3. 修改RDS实例管理新增权限
UPDATE `sys_menu` 
SET `perms` = 'aliyun:rdsInstance:add',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` = 2051;

-- 4. 修改RDS实例管理修改权限
UPDATE `sys_menu` 
SET `perms` = 'aliyun:rdsInstance:edit',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` = 2052;

-- 5. 修改RDS实例管理删除权限
UPDATE `sys_menu` 
SET `perms` = 'aliyun:rdsInstance:remove',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` = 2053;

-- 6. 修改RDS实例管理导出权限
UPDATE `sys_menu` 
SET `perms` = 'aliyun:rdsInstance:export',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` = 2054;

-- 查询验证
SELECT menu_id, menu_name, parent_id, path, component, perms 
FROM `sys_menu` 
WHERE `menu_id` BETWEEN 2049 AND 2054 
ORDER BY menu_id;
