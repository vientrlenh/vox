package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.languagelevel.SchoolLevel;

public interface SchoolLevelRepository {
    Optional<SchoolLevel> findById(UUID id);
    Optional<SchoolLevel> findBySchoolIdAndLanguageIdAndCode(UUID schoolId, UUID languageId, String code);
    SchoolLevel save(SchoolLevel schoolLevel);
}
