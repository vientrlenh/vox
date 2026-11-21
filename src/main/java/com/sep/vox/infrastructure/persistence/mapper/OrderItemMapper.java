package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.order.OrderItem;
import com.sep.vox.domain.model.order.OrderItemType;
import com.sep.vox.infrastructure.persistence.entity.order.OrderItemJpaEntity;

public final class OrderItemMapper {

    private OrderItemMapper() {}

    public static OrderItem toDomain(OrderItemJpaEntity jpa) {
        return OrderItem.builder()
            .id(jpa.getId())
            .orderId(jpa.getOrderId())
            .type(fromString(jpa.getType()))
            .itemId(jpa.getItemId())
            .unitPriceVnd(jpa.getUnitPriceVnd())
            .amountVnd(jpa.getAmountVnd())
            .quantity(jpa.getQuantity())
            .build();
    }

    public static OrderItemJpaEntity toJpa(OrderItem domain) {
        return OrderItemJpaEntity.builder()
            .id(domain.getId())
            .orderId(domain.getOrderId())
            .type(valueOf(domain.getType()))
            .itemId(domain.getItemId())
            .unitPriceVnd(domain.getUnitPriceVnd())
            .amountVnd(domain.getAmountVnd())
            .quantity(domain.getQuantity())
            .build();
    }

    private static OrderItemType fromString(String type) {
        if (type == null)
            return null;
        try {
            return OrderItemType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại dòng hàng khi chuyển đổi sang domain model không hợp lệ: " + type);
        }
    }

    private static String valueOf(OrderItemType type) {
        return type == null ? null : type.name();
    }
}
