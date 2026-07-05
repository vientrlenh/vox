package com.sep.vox.domain.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;

public interface AssessmentPolicyRepository {
    Optional<AssessmentPolicy> findById(UUID id);
    AssessmentPolicy save(AssessmentPolicy policy);
    List<AssessmentPolicy> saveAll(List<AssessmentPolicy> policies);

    // Danh sách Assessment Policy áp dụng toàn hệ thống (schoolId IS NULL)
    PageResult<AssessmentPolicy> findAllSystemWide(String status, UUID languageId, int page, int size);
    // Danh sách Assessment Policy của một trường học
    PageResult<AssessmentPolicy> findAllBySchoolId(UUID schoolId, String status, UUID languageId, int page, int size);
    Optional<AssessmentPolicy> findActivePolicy(UUID schoolId, UUID languageId, UUID classId, UUID gradeId, UUID gradeLevelId, OffsetDateTime atTime);
    boolean existsByFrameworkVersionId(UUID frameworkVersionId);

    // Kiểm tra đã tồn tại policy DRAFT/PUBLISHED nào trùng scope VÀ cùng Rubric Version hay chưa (chặn tạo trùng)
    boolean existsActiveForScope(UUID schoolId, UUID languageId, UUID frameworkVersionId,
            UUID schoolGradeLevelId, UUID schoolGradeId, UUID schoolClassId, UUID rubricVersionId);

    // Lấy version lớn nhất đã từng tồn tại cho scope (kể cả ARCHIVED) để tính version kế tiếp
    int findMaxVersionForScope(UUID schoolId, UUID languageId, UUID frameworkVersionId,
            UUID schoolGradeLevelId, UUID schoolGradeId, UUID schoolClassId);

    // Kiểm tra đã tồn tại Assessment Policy nào liên kết với Rubric Version này đang ở trạng thái PUBLISHED hay chưa
    boolean existsPublishedByRubricVersionId(UUID rubricVersionId);

    void deleteById(UUID id);
}
