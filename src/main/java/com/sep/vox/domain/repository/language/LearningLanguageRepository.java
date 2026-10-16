package com.sep.vox.domain.repository.language;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.language.LearningLanguage;

public interface LearningLanguageRepository {
    Optional<LearningLanguage> findById(UUID id);
    LearningLanguage save(LearningLanguage langauge);
    Optional<LearningLanguage> findByIdAndIsAvailable(UUID id);
    boolean existsByIdAndIsAvailable(UUID id);
}
