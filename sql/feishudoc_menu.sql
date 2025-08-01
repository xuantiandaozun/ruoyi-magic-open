-- 菜单 SQL
-- @SQL_STATEMENT_START
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('飞书文档信息', '2055', '1', 'feishudoc', 'feishu/feishudoc/index', 1, 0, 'C', '0', '0', 'feishu:feishudoc:list', '#', 'admin', sysdate(), '', null, '飞书文档信息菜单');
-- @SQL_STATEMENT_END

-- 记录新插入的主菜单ID
-- @SQL_STATEMENT_START
SET @parentId = LAST_INSERT_ID();
-- @SQL_STATEMENT_END

-- 按钮 SQL
-- @SQL_STATEMENT_START
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('飞书文档信息查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'feishu:feishudoc:query',        '#', 'admin', sysdate(), '', null, '');
-- @SQL_STATEMENT_END

-- @SQL_STATEMENT_START
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('飞书文档信息新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'feishu:feishudoc:add',          '#', 'admin', sysdate(), '', null, '');
-- @SQL_STATEMENT_END

-- @SQL_STATEMENT_START
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('飞书文档信息修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'feishu:feishudoc:edit',         '#', 'admin', sysdate(), '', null, '');
-- @SQL_STATEMENT_END

-- @SQL_STATEMENT_START
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('飞书文档信息删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'feishu:feishudoc:remove',       '#', 'admin', sysdate(), '', null, '');
-- @SQL_STATEMENT_END

-- @SQL_STATEMENT_START
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('飞书文档信息导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'feishu:feishudoc:export',       '#', 'admin', sysdate(), '', null, '');
-- @SQL_STATEMENT_END