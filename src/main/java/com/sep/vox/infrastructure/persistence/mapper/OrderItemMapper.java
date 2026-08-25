package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.order.OrderItem;
import com.sep.vox.domain.model.order.OrderItemType;
import com.sep.vox.infrastructure.persistence.entity.OrderItemJpaEntity;

public final class OrderItemMapper {

    private OrderItemMapper() {}

    public static OrderItem toDomain(OrderItemJpaEntity jpa) {
        return new OrderItem(
            jpa.getId(),
            jpa.getOrderId(),
            fromString(jpa.getType()),
            jpa.getItemId(),
            jpa.getUnitPriceVnd(),
            jpa.getAmountVnd(),
            jpa.getQuantity()
        );
    }

    public static OrderItemJpaEntity toJpa(OrderItem domain) {
        return new OrderItemJpaEntity(
            domain.getId(),
            domain.getOrderId(),
            valueOf(domain.getType()),
            domain.getItemId(),
            domain.getUnitPriceVnd(),
            domain.getAmountVnd(),
            domain.getQuantity()
        );
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
