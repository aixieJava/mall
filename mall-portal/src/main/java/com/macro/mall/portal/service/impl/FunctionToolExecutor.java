package com.macro.mall.portal.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.macro.mall.model.OmsOrder;
import com.macro.mall.model.PmsProduct;
import com.macro.mall.portal.domain.ChatResponse.OrderCard;
import com.macro.mall.portal.domain.ChatResponse.ProductCard;
import com.macro.mall.portal.domain.OmsOrderDetail;
import com.macro.mall.portal.domain.PmsPortalProductDetail;
import com.macro.mall.portal.service.OmsPortalOrderService;
import com.macro.mall.portal.service.PmsPortalProductService;
import com.macro.mall.portal.util.StpMemberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 工具执行器 —— 提供具体方法供 Spring AI FunctionCallback 调用
 */
@Component
public class FunctionToolExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionToolExecutor.class);

    @Autowired
    private PmsPortalProductService productService;
    @Autowired
    private OmsPortalOrderService orderService;

    // ── 工具方法 ──

    public List<ProductToolResult> searchProducts(SearchProductsRequest request) {
        String keyword = request.keyword != null ? request.keyword : "";
        int sort = request.sort != null ? request.sort : 1;
        LOGGER.info("searchProducts: keyword={}, sort={}", keyword, sort);
        List<PmsProduct> products = productService.search(keyword, null, null, 1, 5, sort);
        if (CollUtil.isEmpty(products)) {
            return List.of();
        }
        return products.stream().map(p -> {
            ProductToolResult r = new ProductToolResult();
            r.id = p.getId();
            r.name = p.getName();
            r.subTitle = p.getSubTitle();
            r.price = p.getPrice();
            r.pic = p.getPic();
            r.sale = p.getSale();
            return r;
        }).collect(Collectors.toList());
    }

    public ProductToolResult getProductDetail(GetProductDetailRequest request) {
        LOGGER.info("getProductDetail: productId={}", request.productId);
        PmsPortalProductDetail detail = productService.detail(request.productId);
        if (detail == null || detail.getProduct() == null) {
            return null;
        }
        ProductToolResult r = new ProductToolResult();
        r.id = detail.getProduct().getId();
        r.name = detail.getProduct().getName();
        r.subTitle = detail.getProduct().getSubTitle();
        r.price = detail.getProduct().getPrice();
        r.pic = detail.getProduct().getPic();
        r.stock = detail.getProduct().getStock();
        r.sale = detail.getProduct().getSale();
        r.description = detail.getProduct().getDescription();
        r.brandName = detail.getBrand() != null ? detail.getBrand().getName() : "";
        return r;
    }

    public List<OrderToolResult> listMyOrders(ListMyOrdersRequest request) {
        LOGGER.info("listMyOrders: status={}", request.status);
        if (!isLoggedIn()) {
            throw new RuntimeException("请先登录后再查询订单");
        }
        int status = request.status != null ? request.status : -1;
        var page = orderService.list(status, 1, 5);
        if (CollUtil.isEmpty(page.getList())) {
            return List.of();
        }
        return page.getList().stream().map(o -> {
            OrderToolResult r = new OrderToolResult();
            r.id = o.getId();
            r.orderSn = o.getOrderSn();
            r.status = o.getStatus();
            r.statusText = getStatusText(o.getStatus());
            r.payAmount = o.getPayAmount();
            r.createTime = o.getCreateTime() != null ? o.getCreateTime().toString() : "";
            return r;
        }).collect(Collectors.toList());
    }

    public OrderToolResult queryOrder(QueryOrderRequest request) {
        LOGGER.info("queryOrder: orderId={}", request.orderId);
        if (!isLoggedIn()) {
            throw new RuntimeException("请先登录后再查询订单");
        }
        OmsOrderDetail detail = orderService.detail(request.orderId);
        if (detail == null) {
            return null;
        }
        OrderToolResult r = new OrderToolResult();
        r.id = detail.getId();
        r.orderSn = detail.getOrderSn();
        r.status = detail.getStatus();
        r.statusText = getStatusText(detail.getStatus());
        r.totalAmount = detail.getTotalAmount();
        r.payAmount = detail.getPayAmount();
        r.createTime = detail.getCreateTime() != null ? detail.getCreateTime().toString() : "";
        r.paymentTime = detail.getPaymentTime() != null ? detail.getPaymentTime().toString() : "";
        r.receiverName = detail.getReceiverName();
        r.receiverPhone = detail.getReceiverPhone();
        return r;
    }

    // ── 卡片提取（供前端展示） ──

    public List<ProductCard> extractProductCards(String functionName, Object result) {
        if (!"searchProducts".equals(functionName) && !"getProductDetail".equals(functionName)) {
            return null;
        }
        if (result == null) return null;
        try {
            if (result instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof ProductToolResult) {
                    List<ProductCard> cards = new ArrayList<>();
                    for (Object item : list) {
                        ProductToolResult p = (ProductToolResult) item;
                        ProductCard card = new ProductCard();
                        card.setId(p.id);
                        card.setName(p.name);
                        card.setPic(p.pic);
                        card.setPrice(p.price);
                        card.setSubTitle(p.subTitle);
                        cards.add(card);
                    }
                    return cards.size() > 3 ? cards.subList(0, 3) : cards;
                }
            }
            if (result instanceof ProductToolResult p) {
                ProductCard card = new ProductCard();
                card.setId(p.id);
                card.setName(p.name);
                card.setPic(p.pic);
                card.setPrice(p.price);
                card.setSubTitle(p.subTitle);
                return List.of(card);
            }
        } catch (Exception ignored) {}
        return null;
    }

    public OrderCard extractOrderCard(String functionName, Object result) {
        if (!"queryOrder".equals(functionName)) return null;
        if (result instanceof OrderToolResult o) {
            OrderCard card = new OrderCard();
            card.setId(o.id);
            card.setOrderSn(o.orderSn);
            card.setStatusText(o.statusText);
            card.setPayAmount(o.payAmount);
            card.setCreateTime(o.createTime);
            return card;
        }
        return null;
    }

    // ── 辅助 ──

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

    // ── 请求 Record ──

    public static class SearchProductsRequest {
        @JsonPropertyDescription("搜索关键词")
        public String keyword;
        @JsonPropertyDescription("排序: 1新品 2销量 3价格升 4价格降")
        public Integer sort;
    }

    public static class GetProductDetailRequest {
        @JsonPropertyDescription("商品ID")
        public Long productId;
    }

    public static class ListMyOrdersRequest {
        @JsonPropertyDescription("订单状态: 0待付款 1待发货 2已发货 3已完成 4已关闭，不传查全部")
        public Integer status;
    }

    public static class QueryOrderRequest {
        @JsonPropertyDescription("订单ID")
        public Long orderId;
    }

    // ── 响应 POJO（供 LLM 消费） ──

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProductToolResult {
        public Long id;
        public String name;
        public String subTitle;
        public BigDecimal price;
        public String pic;
        public Integer sale;
        public Integer stock;
        public String description;
        public String brandName;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderToolResult {
        public Long id;
        public String orderSn;
        public Integer status;
        public String statusText;
        public BigDecimal payAmount;
        public BigDecimal totalAmount;
        public String createTime;
        public String paymentTime;
        @JsonProperty("receiver_name")
        public String receiverName;
        @JsonProperty("receiver_phone")
        public String receiverPhone;
    }
}
