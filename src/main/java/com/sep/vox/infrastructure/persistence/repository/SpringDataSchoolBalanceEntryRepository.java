package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.school.SchoolBalanceEntryJpaEntity;

public interface SpringDataSchoolBalanceEntryRepository extends JpaRepository<SchoolBalanceEntryJpaEntity, UUID> {

    // The two versions are nearly identical, differing in exactly one entryType line. Intentionally NOT merged into one statement with
    // ":entryType IS NULL OR ..." -- a null parameter in JPQL must be wrapped in CAST for Postgres to infer the type,
    // and forgetting the CAST only fails at runtime, exactly in the "no filter" branch which is the most commonly used.
    //
    // The secondary sort key by id is MANDATORY, not cosmetic: one assessment generates many entries within
    // the same Instant, and ORDER BY occurred_at alone leaves the order among them undefined -- with pagination, the
    // result is one row appearing twice on two pages while another row disappears. The id is a uuidv7 so it grows with time:
    // both stable and not breaking the chronological order.
    @Query("""
        SELECT e FROM SchoolBalanceEntryJpaEntity e
        WHERE e.schoolId = :schoolId
          AND e.occurredAt >= :from AND e.occurredAt < :to
        ORDER BY e.occurredAt DESC, e.id DESC
        """)
    Page<SchoolBalanceEntryJpaEntity> findPageBySchoolIdInRange(
        @Param("schoolId") UUID schoolId,
        @Param("from") Instant from,
        @Param("to") Instant to,
        Pageable pageable);

    @Query("""
        SELECT e FROM SchoolBalanceEntryJpaEntity e
        WHERE e.schoolId = :schoolId
          AND e.entryType = :entryType
          AND e.occurredAt >= :from AND e.occurredAt < :to
        ORDER BY e.occurredAt DESC, e.id DESC
        """)
    Page<SchoolBalanceEntryJpaEntity> findPageBySchoolIdAndEntryTypeInRange(
        @Param("schoolId") UUID schoolId,
        @Param("entryType") String entryType,
        @Param("from") Instant from,
        @Param("to") Instant to,
        Pageable pageable);

    // HALF-OPEN range [from, to) and not BETWEEN: BETWEEN is inclusive at both ends, so two consecutive
    // periods (January -> 31/01, February -> 01/02) would still double-count entries that fall exactly on the boundary.
    @Query("""
        SELECT e FROM SchoolBalanceEntryJpaEntity e
        WHERE e.schoolId = :schoolId
          AND e.occurredAt >= :from AND e.occurredAt < :to
        ORDER BY e.occurredAt DESC
        """)
    List<SchoolBalanceEntryJpaEntity> findBySchoolIdInRange(
        @Param("schoolId") UUID schoolId,
        @Param("from") Instant from,
        @Param("to") Instant to);

    boolean existsByOrderIdAndEntryType(UUID orderId, String entryType);

    // COALESCE to 0: SUM over an empty set returns NULL, and if it leaked out every caller would have to guard against null itself.
    @Query("""
        SELECT COALESCE(SUM(e.amountVnd), 0) FROM SchoolBalanceEntryJpaEntity e
        WHERE e.schoolId = :schoolId
          AND e.entryType = :entryType
          AND e.occurredAt >= :from AND e.occurredAt < :to
        """)
    BigDecimal sumAmountBySchoolIdAndEntryTypeInRange(
        @Param("schoolId") UUID schoolId,
        @Param("entryType") String entryType,
        @Param("from") Instant from,
        @Param("to") Instant to);
}
