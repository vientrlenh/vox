package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.schoolgrade.SchoolGrade;

public interface SchoolGradeRepository {
    Optional<SchoolGrade> findById(UUID id);
    Optional<SchoolGrade> findBySchoolIdAndCode(UUID schoolId, String code);
    SchoolGrade save(SchoolGrade grade);
}
