package com.sep.vox.domain.repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Catalog khối lớp dùng chung toàn hệ thống. Trước đây mỗi trường tự khai một bộ riêng
 * (SchoolGradeLevelRepository, mọi truy vấn đều kèm schoolId) -- nay khối lớp là dữ liệu
 * toàn cục nên schoolId đã bị bỏ khỏi toàn bộ chữ ký.
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
    int updateGradeLevelAtomic(UUID id, String name, String description, Integer order,
            Instant updatedAt, UUID updatedBy);
    boolean existsByCode(String code);
    boolean existsByOrder(int order);
    void deleteById(UUID id);
}
