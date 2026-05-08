package com.ruoyi.project.system.service.impl;

import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.ai.service.IAiQuotaCheckService;
import com.ruoyi.project.system.domain.SysOauthAccount;
import com.ruoyi.project.system.domain.SysUser;
import com.ruoyi.project.system.domain.dto.GoogleOAuthLoginResult;
import com.ruoyi.project.system.service.IGoogleOAuthService;
import com.ruoyi.project.system.service.ISysOauthAccountService;
import com.ruoyi.project.system.service.ISysUserService;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Google OAuth 服务实现（插件用户登录专用）
 * <p>
 * 流程：
 * 1. 用 Google id_token 调 tokeninfo 接口校验 & 获取用户信息
 * 2. 查 sys_oauth_account（provider=google, open_id=sub）
 * 3. 若不存在则自动注册 sys_user（user_type=01）并创建 oauth 绑定记录
 * 4. 更新最近登录时间，用 Sa-Token 签发系统 token 返回
 */
@Slf4j
@Service
public class GoogleOAuthServiceImpl implements IGoogleOAuthService {

    /** Google tokeninfo 验证端点 */
    private static final String GOOGLE_TOKENINFO_URL = "https://goog.zhoudw.club/tokeninfo?id_token=";

    private static final String PROVIDER_GOOGLE = "google";
    /** 插件用户 user_type */
    private static final String USER_TYPE_REGISTERED = "01";
    /** 默认插件用户角色 KEY（对应 sys_role.role_key） */
    private static final String PLUGIN_USER_TIER = "free";
    /** 产品类型 */
    private static final String PRODUCT_TYPE_PLUGIN = "plugin";

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysOauthAccountService oauthAccountService;

    @Autowired
    private IAiQuotaCheckService quotaCheckService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GoogleOAuthLoginResult loginWithIdToken(String idToken) {
        if (StrUtil.isBlank(idToken)) {
            throw new ServiceException("id_token 不能为空");
        }

        // 1. 验证 id_token 并获取 Google 用户信息
        JSONObject googleUser = verifyIdToken(idToken);
        String sub = googleUser.getStr("sub");
        String email = googleUser.getStr("email");
        String name = StrUtil.blankToDefault(googleUser.getStr("name"), email);
        String picture = googleUser.getStr("picture");

        if (StrUtil.isBlank(sub)) {
            throw new ServiceException("Google 返回的用户信息无效");
        }

        // 2. 查找已有 OAuth 绑定
        SysOauthAccount oauthAccount = findOauthAccount(sub);

        SysUser sysUser;
        if (oauthAccount == null) {
            // 3. 自动注册新用户
            sysUser = registerNewUser(email, name, picture);
            oauthAccount = createOauthAccount(sysUser.getUserId(), sub, email, name, picture, googleUser.toString());
        } else {
            sysUser = userService.getById(oauthAccount.getUserId());
            if (sysUser == null || "1".equals(sysUser.getStatus())) {
                throw new ServiceException("账号已停用，请联系管理员");
            }
            // 更新 OAuth 账号信息（昵称/头像可能变化）
            oauthAccount.setNickname(name);
            oauthAccount.setAvatar(picture);
            oauthAccount.setLastLoginTime(new Date());
            oauthAccountService.updateById(oauthAccount);
        }

        // 4. 更新用户最后登录信息
        sysUser.setLoginDate(new Date());
        userService.updateById(sysUser);

        // 5. Sa-Token 登录
        StpUtil.login(sysUser.getUserId());
        String token = StpUtil.getTokenValue();

        // 6. 查询今日配额使用情况
        IAiQuotaCheckService.QuotaUsageInfo usage =
                quotaCheckService.getUsageInfo(sysUser.getUserId(), PLUGIN_USER_TIER, PRODUCT_TYPE_PLUGIN);

        return GoogleOAuthLoginResult.builder()
                .token(token)
                .userId(sysUser.getUserId())
                .nickName(sysUser.getNickName())
                .email(sysUser.getEmail())
                .avatar(sysUser.getAvatar())
                .userTier(PLUGIN_USER_TIER)
                .todayUsedRequests(usage.getTodayUsedRequests())
                .todayRequestLimit(usage.getRequestLimit())
                .build();
    }

    // -----------------------------------------------------------------------
    // 私有方法
    // -----------------------------------------------------------------------

    /**
     * 调用 Google tokeninfo 验证 id_token 并返回用户信息
     */
    private JSONObject verifyIdToken(String idToken) {
        try {
            HttpResponse response = HttpRequest.get(GOOGLE_TOKENINFO_URL + idToken)
                    .timeout(10_000)
                    .execute();
            if (!response.isOk()) {
                log.warn("Google tokeninfo 请求失败: {}", response.body());
                throw new ServiceException("Google 身份验证失败，请重新登录");
            }
            JSONObject result = JSONUtil.parseObj(response.body());
            // error 字段存在表示 token 无效
            if (StrUtil.isNotBlank(result.getStr("error"))) {
                log.warn("Google tokeninfo 返回错误: {}", result.getStr("error_description"));
                throw new ServiceException("Google Token 已过期或无效，请重新授权");
            }
            return result;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("验证 Google id_token 失败", e);
            throw new ServiceException("Google 身份验证异常：" + e.getMessage());
        }
    }

    private SysOauthAccount findOauthAccount(String sub) {
        QueryWrapper qw = QueryWrapper.create()
                .from("sys_oauth_account")
                .where("provider = '" + PROVIDER_GOOGLE + "'")
                .and("open_id = '" + sub + "'")
                .and("del_flag = '0'");
        return oauthAccountService.getOne(qw);
    }

    private SysUser registerNewUser(String email, String name, String avatar) {
        SysUser user = new SysUser();
        // 用邮箱前缀 + 随机串作为 userName，保证唯一
        String baseName = StrUtil.isNotBlank(email)
                ? email.split("@")[0]
                : "user";
        user.setUserName(baseName + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6));
        user.setNickName(name);
        user.setEmail(email);
        user.setAvatar(avatar);
        user.setUserType(USER_TYPE_REGISTERED);
        user.setStatus("0");
        user.setDelFlag("0");
        // 随机密码（不可用于密码登录）
        user.setPassword(UUID.randomUUID().toString());
        user.setCreateBy("google_oauth");
        user.setUpdateBy("google_oauth");
        userService.save(user);
        log.info("Google OAuth 自动注册新用户: userId={}, email={}", user.getUserId(), email);
        return user;
    }

    private SysOauthAccount createOauthAccount(Long userId, String sub, String email,
            String nickname, String avatar, String rawJson) {
        SysOauthAccount account = new SysOauthAccount();
        account.setUserId(userId);
        account.setProvider(PROVIDER_GOOGLE);
        account.setOpenId(sub);
        account.setEmail(email);
        account.setNickname(nickname);
        account.setAvatar(avatar);
        account.setRawJson(rawJson);
        account.setBindTime(new Date());
        account.setLastLoginTime(new Date());
        account.setStatus("0");
        account.setDelFlag("0");
        account.setCreateBy("google_oauth");
        account.setUpdateBy("google_oauth");
        oauthAccountService.save(account);
        return account;
    }
}
