package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sep.vox.infrastructure.persistence.entity.ImportSessionJpaEntity;

public interface SpringDataImportSessionRepository extends JpaRepository<ImportSessionJpaEntity, UUID> {
    @Query("""
        select s
        from ImportSessionJpaEntity s
        where s.schoolId = :schoolId
          and (:type is null or s.type = :type)
          and (:status is null or s.status = :status)
        """)
    Page<ImportSessionJpaEntity> findBySchoolIdWithFilters(UUID schoolId, String type, String status, Pageable pageable);
}
