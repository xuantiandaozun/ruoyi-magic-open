package com.ruoyi.project.miniapp.controller.bill;

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
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.bill.domain.BillAccount;
import com.ruoyi.project.bill.service.IBillAccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "小程序-账户管理")
@RestController
@RequestMapping("/miniapp/bill/account")
public class MiniAppBillAccountController extends BillMiniAppBaseController {

    @Autowired
    private IBillAccountService billAccountService;

    @Operation(summary = "查询账户列表")
    @GetMapping("/list")
    public TableDataInfo list(BillAccount billAccount) {
        billAccount.setUserId(getBillUserId());
        PageDomain pageDomain = TableSupport.buildPageRequest();
        QueryWrapper queryWrapper = buildFlexQueryWrapper(billAccount);
        Page<BillAccount> page = billAccountService.page(
                new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize()), queryWrapper);
        return getDataTable(page);
    }

    @Operation(summary = "查询用户账户列表")
    @GetMapping("/user/{userId}")
    public AjaxResult listByUser(@PathVariable Long userId) {
        if (!getBillUserId().equals(userId)) {
            userId = getBillUserId();
        }
        requireBillProfile();
        List<BillAccount> list = billAccountService.selectByUserId(userId);
        return success(list);
    }

    @Operation(summary = "获取账户详细信息")
    @GetMapping("/{accountId}")
    public AjaxResult getInfo(@PathVariable Long accountId) {
        return success(billAccountService.getById(accountId));
    }

    @Operation(summary = "新增账户")
    @PostMapping
    public AjaxResult add(@RequestBody BillAccount billAccount) {
        billAccount.setUserId(getBillUserId());
        if (billAccount.getBalance() == null) {
            billAccount.setBalance(BigDecimal.ZERO);
        }
        return toAjax(billAccountService.save(billAccount) ? 1 : 0);
    }

    @Operation(summary = "修改账户")
    @PutMapping
    public AjaxResult edit(@RequestBody BillAccount billAccount) {
        return toAjax(billAccountService.updateById(billAccount) ? 1 : 0);
    }

    @Operation(summary = "删除账户")
    @DeleteMapping("/{accountIds}")
    public AjaxResult remove(@PathVariable Long[] accountIds) {
        return toAjax(billAccountService.removeByIds(Arrays.asList(accountIds)) ? accountIds.length : 0);
    }

    @Operation(summary = "查询账户总资产")
    @GetMapping("/totalAssets/{userId}")
    public AjaxResult getTotalAssets(@PathVariable Long userId) {
        if (!getBillUserId().equals(userId)) {
            userId = getBillUserId();
        }
        List<BillAccount> accounts = billAccountService.selectByUserId(userId);
        BigDecimal totalAssets = accounts.stream()
                .map(BillAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return success(totalAssets);
    }

    @Operation(summary = "启用/禁用账户")
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestParam Long accountId, @RequestParam String status) {
        BillAccount account = billAccountService.getById(accountId);
        if (account == null) {
            return error("账户不存在");
        }
        account.setStatus(status);
        return toAjax(billAccountService.updateById(account) ? 1 : 0);
    }
}
