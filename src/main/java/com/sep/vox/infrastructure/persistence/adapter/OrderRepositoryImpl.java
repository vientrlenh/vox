package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.OrderJpaEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.common.StringNormalization;
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
        OrderJpaEntity entity = OrderMapper.toJpa(order);
        OrderJpaEntity saved = springDataOrderRepository.save(entity);
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
        Page<OrderJpaEntity> result = springDataOrderRepository
            // The page arrives 1-BASED (first page = 1) like every other repository in the project, while
            // PageRequest counts from 0 -- without this subtraction the first page can never be fetched.
            .findBySchoolIdOrderByCreatedAtDesc(schoolId, PageRequest.of(page - 1, size));
        return new PageResult<>(
            result.getContent().stream().map(OrderMapper::toDomain).toList(),
            page,
            size,
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Override
    public PageResult<Order> findForAdmin(
            UUID schoolId, OrderStatus status, OrderType type, String keyword, int page, int size) {
        Page<OrderJpaEntity> result = springDataOrderRepository.findForAdmin(
            schoolId,
            status == null ? null : status.name(),
            type == null ? null : type.name(),
            StringNormalization.toLikePattern(keyword),
            // 1-based in, 0-based down to PageRequest -- see findBySchoolId.
            PageRequest.of(page - 1, size)
        );
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
            types.stream().map(type -> type.name()).toList(),
            status.name()
        );
    }

    @Override
    public BigDecimal sumTotalAmountByStatusInRange(OrderStatus status, Instant from, Instant to) {
        return springDataOrderRepository.sumTotalAmountByStatusInRange(status.name(), from, to);
    }

    @Override
    public List<Order> findByStatusInRange(OrderStatus status, Instant from, Instant to) {
        return springDataOrderRepository
            .findByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(status.name(), from, to).stream()
            .map(OrderMapper::toDomain)
            .toList();
    }

    @Override
    public List<Order> findBySchoolIdAndStatusInRange(
            UUID schoolId, OrderStatus status, Instant from, Instant to) {
        return springDataOrderRepository
            .findBySchoolIdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                schoolId, status.name(), from, to).stream()
            .map(OrderMapper::toDomain)
            .toList();
    }
}
