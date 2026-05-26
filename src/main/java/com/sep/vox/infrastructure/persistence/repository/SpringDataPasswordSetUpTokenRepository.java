package com.sep.vox.infrastructure.persistence.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.PasswordSetUpTokenJpaEntity;

public interface SpringDataPasswordSetUpTokenRepository extends JpaRepository<PasswordSetUpTokenJpaEntity, UUID> {
    Optional<PasswordSetUpTokenJpaEntity> findByUserIdAndTokenHash(UUID userId, String tokenHash);
    
    @Modifying
    @Query("""
        UPDATE PasswordSetUpTokenJpaEntity p 
        SET p.usedAt = :now 
        WHERE p.tokenHash = :tokenHash 
        AND p.userId = :userId 
        AND p.usedAt IS NULL 
        AND p.expiredAt > :now
    """)
    int updateUsedToken(@Param("userId") UUID userId, @Param("tokenHash") String tokenHash, @Param("now") OffsetDateTime now);
}
