package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;

public interface SupportedLanguageRepository {
    Optional<SupportedLanguage> findById(UUID id);
    Optional<SupportedLanguage> findByCode(String code);
    List<SupportedLanguage> findByCodeIn(Collection<String> codes);
    PageResult<SupportedLanguage> findAll(String search, Boolean isActive, PageRequest pageRequest);
    SupportedLanguage save(SupportedLanguage supportedLanguage);
    long count();
    boolean existsById(UUID id);
    boolean existsByIdAndIsActive(UUID id, boolean isActive);
}
