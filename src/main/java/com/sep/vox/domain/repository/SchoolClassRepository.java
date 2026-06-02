package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.schoolclass.SchoolClass;

public interface SchoolClassRepository {
    Optional<SchoolClass> findById(UUID id);
    PageResult<SchoolClass> findBySchoolId(UUID schoolId, PageRequest pageRequest);
    Optional<SchoolClass> findBySchoolIdAndCode(UUID schoolId, String code);
    List<SchoolClass> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes);
    List<SchoolClass> findBySchoolIdAndName(UUID schoolId, String name);
    List<SchoolClass> findBySchoolIdAndLanguageIdAndTargetSchoolLevelVersionId(UUID schoolId, UUID languageId, UUID levelId);
    SchoolClass save(SchoolClass schoolClass);
    void deleteById(UUID id);
}
