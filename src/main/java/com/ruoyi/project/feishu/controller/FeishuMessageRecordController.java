package com.ruoyi.project.feishu.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
import com.ruoyi.project.feishu.domain.FeishuMessageRecord;
import com.ruoyi.project.feishu.service.IFeishuMessageRecordService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 飞书消息发送记录Controller
 * 
 * @author ruoyi
 * @date 2025-11-27
 */
@Tag(name = "飞书消息记录管理", description = "飞书消息发送记录相关接口")
@RestController
@RequestMapping("/system/feishu/record")
public class FeishuMessageRecordController extends BaseController {

    @Autowired
    private IFeishuMessageRecordService feishuMessageRecordService;

    /**
     * 查询飞书消息发送记录列表
     */
    @Operation(summary = "查询消息记录列表", description = "分页查询飞书消息发送记录")
    @SaCheckPermission("system:feishu:record:list")
    @GetMapping("/list")
    public TableDataInfo list(FeishuMessageRecord feishuMessageRecord) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        
        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = buildFlexQueryWrapper(feishuMessageRecord);
        
        // 使用 MyBatisFlex 的分页方法
        Page<FeishuMessageRecord> page = feishuMessageRecordService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 导出飞书消息发送记录列表
     */
    @Operation(summary = "导出消息记录", description = "导出飞书消息发送记录列表")
    @SaCheckPermission("system:feishu:record:export")
    @Log(title = "飞书消息发送记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, FeishuMessageRecord feishuMessageRecord) {
        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = buildFlexQueryWrapper(feishuMessageRecord);
        
        List<FeishuMessageRecord> list = feishuMessageRecordService.list(queryWrapper);
        MagicExcelUtil<FeishuMessageRecord> util = new MagicExcelUtil<>(FeishuMessageRecord.class);
        util.exportExcel(response, list, "飞书消息发送记录数据");
    }

    /**
     * 获取飞书消息发送记录详细信息
     */
    @Operation(summary = "查询消息记录详情", description = "根据ID查询飞书消息发送记录详情")
    @SaCheckPermission("system:feishu:record:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "消息记录ID", required = true) @PathVariable("id") Long id) {
        return success(feishuMessageRecordService.getById(id));
    }

    /**
     * 删除飞书消息发送记录
     */
    @Operation(summary = "删除消息记录", description = "根据ID删除飞书消息发送记录")
    @SaCheckPermission("system:feishu:record:remove")
    @Log(title = "飞书消息发送记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "消息记录ID列表", required = true) @PathVariable Long[] ids) {
        return toAjax(feishuMessageRecordService.removeByIds(Arrays.asList(ids)));
    }
}
