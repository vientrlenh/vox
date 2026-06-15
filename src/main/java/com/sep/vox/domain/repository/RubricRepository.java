package com.sep.vox.domain.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;

public interface RubricRepository {
    Optional<Rubric> findById(UUID id);
    Rubric save(Rubric rubric);
    void  deleteById (UUID id);

    boolean existsByOwnerTypeAndSchoolIdAndLanguageId(String ownerType, UUID schoolId, UUID languageId);
    boolean existsByOwnerTypeAndLanguageId(String ownerType, UUID languageId);

    void updateRubricAtomic(UUID id, String name, String description);

    PageResult<Rubric> findAllByOwnerType(RubricOwnerType ownerType, int page, int size);

    PageResult<Rubric> findAllByOwnerTypeAndSchoolId(RubricOwnerType ownerType, UUID schoolId, int page, int size);
}