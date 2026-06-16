package com.sep.vox.domain.repository;

import com.sep.vox.domain.model.school.SchoolGradeLevel;

import java.util.Optional;
import java.util.UUID;

public interface SchoolGradeLevelRepository {
    Optional<SchoolGradeLevel> findById(UUID id);
    Optional<SchoolGradeLevel> findBySchoolIdAndCode(UUID schoolId, String code);
    SchoolGradeLevel save(SchoolGradeLevel gradeLevel);
    boolean existsBySchoolIdAndCode(UUID schoolId, String code);
    boolean existsBySchoolIdAndOrder(UUID schoolId, int order);
    void deleteById(UUID id);
}
