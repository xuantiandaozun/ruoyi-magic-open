package com.ruoyi.project.ai.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.service.V;

import java.util.Map;

/**
 * 简单工具Agent接口
 * 用于LangChain4j Agent的基本实现，支持工具调用
 * 
 * @author ruoyi
 */
public interface SimpleToolAgent extends UntypedAgent {

    /**
     * 执行Agent任务
     * 
     * @param input 输入参数映射
     * @return 执行结果
     */
    @Agent("A simple agent that can use tools to complete tasks")
    Object invoke(Map<String, Object> input);
    
    /**
     * 处理用户消息
     * 
     * @param message 用户消息
     * @return 处理结果
     */
    @Agent("Process user message and use tools if needed")
    String processMessage(@V("message") String message);
    
    /**
     * 根据系统提示和用户输入执行任务
     * 
     * @param systemPrompt 系统提示
     * @param userInput 用户输入
     * @return 执行结果
     */
    @Agent("Execute task based on system prompt and user input")
    String executeTask(@V("systemPrompt") String systemPrompt, @V("userInput") String userInput);
}