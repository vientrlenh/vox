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

import com.sep.vox.infrastructure.persistence.entity.order.OrderJpaEntity;

import jakarta.persistence.LockModeType;

public interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {

    List<OrderJpaEntity> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);

    Page<OrderJpaEntity> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId, Pageable pageable);

    List<OrderJpaEntity> findByStatus(String status);

    /**
     * Order list for the System Admin: filter by school/status/type, search by school name.
     *
     * <p>Every filter can be skipped by passing null -- merged into one statement instead of several methods
     * because the on/off combinations of four filters make 16 variants.
     *
     * <p>Additionally ordered by id DESC after createdAt: two orders created in the same tick (a school clicking fast, or
     * import) ordered only by createdAt make Postgres return an arbitrary order, and pagination would repeat or skip
     * rows between pages. id is a uuidv7, so "id DESC" is "newest first".
     */
    @Query("""
        SELECT o FROM OrderJpaEntity o
        WHERE (:schoolId IS NULL OR o.schoolId = :schoolId)
          AND (:status IS NULL OR o.status = :status)
          AND (:type IS NULL OR o.type = :type)
          AND (:keywordPattern IS NULL OR EXISTS (
                SELECT 1 FROM SchoolJpaEntity sc
                WHERE sc.id = o.schoolId AND LOWER(sc.name) LIKE :keywordPattern
              ))
        ORDER BY o.createdAt DESC, o.id DESC
        """)
    Page<OrderJpaEntity> findForAdmin(
        @Param("schoolId") UUID schoolId,
        @Param("status") String status,
        @Param("type") String type,
        @Param("keywordPattern") String keywordPattern,
        Pageable pageable
    );

    boolean existsBySchoolIdAndTypeInAndStatus(UUID schoolId, Collection<String> types, String status);

    // Mirrors exactly the condition of uq_orders_one_open_subscription_order. It can return Optional because the unique
    // index guarantees at most one matching row.
    @Query("""
        SELECT o FROM OrderJpaEntity o
        WHERE o.schoolId = :schoolId
          AND o.status = 'PENDING'
          AND o.type IN ('SUBSCRIPTION_REQUEST', 'SUBSCRIPTION_UPGRADE')
        """)
    Optional<OrderJpaEntity> findOpenSubscriptionOrderBySchoolId(@Param("schoolId") UUID schoolId);

    // HALF-OPEN range [from, to) -- same reason as SpringDataSchoolBalanceEntryRepository.
    @Query("""
        SELECT COALESCE(SUM(o.totalAmountVnd), 0) FROM OrderJpaEntity o
        WHERE o.status = :status
          AND o.createdAt >= :from AND o.createdAt < :to
        """)
    BigDecimal sumTotalAmountByStatusInRange(
        @Param("status") String status,
        @Param("from") Instant from,
        @Param("to") Instant to);

    /**
     * Orders by status within a range, the same HALF-OPEN range [from, to) as
     * {@link #sumTotalAmountByStatusInRange} -- the two statements must use the same time boundary, otherwise
     * the total revenue and the monthly chart on the same screen would not add up to
     * the same number.
     *
     * <p>It exists separately instead of using {@code findByStatus}: the dashboard only draws the latest 12-24 months, while
     * findByStatus loads EVERY successful order ever -- a list that only grows and never
     * shrinks.
     */
    List<OrderJpaEntity> findByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        String status, Instant from, Instant to);

    /**
     * The SCHOOL-FILTERED version of the statement above, for a school's own admin screen.
     *
     * <p>It must be a separate statement rather than reusing the one above and filtering in Java: the one above has NO
     * school_id condition, so calling it from a school's screen would put the revenue of ALL schools into the spend total
     * and the 12-month chart of the logged-in school -- a leak of figures between schools, not just an extra load.
     */
    List<OrderJpaEntity> findBySchoolIdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        UUID schoolId, String status, Instant from, Instant to);

    // PESSIMISTIC_WRITE: blocks two parallel payment finalizations on the same order (the gateway webhook racing
    // with PendingOrderReconciler). @Version on the entity only detects the conflict AFTER both have
    // done redundant work; the lock here blocks it from the start.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OrderJpaEntity> findWithLockById(UUID id);
}
