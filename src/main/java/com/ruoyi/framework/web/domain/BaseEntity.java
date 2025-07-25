package com.ruoyi.framework.web.domain;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.mybatisflex.annotation.Column;

import lombok.Data;

/**
 * Entity基类
 */
@Data
public class BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 搜索值 */
    @JsonIgnore
    @Column(ignore = true)
    private String searchValue;

    /** 创建者 */
    @Column()
    private String createBy;

    /** 创建时间 */
    @Column(onInsertValue = "now()")
    private Date createTime;

    /** 更新者 */
    @Column()
    private String updateBy;
    
    /** 更新时间 */
    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private Date updateTime;

    /** 备注 */
    private String remark;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnore
    @Column(ignore = true)
    private Map<String, Object> params;

    public Map<String, Object> getParams() {
        if (params == null) {
            params = new HashMap<>();
        }
        return params;
    }
}
