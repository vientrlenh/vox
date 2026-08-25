package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.order.OrderType;
import com.sep.vox.infrastructure.persistence.entity.OrderJpaEntity;

public final class OrderMapper {

    private OrderMapper() {}

    public static Order toDomain(OrderJpaEntity jpa) {
        return new Order(
            jpa.getId(),
            jpa.getSchoolId(),
            typeFromString(jpa.getType()),
            jpa.getDescription(),
            jpa.getTotalAmountVnd(),
            jpa.getChargedFeeVnd(),
            jpa.getDiscountAmountVnd(),
            statusFromString(jpa.getStatus()),
            jpa.getNotes(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy(),
            jpa.getVersion()
        );
    }

    public static OrderJpaEntity toJpa(Order domain) {
        return new OrderJpaEntity(
            domain.getId(),
            domain.getSchoolId(),
            valueOf(domain.getType()),
            domain.getDescription(),
            domain.getTotalAmountVnd(),
            domain.getChargedFeeVnd(),
            domain.getDiscountAmountVnd(),
            valueOf(domain.getStatus()),
            domain.getNotes(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getCreatedBy(),
            domain.getUpdatedBy(),
            domain.getVersion()
        );
    }

    private static OrderType typeFromString(String type) {
        if (type == null)
            return null;
        try {
            return OrderType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại đơn hàng khi chuyển đổi sang domain model không hợp lệ: " + type);
        }
    }

    private static OrderStatus statusFromString(String status) {
        if (status == null)
            return null;
        try {
            return OrderStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái đơn hàng khi chuyển đổi sang domain model không hợp lệ: " + status);
        }
    }

    private static String valueOf(OrderType type) {
        return type == null ? null : type.name();
    }

    private static String valueOf(OrderStatus status) {
        return status == null ? null : status.name();
    }
}
