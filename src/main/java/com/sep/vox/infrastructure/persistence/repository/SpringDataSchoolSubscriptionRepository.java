package com.sep.vox.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SchoolSubscriptionJpaEntity;

public interface SpringDataSchoolSubscriptionRepository extends JpaRepository<SchoolSubscriptionJpaEntity, UUID> {
    Optional<SchoolSubscriptionJpaEntity> findFirstBySchoolIdAndStatus(UUID schoolId, String status);
    List<SchoolSubscriptionJpaEntity> findAllBySchoolId(UUID schoolId);
    boolean existsByPlanIdAndStatus(UUID planId, String status);

    // Bulk update bỏ qua persistence context nên KHÔNG tự tăng cột @Version — phải tăng tay
    // (s.version = s.version + 1) để bất kỳ bản ghi nào đang được load ở nơi khác (vd
    // CancelSubscriptionUseCase) mà save() sau khi job này chạy sẽ bị Hibernate phát hiện lệch
    // version và ném OptimisticLockException, thay vì âm thầm ghi đè EXPIRED ngược lại ACTIVE.
    @Modifying
    @Query("""
        UPDATE SchoolSubscriptionJpaEntity s
        SET s.status = 'EXPIRED', s.version = s.version + 1
        WHERE s.status = 'ACTIVE' AND s.endDate < :today
    """)
    int expireOverdue(@Param("today") LocalDate today);

    @Query("""
        SELECT s FROM SchoolSubscriptionJpaEntity s
        WHERE (:planId IS NULL OR s.planId = :planId)
          AND (:status IS NULL OR s.status = :status)
          AND (:keywordPattern IS NULL OR EXISTS (
                SELECT 1 FROM SchoolJpaEntity sc
                WHERE sc.id = s.schoolId AND LOWER(sc.name) LIKE :keywordPattern
              ))
    """)
    Page<SchoolSubscriptionJpaEntity> findAllForAdmin(
        @Param("planId") UUID planId,
        @Param("status") String status,
        @Param("keywordPattern") String keywordPattern,
        Pageable pageable
    );
}
