package com.ruoyi.project.ai.util;

import com.ruoyi.project.ai.tool.ToolExecutionResult;

import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * 工具执行结果处理器
 * 用于解析和判断工具执行结果，以确定工作流是否应该继续
 * 
 * @author ruoyi-magic
 * @date 2024-12-25
 */
public class ToolResultProcessor {
    
    /**
     * 判断字符串是否为JSON对象
     */
    private static boolean isJsonObject(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            JSONUtil.parseObj(str);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }
    
    /**
     * 判断工具执行结果是否为成功
     * 
     * @param resultJson 工具执行结果JSON字符串
     * @return 如果成功返回true，否则返回false
     */
    public static boolean isSuccess(String resultJson) {
        try {
            if (resultJson == null || resultJson.isEmpty()) {
                return false;
            }
            
            // 尝试解析为JSON
            if (isJsonObject(resultJson)) {
                JSONObject json = JSONUtil.parseObj(resultJson);
                Boolean success = json.getBool("success");
                return success != null && success;
            }
        } catch (Exception e) {
            // 如果解析失败，认为是失败
        }
        
        return false;
    }
    
    /**
     * 从工具结果中提取数据部分
     * 用于作为下一步的输入
     * 
     * @param resultJson 工具执行结果JSON字符串
     * @return 数据内容，如果不是标准格式则返回原字符串
     */
    public static Object extractData(String resultJson) {
        try {
            if (resultJson == null || resultJson.isEmpty()) {
                return null;
            }
            
            if (isJsonObject(resultJson)) {
                JSONObject json = JSONUtil.parseObj(resultJson);
                Object data = json.get("data");
                String message = json.getStr("message");
                
                // 优先返回data，如果为空则返回message
                return data != null ? data : message;
            }
        } catch (Exception e) {
            // 解析失败，返回原字符串
        }
        
        return resultJson;
    }
    
    /**
     * 从工具结果中提取完整的ToolExecutionResult对象
     * 
     * @param resultJson 工具执行结果JSON字符串
     * @return ToolExecutionResult对象，如果解析失败返回null
     */
    public static ToolExecutionResult parse(String resultJson) {
        try {
            if (resultJson == null || resultJson.isEmpty()) {
                return null;
            }
            
            if (isJsonObject(resultJson)) {
                return JSONUtil.toBean(JSONUtil.parseObj(resultJson), ToolExecutionResult.class);
            }
        } catch (Exception e) {
            // 解析失败
        }
        
        return null;
    }
    
    /**
     * 获取工具执行结果的消息
     * 
     * @param resultJson 工具执行结果JSON字符串
     * @return 消息内容
     */
    public static String getMessage(String resultJson) {
        try {
            if (resultJson == null || resultJson.isEmpty()) {
                return null;
            }
            
            if (isJsonObject(resultJson)) {
                JSONObject json = JSONUtil.parseObj(resultJson);
                return json.getStr("message");
            }
        } catch (Exception e) {
            // 解析失败
        }
        
        return resultJson;
    }
    
    /**
     * 判断结果是否为空数据
     * 当success=false且message表示空数据时返回true
     * 
     * @param resultJson 工具执行结果JSON字符串
     * @return 如果是空数据返回true
     */
    public static boolean isEmpty(String resultJson) {
        try {
            if (resultJson == null || resultJson.isEmpty()) {
                return true;
            }
            
            if (isJsonObject(resultJson)) {
                JSONObject json = JSONUtil.parseObj(resultJson);
                Boolean success = json.getBool("success");
                return success != null && !success;
            }
        } catch (Exception e) {
            // 解析失败认为是空
        }
        
        return true;
    }
    
    /**
     * 生成AI可理解的结果描述
     * 用于在System Prompt中告知AI如何处理工具结果
     * 
     * @param resultJson 工具执行结果JSON字符串
     * @return AI可理解的描述
     */
    public static String toAiFriendlyMessage(String resultJson) {
        try {
            ToolExecutionResult result = parse(resultJson);
            if (result != null) {
                if (result.getSuccess()) {
                    return String.format("✓ [%s] %s", 
                        result.getOperationType(), 
                        result.getMessage());
                } else {
                    return String.format("✗ [%s] %s - 工作流将停止", 
                        result.getOperationType(), 
                        result.getMessage());
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
        
        return resultJson;
    }
}
