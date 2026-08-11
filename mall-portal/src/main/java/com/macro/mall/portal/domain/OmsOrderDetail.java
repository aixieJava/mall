package com.macro.mall.portal.domain;

import com.macro.mall.model.OmsOrder;
import com.macro.mall.model.OmsOrderItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 包含订单商品信息的订单详情
 * Created by macro on 2018/9/4.
 */
@Schema(description = "订单详情（含商品信息）")
public class OmsOrderDetail extends OmsOrder {
    @Schema(description = "订单商品列表")
    private List<OmsOrderItem> orderItemList;

    public List<OmsOrderItem> getOrderItemList() {
        return orderItemList;
    }

    public void setOrderItemList(List<OmsOrderItem> orderItemList) {
        this.orderItemList = orderItemList;
    }
}
