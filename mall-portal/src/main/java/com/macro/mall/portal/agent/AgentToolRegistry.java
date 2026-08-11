package com.macro.mall.portal.agent;

import com.macro.mall.portal.domain.ChatResponse;
import com.macro.mall.portal.service.impl.FunctionToolExecutor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 工具注册器：统一构建供 LLM 调用的 ToolCallback，并把工具结果捕获到「请求级」的 Map，
 * 供后续提取前端卡片。capturedResults 由调用方按请求传入，避免实例级共享造成并发串扰。
 */
@Component
public class AgentToolRegistry {

    private final FunctionToolExecutor executor;

    public AgentToolRegistry(FunctionToolExecutor executor) {
        this.executor = executor;
    }

    /** 给 LLM 看的工具清单，用于 Planner 规划 */
    public String toolCatalog() {
        return """
                - searchProducts(keyword, sort): 搜索商城商品
                - getProductDetail(productId): 查询商品详情
                - listMyOrders(status): 查询当前用户订单列表（需登录）
                - queryOrder(orderId): 查询订单详情（需登录）
                """;
    }

    /**
     * 构建工具回调，工具执行结果写入传入的 captured（请求级）。
     */
    public List<ToolCallback> buildCallbacks(Map<String, Object> captured) {
        List<ToolCallback> callbacks = new ArrayList<>();

        callbacks.add(FunctionToolCallback.builder("searchProducts",
                        (Function<FunctionToolExecutor.SearchProductsRequest, List<FunctionToolExecutor.ProductToolResult>>) req -> {
                            var result = executor.searchProducts(req);
                            captured.put("searchProducts", result);
                            return result;
                        })
                .description("搜索商城商品")
                .inputType(FunctionToolExecutor.SearchProductsRequest.class)
                .build());

        callbacks.add(FunctionToolCallback.builder("getProductDetail",
                        (Function<FunctionToolExecutor.GetProductDetailRequest, FunctionToolExecutor.ProductToolResult>) req -> {
                            var result = executor.getProductDetail(req);
                            captured.put("getProductDetail", result);
                            return result;
                        })
                .description("查询商品详情")
                .inputType(FunctionToolExecutor.GetProductDetailRequest.class)
                .build());

        callbacks.add(FunctionToolCallback.builder("listMyOrders",
                        (Function<FunctionToolExecutor.ListMyOrdersRequest, List<FunctionToolExecutor.OrderToolResult>>) req -> {
                            var result = executor.listMyOrders(req);
                            captured.put("listMyOrders", result);
                            return result;
                        })
                .description("查询用户订单列表")
                .inputType(FunctionToolExecutor.ListMyOrdersRequest.class)
                .build());

        callbacks.add(FunctionToolCallback.builder("queryOrder",
                        (Function<FunctionToolExecutor.QueryOrderRequest, FunctionToolExecutor.OrderToolResult>) req -> {
                            var result = executor.queryOrder(req);
                            captured.put("queryOrder", result);
                            return result;
                        })
                .description("查询订单详情")
                .inputType(FunctionToolExecutor.QueryOrderRequest.class)
                .build());

        return callbacks;
    }

    public List<ChatResponse.ProductCard> extractProducts(Map<String, Object> captured) {
        for (var entry : captured.entrySet()) {
            var cards = executor.extractProductCards(entry.getKey(), entry.getValue());
            if (cards != null) return cards;
        }
        return null;
    }

    public ChatResponse.OrderCard extractOrder(Map<String, Object> captured) {
        for (var entry : captured.entrySet()) {
            var card = executor.extractOrderCard(entry.getKey(), entry.getValue());
            if (card != null) return card;
        }
        return null;
    }
}
