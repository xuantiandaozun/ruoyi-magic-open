-- 阿里云地域字典类型数据
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark) 
VALUES ('阿里云地域', 'aliyun_region', '0', 'admin', sysdate(), 'admin', sysdate(), '阿里云RDS地域字典');

-- 注意：字典数据将通过接口同步，无需手动插入
-- 可以通过以下接口进行同步：
-- POST /system/aliyun/region/sync

-- 查询阿里云地域字典数据的SQL
-- SELECT dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark 
-- FROM sys_dict_data 
-- WHERE dict_type = 'aliyun_region' AND del_flag = '0' 
-- ORDER BY dict_sort ASC;