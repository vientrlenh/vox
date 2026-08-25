package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.order.OrderType;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.infrastructure.persistence.mapper.OrderMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataOrderRepository;

@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private final SpringDataOrderRepository springDataOrderRepository;

    public OrderRepositoryImpl(SpringDataOrderRepository springDataOrderRepository) {
        this.springDataOrderRepository = springDataOrderRepository;
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return springDataOrderRepository.findById(id).map(OrderMapper::toDomain);
    }

    @Override
    public Optional<Order> findByIdForUpdate(UUID id) {
        return springDataOrderRepository.findWithLockById(id).map(OrderMapper::toDomain);
    }

    @Override
    public Order save(Order order) {
        var entity = OrderMapper.toJpa(order);
        var saved = springDataOrderRepository.save(entity);
        return OrderMapper.toDomain(saved);
    }

    @Override
    public List<Order> findBySchoolId(UUID schoolId) {
        return springDataOrderRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId).stream()
            .map(OrderMapper::toDomain)
            .toList();
    }

    @Override
    public PageResult<Order> findBySchoolId(UUID schoolId, int page, int size) {
        var result = springDataOrderRepository
            .findBySchoolIdOrderByCreatedAtDesc(schoolId, PageRequest.of(page, size));
        return new PageResult<>(
            result.getContent().stream().map(OrderMapper::toDomain).toList(),
            page,
            size,
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return springDataOrderRepository.findByStatus(status.name()).stream()
            .map(OrderMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<Order> findOpenSubscriptionOrderBySchoolId(UUID schoolId) {
        return springDataOrderRepository.findOpenSubscriptionOrderBySchoolId(schoolId).map(OrderMapper::toDomain);
    }

    @Override
    public boolean existsBySchoolIdAndTypeInAndStatus(UUID schoolId, List<OrderType> types, OrderStatus status) {
        return springDataOrderRepository.existsBySchoolIdAndTypeInAndStatus(
            schoolId,
            types.stream().map(OrderType::name).toList(),
            status.name()
        );
    }

    @Override
    public BigDecimal sumTotalAmountByStatusInRange(OrderStatus status, Instant from, Instant to) {
        return springDataOrderRepository.sumTotalAmountByStatusInRange(status.name(), from, to);
    }
}
