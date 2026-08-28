package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.sep.vox.infrastructure.persistence.entity.PracticePaperJpaEntity;

public interface SpringDataPracticePaperRepository
        extends JpaRepository<PracticePaperJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PracticePaperJpaEntity> findByIdAndStudentIdAndStatusAndReservationExpiresAtAfter(
        UUID id,
        UUID studentId,
        String status,
        Instant now
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM (
            SELECT origin
            FROM practice_papers
            WHERE student_id = :studentId
            ORDER BY created_at DESC
            LIMIT 10
        ) recent
        WHERE origin = 'EPSILON'
        """, nativeQuery = true)
    int countRecentEpsilonPapers(@Param("studentId") UUID studentId);

    @Query(value = """
        SELECT COALESCE(SUM(reserved_quota_seconds), 0)
        FROM practice_papers
        WHERE student_id = :studentId
          AND status = 'RESERVED'
          AND reservation_expires_at > CURRENT_TIMESTAMP
        """, nativeQuery = true)
    int sumReservedQuotaSeconds(@Param("studentId") UUID studentId);
}
