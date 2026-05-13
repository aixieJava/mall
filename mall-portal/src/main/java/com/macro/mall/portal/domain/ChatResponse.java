package com.macro.mall.portal.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 客服聊天响应
 */
@Data
@Schema(description = "AI客服聊天响应")
public class ChatResponse {

    @Schema(description = "AI回复文本")
    private String reply;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "推荐商品列表")
    private List<ProductCard> products;

    @Schema(description = "订单信息")
    private OrderCard order;

    @Data
    @Schema(description = "商品卡片")
    public static class ProductCard {
        @Schema(description = "商品ID")
        private Long id;
        @Schema(description = "商品名称")
        private String name;
        @Schema(description = "商品图片")
        private String pic;
        @Schema(description = "价格")
        private BigDecimal price;
        @Schema(description = "副标题")
        private String subTitle;
    }

    @Data
    @Schema(description = "订单卡片")
    public static class OrderCard {
        @Schema(description = "订单ID")
        private Long id;
        @Schema(description = "订单号")
        private String orderSn;
        @Schema(description = "订单状态文本")
        private String statusText;
        @Schema(description = "实付金额")
        private BigDecimal payAmount;
        @Schema(description = "创建时间")
        private String createTime;
    }
}
