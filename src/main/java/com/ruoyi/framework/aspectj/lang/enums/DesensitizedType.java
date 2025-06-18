package com.ruoyi.framework.aspectj.lang.enums;

import java.util.function.Function;
import cn.hutool.core.util.DesensitizedUtil;

/**
 * 脱敏类型
 *
 * @author ruoyi
 */
public enum DesensitizedType
{
    /**
     * 姓名，第2位星号替换
     */
    USERNAME(s -> DesensitizedUtil.chineseName(s)),

    /**
     * 密码，全部字符都用*代替
     */
    PASSWORD(s -> "******"),

    /**
     * 身份证，中间10位星号替换
     */
    ID_CARD(s -> DesensitizedUtil.idCardNum(s, 4, 4)),

    /**
     * 手机号，中间4位星号替换
     */
    PHONE(s -> DesensitizedUtil.mobilePhone(s)),

    /**
     * 电子邮箱，仅显示第一个字母和@后面的地址显示，其他星号替换
     */
    EMAIL(s -> DesensitizedUtil.email(s)),

    /**
     * 银行卡号，保留最后4位，其他星号替换
     */
    BANK_CARD(s -> DesensitizedUtil.bankCard(s)),

    /**
     * 车牌号码，包含普通车辆、新能源车辆
     */
    CAR_LICENSE(s -> s.substring(0, 2) + "****" + s.substring(s.length() - 2));

    private final Function<String, String> desensitizer;

    DesensitizedType(Function<String, String> desensitizer)
    {
        this.desensitizer = desensitizer;
    }

    public Function<String, String> desensitizer()
    {
        return desensitizer;
    }
}
