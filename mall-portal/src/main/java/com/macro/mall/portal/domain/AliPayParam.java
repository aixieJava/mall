package com.macro.mall.portal.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @auther macrozheng
 * @description 支付宝支付请求参数
 * @date 2023/9/8
 * @github https://github.com/macrozheng
 */
@Data
@Schema(description = "支付宝支付请求参数")
public class AliPayParam {
    @Schema(description = "商户订单号，商家自定义，保持唯一性")
    private String outTradeNo;
    @Schema(description = "商品的标题/交易标题/订单标题/订单关键字等")
    private String subject;
    @Schema(description = "订单总金额，单位为元，精确到小数点后两位", example = "0.01")
    private BigDecimal totalAmount;
}
