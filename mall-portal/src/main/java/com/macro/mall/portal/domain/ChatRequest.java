package com.macro.mall.portal.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 客服聊天请求
 */
@Data
@Schema(description = "AI客服聊天请求")
public class ChatRequest {

    @Schema(description = "用户消息")
    private String message;

    @Schema(description = "会话ID（首次为空，后续传入上次返回的sessionId以保持上下文）")
    private String sessionId;
}
