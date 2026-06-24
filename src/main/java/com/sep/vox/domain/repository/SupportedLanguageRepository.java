package com.sep.vox.domain.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;

public interface SupportedLanguageRepository {
    Optional<SupportedLanguage> findById(UUID id);
    Optional<SupportedLanguage> findByCode(String code);
    List<SupportedLanguage> findByCodeIn(Collection<String> codes);
    PageResult<SupportedLanguage> findAll(String search, Boolean isActive, int pageNumber, int size);
    SupportedLanguage save(SupportedLanguage supportedLanguage);
    int updateMutableFields(UUID id, String code, boolean codeProvided, String name, boolean nameProvided,
            String description, boolean descriptionProvided, Boolean isActive, boolean isActiveProvided,
            OffsetDateTime updatedAt, UUID updatedBy);
    long count();
    boolean existsById(UUID id);
    boolean existsByIdAndIsActive(UUID id, boolean isActive);
    List<SupportedLanguage> findByIdIn(Collection<UUID> ids);
}
