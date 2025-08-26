-- 菜单 SQL
-- @SQL_STATEMENT_START
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('英文博客', '2062', '1', 'enBlog', 'article/enBlog/index', 1, 0, 'C', '0', '0', 'article:enBlog:list', '#', 'admin', sysdate(), '', null, '英文博客菜单');
-- @SQL_STATEMENT_END

-- 记录新插入的主菜单ID
-- @SQL_STATEMENT_START
SET @parentId = LAST_INSERT_ID();
-- @SQL_STATEMENT_END

-- 按钮 SQL
-- @SQL_STATEMENT_START
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('英文博客查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'article:enBlog:query',        '#', 'admin', sysdate(), '', null, '');
-- @SQL_STATEMENT_END

-- @SQL_STATEMENT_START
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('英文博客新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'article:enBlog:add',          '#', 'admin', sysdate(), '', null, '');
-- @SQL_STATEMENT_END

-- @SQL_STATEMENT_START
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('英文博客修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'article:enBlog:edit',         '#', 'admin', sysdate(), '', null, '');
-- @SQL_STATEMENT_END

-- @SQL_STATEMENT_START
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('英文博客删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'article:enBlog:remove',       '#', 'admin', sysdate(), '', null, '');
-- @SQL_STATEMENT_END

-- @SQL_STATEMENT_START
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('英文博客导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'article:enBlog:export',       '#', 'admin', sysdate(), '', null, '');
-- @SQL_STATEMENT_END