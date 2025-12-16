package com.ruoyi.project.bill.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.bill.domain.BillAccount;
import com.ruoyi.project.bill.mapper.BillAccountMapper;
import com.ruoyi.project.bill.service.IBillAccountService;

/**
 * 账户Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Service
public class BillAccountServiceImpl extends ServiceImpl<BillAccountMapper, BillAccount> implements IBillAccountService {
    @Override
    public List<BillAccount> selectAccountListByUserId(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("user_id", userId)
                .eq("status", "0")
                .orderBy("sort_order ASC");

        return this.list(queryWrapper);
    }

    @Override
    public boolean updateAccountBalance(Long accountId, BigDecimal amount) {
        BillAccount account = this.getById(accountId);
        if (account != null) {
            BigDecimal newBalance = account.getBalance().add(amount);
            account.setBalance(newBalance);
            return this.updateById(account);
        }
        return false;
    }

    @Override
    public List<BillAccount> selectByUserId(Long userId) {
        return selectAccountListByUserId(userId);
    }

    @Override
    public boolean updateBalance(Long accountId, BigDecimal newBalance) {
        BillAccount account = this.getById(accountId);
        if (account == null) {
            return false;
        }
        account.setBalance(newBalance);
        return this.updateById(account);
    }
}
