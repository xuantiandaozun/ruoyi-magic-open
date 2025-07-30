package com.ruoyi.framework.config.magic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.ssssssss.magicapi.core.context.RequestEntity;
import org.ssssssss.magicapi.core.interceptor.ResultProvider;
import org.ssssssss.magicapi.modules.db.model.Page;

import com.ruoyi.common.constant.HttpStatus;

/**
 * Magic API 自定义返回结果提供者
 * 统一返回格式，与项目的 AjaxResult 保持一致
 * 
 * @author ruoyi
 */
@Component
public class CustomResultProvider implements ResultProvider {

    /**
     * 定义返回结果，与 AjaxResult 格式保持一致
     * 
     * @param requestEntity 请求实体
     * @param code 状态码
     * @param message 消息
     * @param data 数据
     * @return 统一格式的返回结果
     */
    @Override
    public Object buildResult(RequestEntity requestEntity, int code, String message, Object data) {
        Map<String, Object> result = new HashMap<>();
        
        // 转换状态码，使其与项目的HttpStatus保持一致
        int resultCode;
        switch (code) {
            case 1: // magic-api 执行成功
                resultCode = HttpStatus.SUCCESS;
                break;
            case 0: // magic-api 参数验证失败
                resultCode = HttpStatus.BAD_REQUEST;
                break;
            case -1: // magic-api 系统异常
                resultCode = HttpStatus.ERROR;
                break;
            default:
                resultCode = code;
                break;
        }
        
        result.put("code", resultCode);
        result.put("msg", message != null ? message : (resultCode == HttpStatus.SUCCESS ? "操作成功" : "操作失败"));
        
        if (data != null) {
            result.put("data", data);
        }
        
        return result;
    }

    /**
     * 定义分页返回结果
     * 
     * @param requestEntity 请求实体
     * @param page 分页对象
     * @param total 总数
     * @param data 数据列表
     * @return 分页结果
     */
    @Override
    public Object buildPageResult(RequestEntity requestEntity, Page page, long total, List<Map<String, Object>> data) {
        Map<String, Object> pageResult = new HashMap<>();
        pageResult.put("total", total);
        pageResult.put("rows", data);
        pageResult.put("page", page.getOffset());
        pageResult.put("size", page.getLimit());
        
        return pageResult;
    }
}