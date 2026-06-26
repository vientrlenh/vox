package com.sep.vox.infrastructure.persistence.repository;

import com.sep.vox.infrastructure.persistence.entity.SchoolGradeLevelJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataSchoolGradeLevelRepository extends JpaRepository<SchoolGradeLevelJpaEntity, UUID> {
    Optional<SchoolGradeLevelJpaEntity> findBySchoolId(UUID schoolId);
    boolean existsBySchoolIdAndCode(UUID schoolId, String code);
    boolean existsBySchoolIdAndOrder(UUID schoolId, int order);

    @Query("""
        SELECT sgl
        FROM SchoolGradeLevelJpaEntity sgl
        WHERE sgl.schoolId = :schoolId
            AND (:searchPattern IS NULL
                OR LOWER(sgl.code) LIKE :searchPattern
                OR LOWER(sgl.name) LIKE :searchPattern)
            AND (:status IS NULL OR sgl.status = :status)
        ORDER BY sgl.order ASC
        """)
    Page<SchoolGradeLevelJpaEntity> findBySchoolIdWithFilters(
        @Param("schoolId") UUID schoolId,
        @Param("searchPattern") String searchPattern,
        @Param("status") String status,
        Pageable pageable
    );
}

