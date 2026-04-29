package com.ruoyi.project.ai.workflow;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ruoyi.project.ai.service.impl.LangChain4jAgentService;

import cn.hutool.core.collection.CollUtil;

/**
 * AI模型调用网关，隔离业务工作流和具体AI框架。
 */
@Component
public class AiGateway {

    private final LangChain4jAgentService agentService;

    public AiGateway(LangChain4jAgentService agentService) {
        this.agentService = agentService;
    }

    public String chat(Long modelConfigId, String systemPrompt, String userPrompt, List<String> tools) {
        if (CollUtil.isEmpty(tools)) {
            return agentService.chatWithSystem(modelConfigId, systemPrompt, userPrompt);
        }
        return agentService.chatWithTools(modelConfigId, systemPrompt, userPrompt, tools);
    }
}
