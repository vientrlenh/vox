package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sep.vox.infrastructure.persistence.entity.ImportSessionJpaEntity;

public interface SpringDataImportSessionRepository extends JpaRepository<ImportSessionJpaEntity, UUID> {
    @Query("""
        SELECT s
        FROM ImportSessionJpaEntity s
        WHERE s.schoolId = :schoolId
          AND (:type IS NULL OR s.type = :type)
          AND (:status IS NULL OR s.status = :status)
        """)
    Page<ImportSessionJpaEntity> findBySchoolIdWithFilters(UUID schoolId, String type, String status, Pageable pageable);
}
