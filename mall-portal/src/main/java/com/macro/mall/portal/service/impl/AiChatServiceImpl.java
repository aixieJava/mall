package com.macro.mall.portal.service.impl;

import cn.hutool.core.util.StrUtil;
import com.macro.mall.portal.agent.AgentOrchestrator;
import com.macro.mall.portal.domain.ChatRequest;
import com.macro.mall.portal.domain.ChatResponse;
import com.macro.mall.portal.service.AiChatService;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * AI 客服服务：委托给 AgentOrchestrator 执行 Plan-Act-Reflect 闭环。
 */
@Service
public class AiChatServiceImpl implements AiChatService {

    private final AgentOrchestrator orchestrator;

    public AiChatServiceImpl(AgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        String sessionId = resolveSessionId(request);
        return orchestrator.run(request.getMessage(), sessionId);
    }

    private String resolveSessionId(ChatRequest request) {
        if (StrUtil.isNotBlank(request.getSessionId())) {
            return request.getSessionId();
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
