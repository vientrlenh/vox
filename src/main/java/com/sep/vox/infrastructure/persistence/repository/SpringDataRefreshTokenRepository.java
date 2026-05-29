package com.sep.vox.infrastructure.persistence.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.RefreshTokenJpaEntity;

import jakarta.persistence.LockModeType;

public interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {
    List<RefreshTokenJpaEntity> findBySessionId(UUID sessionId);
    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefreshTokenJpaEntity r WHERE r.tokenHash = :tokenHash")
    Optional<RefreshTokenJpaEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("""
        UPDATE RefreshTokenJpaEntity r 
        SET r.usedAt = :now, 
            r.replacedBy = :newTokenId 
        WHERE r.id = :oldTokenId 
            AND r.usedAt IS NULL 
            AND r.expiredAt > :now
    """)
    int markUsedAndReplacedBy(@Param("oldTokenId") UUID oldTokenId, @Param("newTokenId") UUID newTokenId, @Param("now") OffsetDateTime now);
}
