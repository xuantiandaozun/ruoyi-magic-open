package com.ruoyi.project.feishu.controller;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.gson.JsonParser;
import com.lark.oapi.Client;
import com.lark.oapi.core.request.RequestOptions;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.service.drive.v1.model.ListFileReq;
import com.lark.oapi.service.drive.v1.model.ListFileResp;
import com.lark.oapi.service.drive.v1.model.File;
import com.ruoyi.common.utils.FeishuConfigUtils;
import com.ruoyi.project.system.config.FeishuConfig;
import com.ruoyi.project.system.service.IFeishuOAuthService;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
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
import com.ruoyi.common.utils.poi.MagicExcelUtil;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.project.feishu.domain.FeishuDoc;
import com.ruoyi.project.feishu.service.IFeishuDocService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 飞书文档信息Controller
 * 
 * @author ruoyi
 * @date 2025-07-31 16:47:44
 */
@Slf4j
@Tag(name = "飞书文档信息")
@RestController
@RequestMapping("/feishu/feishudoc")
public class FeishuDocController extends BaseController
{
    @Autowired
    private IFeishuDocService feishuDocService;
    
    @Autowired
    private IFeishuOAuthService feishuOAuthService;


    

    /**
     * 查询飞书文档信息列表
     */
    @Operation(summary = "查询飞书文档信息列表")
    @SaCheckPermission("feishu:feishudoc:list")
    @GetMapping("/list")
    public TableDataInfo list(FeishuDoc feishuDoc)
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        
        // 创建 MyBatisFlex 的 QueryWrapper
        QueryWrapper queryWrapper = buildFlexQueryWrapper(feishuDoc);
        
