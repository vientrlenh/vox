package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.OrderItemJpaEntity;

public interface SpringDataOrderItemRepository extends JpaRepository<OrderItemJpaEntity, UUID> {
    List<OrderItemJpaEntity> findByOrderId(UUID orderId);
    List<OrderItemJpaEntity> findByOrderIdIn(Collection<UUID> orderIds);
}
