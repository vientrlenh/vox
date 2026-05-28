package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.RefreshTokenJpaEntity;

public interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {
    List<RefreshTokenJpaEntity> findBySessionId(UUID sessionId);
    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);
}
