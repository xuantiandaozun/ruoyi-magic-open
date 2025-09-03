-- 菜单 SQL
-- @SQL_STATEMENT_START
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('自媒体文章', '2062', '1', 'socialArticle', 'article/socialArticle/index', 1, 0, 'C', '0', '0', 'article:socialArticle:list', '#', 'admin', sysdate(), '', null, '自媒体文章菜单');
-- @SQL_STATEMENT_END

-- 记录新插入的主菜单ID
-- @SQL_STATEMENT_START
SET @parentId = LAST_INSERT_ID();
-- @SQL_STATEMENT_END

-- 按钮 SQL
-- @SQL_STATEMENT_START
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('自媒体文章查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'article:socialArticle:query',        '#', 'admin', sysdate(), '', null, '');
-- @SQL_STATEMENT_END

-- @SQL_STATEMENT_START
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('自媒体文章新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'article:socialArticle:add',          '#', 'admin', sysdate(), '', null, '');
-- @SQL_STATEMENT_END

-- @SQL_STATEMENT_START
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('自媒体文章修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'article:socialArticle:edit',         '#', 'admin', sysdate(), '', null, '');
-- @SQL_STATEMENT_END

-- @SQL_STATEMENT_START
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('自媒体文章删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'article:socialArticle:remove',       '#', 'admin', sysdate(), '', null, '');
-- @SQL_STATEMENT_END

-- @SQL_STATEMENT_START
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('自媒体文章导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'article:socialArticle:export',       '#', 'admin', sysdate(), '', null, '');
-- @SQL_STATEMENT_END