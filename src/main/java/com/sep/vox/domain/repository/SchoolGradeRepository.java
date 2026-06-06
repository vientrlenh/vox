package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolGrade;

public interface SchoolGradeRepository {
    Optional<SchoolGrade> findById(UUID id);
    Optional<SchoolGrade> findBySchoolIdAndCode(UUID schoolId, String code);
    SchoolGrade save(SchoolGrade grade);
}
