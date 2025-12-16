package com.ruoyi.project.bill.service;

import java.util.List;

import com.mybatisflex.core.service.IService;
import com.ruoyi.project.bill.domain.BillAccount;

/**
 * 账户Service接口
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
public interface IBillAccountService extends IService<BillAccount> {
    /**
     * 查询用户的账户列表
     * 
     * @param userId 用户ID
     * @return 账户列表
     */
    List<BillAccount> selectAccountListByUserId(Long userId);

    /**
     * 查询用户的账户列表（别名方法）
     * 
     * @param userId 用户ID
     * @return 账户列表
     */
    List<BillAccount> selectByUserId(Long userId);

    /**
     * 更新账户余额
     * 
     * @param accountId 账户ID
     * @param amount    金额变动（正数为增加，负数为减少）
     * @return 是否成功
     */
    boolean updateAccountBalance(Long accountId, java.math.BigDecimal amount);

    /**
     * 更新账户余额（设置新余额）
     * 
     * @param accountId  账户ID
     * @param newBalance 新余额
     * @return 是否成功
     */
    boolean updateBalance(Long accountId, java.math.BigDecimal newBalance);
}
