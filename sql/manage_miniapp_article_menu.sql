-- ============================================================
-- 小程序管理 + 博客评论/点赞 菜单权限配置
-- 执行后刷新浏览器，超级管理员自动可见；其他角色需在「角色管理」中分配
-- ============================================================

-- ===================== 一、小程序管理（顶级目录） =====================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '小程序管理', 0, 7, 'miniappManage', NULL, 1, 0, 'M', '0', '0', NULL, 'phone', 'admin', NOW(), 'admin', NOW(), '小程序后台管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '小程序管理' AND parent_id = 0 AND del_flag = '0');

-- 1.1 小程序配置
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '小程序配置',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '小程序管理' AND parent_id = 0 AND del_flag = '0' LIMIT 1),
       1, 'appConfig', 'miniapp/appConfig/index', 1, 0, 'C', '0', '0', 'manage:miniapp:list', 'component', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:miniapp:list' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '小程序配置查询', (SELECT menu_id FROM sys_menu WHERE perms = 'manage:miniapp:list' AND del_flag = '0' LIMIT 1), 1, '#', '', 1, 0, 'F', '0', '0', 'manage:miniapp:query', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:miniapp:query' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '小程序配置新增', (SELECT menu_id FROM sys_menu WHERE perms = 'manage:miniapp:list' AND del_flag = '0' LIMIT 1), 2, '#', '', 1, 0, 'F', '0', '0', 'manage:miniapp:add', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:miniapp:add' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '小程序配置修改', (SELECT menu_id FROM sys_menu WHERE perms = 'manage:miniapp:list' AND del_flag = '0' LIMIT 1), 3, '#', '', 1, 0, 'F', '0', '0', 'manage:miniapp:edit', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:miniapp:edit' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '小程序配置删除', (SELECT menu_id FROM sys_menu WHERE perms = 'manage:miniapp:list' AND del_flag = '0' LIMIT 1), 4, '#', '', 1, 0, 'F', '0', '0', 'manage:miniapp:remove', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:miniapp:remove' AND del_flag = '0');

-- 1.2 小程序用户
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '小程序用户',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '小程序管理' AND parent_id = 0 AND del_flag = '0' LIMIT 1),
       2, 'miniUser', 'miniapp/user/index', 1, 0, 'C', '0', '0', 'manage:miniuser:list', 'user', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:miniuser:list' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '小程序用户查询', (SELECT menu_id FROM sys_menu WHERE perms = 'manage:miniuser:list' AND del_flag = '0' LIMIT 1), 1, '#', '', 1, 0, 'F', '0', '0', 'manage:miniuser:query', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:miniuser:query' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '小程序用户修改', (SELECT menu_id FROM sys_menu WHERE perms = 'manage:miniuser:list' AND del_flag = '0' LIMIT 1), 2, '#', '', 1, 0, 'F', '0', '0', 'manage:miniuser:edit', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:miniuser:edit' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '小程序用户删除', (SELECT menu_id FROM sys_menu WHERE perms = 'manage:miniuser:list' AND del_flag = '0' LIMIT 1), 3, '#', '', 1, 0, 'F', '0', '0', 'manage:miniuser:remove', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:miniuser:remove' AND del_flag = '0');

-- 1.3 用户反馈
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '用户反馈',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '小程序管理' AND parent_id = 0 AND del_flag = '0' LIMIT 1),
       3, 'miniFeedback', 'miniapp/feedback/index', 1, 0, 'C', '0', '0', 'manage:minifeedback:list', 'message', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:minifeedback:list' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '用户反馈查询', (SELECT menu_id FROM sys_menu WHERE perms = 'manage:minifeedback:list' AND del_flag = '0' LIMIT 1), 1, '#', '', 1, 0, 'F', '0', '0', 'manage:minifeedback:query', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:minifeedback:query' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '用户反馈回复', (SELECT menu_id FROM sys_menu WHERE perms = 'manage:minifeedback:list' AND del_flag = '0' LIMIT 1), 2, '#', '', 1, 0, 'F', '0', '0', 'manage:minifeedback:edit', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:minifeedback:edit' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '用户反馈删除', (SELECT menu_id FROM sys_menu WHERE perms = 'manage:minifeedback:list' AND del_flag = '0' LIMIT 1), 3, '#', '', 1, 0, 'F', '0', '0', 'manage:minifeedback:remove', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:minifeedback:remove' AND del_flag = '0');

