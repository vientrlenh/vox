package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    
    @Modifying
    @Query("""
        UPDATE ImportSessionJpaEntity i 
            SET i.status = 'IMPORTING', 
                i.confirmedMappingJson = :confirmedMapping, 
                i.updatedAt = :now, 
                i.updatedBy = :updatedBy 
        WHERE i.id = :id 
            AND i.type = :type 
            AND i.status = 'PREVIEWED' 
            AND i.expiresAt > :now     
    """)
    int markImporting(@Param("id") UUID id,
                    @Param("type") String type,
                    @Param("confirmedMapping") String confirmedMapping,
                    @Param("now") Instant now,
                    @Param("updatedBy") UUID updatedBy);

    @Modifying
    @Query("""
        UPDATE ImportSessionJpaEntity i
            SET i.status = 'QUEUED',
                i.confirmedMappingJson = :confirmedMapping,
                i.updatedAt = :now,
                i.updatedBy = :updatedBy
        WHERE i.id = :id
            AND i.type = :type
            AND i.status = 'PREVIEWED'
            AND i.expiresAt > :now
    """)
    int markQueued(@Param("id") UUID id,
                    @Param("type") String type,
                    @Param("confirmedMapping") String confirmedMapping,
                    @Param("now") Instant now,
                    @Param("updatedBy") UUID updatedBy);


    @Query(value = """
        SELECT i.id FROM import_sessions i 
        WHERE i.status = 'QUEUED' 
        ORDER BY i.created_at 
        FOR UPDATE SKIP LOCKED 
        LIMIT :limit
    """, nativeQuery = true)
    List<UUID> lockQueueIds(@Param("limit") int limit);

    @Modifying
    @Query("""
        UPDATE ImportSessionJpaEntity i 
            SET i.status = 'IMPORTING', 
                i.claimedBy = :worker, 
                i.claimedAt = :now, 
                i.leaseExpiresAt = :leaseUntil, 
                i.updatedAt = :now, 
                i.updatedBy = :worker
        WHERE i.id IN :ids
    """)
    int markClaimed(@Param("ids") Collection<UUID> ids, 
                    @Param("worker") UUID worker, 
                    @Param("now") Instant now, 
                    @Param("leaseUntil") Instant leaseUntil);

    
    @Modifying
    @Query("""
        UPDATE ImportSessionJpaEntity i 
            SET i.leaseExpiresAt = :leaseUntil 
        WHERE i.id = :id
    """)
    void extendLease(@Param("id") UUID id, @Param("leaseUntil") Instant leaseUntil); 

    @Modifying
    @Query(value = """
        UPDATE import_sessions
            SET status = CASE WHEN attempts < :maxAttempts THEN 'QUEUED' ELSE 'FAILED' END,
                attempts = attempts + 1,
                claimed_by = NULL,
                claimed_at = NULL,
                lease_expires_at = NULL,
                failure_reason = CASE WHEN attempts >= :maxAttempts
                    THEN 'Vượt số lần thử lại' ELSE failure_reason END,
                updated_at = :now
        WHERE status = 'IMPORTING' AND lease_expires_at < :now
    """, nativeQuery = true)
    int requeueExpiredLeases(@Param("now") Instant now, @Param("maxAttempts") int maxAttempts);
}
