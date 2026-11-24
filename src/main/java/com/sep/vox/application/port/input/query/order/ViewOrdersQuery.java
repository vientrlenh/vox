package com.sep.vox.application.port.input.query.order;

import java.util.UUID;

import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.order.OrderType;

/**
 * The order-list filter for the System Admin. Every filter field is nullable = "do not filter by this criterion".
 *
 * <p>status and type arrive already as enums: the GraphQL schema rejects an unknown value before the
 * use case runs.
 */
public record ViewOrdersQuery(
    UUID schoolId,
    OrderStatus status,
    OrderType type,
    String keyword,
    int page,
    int size
) {
}
