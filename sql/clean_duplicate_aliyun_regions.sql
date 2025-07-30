-- 清理阿里云地域字典中的重复数据和已关停地域
-- 执行前请先备份数据

-- 1. 删除包含"已关停"的地域数据
DELETE FROM sys_dict_data 
WHERE dict_type = 'aliyun_region' 
AND dict_label LIKE '%已关停%';

-- 2. 删除重复的regionId，保留最小的dict_code
DELETE d1 FROM sys_dict_data d1
INNER JOIN sys_dict_data d2 
WHERE d1.dict_type = 'aliyun_region' 
AND d2.dict_type = 'aliyun_region'
AND d1.dict_value = d2.dict_value  -- 相同的regionId
AND d1.dict_code > d2.dict_code;   -- 保留最小的dict_code

-- 3. 重新排序dict_sort字段
SET @row_number = 0;
UPDATE sys_dict_data 
SET dict_sort = (@row_number := @row_number + 1)
WHERE dict_type = 'aliyun_region'
ORDER BY dict_code;

-- 4. 查询清理后的结果
SELECT 
    dict_code,
    dict_label,
    dict_value,
    dict_sort,
    create_time
FROM sys_dict_data 
WHERE dict_type = 'aliyun_region'
ORDER BY dict_sort;

-- 5. 统计清理后的数据量
SELECT 
    COUNT(*) as total_regions,
    COUNT(DISTINCT dict_value) as unique_regions
FROM sys_dict_data 
WHERE dict_type = 'aliyun_region';