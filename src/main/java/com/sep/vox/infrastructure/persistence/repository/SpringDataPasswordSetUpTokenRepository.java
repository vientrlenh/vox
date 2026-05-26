package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.PasswordSetUpTokenJpaEntity;

public interface SpringDataPasswordSetUpTokenRepository extends JpaRepository<PasswordSetUpTokenJpaEntity, UUID> {
    Optional<PasswordSetUpTokenJpaEntity> findByUserIdAndTokenHash(UUID userId, String tokenHash);
}
