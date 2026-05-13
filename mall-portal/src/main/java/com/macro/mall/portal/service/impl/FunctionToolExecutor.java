package com.macro.mall.portal.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.macro.mall.model.OmsOrder;
import com.macro.mall.model.PmsProduct;
import com.macro.mall.portal.domain.ChatResponse.OrderCard;
import com.macro.mall.portal.domain.ChatResponse.ProductCard;
import com.macro.mall.portal.domain.OmsOrderDetail;
import com.macro.mall.portal.domain.PmsPortalProductDetail;
import com.macro.mall.portal.service.OmsPortalOrderService;
import com.macro.mall.portal.service.PmsPortalProductService;
import com.macro.mall.portal.service.UmsMemberService;
import com.macro.mall.portal.util.StpMemberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 执行 AI 返回的 function call，调用项目现有服务
 */
@Component
public class FunctionToolExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionToolExecutor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private PmsPortalProductService productService;
    @Autowired
    private OmsPortalOrderService orderService;
    @Autowired
    private UmsMemberService memberService;

    /**
     * 执行工具调用，返回 JSON 字符串供 LLM 理解
     */
    public String execute(String functionName, String arguments) {
        LOGGER.info("执行工具调用: function={}, args={}", functionName, arguments);
        try {
            Map<String, Object> args = MAPPER.readValue(arguments, new TypeReference<Map<String, Object>>() {});
            return switch (functionName) {
                case "searchProducts" -> searchProducts(args);
                case "getProductDetail" -> getProductDetail(args);
                case "listMyOrders" -> listMyOrders(args);
                case "queryOrder" -> queryOrder(args);
                default -> "{\"error\": \"未知功能: " + functionName + "\"}";
            };
        } catch (Exception e) {
            LOGGER.error("工具调用失败: function={}", functionName, e);
            return "{\"error\": \"执行失败: " + e.getMessage() + "\"}";
        }
    }

    private String searchProducts(Map<String, Object> args) {
        String keyword = (String) args.getOrDefault("keyword", "");
        Integer sort = args.get("sort") != null ? ((Number) args.get("sort")).intValue() : 1;
        List<PmsProduct> products = productService.search(keyword, null, null, 1, 5, sort);
        if (CollUtil.isEmpty(products)) {
            return "{\"message\": \"未找到与'" + keyword + "'相关的商品\"}";
        }
        List<Map<String, Object>> list = products.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("subTitle", p.getSubTitle());
            m.put("price", p.getPrice());
            m.put("pic", p.getPic());
            m.put("sale", p.getSale());
            return m;
        }).collect(Collectors.toList());
        try { return MAPPER.writeValueAsString(list); } catch (Exception e) { return "[]"; }
    }

    private String getProductDetail(Map<String, Object> args) {
        Long productId = ((Number) args.get("productId")).longValue();
        PmsPortalProductDetail detail = productService.detail(productId);
        if (detail == null || detail.getProduct() == null) {
            return "{\"error\": \"商品不存在\"}";
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", detail.getProduct().getId());
        m.put("name", detail.getProduct().getName());
        m.put("subTitle", detail.getProduct().getSubTitle());
        m.put("price", detail.getProduct().getPrice());
        m.put("stock", detail.getProduct().getStock());
        m.put("sale", detail.getProduct().getSale());
        m.put("description", detail.getProduct().getDescription());
        m.put("brandName", detail.getBrand() != null ? detail.getBrand().getName() : "");
        try { return MAPPER.writeValueAsString(m); } catch (Exception e) { return "{}"; }
    }

    private String listMyOrders(Map<String, Object> args) {
        if (!isLoggedIn()) return "{\"error\": \"请先登录后再查询订单\"}";
        Integer status = args.get("status") != null ? ((Number) args.get("status")).intValue() : -1;
        var page = orderService.list(status, 1, 5);
        if (CollUtil.isEmpty(page.getList())) {
            return "{\"message\": \"暂无订单记录\"}";
        }
        List<Map<String, Object>> list = page.getList().stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("orderSn", o.getOrderSn());
            m.put("status", o.getStatus());
            m.put("statusText", getStatusText(o.getStatus()));
            m.put("payAmount", o.getPayAmount());
            m.put("createTime", o.getCreateTime() != null ? o.getCreateTime().toString() : "");
            return m;
        }).collect(Collectors.toList());
        try { return MAPPER.writeValueAsString(list); } catch (Exception e) { return "[]"; }
    }

    private String queryOrder(Map<String, Object> args) {
        if (!isLoggedIn()) return "{\"error\": \"请先登录后再查询订单\"}";
        Long orderId = ((Number) args.get("orderId")).longValue();
        OmsOrderDetail detail = orderService.detail(orderId);
        if (detail == null) {
            return "{\"error\": \"订单不存在\"}";
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", detail.getId());
        m.put("orderSn", detail.getOrderSn());
        m.put("status", detail.getStatus());
        m.put("statusText", getStatusText(detail.getStatus()));
        m.put("totalAmount", detail.getTotalAmount());
        m.put("payAmount", detail.getPayAmount());
        m.put("createTime", detail.getCreateTime() != null ? detail.getCreateTime().toString() : "");
        m.put("paymentTime", detail.getPaymentTime() != null ? detail.getPaymentTime().toString() : "");
        m.put("receiverName", detail.getReceiverName());
        m.put("receiverPhone", detail.getReceiverPhone());
        try { return MAPPER.writeValueAsString(m); } catch (Exception e) { return "{}"; }
    }

    private boolean isLoggedIn() {
        try {
            return StpMemberUtil.isLogin();
        } catch (Exception e) {
            return false;
        }
    }

    private String getStatusText(Integer status) {
        return switch (status) {
            case 0 -> "待付款";
            case 1 -> "待发货";
            case 2 -> "已发货";
            case 3 -> "已完成";
            case 4 -> "已关闭";
            default -> "未知";
        };
    }

    /**
     * 从工具返回值中提取商品卡片（用于前端展示）
     */
    public List<ProductCard> extractProductCards(String functionName, String toolResult) {
        if (!"searchProducts".equals(functionName) && !"getProductDetail".equals(functionName)) {
            return null;
        }
        try {
            if ("searchProducts".equals(functionName)) {
                List<ProductCard> cards = MAPPER.readValue(toolResult, new TypeReference<List<ProductCard>>() {});
                return CollUtil.isEmpty(cards) ? null : cards.subList(0, Math.min(cards.size(), 3));
            }
            if ("getProductDetail".equals(functionName)) {
                ProductCard card = MAPPER.readValue(toolResult, ProductCard.class);
                return card != null ? List.of(card) : null;
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 从工具返回值中提取订单卡片
     */
    public OrderCard extractOrderCard(String functionName, String toolResult) {
        if (!"queryOrder".equals(functionName)) return null;
        try {
            return MAPPER.readValue(toolResult, OrderCard.class);
        } catch (Exception e) {
            return null;
        }
    }
}
