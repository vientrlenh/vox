package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.OutboxJpaEntity;

public interface SpringDataOutboxRepository extends JpaRepository<OutboxJpaEntity, UUID> {

    @Query(value = """
        SELECT * FROM outboxes 
        WHERE status = 'PENDING'
            AND (next_retry_at IS NULL OR next_retry_at <= :now)
        ORDER BY id ASC 
        LIMIT :size 
        FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<OutboxJpaEntity> lockPendingEvents(
        @Param("now") Instant now, 
        @Param("size") int size
    );


    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE OutboxJpaEntity o 
        SET o.status = 'PROCESSING', o.nextRetryAt = :leaseExpiresAt 
        WHERE o.id IN :ids  
    """)
    int markProcessing(
        @Param("ids") Collection<UUID> ids, 
        @Param("leaseExpiresAt") Instant leaseExpiresAt
    );


    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE OutboxJpaEntity o 
        SET o.status = 'PUBLISHED', o.publishedAt = :publishedAt 
        WHERE o.id = :id  
    """)
    int markPublished(
        @Param("id") UUID id, 
        @Param("publishedAt") Instant publishedAt
    );


    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE OutboxJpaEntity o 
        SET o.status = :status, 
            o.retryCount = o.retryCount + 1, 
            o.lastError = :lastError, 
            o.nextRetryAt = :nextRetryAt 
        WHERE o.id = :id 
    """)
    int markFailed(
        @Param("id") UUID id, 
        @Param("status") String status, 
        @Param("lastError") String lastError, 
        @Param("nextRetryAt") Instant nextRetryAt
    );


    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE OutboxJpaEntity o 
        SET o.status = 'PENDING' 
        WHERE o.status = 'PROCESSING' 
            AND o.nextRetryAt IS NOT NULL 
            AND o.nextRetryAt <= :now 
    """)
    int releaseExpiredLeases(
        @Param("now") Instant now
    );
}
