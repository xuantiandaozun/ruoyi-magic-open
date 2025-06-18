package com.ruoyi.project.system.domain;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据源配置对象 sys_data_source
 * 
 * @author ruoyi-magic
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_data_source")
public class SysDataSource extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 数据源ID */
    @Id(keyType = KeyType.Auto)
    private Long dataSourceId;

    /** 数据源名称 */
    @Excel(name = "数据源名称")
    private String name;
    
    /** 数据库连接URL */
    @Excel(name = "数据库连接URL")
    private String url;
    
    /** 数据库用户名 */
    @Excel(name = "数据库用户名")
    private String username;
    
    /** 数据库密码 */
    private String password;
    
    /** 数据库驱动类名 */
    @Excel(name = "数据库驱动类名")
    private String driverClassName;
    
    /** 数据库名称 */
    @Excel(name = "数据库名称")
    private String databaseName;
    
    /** 数据源描述 */
    @Excel(name = "数据源描述")
    private String description;
    
    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    
    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete = true)
    private String delFlag;
}
