package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SupportedLanguageJpaEntity;

public interface SpringDataSupportedLanguageRepository extends JpaRepository<SupportedLanguageJpaEntity, UUID>{
    Optional<SupportedLanguageJpaEntity> findByCode(String code);
    List<SupportedLanguageJpaEntity> findByCodeIn(Collection<String> codes);
    boolean existsByIdAndIsActive(UUID id, boolean isActive);

    @Query("""
        SELECT language
        FROM SupportedLanguageJpaEntity language
        WHERE (:isActive IS NULL OR language.isActive = :isActive)
            AND (:searchPattern IS NULL
                OR LOWER(language.code) LIKE :searchPattern
                OR LOWER(language.name) LIKE :searchPattern)
        """)
    Page<SupportedLanguageJpaEntity> findAllWithSearchAndFilters(
        @Param("searchPattern") String searchPattern,
        @Param("isActive") Boolean isActive,
        Pageable pageable
    );
}
