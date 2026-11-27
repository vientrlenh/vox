package com.sep.vox.domain.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolGrade;

public interface SchoolGradeRepository {
    Optional<SchoolGrade> findById(UUID id);
    Optional<SchoolGrade> findBySchoolIdAndCode(UUID schoolId, String code);
    Optional<SchoolGrade> findBySchoolIdAndName(UUID schoolId, String name);
    SchoolGrade save(SchoolGrade grade);

    // gradeLevelId now points at the global catalog, so a class code is only unique within
    // (schoolId, gradeLevelId) -- schoolId must be passed along, unlike the old version that only needed levelId.
    boolean existsBySchoolIdAndGradeLevelIdAndCode(UUID schoolId, UUID gradeLevelId, String code);
    Optional<SchoolGrade> findBySchoolIdAndGradeLevelIdAndCode(UUID schoolId, UUID gradeLevelId, String code);
    List<SchoolGrade> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes);
    List<SchoolGrade> findBySchoolIdAndNameIn(UUID schoolId, Collection<String> names);

    // Scans every school: used to block deleting a grade in the shared catalog.
    boolean existsByGradeLevelId(UUID gradeLevelId);

    boolean existsByGradeLevelIdAndStatusNot(UUID gradeLevelId, String status);

    PageResult<SchoolGrade> findBySchoolId(UUID schoolId, UUID gradeLevelId, String status, int pageNumber, int size);

    boolean existsBySchoolIdAndStatus(UUID schoolId, String status);
    int updateSchoolGradeAtomic(UUID id, String name, String description, boolean descriptionProvided, LocalDate startDate, LocalDate endDate);
    void deleteById(UUID schoolGradeId);
    List<SchoolGrade> findByIdIn(Collection<UUID> ids);
}