        // 使用 MyBatisFlex 的分页方法
        Page<FeishuDoc> page = feishuDocService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return getDataTable(page);
    }

    /**
     * 导出飞书文档信息列表
     */
    @Operation(summary = "导出飞书文档信息列表")
    @SaCheckPermission("feishu:feishudoc:export")
    @Log(title = "飞书文档信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, FeishuDoc feishuDoc)
    {
        QueryWrapper queryWrapper = QueryWrapper.create();
        // 这里需要根据实际业务编写查询条件
        
        List<FeishuDoc> list = feishuDocService.list(queryWrapper);
        MagicExcelUtil<FeishuDoc> util = new MagicExcelUtil<>(FeishuDoc.class);
        util.exportExcel(response, list, "飞书文档信息数据");
    }

    /**
     * 获取飞书文档信息详细信息
     */
    @Operation(summary = "获取飞书文档信息详细信息")
    @SaCheckPermission("feishu:feishudoc:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(feishuDocService.getById(id));
    }

    /**
     * 新增飞书文档信息
     */
    @Operation(summary = "新增飞书文档信息")
    @SaCheckPermission("feishu:feishudoc:add")
    @Log(title = "飞书文档信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody FeishuDoc feishuDoc)
    {
        return toAjax(feishuDocService.save(feishuDoc) ? 1 : 0);
    }

    /**
     * 修改飞书文档信息
     */
    @Operation(summary = "修改飞书文档信息")
    @SaCheckPermission("feishu:feishudoc:edit")
    @Log(title = "飞书文档信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody FeishuDoc feishuDoc)
    {
        return toAjax(feishuDocService.updateById(feishuDoc) ? 1 : 0);
    }

    /**
     * 删除飞书文档信息
     */
    @Operation(summary = "删除飞书文档信息")
    @SaCheckPermission("feishu:feishudoc:remove")
    @Log(title = "飞书文档信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(feishuDocService.removeByIds(Arrays.asList(ids)) ? ids.length : 0);
    }
    
    /**
     * 同步飞书文档
     */
    @Operation(summary = "同步飞书文档")
    @SaCheckPermission("feishu:feishudoc:sync")
    @Log(title = "同步飞书文档", businessType = BusinessType.OTHER)
    @PostMapping("/sync")
    public AjaxResult syncFeishuDocs(@RequestParam(value = "keyName", required = false) String keyName,
                                   @RequestParam(value = "orderBy", required = false) String orderBy,
                                   @RequestParam(value = "direction", required = false) String direction,
                                   @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                   @RequestParam(value = "pageToken", required = false) String pageToken)
    {
        try {
            String result = feishuDocService.syncFeishuDocuments(keyName, orderBy, direction, pageSize, pageToken);
            return success(result);
        } catch (Exception e) {
            log.error("同步飞书文档异常", e);
            return error("同步异常: " + e.getMessage());
        }
    }
    
    /**
     * 上传文件到飞书
     */
    @Operation(summary = "上传文件到飞书")
    @SaCheckPermission("feishu:feishudoc:upload")
    @Log(title = "上传文件到飞书", businessType = BusinessType.OTHER)
    @PostMapping("/upload")
    public AjaxResult uploadFile(@RequestParam("file") java.io.File file,
                               @RequestParam(value = "fileName", required = false) String fileName,
                               @RequestParam(value = "parentType", required = false) String parentType,
                               @RequestParam(value = "parentNode", required = false) String parentNode,
                               @RequestParam(value = "keyName", required = false) String keyName)
    {
        try {
            FeishuDoc result = feishuDocService.uploadFileToFeishu(file, fileName, parentType, parentNode, keyName);
            return success(result);
        } catch (Exception e) {
            log.error("上传文件到飞书异常", e);
            return error("上传异常: " + e.getMessage());
        }
    }
    
    /**
     * 删除飞书文档
     */
    @Operation(summary = "删除飞书文档")
    @SaCheckPermission("feishu:feishudoc:delete")
    @Log(title = "删除飞书文档", businessType = BusinessType.DELETE)
    @DeleteMapping("/feishu/{fileToken}")
    public AjaxResult deleteFeishuFile(@PathVariable("fileToken") String fileToken,
                                     @RequestParam(value = "type", required = false) String type,
                                     @RequestParam(value = "keyName", required = false) String keyName)
    {
        try {
            boolean result = feishuDocService.deleteFeishuFile(fileToken, type, keyName);
            return toAjax(result ? 1 : 0);
        } catch (Exception e) {
            log.error("删除飞书文档异常", e);
            return error("删除异常: " + e.getMessage());
        }
    }
    
    /**
     * 创建飞书文件夹
     */
    @Operation(summary = "创建飞书文件夹")
    @SaCheckPermission("feishu:feishudoc:createFolder")
    @Log(title = "创建飞书文件夹", businessType = BusinessType.INSERT)
    @PostMapping("/createFolder")
    public AjaxResult createFolder(@RequestParam("name") String name,
                                 @RequestParam(value = "folderToken", required = false) String folderToken,
                                 @RequestParam(value = "keyName", required = false) String keyName)
    {
        try {
            FeishuDoc result = feishuDocService.createFeishuFolder(name, folderToken, keyName);
            return success(result);
        } catch (Exception e) {
            log.error("创建飞书文件夹异常", e);
            return error("创建文件夹异常: " + e.getMessage());
        }
    }
    
    /**
     * 创建导入任务
     */
    @Operation(summary = "创建导入任务")
    @SaCheckPermission("feishu:feishudoc:import")
    @Log(title = "创建导入任务", businessType = BusinessType.OTHER)
    @PostMapping("/createImportTask")
    public AjaxResult createImportTask(@RequestParam("fileExtension") String fileExtension,
                                     @RequestParam("fileToken") String fileToken,
                                     @RequestParam("type") String type,
                                     @RequestParam("fileName") String fileName,
                                     @RequestParam(value = "mountType", required = false) Integer mountType,
                                     @RequestParam("mountKey") String mountKey,
                                     @RequestParam(value = "keyName", required = false) String keyName)
    {
        try {
            String result = feishuDocService.createImportTask(fileExtension, fileToken, type, fileName, mountType, mountKey, keyName);
            return success(result);
        } catch (Exception e) {
            log.error("创建导入任务异常", e);
            return error("创建导入任务异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取飞书配置
     */
    private FeishuConfig getFeishuConfig(String keyName) {
        return FeishuConfigUtils.getFeishuConfig(keyName);
    }
}
