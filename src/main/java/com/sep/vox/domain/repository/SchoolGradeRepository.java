package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolGrade;

public interface SchoolGradeRepository {
    Optional<SchoolGrade> findById(UUID id);
    Optional<SchoolGrade> findBySchoolIdAndCode(UUID schoolId, String code);
    List<SchoolGrade> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes);
    SchoolGrade save(SchoolGrade grade);

    List<SchoolGrade> findAllById(List<UUID> gradeIds);
}
