package com.sep.vox.domain.repository;

import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.domain.shared.PageResult;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GradeLevelRepository {
    Optional<GradeLevel> findById(UUID id);
    Optional<GradeLevel> findByCode(String code);
    Optional<GradeLevel> findByName(String name);
    PageResult<GradeLevel> findAll(String search, GradeLevelStatus status, int page, int size);
    List<GradeLevel> findByCodeIn(Collection<String> codes);
    List<GradeLevel> findByNameIn(Collection<String> names);
    List<GradeLevel> findByIdIn(Collection<UUID> ids);
    GradeLevel save(GradeLevel gradeLevel);
    int updateGradeLevelAtomic(UUID id, String name, String description, Integer order,
            Instant updatedAt, UUID updatedBy);
    boolean existsByCode(String code);
    boolean existsByOrder(int order);
    void deleteById(UUID id);
    List<GradeLevel> saveAll(Collection<GradeLevel> gradeLevels);
}
