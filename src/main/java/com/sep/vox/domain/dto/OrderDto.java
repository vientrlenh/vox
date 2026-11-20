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
    OrderType type, 
    String description, 
    BigDecimal subtotalAmountVnd, 
    BigDecimal totalAmountVnd, 
    BigDecimal chargedFeeVnd, 
    BigDecimal discountAmountVnd, 
    OrderStatus status, 
    String notes, 
    Instant createdAt, 
    Instant updatedAt, 
    Instant expiresAt
) {

    public static OrderDto toDto(Order order) {
        return new OrderDto(
            order.getId(), 
            order.getSchoolId(), 
            order.getType(), 
            order.getDescription(), 
            order.getSubtotalAmountVnd(), 
            order.getTotalAmountVnd(), 
            order.getChargedFeeVnd(), 
            order.getDiscountAmountVnd(), 
            order.getStatus(), 
            order.getNotes(), 
            order.getCreatedAt(), 
            order.getUpdatedAt(), 
            order.getExpiresAt()
        );
    }
}
