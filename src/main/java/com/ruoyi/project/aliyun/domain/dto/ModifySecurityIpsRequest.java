package com.ruoyi.project.aliyun.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改RDS实例白名单请求DTO
 * 
 * @author ruoyi
 */
@Data
public class ModifySecurityIpsRequest {
    
    /**
     * 安全IP列表，多个IP用逗号分隔
     * 支持IP地址和CIDR格式，如：192.168.1.1,10.0.0.0/24
     */
    @NotBlank(message = "安全IP列表不能为空")
    private String securityIps;
    
    /**
     * 白名单组名称，可选，默认为"default"
     * 如果不填写，系统将使用默认的白名单组
     */
    private String dbInstanceIPArrayName;
    
    /**
     * 获取白名单组名称，如果为空则返回默认值
     */
    public String getDbInstanceIPArrayName() {
        return (dbInstanceIPArrayName != null && !dbInstanceIPArrayName.trim().isEmpty()) 
            ? dbInstanceIPArrayName.trim() : "default";
    }
}