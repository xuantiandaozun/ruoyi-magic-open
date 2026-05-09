package com.ruoyi.project.ai.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.ai.domain.AiUsageQuota;
import com.ruoyi.project.ai.domain.AiUsageSummaryDaily;
import com.ruoyi.project.ai.service.IAiQuotaCheckService;
import com.ruoyi.project.ai.service.IAiUsageQuotaService;
import com.ruoyi.project.ai.service.IAiUsageSummaryDailyService;
import com.ruoyi.project.system.domain.SysOauthAccount;
import com.ruoyi.project.system.domain.SysUser;
import com.ruoyi.project.system.service.ISysOauthAccountService;
import com.ruoyi.project.system.service.ISysUserService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * AI插件用户管理
 * <p>
 * 专门管理通过插件（Google OAuth）登录的用户，提供：
 * 1. 插件用户列表（含额度信息）
 * 2. 查看用户今日用量
 * 3. 为单个用户设置专属额度
 */
@Slf4j
@Tag(name = "AI插件用户管理")
@RestController
@RequestMapping("/ai/pluginUser")
public class PluginUserManageController extends BaseController {

    private static final String PRODUCT_TYPE = "plugin";
    private static final String USER_TIER_FREE = "free";

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysOauthAccountService oauthAccountService;

    @Autowired
    private IAiUsageQuotaService quotaService;

    @Autowired
    private IAiUsageSummaryDailyService summaryDailyService;

    @Autowired
    private IAiQuotaCheckService quotaCheckService;

    /**
     * 查询插件用户列表（分页）
     * <p>
     * 自动关联 sys_user, sys_oauth_account, ai_usage_quota, ai_usage_summary_daily
     * 展示每个用户的当前额度配置和今日使用情况
     */
    @SaCheckPermission("ai:pluginUser:list")
    @Operation(summary = "查询插件用户列表")
    @GetMapping("/list")
    public TableDataInfo list(String nickname, String email, String status) {
        PageDomain pageDomain = TableSupport.buildPageRequest();

        // 1. 查询所有 Google OAuth 账号
        QueryWrapper qw = QueryWrapper.create()
                .from("sys_oauth_account")
                .where("provider = 'google'")
                .and("del_flag = '0'");
        if (nickname != null && !nickname.isEmpty()) {
            qw.and("nickname LIKE '%" + nickname + "%'");
        }
        if (email != null && !email.isEmpty()) {
            qw.and("email LIKE '%" + email + "%'");
        }
        if (status != null && !status.isEmpty()) {
            qw.and("status = '" + status + "'");
        }
        qw.orderBy("bind_time DESC");

        Page<SysOauthAccount> oauthPage = oauthAccountService.page(
                new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize()), qw);

