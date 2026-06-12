package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;

public interface RubricRepository {
    Optional<Rubric> findById(UUID id);
    Rubric save(Rubric rubric);
    void  deleteById (UUID id);

    boolean existsByOwnerTypeAndSchoolIdAndLanguageId(RubricOwnerType ownerType, UUID schoolId, UUID languageId);
    boolean existsByOwnerTypeAndLanguageId(RubricOwnerType ownerType, UUID languageId);
}
