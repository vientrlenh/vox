package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;

public interface RubricRepository {
    Optional<Rubric> findById(UUID id);
    List<Rubric> findByIdIn(Collection<UUID> ids);

    Rubric save(Rubric rubric);

    void deleteById(UUID id);

    boolean existsByOwnerTypeAndSchoolIdAndLanguageId(String ownerType, UUID schoolId, UUID languageId);

    boolean existsByOwnerTypeAndLanguageId(String ownerType, UUID languageId);

    /** Chặn trùng mã trong cùng phạm vi -- soi đúng unique index của bảng rubrics. */
    boolean existsByOwnerTypeAndSchoolIdAndLanguageIdAndFrameworkIdAndCode(
            String ownerType, UUID schoolId, UUID languageId, UUID frameworkId, String code);

    void updateRubricAtomic(UUID id, String name, String description);

    PageResult<Rubric> findAllByOwnerType(RubricOwnerType ownerType, int page, int size);

    PageResult<Rubric> findAllByOwnerTypeAndSchoolId(RubricOwnerType ownerType, UUID schoolId, int page, int size);

    PageResult<Rubric> searchSystemRubrics(String keyword, UUID frameworkId, UUID languageId, int page, int size);

    PageResult<Rubric> searchSchoolRubrics(UUID schoolId, String keyword, UUID frameworkId, UUID languageId, int page, int size);


}
