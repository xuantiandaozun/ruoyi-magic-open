package com.ruoyi.project.miniapp.domain.vo;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAppLoginUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long miniUserId;

    private Long miniAppId;

    private String appCode;

    private String openid;
}
