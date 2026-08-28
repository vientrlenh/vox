package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.order.OrderItem;
import com.sep.vox.domain.model.order.OrderItemType;

public record OrderItemDto(
    UUID id, 
    UUID orderId, 
    String type, 
    UUID itemId, 
    BigDecimal unitPriceVnd, 
    BigDecimal amountVnd, 
    Integer quantity
) {
    
    public static OrderItemDto toDto(OrderItem item) {
        return new OrderItemDto(
            item.getId(), 
            item.getOrderId(), 
            valueOf(item.getType()), 
            item.getItemId(), 
            item.getUnitPriceVnd(), 
            item.getAmountVnd(), 
            item.getQuantity()
        );
    }

    private static String valueOf(OrderItemType type) {
        return type == null ? null : type.name();
    }
}
