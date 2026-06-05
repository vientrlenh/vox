package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SupportedLanguageJpaEntity;

public interface SpringDataSupportedLanguageRepository extends JpaRepository<SupportedLanguageJpaEntity, UUID>{
    Optional<SupportedLanguageJpaEntity> findByCode(String code);
    boolean existsByIdAndIsActive(UUID id, boolean isActive);
}
