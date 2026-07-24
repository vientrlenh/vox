package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SchoolSubscriptionJpaEntity;

public interface SpringDataSchoolSubscriptionRepository extends JpaRepository<SchoolSubscriptionJpaEntity, UUID> {
    Optional<SchoolSubscriptionJpaEntity> findFirstBySchoolIdAndStatus(UUID schoolId, String status);
    List<SchoolSubscriptionJpaEntity> findAllBySchoolId(UUID schoolId);

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
