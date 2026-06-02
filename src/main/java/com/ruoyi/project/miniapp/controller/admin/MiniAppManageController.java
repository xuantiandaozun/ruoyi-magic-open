package com.ruoyi.project.miniapp.controller.admin;

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
import org.springframework.web.bind.annotation.RestController;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.miniapp.domain.MiniApp;
import com.ruoyi.project.miniapp.service.IMiniAppService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 小程序配置管理 Controller
 */
@Tag(name = "小程序配置管理")
@RestController
@RequestMapping("/manage/miniapp")
public class MiniAppManageController extends BaseController {

    @Autowired
    private IMiniAppService miniAppService;

    @Operation(summary = "查询小程序配置列表")
    @SaCheckPermission("manage:miniapp:list")
    @GetMapping("/list")
    public TableDataInfo list(MiniApp miniApp) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        QueryWrapper queryWrapper = buildFlexQueryWrapper(miniApp);
        Page<MiniApp> page = miniAppService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    @Operation(summary = "查询所有小程序列表(不分页)")
    @SaCheckPermission("manage:miniapp:list")
    @GetMapping("/all")
    public AjaxResult listAll() {
        List<MiniApp> list = miniAppService.list();
        return success(list);
    }

    @Operation(summary = "获取小程序配置详情")
    @SaCheckPermission("manage:miniapp:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(miniAppService.getById(id));
    }

    @Operation(summary = "新增小程序配置")
    @SaCheckPermission("manage:miniapp:add")
    @Log(title = "小程序配置管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MiniApp miniApp) {
        return toAjax(miniAppService.save(miniApp) ? 1 : 0);
    }

    @Operation(summary = "修改小程序配置")
    @SaCheckPermission("manage:miniapp:edit")
    @Log(title = "小程序配置管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MiniApp miniApp) {
        return toAjax(miniAppService.updateById(miniApp) ? 1 : 0);
    }

    @Operation(summary = "删除小程序配置")
    @SaCheckPermission("manage:miniapp:remove")
    @Log(title = "小程序配置管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(miniAppService.removeByIds(Arrays.asList(ids)) ? ids.length : 0);
    }
}
