package com.sep.vox.domain.repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.school.SchoolGradeLevelStatus;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolGradeLevelRepository {
    Optional<SchoolGradeLevel> findById(UUID id);
    Optional<SchoolGradeLevel> findBySchoolIdAndCode(UUID schoolId, String code);
    Optional<SchoolGradeLevel> findBySchoolIdAndName(UUID schoolId, String name);
    PageResult<SchoolGradeLevel> findBySchoolId(UUID schoolId, String search, SchoolGradeLevelStatus status,
            int page, int size);
    List<SchoolGradeLevel> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes);
    List<SchoolGradeLevel> findBySchoolIdAndNameIn(UUID schoolId, Collection<String> names);
    List<SchoolGradeLevel> findByIdIn(Collection<UUID> ids);
    SchoolGradeLevel save(SchoolGradeLevel gradeLevel);
    int updateSchoolGradeLevelAtomic(UUID id, String name, String description, Integer order,
            OffsetDateTime updatedAt, UUID updatedBy);
    boolean existsBySchoolIdAndCode(UUID schoolId, String code);
    boolean existsBySchoolIdAndOrder(UUID schoolId, int order);
    void deleteById(UUID id);
}
