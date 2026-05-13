package com.macro.mall.portal.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * AI 模型 HTTP 客户端，封装 OpenAI 兼容 API 调用
 */
@Component
public class AiModelClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiModelClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${ai.chat.api-url}")
    private String apiUrl;

    @Value("${ai.chat.api-key}")
    private String apiKey;

    @Value("${ai.chat.model}")
    private String model;

    private final RestClient restClient = RestClient.create();

    public ChatCompletionResponse chat(ChatCompletionRequest request) {
        request.setModel(model);
        LOGGER.info("调用 AI 模型: model={}, messages count={}", model, request.getMessages().size());
        return restClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ChatCompletionResponse.class);
    }

    // ── OpenAI 兼容 API 模型 ──

    @Data
    public static class ChatCompletionRequest {
        private String model;
        private List<Message> messages;
        private List<Tool> tools;
        @JsonProperty("tool_choice")
        private String toolChoice = "auto";
        @JsonProperty("max_tokens")
        private Integer maxTokens = 2000;
        private Double temperature = 0.7;
    }

    @Data
    public static class Message {
        private String role;
        private String content;
        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;
        @JsonProperty("tool_call_id")
        private String toolCallId;
        private String name;

        public static Message system(String content) {
            Message m = new Message();
            m.role = "system";
            m.content = content;
            return m;
        }

        public static Message user(String content) {
            Message m = new Message();
            m.role = "user";
            m.content = content;
            return m;
        }

        public static Message assistant(String content) {
            Message m = new Message();
            m.role = "assistant";
            m.content = content;
            return m;
        }

        public static Message tool(String toolCallId, String name, String content) {
            Message m = new Message();
            m.role = "tool";
            m.toolCallId = toolCallId;
            m.name = name;
            m.content = content;
            return m;
        }
    }

    @Data
    public static class Tool {
        private String type = "function";
        private Function function;

        @Data
        public static class Function {
            private String name;
            private String description;
            private Map<String, Object> parameters;
        }
    }

    @Data
    public static class ToolCall {
        private String id;
        private String type = "function";
        private FunctionCall function;

        @Data
        public static class FunctionCall {
            private String name;
            private String arguments;
        }
    }

    @Data
    public static class ChatCompletionResponse {
        private String id;
        private List<Choice> choices;
        private Usage usage;

        @Data
        public static class Choice {
            private Integer index;
            private Message message;
            @JsonProperty("finish_reason")
            private String finishReason;
        }

        @Data
        public static class Usage {
            @JsonProperty("prompt_tokens")
            private Integer promptTokens;
            @JsonProperty("completion_tokens")
            private Integer completionTokens;
            @JsonProperty("total_tokens")
            private Integer totalTokens;
        }
    }
}
