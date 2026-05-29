package com.ruoyi.project.miniapp.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.project.bill.domain.BillAccount;
import com.ruoyi.project.bill.domain.BillUserProfile;
import com.ruoyi.project.bill.service.IBillAccountService;
import com.ruoyi.project.bill.service.IBillUserProfileService;

import lombok.RequiredArgsConstructor;

/**
 * 小程序记账用户初始化：扩展资料 + 默认账户。
 */
@Service
@RequiredArgsConstructor
public class BillMiniAppBootstrapService {

    public static final String BILL_APP_CODE = "lingxi-ledger";

    private final IBillUserProfileService billUserProfileService;
    private final IBillAccountService billAccountService;

    @Transactional
    public BillUserProfile ensureUserReady(Long miniUserId, String openid) {
        BillUserProfile profile = billUserProfileService.ensureForMiniUser(miniUserId, openid);
        ensureDefaultAccounts(profile);
        return profile;
    }

    private void ensureDefaultAccounts(BillUserProfile profile) {
        Long userId = profile.getMiniUserId() != null ? profile.getMiniUserId() : profile.getUserId();
        if (userId == null) {
            return;
        }

        List<BillAccount> accounts = billAccountService.selectByUserId(userId);
        if (!accounts.isEmpty()) {
            if (profile.getDefaultAccountId() == null) {
                profile.setDefaultAccountId(accounts.get(0).getAccountId());
                billUserProfileService.updateById(profile);
            }
            return;
        }

        BillAccount wechatAccount = createAccount(userId, "微信", "1", 1);
        createAccount(userId, "支付宝", "2", 2);
        createAccount(userId, "现金", "0", 3);

        profile.setDefaultAccountId(wechatAccount.getAccountId());
        billUserProfileService.updateById(profile);
    }

    private BillAccount createAccount(Long userId, String accountName, String accountType, int sortOrder) {
        BillAccount account = new BillAccount();
        account.setUserId(userId);
        account.setAccountName(accountName);
        account.setAccountType(accountType);
        account.setBalance(BigDecimal.ZERO);
        account.setSortOrder(sortOrder);
        account.setStatus("0");
        billAccountService.save(account);
        return account;
    }
}