        // 2. 关联 sys_user + 额度信息
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysOauthAccount oauth : oauthPage.getRecords()) {
            Map<String, Object> row = new HashMap<>();
            row.put("userId", oauth.getUserId());
            row.put("provider", oauth.getProvider());
            row.put("email", oauth.getEmail());
            row.put("nickname", oauth.getNickname());
            row.put("avatar", oauth.getAvatar());
            row.put("bindTime", oauth.getBindTime());
            row.put("lastLoginTime", oauth.getLastLoginTime());
            row.put("status", oauth.getStatus());

            // 关联用户基本信息
            if (oauth.getUserId() != null) {
                SysUser user = userService.getById(oauth.getUserId());
                if (user != null) {
                    row.put("userName", user.getUserName());
                    row.put("userStatus", user.getStatus());
                }
            }

            // 额度信息
            IAiQuotaCheckService.QuotaUsageInfo usageInfo =
                    quotaCheckService.getUsageInfo(oauth.getUserId(), USER_TIER_FREE, PRODUCT_TYPE);
            row.put("todayUsedRequests", usageInfo.getTodayUsedRequests());
            row.put("requestLimit", usageInfo.getRequestLimit());
            row.put("remainingRequests", usageInfo.getRemainingRequests());
            row.put("todayUsedTokens", usageInfo.getTodayUsedTokens());
            row.put("tokenLimit", usageInfo.getTokenLimit());

            // 检查是否有个人专属配额
            AiUsageQuota personalQuota = getPersonalQuota(oauth.getUserId());
            row.put("hasPersonalQuota", personalQuota != null);
            row.put("personalQuotaId", personalQuota != null ? personalQuota.getId() : null);
            // 回显专属额度的编辑字段
            row.put("personalQuotaCode", personalQuota != null ? personalQuota.getQuotaCode() : null);
            row.put("personalRequestLimit", personalQuota != null ? personalQuota.getRequestLimit() : null);
            row.put("personalTokenLimit", personalQuota != null ? personalQuota.getTokenLimit() : null);
            row.put("personalConcurrentLimit", personalQuota != null ? personalQuota.getConcurrentLimit() : null);
            row.put("personalEnabled", personalQuota != null ? personalQuota.getEnabled() : null);
            row.put("personalRemark", personalQuota != null ? personalQuota.getRemark() : null);

            rows.add(row);
        }

        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setMsg("查询成功");
        rspData.setRows(rows);
        rspData.setTotal(oauthPage.getTotalRow());
        return rspData;
    }

    /**
     * 获取单个插件用户详情（含额度配置和今日用量）
     */
    @SaCheckPermission("ai:pluginUser:query")
    @Operation(summary = "获取插件用户详情")
    @GetMapping("/{userId}")
    public AjaxResult getInfo(@PathVariable Long userId) {
        Map<String, Object> data = new HashMap<>();

        // 用户基本信息
        SysUser user = userService.getById(userId);
        if (user == null) {
            return AjaxResult.error("用户不存在");
        }
        data.put("user", user);

        // OAuth 账号信息
        QueryWrapper qw = QueryWrapper.create()
                .from("sys_oauth_account")
                .where("user_id = " + userId)
                .and("provider = 'google'")
                .and("del_flag = '0'")
                .limit(1);
        SysOauthAccount oauth = oauthAccountService.getOne(qw);
        data.put("oauth", oauth);

        // 个人专属额度
        AiUsageQuota personalQuota = getPersonalQuota(userId);
        data.put("personalQuota", personalQuota);

        // 今日用量
        IAiQuotaCheckService.QuotaUsageInfo usageInfo =
                quotaCheckService.getUsageInfo(userId, USER_TIER_FREE, PRODUCT_TYPE);
        data.put("usageInfo", usageInfo);

        // 今日明细（最近 10 条）
        String today = DateUtil.today();
        QueryWrapper summaryQw = QueryWrapper.create()
                .from("ai_usage_summary_daily")
                .where("user_id = " + userId)
                .and("product_type = '" + PRODUCT_TYPE + "'")
                .and("DATE(summary_date) = '" + today + "'")
                .orderBy("update_time DESC")
                .limit(10);
        List<AiUsageSummaryDaily> summaries = summaryDailyService.list(summaryQw);
        data.put("todaySummaries", summaries);

        return AjaxResult.success(data);
    }

    /**
     * 为单个用户设置专属额度
     * <p>
     * 如果已有个人额度配置，则更新；否则新增
     */
    @SaCheckPermission("ai:pluginUser:setQuota")
    @Operation(summary = "设置用户专属额度")
    @PostMapping("/setQuota")
    public AjaxResult setQuota(@RequestBody AiUsageQuota quota) {
        if (quota.getUserId() == null) {
            return AjaxResult.error("userId 不能为空");
        }

        // 校验用户是否存在
        SysUser user = userService.getById(quota.getUserId());
        if (user == null) {
            return AjaxResult.error("用户不存在");
        }

        // 检查是否已有个人配额
        AiUsageQuota existing = getPersonalQuota(quota.getUserId());

        if (existing != null) {
            // 更新
            quota.setId(existing.getId());
            quota.setUpdateBy(getUsername());
            return toAjax(quotaService.updateById(quota));
        } else {
            // 新增
            quota.setProductType(PRODUCT_TYPE);
            quota.setUserTier(USER_TIER_FREE);
            quota.setQuotaPeriod("daily");
            quota.setEnabled("Y");
            quota.setDelFlag("0");
            quota.setCreateBy(getUsername());
            quota.setUpdateBy(getUsername());
            return toAjax(quotaService.save(quota));
        }
    }

    /**
     * 删除用户专属额度（恢复为等级默认额度）
     */
    @SaCheckPermission("ai:pluginUser:removeQuota")
    @Operation(summary = "删除用户专属额度")
    @DeleteMapping("/quota/{userId}")
    public AjaxResult removeQuota(@PathVariable Long userId) {
        AiUsageQuota personalQuota = getPersonalQuota(userId);
        if (personalQuota == null) {
            return AjaxResult.error("该用户没有专属额度配置");
        }
        return toAjax(quotaService.removeById(personalQuota.getId()));
    }

    /**
     * 批量删除用户专属额度
     */
    @SaCheckPermission("ai:pluginUser:removeQuota")
    @Operation(summary = "批量删除用户专属额度")
    @DeleteMapping("/quota/batch/{userIds}")
    public AjaxResult batchRemoveQuota(@PathVariable Long[] userIds) {
        List<Long> quotaIds = new ArrayList<>();
        for (Long userId : userIds) {
            AiUsageQuota quota = getPersonalQuota(userId);
            if (quota != null) {
                quotaIds.add(quota.getId());
            }
        }
        if (quotaIds.isEmpty()) {
            return AjaxResult.error("所选用户均无专属额度配置");
        }
        return toAjax(quotaService.removeByIds(quotaIds));
    }

    // -----------------------------------------------------------------------
    // 私有方法
    // -----------------------------------------------------------------------

    /**
     * 获取用户的个人专属额度配置（若无则返回 null）
     */
    private AiUsageQuota getPersonalQuota(Long userId) {
        if (userId == null) return null;
        QueryWrapper qw = QueryWrapper.create()
                .from("ai_usage_quota")
                .where("user_id = " + userId)
                .and("product_type = '" + PRODUCT_TYPE + "'")
                .and("del_flag = '0'")
                .limit(1);
        return quotaService.getOne(qw);
    }
}
