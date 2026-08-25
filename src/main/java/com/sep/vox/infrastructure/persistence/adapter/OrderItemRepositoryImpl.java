package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.order.OrderItem;
import com.sep.vox.domain.repository.OrderItemRepository;
import com.sep.vox.infrastructure.persistence.mapper.OrderItemMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataOrderItemRepository;

@Repository
public class OrderItemRepositoryImpl implements OrderItemRepository {

    private final SpringDataOrderItemRepository springDataOrderItemRepository;

    public OrderItemRepositoryImpl(SpringDataOrderItemRepository springDataOrderItemRepository) {
        this.springDataOrderItemRepository = springDataOrderItemRepository;
    }

    @Override
    public Optional<OrderItem> findById(UUID id) {
        return springDataOrderItemRepository.findById(id).map(OrderItemMapper::toDomain);
    }

    @Override
    public OrderItem save(OrderItem item) {
        var entity = OrderItemMapper.toJpa(item);
        var saved = springDataOrderItemRepository.save(entity);
        return OrderItemMapper.toDomain(saved);
    }

    @Override
    public List<OrderItem> saveAll(Collection<OrderItem> items) {
        var entities = items.stream().map(OrderItemMapper::toJpa).toList();
        return springDataOrderItemRepository.saveAll(entities).stream()
            .map(OrderItemMapper::toDomain)
            .toList();
    }

    @Override
    public List<OrderItem> findByOrderId(UUID orderId) {
        return springDataOrderItemRepository.findByOrderId(orderId).stream()
            .map(OrderItemMapper::toDomain)
            .toList();
    }

    @Override
    public List<OrderItem> findByOrderIdIn(Collection<UUID> orderIds) {
        return springDataOrderItemRepository.findByOrderIdIn(orderIds).stream()
            .map(OrderItemMapper::toDomain)
            .toList();
    }
}
