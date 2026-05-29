package com.ruoyi.project.miniapp.controller.bill;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.project.bill.domain.BillUserProfile;
import com.ruoyi.project.bill.service.IBillUserProfileService;
import com.ruoyi.project.miniapp.service.impl.BillMiniAppBootstrapService;
import com.ruoyi.project.miniapp.util.MiniAppSecurityUtils;

/**
 * 小程序记账接口基类，统一解析 mini_user 与 bill 用户上下文。
 */
public abstract class BillMiniAppBaseController extends BaseController {

    @Autowired
    protected IBillUserProfileService billUserProfileService;

    @Autowired
    protected BillMiniAppBootstrapService billMiniAppBootstrapService;

    protected Long getBillUserId() {
        return MiniAppSecurityUtils.getLoginUser().getMiniUserId();
    }

    protected BillUserProfile requireBillProfile() {
        var loginUser = MiniAppSecurityUtils.getLoginUser();
        return billMiniAppBootstrapService.ensureUserReady(loginUser.getMiniUserId(), loginUser.getOpenid());
    }

    protected Map<String, Object> getQueryScope() {
        Map<String, Object> scope = new HashMap<>();
        Long userId = getBillUserId();
        BillUserProfile userProfile = billUserProfileService.selectByMiniUserId(userId);
        if (userProfile == null) {
            userProfile = requireBillProfile();
        }

        if (userProfile.getFamilyId() != null && userProfile.getFamilyId() > 0) {
            scope.put("isFamilyMode", true);
            scope.put("queryId", userProfile.getFamilyId());
            scope.put("queryType", "family");
            scope.put("userId", userId);
        } else {
            scope.put("isFamilyMode", false);
            scope.put("queryId", userId);
            scope.put("queryType", "user");
            scope.put("userId", userId);
        }
        return scope;
    }
}
