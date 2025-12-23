package com.ruoyi.framework.service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ruoyi.framework.redis.RedisCache;

import cn.hutool.core.util.StrUtil;

/**
 * 邮件服务
 * 
 * @author ruoyi
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private RedisCache redisCache;

    @Value("${spring.mail.username:}")
    private String from;

    /**
     * 验证码 Redis Key 前缀
     */
    private static final String VERIFY_CODE_PREFIX = "verify_code:email:";

    /**
     * 验证码有效期（分钟）
     */
    private static final int VERIFY_CODE_EXPIRE_MINUTES = 5;

    /**
     * 发送邮箱验证码
     * 
     * @param email 邮箱地址
     * @return 是否发送成功
     */
    public boolean sendVerifyCode(String email) {
        if (StrUtil.isEmpty(email)) {
            logger.error("邮箱地址不能为空");
            return false;
        }

        // 检查邮件发送器是否配置
        if (mailSender == null) {
            logger.error("邮件发送器未配置，请在 application.yml 中配置 spring.mail 相关参数");
            return false;
        }

        if (StrUtil.isEmpty(from)) {
            logger.error("发件人邮箱未配置，请在 application.yml 中配置 spring.mail.username");
            return false;
        }

        try {
            // 生成6位随机验证码
            String verifyCode = generateVerifyCode();

            // 存储验证码到 Redis，设置5分钟过期
            String redisKey = VERIFY_CODE_PREFIX + email;
            redisCache.setCacheObject(redisKey, verifyCode, VERIFY_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

            // 发送邮件
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject("【账单APP】邮箱验证码");
            message.setText(buildEmailContent(verifyCode));

            mailSender.send(message);

            logger.info("验证码已发送到邮箱: {}", email);
            return true;

        } catch (Exception e) {
            logger.error("发送邮件失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 异步发送邮箱验证码
     * 该方法会立即返回，邮件发送在后台线程中执行
     * 
     * @param email 邮箱地址
     * @return 是否通过初步验证（不代表邮件发送成功）
     */
    public boolean sendVerifyCodeAsync(String email) {
        if (StrUtil.isEmpty(email)) {
            logger.error("邮箱地址不能为空");
            return false;
        }

        // 检查邮件发送器是否配置
        if (mailSender == null) {
            logger.error("邮件发送器未配置，请在 application.yml 中配置 spring.mail 相关参数");
            return false;
        }

        if (StrUtil.isEmpty(from)) {
            logger.error("发件人邮箱未配置，请在 application.yml 中配置 spring.mail.username");
            return false;
        }

        try {
            // 生成6位随机验证码
            String verifyCode = generateVerifyCode();

            // 存储验证码到 Redis，设置5分钟过期
            String redisKey = VERIFY_CODE_PREFIX + email;
            redisCache.setCacheObject(redisKey, verifyCode, VERIFY_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

            // 异步发送邮件
            sendEmailAsync(email, verifyCode);

            logger.info("验证码已生成，正在异步发送到邮箱: {}", email);
            return true;

        } catch (Exception e) {
            logger.error("生成验证码失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 异步发送邮件（内部方法）
     * 
     * @param email      邮箱地址
     * @param verifyCode 验证码
     */
    @Async
    protected void sendEmailAsync(String email, String verifyCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject("【账单APP】邮箱验证码");
            message.setText(buildEmailContent(verifyCode));

            mailSender.send(message);

            logger.info("验证码邮件已成功发送到: {}", email);
        } catch (Exception e) {
            logger.error("异步发送邮件失败，邮箱: {}, 错误: {}", email, e.getMessage(), e);
        }
    }

    /**
     * 验证邮箱验证码
     * 
     * @param email 邮箱地址
     * @param code  验证码
     * @return 是否验证成功
     */
    public boolean verifyCode(String email, String code) {
        if (StrUtil.isEmpty(email) || StrUtil.isEmpty(code)) {
            return false;
        }

        String redisKey = VERIFY_CODE_PREFIX + email;
        String cachedCode = redisCache.getCacheObject(redisKey);

        if (StrUtil.isEmpty(cachedCode)) {
            logger.warn("验证码已过期或不存在: {}", email);
            return false;
        }

        // 验证成功后删除验证码
        if (code.equals(cachedCode)) {
            redisCache.deleteObject(redisKey);
            return true;
        }

        return false;
    }

    /**
     * 生成6位随机验证码
     * 
     * @return 验证码
     */
    private String generateVerifyCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000; // 生成100000-999999之间的随机数
        return String.valueOf(code);
    }

    /**
     * 构建邮件内容
     * 
     * @param verifyCode 验证码
     * @return 邮件内容
     */
    private String buildEmailContent(String verifyCode) {
        StringBuilder content = new StringBuilder();
        content.append("您好！\n\n");
        content.append("您正在进行邮箱验证，验证码为：\n\n");
        content.append("    ").append(verifyCode).append("\n\n");
        content.append("验证码有效期为 ").append(VERIFY_CODE_EXPIRE_MINUTES).append(" 分钟，请尽快使用。\n\n");
        content.append("如果这不是您的操作，请忽略此邮件。\n\n");
        content.append("——若依账单系统");
        return content.toString();
    }
}
