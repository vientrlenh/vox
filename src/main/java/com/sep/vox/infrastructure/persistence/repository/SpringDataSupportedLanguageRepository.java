package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SupportedLanguageJpaEntity;

public interface SpringDataSupportedLanguageRepository extends JpaRepository<SupportedLanguageJpaEntity, UUID>{
    Optional<SupportedLanguageJpaEntity> findByCode(String code);
    List<SupportedLanguageJpaEntity> findByCodeIn(Collection<String> codes);
    boolean existsByIdAndIsActive(UUID id, boolean isActive);
    List<SupportedLanguageJpaEntity> findByIdIn(Collection<UUID> ids);
}
