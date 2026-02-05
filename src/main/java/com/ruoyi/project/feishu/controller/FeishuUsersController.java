package com.ruoyi.project.feishu.controller;


import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mybatisflex.core.paginate.Page;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.project.feishu.domain.FeishuUsers;
import com.ruoyi.project.feishu.service.IFeishuUsersService;
import com.ruoyi.project.secretkey.domain.SecretKeyInfo;
import com.ruoyi.project.secretkey.service.ISecretKeyInfoService;

import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 飞书用户信息Controller
 * 
 * @author ruoyi
 * @date 2025-11-27
 */
@RestController
@RequestMapping("/feishu/users")
public class FeishuUsersController extends BaseController {
    
    @Autowired
    private IFeishuUsersService feishuUsersService;

    @Autowired
    private ISecretKeyInfoService secretKeyInfoService;

    /**
     * 查询飞书用户信息列表
     */
    @SaCheckPermission("feishu:users:list")
    @GetMapping("/list")
    public TableDataInfo list(FeishuUsers feishuUsers) {
        Page<FeishuUsers> page = feishuUsersService.page(getPage(), buildFlexQueryWrapper(feishuUsers));
        syncKeyName(page.getRecords());
        return getDataTable(page);
    }

    /**
     * 获取飞书用户信息详细信息
     */
    @SaCheckPermission("feishu:users:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        FeishuUsers user = feishuUsersService.getById(id);
        syncKeyName(user);
        return success(user);
    }

    /**
     * 根据open_id查询飞书用户
     */
    @SaCheckPermission("feishu:users:query")
    @GetMapping("/openId/{openId}")
    public AjaxResult getByOpenId(@PathVariable("openId") String openId) {
        FeishuUsers user = feishuUsersService.selectByOpenId(openId);
        syncKeyName(user);
        return success(user);
    }

    /**
     * 根据手机号查询飞书用户
     */
    @SaCheckPermission("feishu:users:query")
    @GetMapping("/mobile/{mobile}")
    public AjaxResult getByMobile(@PathVariable("mobile") String mobile) {
        FeishuUsers user = feishuUsersService.selectByMobile(mobile);
        syncKeyName(user);
        return success(user);
    }

    /**
     * 新增飞书用户信息
     */
    @SaCheckPermission("feishu:users:add")
    @Log(title = "飞书用户信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody FeishuUsers feishuUsers) {
        return toAjax(feishuUsersService.saveOrUpdateUser(feishuUsers));
    }

    /**
     * 修改飞书用户信息
     */
    @SaCheckPermission("feishu:users:edit")
    @Log(title = "飞书用户信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody FeishuUsers feishuUsers) {
        return toAjax(feishuUsersService.updateById(feishuUsers));
    }

    /**
     * 删除飞书用户信息
     */
    @SaCheckPermission("feishu:users:remove")
    @Log(title = "飞书用户信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(feishuUsersService.removeByIds(List.of(ids)));
    }

    private void syncKeyName(FeishuUsers user) {
        if (user == null || user.getKeyId() == null) {
            return;
        }
        SecretKeyInfo keyInfo = secretKeyInfoService.getById(user.getKeyId());
        if (keyInfo != null) {
            user.setKeyName(keyInfo.getKeyName());
        } else {
            user.setKeyName(null);
        }
    }

    private void syncKeyName(List<FeishuUsers> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<Long> keyIds = users.stream()
                .map(FeishuUsers::getKeyId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (keyIds.isEmpty()) {
            return;
        }
        List<SecretKeyInfo> keyInfos = secretKeyInfoService.listByIds(keyIds);
        Map<Long, String> keyNameMap = keyInfos.stream()
                .collect(Collectors.toMap(SecretKeyInfo::getId, SecretKeyInfo::getKeyName, (a, b) -> a));
        for (FeishuUsers user : users) {
            if (user != null && user.getKeyId() != null) {
                user.setKeyName(keyNameMap.get(user.getKeyId()));
            }
        }
    }


}
