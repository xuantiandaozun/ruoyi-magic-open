package com.ruoyi.project.miniapp.domain.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateMiniUserProfileRequest {
    @Size(max = 128, message = "昵称长度不能超过128个字符")
    private String nickname;

    @Size(max = 500, message = "头像地址长度不能超过500个字符")
    private String avatar;

    @Size(max = 32, message = "手机号长度不能超过32个字符")
    private String mobile;

    @Size(max = 128, message = "邮箱长度不能超过128个字符")
    private String email;
}
