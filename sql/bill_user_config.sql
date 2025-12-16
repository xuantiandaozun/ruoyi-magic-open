-- =====================================================
-- 账单用户类型和角色配置 SQL
-- =====================================================

-- 1. 修改 sys_user 表的 user_type 字段注释（已存在字段，只是完善注释）
-- user_type: 00-系统用户, 01-注册用户
-- 注意：这个是注释性SQL，实际字段已存在，无需执行ALTER，只是说明新的业务规则

-- 2. 添加"记账用户"角色
-- 如果角色已存在，先检查是否需要更新，否则插入
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES 
(100, '记账用户', 'bill_user', 5, '1', 1, 1, '0', '0', 'admin', NOW(), '', NULL, '记账系统普通用户角色')
ON DUPLICATE KEY UPDATE 
`role_name` = '记账用户',
`role_key` = 'bill_user',
`role_sort` = 5,
`remark` = '记账系统普通用户角色';

-- 3. 添加用户类型字典
-- 先检查字典类型是否存在
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`)
VALUES 
(120, '用户类型', 'sys_user_type', '0', 'admin', NOW(), '系统用户类型列表')
ON DUPLICATE KEY UPDATE
`dict_name` = '用户类型',
`dict_type` = 'sys_user_type';

-- 添加字典数据
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
VALUES 
(330, 1, '系统用户', '00', 'sys_user_type', '', 'primary', 'Y', '0', 'admin', NOW(), '系统内部用户'),
(331, 2, '注册用户', '01', 'sys_user_type', '', 'success', 'N', '0', 'admin', NOW(), '通过注册接口注册的用户')
ON DUPLICATE KEY UPDATE
`dict_label` = VALUES(`dict_label`),
`dict_value` = VALUES(`dict_value`),
`dict_type` = VALUES(`dict_type`);

-- =====================================================
-- 使用说明
-- =====================================================
-- 1. 用户类型说明：
--    00 - 系统用户：通过后台管理系统创建的用户
--    01 - 注册用户：通过前端注册接口注册的用户
--
-- 2. 记账用户角色：
--    role_id: 100
--    role_key: bill_user
--    
-- 3. 注册用户默认绑定角色 ID: 100 (记账用户)
--
-- 4. 登录时需验证：
--    - user_type = '01' (注册用户)
--    - 绑定角色包含 'bill_user'
-- =====================================================
