package com.ruoyi.project.ai.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.hutool.core.util.StrUtil;

/**
 * 提示词变量处理工具类
 * 支持在用户提示词中使用变量占位符，如：{{variable_name}}
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
public class PromptVariableProcessor {
    
    /**
     * 变量占位符正则表达式：匹配 {{variable_name}} 格式
     */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    
    /**
     * 处理用户提示词中的变量占位符
     * 
     * @param userPrompt 用户提示词模板
     * @param variables 变量映射表
     * @return 处理后的用户提示词
     */
    public static String processVariables(String userPrompt, Map<String, Object> variables) {
        if (StrUtil.isBlank(userPrompt) || variables == null || variables.isEmpty()) {
            return userPrompt;
        }
        
        Matcher matcher = VARIABLE_PATTERN.matcher(userPrompt);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            Object variableValue = variables.get(variableName);
            
            // 如果变量存在，则替换；否则保留原占位符
            String replacement = variableValue != null ? variableValue.toString() : matcher.group(0);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * 检查用户提示词中是否包含变量占位符
     * 
     * @param userPrompt 用户提示词
     * @return 是否包含变量占位符
     */
    public static boolean hasVariables(String userPrompt) {
        if (StrUtil.isBlank(userPrompt)) {
            return false;
        }
        return VARIABLE_PATTERN.matcher(userPrompt).find();
    }
    
    /**
     * 提取用户提示词中的所有变量名
     * 
     * @param userPrompt 用户提示词
     * @return 变量名数组
     */
    public static String[] extractVariableNames(String userPrompt) {
        if (StrUtil.isBlank(userPrompt)) {
            return new String[0];
        }
        
        Matcher matcher = VARIABLE_PATTERN.matcher(userPrompt);
        java.util.List<String> variableNames = new java.util.ArrayList<>();
        
        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            if (!variableNames.contains(variableName)) {
                variableNames.add(variableName);
            }
        }
        
        return variableNames.toArray(new String[0]);
    }
    
    /**
     * 验证用户提示词中的变量是否都有对应的值
     * 
     * @param userPrompt 用户提示词
     * @param variables 变量映射表
     * @return 缺失的变量名数组
     */
    public static String[] validateVariables(String userPrompt, Map<String, Object> variables) {
        String[] requiredVariables = extractVariableNames(userPrompt);
        java.util.List<String> missingVariables = new java.util.ArrayList<>();
        
        for (String variableName : requiredVariables) {
            if (variables == null || !variables.containsKey(variableName)) {
                missingVariables.add(variableName);
            }
        }
        
        return missingVariables.toArray(new String[0]);
    }
}