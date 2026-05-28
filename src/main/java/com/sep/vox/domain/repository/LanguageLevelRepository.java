package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.languagelevel.LanguageLevel;

public interface LanguageLevelRepository {
    Optional<LanguageLevel> findById(UUID id);
    LanguageLevel save(LanguageLevel languageLevel);
}
