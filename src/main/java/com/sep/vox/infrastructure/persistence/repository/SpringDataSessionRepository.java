package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SessionJpaEntity;

public interface SpringDataSessionRepository extends JpaRepository<SessionJpaEntity, UUID>{
    Optional<SessionJpaEntity> findByUserIdAndRefreshTokenHash(UUID userId, String refreshTokenHash);
}
