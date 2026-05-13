package com.macro.mall.portal.service;

import com.macro.mall.portal.domain.ChatRequest;
import com.macro.mall.portal.domain.ChatResponse;

/**
 * AI 客服聊天服务
 */
public interface AiChatService {
    ChatResponse chat(ChatRequest request);
}
