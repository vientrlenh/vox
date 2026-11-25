package com.sep.vox.domain.model.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderItemTests {

    @Test
    void forSubscriptionPlanBuysOnePlanAtItsPrice() {
        UUID orderId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        BigDecimal price = new BigDecimal("499000");

        OrderItem item = OrderItem.forSubscriptionPlan(orderId, planId, price);

        assertThat(item.getId()).isNull();
        assertThat(item.getOrderId()).isEqualTo(orderId);
        assertThat(item.getType()).isEqualTo(OrderItemType.SUBSCRIPTION);
        assertThat(item.getItemId()).isEqualTo(planId);
        assertThat(item.getUnitPriceVnd()).isEqualTo(price);
        assertThat(item.getAmountVnd()).isEqualTo(price);
        assertThat(item.getQuantity()).isEqualTo(1);
    }
}
