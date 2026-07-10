package com.sep.vox.infrastructure.persistence.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SupportedLanguageJpaEntity;

public interface SpringDataSupportedLanguageRepository extends JpaRepository<SupportedLanguageJpaEntity, UUID>{
    Optional<SupportedLanguageJpaEntity> findByCode(String code);
    Optional<SupportedLanguageJpaEntity> findByName(String name);
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE SupportedLanguageJpaEntity language
        SET language.code = CASE WHEN :codeProvided = true THEN :code ELSE language.code END,
            language.name = CASE WHEN :nameProvided = true THEN :name ELSE language.name END,
            language.description = CASE WHEN :descriptionProvided = true THEN :description ELSE language.description END,
            language.isActive = CASE WHEN :isActiveProvided = true THEN :isActive ELSE language.isActive END,
            language.updatedAt = :updatedAt,
            language.updatedBy = :updatedBy
        WHERE language.id = :id
        """)
    int updateMutableFields(
        @Param("id") UUID id,
        @Param("code") String code,
        @Param("codeProvided") boolean codeProvided,
        @Param("name") String name,
        @Param("nameProvided") boolean nameProvided,
        @Param("description") String description,
        @Param("descriptionProvided") boolean descriptionProvided,
        @Param("isActive") Boolean isActive,
        @Param("isActiveProvided") boolean isActiveProvided,
        @Param("updatedAt") OffsetDateTime updatedAt,
        @Param("updatedBy") UUID updatedBy
    );
    List<SupportedLanguageJpaEntity> findByIdIn(Collection<UUID> ids);
}
