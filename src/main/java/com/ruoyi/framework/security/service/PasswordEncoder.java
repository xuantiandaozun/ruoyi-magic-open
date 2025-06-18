package com.ruoyi.framework.security.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import java.security.MessageDigest;
import java.util.Objects;

/**
 * 密码加密器
 */
@Component
public class PasswordEncoder {
    private static final String ALGORITHM = "SHA-256";
    private static final String SALT = "ruoyi";

    /**
     * 加密
     */
    public String encode(String rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            byte[] bytes = md.digest((rawPassword + SALT).getBytes());
            return toHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }

    /**
     * 密码匹配
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (StringUtils.isEmpty(rawPassword) || StringUtils.isEmpty(encodedPassword)) {
            return false;
        }
        return Objects.equals(encode(rawPassword), encodedPassword);
    }

    private String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public static void main(String[] args) {
        PasswordEncoder passwordEncoder = new PasswordEncoder();
        String encodedPassword = passwordEncoder.encode("123456");
        System.out.println(encodedPassword);
    }
} 