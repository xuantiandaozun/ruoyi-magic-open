-- ============================================================
-- AI插件用户管理菜单配置
-- 在 AI管理 模块下新增"AI插件用户"菜单
-- ============================================================

-- 1. 查找"AI管理"父菜单ID（假设已存在，名称为"AI管理"）
--    如果你的系统中"AI管理"菜单名称不同，请手动调整
SET @ai_parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name = 'AI管理' AND parent_id = 0 AND del_flag = '0' LIMIT 1);

-- 2. 插入"AI插件用户"菜单（目录）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 
    'AI插件用户',                          -- 菜单名称
    @ai_parent_id,                         -- 父菜单ID（AI管理）
    10,                                    -- 排序（可根据实际调整）
    'pluginUser',                          -- 路由地址
    'ai/pluginUser/index',                 -- 组件路径
    1,                                     -- 是否外链（1=否）
    0,                                     -- 是否缓存（0=缓存）
    'C',                                   -- 菜单类型（C=菜单）
    '0',                                   -- 显示状态（0=显示）
    '0',                                   -- 菜单状态（0=正常）
    'ai:pluginUser:list',                  -- 权限标识
    '#',                                   -- 菜单图标（二级菜单不使用图标）
    'admin',                               -- 创建者
    NOW(),                                 -- 创建时间
    'admin',                               -- 更新者
    NOW(),                                 -- 更新时间
    'AI插件用户管理菜单'                   -- 备注
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu 
    WHERE menu_name = 'AI插件用户' AND parent_id = @ai_parent_id AND del_flag = '0'
);

-- 3. 获取刚插入的菜单ID
SET @plugin_user_menu_id = (SELECT menu_id FROM sys_menu WHERE menu_name = 'AI插件用户' AND parent_id = @ai_parent_id AND del_flag = '0' LIMIT 1);

-- 4. 插入子按钮权限

-- 4.1 查询按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 
    '插件用户查询', @plugin_user_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'ai:pluginUser:query', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:pluginUser:query' AND del_flag = '0');

-- 4.2 设置额度按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 
    '设置用户额度', @plugin_user_menu_id, 2, '#', '', 1, 0, 'F', '0', '0', 'ai:pluginUser:setQuota', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:pluginUser:setQuota' AND del_flag = '0');

-- 4.3 清除额度按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 
    '清除用户额度', @plugin_user_menu_id, 3, '#', '', 1, 0, 'F', '0', '0', 'ai:pluginUser:removeQuota', '#', 'admin', NOW(), 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:pluginUser:removeQuota' AND del_flag = '0');

-- ============================================================
-- 执行完成后，刷新浏览器即可在"AI管理"模块下看到"AI插件用户"菜单
-- 
-- 权限说明：
--   ai:pluginUser:list        - 查看插件用户列表
--   ai:pluginUser:query       - 查询用户详情
--   ai:pluginUser:setQuota    - 设置用户专属额度
--   ai:pluginUser:removeQuota - 清除用户专属额度
-- ============================================================
