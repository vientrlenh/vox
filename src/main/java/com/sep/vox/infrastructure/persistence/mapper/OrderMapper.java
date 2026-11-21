package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.order.OrderType;
import com.sep.vox.infrastructure.persistence.entity.order.OrderJpaEntity;

public final class OrderMapper {

    private OrderMapper() {}

    public static Order toDomain(OrderJpaEntity jpa) {
        return Order.builder()
            .id(jpa.getId())
            .schoolId(jpa.getSchoolId())
            .type(typeFromString(jpa.getType()))
            .description(jpa.getDescription())
            .subtotalAmountVnd(jpa.getSubtotalAmountVnd())
            .totalAmountVnd(jpa.getTotalAmountVnd())
            .chargedFeeVnd(jpa.getChargedFeeVnd())
            .discountAmountVnd(jpa.getDiscountAmountVnd())
            .status(statusFromString(jpa.getStatus()))
            .notes(jpa.getNotes())
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .expiresAt(jpa.getExpiresAt())
            .createdBy(jpa.getCreatedBy())
            .updatedBy(jpa.getUpdatedBy())
            .version(jpa.getVersion())
            .build();
    }

    public static OrderJpaEntity toJpa(Order domain) {
        return OrderJpaEntity.builder()
            .id(domain.getId())
            .schoolId(domain.getSchoolId())
            .type(valueOf(domain.getType()))
            .description(domain.getDescription())
            .subtotalAmountVnd(domain.getSubtotalAmountVnd())
            .totalAmountVnd(domain.getTotalAmountVnd())
            .chargedFeeVnd(domain.getChargedFeeVnd())
            .discountAmountVnd(domain.getDiscountAmountVnd())
            .status(valueOf(domain.getStatus()))
            .notes(domain.getNotes())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .expiresAt(domain.getExpiresAt())
            .createdBy(domain.getCreatedBy())
            .updatedBy(domain.getUpdatedBy())
            .version(domain.getVersion())
            .build();
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
