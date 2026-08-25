package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.order.OrderItem;

public interface OrderItemRepository {
    Optional<OrderItem> findById(UUID id);
    OrderItem save(OrderItem item);
    List<OrderItem> saveAll(Collection<OrderItem> items);
    List<OrderItem> findByOrderId(UUID orderId);

    /** Nạp dòng hàng cho nhiều đơn một lượt -- tránh N+1 ở màn lịch sử đơn / dashboard. */
    List<OrderItem> findByOrderIdIn(Collection<UUID> orderIds);
}
