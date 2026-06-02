package com.macro.mall.portal.service.impl;

import cn.hutool.core.util.StrUtil;
import com.macro.mall.portal.config.RedisChatMemory;
import com.macro.mall.portal.domain.ChatRequest;
import com.macro.mall.portal.domain.ChatResponse;
import com.macro.mall.portal.service.AiChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service
public class AiChatServiceImpl implements AiChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatServiceImpl.class);

    private static final String SYSTEM_PROMPT = """
            你是mall商城的AI智能客服，名叫"小Mall"。你的职责是帮助用户解决购物问题。

            你可以：
            1. 回答关于商品、订单、配送、退换货、支付等常见电商问题
            2. 根据用户需求搜索商品并推荐，推荐时列出名称、价格、卖点
            3. 帮助用户查询订单状态和物流信息

            规则：
            - 回答简洁友好，不超过200字
            - 商品推荐时结合用户需求做个性化建议
            - 订单查询需要用户先登录
            - 遇到无法处理的问题，引导用户联系人工客服
            - 不要编造商品信息，必须通过工具查询真实数据
            - 如果用户问商品相关问题，主动调用searchProducts搜索
            """;

    private final ChatClient chatClient;
    private final RedisChatMemory chatMemory;
    private final FunctionToolExecutor functionToolExecutor;
    private final Map<String, Object> capturedResults = new ConcurrentHashMap<>();

    public AiChatServiceImpl(ChatModel chatModel, RedisChatMemory chatMemory,
                             FunctionToolExecutor functionToolExecutor) {
        this.chatMemory = chatMemory;
        this.functionToolExecutor = functionToolExecutor;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        String sessionId = resolveSessionId(request);
        capturedResults.clear();

        String reply = chatClient.prompt()
                .user(request.getMessage())
                .toolCallbacks(buildCallbacks())
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(sessionId)
                        .build())
                .call()
                .content();

        ChatResponse response = new ChatResponse();
        response.setReply(reply);
        response.setSessionId(sessionId);
        response.setProducts(extractProducts());
        response.setOrder(extractOrder());
        return response;
    }

    private String resolveSessionId(ChatRequest request) {
        if (StrUtil.isNotBlank(request.getSessionId())) {
            return request.getSessionId();
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private List<ToolCallback> buildCallbacks() {
        List<ToolCallback> callbacks = new ArrayList<>();

        callbacks.add(FunctionToolCallback.builder("searchProducts",
                (Function<FunctionToolExecutor.SearchProductsRequest, List<FunctionToolExecutor.ProductToolResult>>) req -> {
                    var result = functionToolExecutor.searchProducts(req);
                    capturedResults.put("searchProducts", result);
                    return result;
                })
                .description("搜索商城商品")
                .inputType(FunctionToolExecutor.SearchProductsRequest.class)
                .build());

        callbacks.add(FunctionToolCallback.builder("getProductDetail",
                (Function<FunctionToolExecutor.GetProductDetailRequest, FunctionToolExecutor.ProductToolResult>) req -> {
                    var result = functionToolExecutor.getProductDetail(req);
                    capturedResults.put("getProductDetail", result);
                    return result;
                })
                .description("查询商品详情")
                .inputType(FunctionToolExecutor.GetProductDetailRequest.class)
                .build());

        callbacks.add(FunctionToolCallback.builder("listMyOrders",
                (Function<FunctionToolExecutor.ListMyOrdersRequest, List<FunctionToolExecutor.OrderToolResult>>) req -> {
                    var result = functionToolExecutor.listMyOrders(req);
                    capturedResults.put("listMyOrders", result);
                    return result;
                })
                .description("查询用户订单列表")
                .inputType(FunctionToolExecutor.ListMyOrdersRequest.class)
                .build());

        callbacks.add(FunctionToolCallback.builder("queryOrder",
                (Function<FunctionToolExecutor.QueryOrderRequest, FunctionToolExecutor.OrderToolResult>) req -> {
                    var result = functionToolExecutor.queryOrder(req);
                    capturedResults.put("queryOrder", result);
                    return result;
                })
                .description("查询订单详情")
                .inputType(FunctionToolExecutor.QueryOrderRequest.class)
                .build());

        return callbacks;
    }

    private List<ChatResponse.ProductCard> extractProducts() {
        for (var entry : capturedResults.entrySet()) {
            var cards = functionToolExecutor.extractProductCards(entry.getKey(), entry.getValue());
            if (cards != null) return cards;
        }
        return null;
    }

    private ChatResponse.OrderCard extractOrder() {
        for (var entry : capturedResults.entrySet()) {
            var card = functionToolExecutor.extractOrderCard(entry.getKey(), entry.getValue());
            if (card != null) return card;
        }
        return null;
    }
}
