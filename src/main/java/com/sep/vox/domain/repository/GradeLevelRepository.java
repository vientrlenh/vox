package com.sep.vox.domain.repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Grade level catalog shared across the whole system. Previously each school declared its own set
 * (SchoolGradeLevelRepository, every query carried schoolId) -- grade levels are now global data
 * so schoolId has been dropped from every signature.
 */
public interface GradeLevelRepository {
    Optional<GradeLevel> findById(UUID id);
    Optional<GradeLevel> findByCode(String code);
    Optional<GradeLevel> findByName(String name);
    PageResult<GradeLevel> findAll(String search, GradeLevelStatus status, int page, int size);
    List<GradeLevel> findByCodeIn(Collection<String> codes);
    List<GradeLevel> findByNameIn(Collection<String> names);
    List<GradeLevel> findByIdIn(Collection<UUID> ids);
    GradeLevel save(GradeLevel gradeLevel);
    int updateGradeLevelAtomic(UUID id, String name, String description, boolean descriptionProvided, Integer order);
    boolean existsByCode(String code);
    boolean existsByOrder(int order);
    void deleteById(UUID id);
}
