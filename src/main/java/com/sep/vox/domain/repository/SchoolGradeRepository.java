package com.sep.vox.domain.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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

    List<SchoolGrade> findAllById(List<UUID> gradeIds);

    // Đổi chữ SchoolId thành SchoolGradeLevelId
    boolean existsBySchoolGradeLevelIdAndCode(UUID schoolGradeLevelId, String code);
    Optional<SchoolGrade> findBySchoolGradeLevelIdAndCode(UUID schoolGradeLevelId, String code);
    List<SchoolGrade> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes);
    List<SchoolGrade> findBySchoolIdAndNameIn(UUID schoolId, Collection<String> names);

    boolean existsBySchoolGradeLevelId(UUID schoolGradeLevelId);

    boolean existsBySchoolGradeLevelIdAndStatusNot(UUID schoolGradeLevelId, String status);

    PageResult<SchoolGrade> findAllBySchoolId(UUID schoolId, UUID schoolGradeLevelId, String status, int pageNumber, int size);

    boolean existsBySchoolIdAndStatus(UUID schoolId, String status);
    int updateSchoolGradeAtomic(UUID id, String name, String description, LocalDate startDate, LocalDate endDate, OffsetDateTime now, UUID updatedBy);
    void deleteById(UUID schoolGradeId);
    List<SchoolGrade> findByIdIn(Collection<UUID> ids);
}
