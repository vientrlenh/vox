package com.sep.vox.domain.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;

public interface SchoolClassRepository {
    Optional<SchoolClass> findById(UUID id);
    PageResult<SchoolClass> findBySchoolId(UUID schoolId, PageRequest pageRequest);
    PageResult<SchoolClass> findBySchoolId(UUID schoolId, String search, SchoolClassStatus status,
            UUID languageId, UUID schoolGradeId, PageRequest pageRequest);
    Optional<SchoolClass> findBySchoolIdAndCode(UUID schoolId, String code);
    List<SchoolClass> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes);
    List<SchoolClass> findBySchoolIdAndName(UUID schoolId, String name);
    List<SchoolClass> findBySchoolIdAndLanguageIdAndTargetSchoolLevelVersionId(UUID schoolId, UUID languageId, UUID levelId);
    SchoolClass save(SchoolClass schoolClass);
    int updateMutableFields(UUID id, UUID schoolId, String name, boolean nameProvided,
            String description, boolean descriptionProvided, SchoolClassStatus status, boolean statusProvided,
            OffsetDateTime updatedAt, UUID updatedBy);
    void deleteById(UUID id);
    List<SchoolClass> findBySchoolIdIn(Collection<UUID> schoolIds, int page, int size);
}
