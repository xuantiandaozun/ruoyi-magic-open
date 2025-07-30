package com.ruoyi.project.common.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mybatisflex.core.paginate.Page;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.project.common.domain.dto.DictQueryRequest;
import com.ruoyi.project.common.domain.vo.DictOption;
import com.ruoyi.project.common.domain.vo.DictQueryResponse;
import com.ruoyi.project.common.service.IDictionaryService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 数据字典查询控制器
 * 
 * @author ruoyi
 */
@Tag(name = "数据字典查询")
@RestController
@RequestMapping("/common/dict")
public class DictionaryController extends BaseController {

    @Autowired
    private IDictionaryService dictionaryService;

    /**
     * 获取表字典数据
     */
    @Operation(summary = "获取表字典数据")
    @SaCheckPermission("common:dict:query")
    @Log(title = "字典查询", businessType = BusinessType.OTHER)
    @GetMapping("/table/{tableName}")
    public AjaxResult getTableDict(
            @Parameter(description = "表名") @PathVariable String tableName,
            @Parameter(description = "显示字段名") @RequestParam(required = false) String labelField,
            @Parameter(description = "值字段名") @RequestParam(required = false) String valueField,
            @Parameter(description = "状态过滤") @RequestParam(required = false) String status) {
        
        try {
            List<DictOption> options = dictionaryService.getTableDict(tableName, labelField, valueField, status);
            
            // 构建响应对象
            DictQueryResponse response = new DictQueryResponse();
            response.setOptions(options);
            response.setTableName(tableName);
            response.setTotal((long) options.size());
            
            // 获取实际使用的字段名
            List<String> columns = dictionaryService.getTableColumns(tableName);
            if (!columns.isEmpty()) {
                String actualLabelField = StrUtil.isNotBlank(labelField) ? labelField : 
                    dictionaryService.getDefaultLabelField(tableName, columns);
                String actualValueField = StrUtil.isNotBlank(valueField) ? valueField : 
                    dictionaryService.getDefaultValueField(tableName, columns);
                response.setActualLabelField(actualLabelField);
                response.setActualValueField(actualValueField);
            }
            
            return success(response);
        } catch (Exception e) {
            logger.error("获取表字典数据失败: tableName={}, error={}", tableName, e.getMessage(), e);
            return error("获取字典数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取自定义表字典数据
     */
    @Operation(summary = "获取自定义表字典数据")
    @SaCheckPermission("common:dict:query")
    @Log(title = "自定义字典查询", businessType = BusinessType.OTHER)
    @PostMapping("/table/{tableName}/custom")
    public AjaxResult getCustomTableDict(
            @Parameter(description = "表名") @PathVariable String tableName,
            @Validated @RequestBody DictQueryRequest request) {
        
        try {
            List<Map<String, Object>> results = dictionaryService.getCustomTableDict(tableName, request);
            
            if (Boolean.TRUE.equals(request.getIncludeFullData())) {
                // 返回完整数据
                return success(results);
            } else {
                // 转换为标准字典格式
                List<DictOption> options = convertToOptions(results, request, tableName);
                
                DictQueryResponse response = new DictQueryResponse();
                response.setOptions(options);
                response.setTableName(tableName);
                response.setTotal((long) results.size());
                
                return success(response);
            }
        } catch (Exception e) {
            logger.error("获取自定义表字典数据失败: tableName={}, error={}", tableName, e.getMessage(), e);
            return error("获取字典数据失败: " + e.getMessage());
        }
    }

    /**
     * 分页获取表字典数据
     */
    @Operation(summary = "分页获取表字典数据")
    @SaCheckPermission("common:dict:query")
    @Log(title = "分页字典查询", businessType = BusinessType.OTHER)
    @PostMapping("/table/{tableName}/page")
    public TableDataInfo getTableDictPage(
            @Parameter(description = "表名") @PathVariable String tableName,
            @Validated @RequestBody DictQueryRequest request) {
        
        try {
            Page<Map<String, Object>> page = dictionaryService.getTableDictPage(tableName, request);
            
            TableDataInfo dataInfo = new TableDataInfo();
            dataInfo.setCode(200);
            dataInfo.setMsg("查询成功");
            dataInfo.setRows(page.getRecords());
            dataInfo.setTotal(page.getTotalRow());
            
            return dataInfo;
        } catch (Exception e) {
            logger.error("分页获取表字典数据失败: tableName={}, error={}", tableName, e.getMessage(), e);
            TableDataInfo errorInfo = new TableDataInfo();
            errorInfo.setCode(500);
            errorInfo.setMsg("查询失败: " + e.getMessage());
            return errorInfo;
        }
    }

    /**
     * 验证表是否存在
     */
    @Operation(summary = "验证表是否存在")
    @SaCheckPermission("common:dict:query")
    @GetMapping("/table/{tableName}/exists")
    public AjaxResult validateTableExists(@Parameter(description = "表名") @PathVariable String tableName) {
        try {
            boolean exists = dictionaryService.validateTableExists(tableName);
            return success("表验证完成", exists);
        } catch (Exception e) {
            logger.error("验证表是否存在失败: tableName={}, error={}", tableName, e.getMessage(), e);
            return error("验证表失败: " + e.getMessage());
        }
    }

    /**
     * 获取表的列信息
     */
    @Operation(summary = "获取表的列信息")
    @SaCheckPermission("common:dict:query")
    @GetMapping("/table/{tableName}/columns")
    public AjaxResult getTableColumns(@Parameter(description = "表名") @PathVariable String tableName) {
        try {
            List<String> columns = dictionaryService.getTableColumns(tableName);
            return success("获取列信息成功", columns);
        } catch (Exception e) {
            logger.error("获取表列信息失败: tableName={}, error={}", tableName, e.getMessage(), e);
            return error("获取列信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取表的默认字段映射
     */
    @Operation(summary = "获取表的默认字段映射")
    @SaCheckPermission("common:dict:query")
    @GetMapping("/table/{tableName}/default-fields")
    public AjaxResult getDefaultFields(@Parameter(description = "表名") @PathVariable String tableName) {
        try {
            List<String> columns = dictionaryService.getTableColumns(tableName);
            if (columns.isEmpty()) {
                return error("表不存在或无法获取列信息");
            }
            
            String defaultLabelField = dictionaryService.getDefaultLabelField(tableName, columns);
            String defaultValueField = dictionaryService.getDefaultValueField(tableName, columns);
            
            Map<String, Object> result = Map.of(
                "labelField", defaultLabelField,
                "valueField", defaultValueField,
                "columns", columns
            );
            
            return success("获取默认字段映射成功", result);
        } catch (Exception e) {
            logger.error("获取默认字段映射失败: tableName={}, error={}", tableName, e.getMessage(), e);
            return error("获取默认字段映射失败: " + e.getMessage());
        }
    }

    /**
     * 转换结果为字典选项格式
     */
    private List<DictOption> convertToOptions(List<Map<String, Object>> results, DictQueryRequest request, String tableName) {
        List<DictOption> options = new java.util.ArrayList<>();
        
        if (results.isEmpty()) {
            return options;
        }
        
        // 确定显示字段和值字段
        List<String> columns = dictionaryService.getTableColumns(tableName);
        String labelField = StrUtil.isNotBlank(request.getLabelField()) ? request.getLabelField() : 
            dictionaryService.getDefaultLabelField(tableName, columns);
        String valueField = StrUtil.isNotBlank(request.getValueField()) ? request.getValueField() : 
            dictionaryService.getDefaultValueField(tableName, columns);
        
        // 转换为驼峰格式的字段名（因为结果已经转换为驼峰格式）
        String camelLabelField = toCamelCase(labelField);
        String camelValueField = toCamelCase(valueField);
        
        for (Map<String, Object> row : results) {
            Object labelObj = row.get(camelLabelField);
            Object valueObj = row.get(camelValueField);
            
            if (labelObj != null && valueObj != null) {
                String label = String.valueOf(labelObj);
                String value = String.valueOf(valueObj);
                
                if (StrUtil.isNotBlank(label) && StrUtil.isNotBlank(value)) {
                    DictOption option = new DictOption(label, value);
                    if (Boolean.TRUE.equals(request.getIncludeFullData())) {
                        option.setData(row);
                    }
                    options.add(option);
                }
            }
        }
        
        return options;
    }

    /**
     * 下划线转驼峰
     */
    private String toCamelCase(String str) {
        if (StrUtil.isBlank(str)) {
            return str;
        }
        
        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = false;
        
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    result.append(Character.toUpperCase(c));
                    nextUpperCase = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }
        
        return result.toString();
    }
}