-- 1.4 订阅消息模板
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '订阅消息模板',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '小程序管理' AND parent_id = 0 AND del_flag = '0' LIMIT 1),
       4, 'subscribeTemplate', 'miniapp/subscribeTemplate/index', 1, 0, 'C', '0', '0', 'manage:miniSubscribeTemplate:list', 'email', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:miniSubscribeTemplate:list' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '订阅模板查询', (SELECT menu_id FROM sys_menu WHERE perms = 'manage:miniSubscribeTemplate:list' AND del_flag = '0' LIMIT 1), 1, '#', '', 1, 0, 'F', '0', '0', 'manage:miniSubscribeTemplate:query', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:miniSubscribeTemplate:query' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '订阅模板新增', (SELECT menu_id FROM sys_menu WHERE perms = 'manage:miniSubscribeTemplate:list' AND del_flag = '0' LIMIT 1), 2, '#', '', 1, 0, 'F', '0', '0', 'manage:miniSubscribeTemplate:add', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:miniSubscribeTemplate:add' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '订阅模板修改', (SELECT menu_id FROM sys_menu WHERE perms = 'manage:miniSubscribeTemplate:list' AND del_flag = '0' LIMIT 1), 3, '#', '', 1, 0, 'F', '0', '0', 'manage:miniSubscribeTemplate:edit', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:miniSubscribeTemplate:edit' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '订阅模板删除', (SELECT menu_id FROM sys_menu WHERE perms = 'manage:miniSubscribeTemplate:list' AND del_flag = '0' LIMIT 1), 4, '#', '', 1, 0, 'F', '0', '0', 'manage:miniSubscribeTemplate:remove', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:miniSubscribeTemplate:remove' AND del_flag = '0');

-- 1.5 翻译任务
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '翻译任务',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '小程序管理' AND parent_id = 0 AND del_flag = '0' LIMIT 1),
       5, 'translateTask', 'miniapp/translateTask/index', 1, 0, 'C', '0', '0', 'manage:translateTask:list', 'documentation', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:translateTask:list' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '翻译任务查询', (SELECT menu_id FROM sys_menu WHERE perms = 'manage:translateTask:list' AND del_flag = '0' LIMIT 1), 1, '#', '', 1, 0, 'F', '0', '0', 'manage:translateTask:query', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:translateTask:query' AND del_flag = '0');

-- 1.6 翻译文档
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '翻译文档',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '小程序管理' AND parent_id = 0 AND del_flag = '0' LIMIT 1),
       6, 'translateDocument', 'miniapp/translateDocument/index', 1, 0, 'C', '0', '0', 'manage:translateDocument:list', 'upload', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:translateDocument:list' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '翻译文档查询', (SELECT menu_id FROM sys_menu WHERE perms = 'manage:translateDocument:list' AND del_flag = '0' LIMIT 1), 1, '#', '', 1, 0, 'F', '0', '0', 'manage:translateDocument:query', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'manage:translateDocument:query' AND del_flag = '0');

-- ===================== 二、博客模块下：评论 & 点赞 =====================
-- 父菜单「博客」menu_id = 2062

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '博客评论', 2062, 3, 'comment', 'article/comment/index', 1, 0, 'C', '0', '0', 'article:comment:list', 'message', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'article:comment:list' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '博客评论查询', (SELECT menu_id FROM sys_menu WHERE perms = 'article:comment:list' AND del_flag = '0' LIMIT 1), 1, '#', '', 1, 0, 'F', '0', '0', 'article:comment:query', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'article:comment:query' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '博客评论审核', (SELECT menu_id FROM sys_menu WHERE perms = 'article:comment:list' AND del_flag = '0' LIMIT 1), 2, '#', '', 1, 0, 'F', '0', '0', 'article:comment:edit', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'article:comment:edit' AND del_flag = '0' AND menu_type = 'F' AND menu_name = '博客评论审核');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '博客评论删除', (SELECT menu_id FROM sys_menu WHERE perms = 'article:comment:list' AND del_flag = '0' LIMIT 1), 3, '#', '', 1, 0, 'F', '0', '0', 'article:comment:remove', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'article:comment:remove' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '博客评论导出', (SELECT menu_id FROM sys_menu WHERE perms = 'article:comment:list' AND del_flag = '0' LIMIT 1), 4, '#', '', 1, 0, 'F', '0', '0', 'article:comment:export', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'article:comment:export' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '博客点赞记录', 2062, 4, 'likeRecord', 'article/likeRecord/index', 1, 0, 'C', '0', '0', 'article:likeRecord:list', 'star', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'article:likeRecord:list' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '点赞记录查询', (SELECT menu_id FROM sys_menu WHERE perms = 'article:likeRecord:list' AND del_flag = '0' LIMIT 1), 1, '#', '', 1, 0, 'F', '0', '0', 'article:likeRecord:query', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'article:likeRecord:query' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '点赞记录删除', (SELECT menu_id FROM sys_menu WHERE perms = 'article:likeRecord:list' AND del_flag = '0' LIMIT 1), 2, '#', '', 1, 0, 'F', '0', '0', 'article:likeRecord:remove', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'article:likeRecord:remove' AND del_flag = '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '点赞记录导出', (SELECT menu_id FROM sys_menu WHERE perms = 'article:likeRecord:list' AND del_flag = '0' LIMIT 1), 3, '#', '', 1, 0, 'F', '0', '0', 'article:likeRecord:export', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'article:likeRecord:export' AND del_flag = '0');

-- ===================== 三、为「管理员」角色(role_id=104) 授权新菜单 =====================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 104, m.menu_id
FROM sys_menu m
WHERE m.del_flag = '0'
  AND (
    m.menu_name = '小程序管理'
    OR m.perms LIKE 'manage:%'
    OR m.perms LIKE 'article:comment:%'
    OR m.perms LIKE 'article:likeRecord:%'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 104 AND rm.menu_id = m.menu_id
  );
