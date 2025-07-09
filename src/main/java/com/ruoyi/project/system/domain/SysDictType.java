package com.ruoyi.project.system.domain;

import java.util.List;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.ruoyi.framework.web.domain.BaseEntity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型表 sys_dict_type
 * 
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_dict_type")
public class SysDictType extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 字典主键 */
    @Id(keyType = KeyType.Auto)
    private Long dictId;

    /** 字典名称 */
    @NotBlank(message = "字典名称不能为空")
    @Size(min = 0, max = 100, message = "字典类型名称长度不能超过100个字符")
    private String dictName;

    /** 字典类型 */
    @NotBlank(message = "字典类型不能为空")
    @Size(min = 0, max = 100, message = "字典类型类型长度不能超过100个字符")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "字典类型必须以字母开头，且只能为（小写字母，数字，下划线）")
    private String dictType;

    /** 状态（0正常 1停用） */
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    @Column(isLogicDelete = true)
    private String delFlag;
    
    /** 字典数据列表（非数据库字段，用于缓存优化） */
    @Column(ignore = true)
    private List<SysDictData> dictDataList;
}
