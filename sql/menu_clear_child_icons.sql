-- ============================================================
-- 清除二级及以下菜单图标（仅一级菜单保留图标）
-- 约定：icon = '#' 表示无图标
-- ============================================================

UPDATE sys_menu
SET icon = '#',
    update_by = 'admin',
    update_time = NOW()
WHERE del_flag = '0'
  AND parent_id != 0
  AND menu_type IN ('M', 'C')
  AND (icon IS NULL OR icon = '' OR icon != '#');
