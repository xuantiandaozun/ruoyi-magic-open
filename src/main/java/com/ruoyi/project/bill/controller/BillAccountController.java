package com.ruoyi.project.bill.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.common.utils.poi.MagicExcelUtil;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.bill.domain.BillAccount;
import com.ruoyi.project.bill.service.IBillAccountService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 账户管理Controller
 * 
 * @author ruoyi
 * @date 2025-12-14
 */
@Tag(name = "账户管理")
@RestController
@RequestMapping("/bill/account")
public class BillAccountController extends BaseController {

    @Autowired
    private IBillAccountService billAccountService;

    /**
     * 查询账户列表
     */
    @Operation(summary = "查询账户列表")
    @SaCheckPermission("bill:account:list")
    @GetMapping("/list")
    public TableDataInfo list(BillAccount billAccount) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();

        QueryWrapper queryWrapper = buildFlexQueryWrapper(billAccount);

        Page<BillAccount> page = billAccountService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 查询用户账户列表（不分页）
     */
    @Operation(summary = "查询用户账户列表")
    @SaCheckPermission("bill:account:query")
    @GetMapping("/user/{userId}")
    public AjaxResult listByUser(@PathVariable Long userId) {
        List<BillAccount> list = billAccountService.selectByUserId(userId);
        return success(list);
    }

    /**
     * 导出账户列表
     */
    @Operation(summary = "导出账户列表")
    @SaCheckPermission("bill:account:export")
    @Log(title = "账户管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BillAccount billAccount) {
        QueryWrapper queryWrapper = buildFlexQueryWrapper(billAccount);
        List<BillAccount> list = billAccountService.list(queryWrapper);
        MagicExcelUtil<BillAccount> util = new MagicExcelUtil<>(BillAccount.class);
        util.exportExcel(response, list, "账户数据");
    }

    /**
     * 获取账户详细信息
     */
    @Operation(summary = "获取账户详细信息")
    @SaCheckPermission("bill:account:query")
    @GetMapping(value = "/{accountId}")
    public AjaxResult getInfo(@PathVariable Long accountId) {
        return success(billAccountService.getById(accountId));
    }

    /**
     * 新增账户
     */
    @Operation(summary = "新增账户")
    @SaCheckPermission("bill:account:add")
    @Log(title = "账户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BillAccount billAccount) {
        // 默认余额为0
        if (billAccount.getBalance() == null) {
            billAccount.setBalance(BigDecimal.ZERO);
        }
        return toAjax(billAccountService.save(billAccount) ? 1 : 0);
    }

    /**
     * 修改账户
     */
    @Operation(summary = "修改账户")
    @SaCheckPermission("bill:account:edit")
    @Log(title = "账户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BillAccount billAccount) {
        return toAjax(billAccountService.updateById(billAccount) ? 1 : 0);
    }

    /**
     * 删除账户
     */
    @Operation(summary = "删除账户")
    @SaCheckPermission("bill:account:remove")
    @Log(title = "账户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{accountIds}")
    public AjaxResult remove(@PathVariable Long[] accountIds) {
        return toAjax(billAccountService.removeByIds(Arrays.asList(accountIds)) ? accountIds.length : 0);
    }

    /**
     * 更新账户余额
     */
    @Operation(summary = "更新账户余额")
    @SaCheckPermission("bill:account:edit")
    @Log(title = "账户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/updateBalance")
    public AjaxResult updateBalance(
            @RequestParam Long accountId,
            @RequestParam BigDecimal amount) {
        return toAjax(billAccountService.updateBalance(accountId, amount) ? 1 : 0);
    }

    /**
     * 账户余额调整（增加或减少）
     */
    @Operation(summary = "账户余额调整")
    @SaCheckPermission("bill:account:edit")
    @Log(title = "账户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/adjustBalance")
    public AjaxResult adjustBalance(
            @RequestParam Long accountId,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "true") Boolean isAdd) {
        BillAccount account = billAccountService.getById(accountId);
        if (account == null) {
            return error("账户不存在");
        }

        BigDecimal newBalance;
        if (isAdd) {
            newBalance = account.getBalance().add(amount);
        } else {
            newBalance = account.getBalance().subtract(amount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                return error("余额不足");
            }
        }

        return toAjax(billAccountService.updateBalance(accountId, newBalance) ? 1 : 0);
    }

    /**
     * 查询账户总资产
     */
    @Operation(summary = "查询账户总资产")
    @SaCheckPermission("bill:account:query")
    @GetMapping("/totalAssets/{userId}")
    public AjaxResult getTotalAssets(@PathVariable Long userId) {
        List<BillAccount> accounts = billAccountService.selectByUserId(userId);
        BigDecimal totalAssets = accounts.stream()
                .map(BillAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return success(totalAssets);
    }

    /**
     * 启用/禁用账户
     */
    @Operation(summary = "启用/禁用账户")
    @SaCheckPermission("bill:account:edit")
    @Log(title = "账户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(
            @RequestParam Long accountId,
            @RequestParam String status) {
        BillAccount account = billAccountService.getById(accountId);
        if (account == null) {
            return error("账户不存在");
        }

        account.setStatus(status);
        return toAjax(billAccountService.updateById(account) ? 1 : 0);
    }
}
