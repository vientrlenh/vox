package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sep.vox.infrastructure.persistence.entity.ImportRowJpaEntity;

public interface SpringDataImportRowRepository extends JpaRepository<ImportRowJpaEntity, UUID> {
    List<ImportRowJpaEntity> findBySessionIdOrderByRowNumber(UUID sessionId);

    @Query("""
        SELECT r
        FROM ImportRowJpaEntity r
        WHERE r.sessionId = :sessionId
          AND (:status IS NULL OR r.status = :status)
        """)
    Page<ImportRowJpaEntity> findBySessionIdWithFilters(UUID sessionId, String status, Pageable pageable);
}
