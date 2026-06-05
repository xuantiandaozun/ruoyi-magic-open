-- 将小程序共用的阿里云 RAM 密钥别名从 OCR 调整为 ALIYUN_RAM
UPDATE secret_key_info
SET key_name = 'ALIYUN_RAM',
    update_time = NOW()
WHERE provider_brand = 'aliyun'
  AND key_name = 'OCR'
  AND del_flag = '0';
