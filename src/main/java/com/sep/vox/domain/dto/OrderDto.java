package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.order.OrderType;

public record OrderDto(
    UUID id, 
    UUID schoolId, 
    String type, 
    String description, 
    BigDecimal subtotalAmountVnd, 
    BigDecimal totalAmountVnd, 
    BigDecimal chargedFeeVnd, 
    BigDecimal discountAmountVnd, 
    String status, 
    String notes, 
    String createdAt, 
    String updatedAt, 
    String expiresAt
) {

    public static OrderDto toDto(Order order) {
        return new OrderDto(
            order.getId(), 
            order.getSchoolId(), 
            valueOf(order.getType()), 
            order.getDescription(), 
            order.getSubtotalAmountVnd(), 
            order.getTotalAmountVnd(), 
            order.getChargedFeeVnd(), 
            order.getDiscountAmountVnd(), 
            valueOf(order.getStatus()), 
            order.getNotes(), 
            valueOf(order.getCreatedAt()), 
            valueOf(order.getUpdatedAt()), 
            valueOf(order.getExpiresAt())
        );
    }

    private static String valueOf(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private static String valueOf(OrderType type) {
        return type == null ? null : type.name();
    }

    private static String valueOf(OrderStatus status) {
        return status == null ? null : status.name();
    }
}
