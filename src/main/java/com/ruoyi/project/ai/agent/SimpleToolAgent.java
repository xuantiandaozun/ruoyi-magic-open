package com.ruoyi.project.ai.agent;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.agentic.Agent;

/**
 * 简单工具Agent接口
 * 用于LangChain4j Agent的基本实现，支持工具调用
 * 
 * @author ruoyi
 */
public interface SimpleToolAgent {
    
    /**
     * 执行Agent任务
     * 
     * @param input 用户输入内容
     * @return Agent执行结果
     */
    @UserMessage("{{input}}")
    @Agent("A general-purpose agent that can use tools to help users")
    String invoke(@V("input") String input);
}