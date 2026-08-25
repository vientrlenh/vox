package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.OrderJpaEntity;

import jakarta.persistence.LockModeType;

public interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {

    List<OrderJpaEntity> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);

    Page<OrderJpaEntity> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId, Pageable pageable);

    List<OrderJpaEntity> findByStatus(String status);

    boolean existsBySchoolIdAndTypeInAndStatus(UUID schoolId, Collection<String> types, String status);

    // Soi đúng điều kiện của uq_orders_one_open_subscription_order. Trả Optional được vì unique
    // index đảm bảo tối đa một dòng khớp.
    @Query("""
        SELECT o FROM OrderJpaEntity o
        WHERE o.schoolId = :schoolId
          AND o.status = 'PENDING'
          AND o.type IN ('SUBSCRIPTION_REQUEST', 'SUBSCRIPTION_UPGRADE')
        """)
    Optional<OrderJpaEntity> findOpenSubscriptionOrderBySchoolId(@Param("schoolId") UUID schoolId);

    // Khoảng NỬA MỞ [from, to) -- cùng lý do với SpringDataSchoolBalanceEntryRepository.
    @Query("""
        SELECT COALESCE(SUM(o.totalAmountVnd), 0) FROM OrderJpaEntity o
        WHERE o.status = :status
          AND o.createdAt >= :from AND o.createdAt < :to
        """)
    BigDecimal sumTotalAmountByStatusInRange(
        @Param("status") String status,
        @Param("from") Instant from,
        @Param("to") Instant to);

    // PESSIMISTIC_WRITE: chặn hai lần chốt thanh toán song song trên cùng một đơn (webhook cổng đua
    // với PendingInvoiceReconciler). @Version trên entity chỉ phát hiện xung đột SAU khi cả hai đã
    // làm việc thừa; khóa ở đây chặn ngay từ đầu.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OrderJpaEntity> findWithLockById(UUID id);
}
