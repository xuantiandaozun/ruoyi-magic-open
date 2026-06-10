package com.ruoyi.project.miniapp.domain.vo;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信 mediaCheckAsync 异步检测结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAppMediaCheckResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String traceId;

    private Integer errcode;

    private String errmsg;

    private String suggest;

    private String label;
}
