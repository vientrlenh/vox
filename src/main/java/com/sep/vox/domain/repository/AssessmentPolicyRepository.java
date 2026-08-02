package com.sep.vox.domain.repository;

import java.time.Instant;
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
    PageResult<AssessmentPolicy> findAllSystemWide(String status, UUID languageId, UUID rubricVersionId,
            Instant effectiveFrom, Instant effectiveTo, int page, int size);
    // Danh sách Assessment Policy của một trường học
    PageResult<AssessmentPolicy> findAllBySchoolId(UUID schoolId, String status, UUID languageId, UUID rubricVersionId,
            Instant effectiveFrom, Instant effectiveTo, int page, int size);
    Optional<AssessmentPolicy> findActivePolicy(UUID schoolId, UUID languageId, UUID classId, UUID gradeId, UUID gradeLevelId, Instant atTime);
    boolean existsByFrameworkVersionId(UUID frameworkVersionId);

    // Kiểm tra còn Assessment Policy nào áp riêng cho một lớp học hay không (dùng khi xóa lớp)
    boolean existsBySchoolClassId(UUID schoolClassId);

    // Kiểm tra đã tồn tại policy DRAFT/PUBLISHED nào trùng scope VÀ cùng Rubric Version hay chưa (chặn tạo trùng)
    boolean existsActiveForScope(UUID schoolId, UUID languageId, UUID frameworkVersionId,
            UUID schoolGradeLevelId, UUID schoolGradeId, UUID schoolClassId, UUID rubricVersionId);

    // Lấy TOÀN BỘ Assessment Policy (mọi trạng thái, kể cả ARCHIVED) trong phạm vi schoolId (null = toàn hệ thống).
    // Dùng để prefetch 1 lần rồi tự tính trong memory (scope đang active để chặn trùng, version lớn nhất theo
    // scope để phát version kế tiếp) khi import hàng loạt, thay vì gọi existsActiveForScope/findMaxVersionForScope
    // riêng lẻ cho từng dòng/từng scope trong file Excel (tránh N+1 query).
    List<AssessmentPolicy> findAllForOwner(UUID schoolId);

    // Lấy version lớn nhất đã từng tồn tại cho scope (kể cả ARCHIVED) để tính version kế tiếp
    int findMaxVersionForScope(UUID schoolId, UUID languageId, UUID frameworkVersionId,
            UUID schoolGradeLevelId, UUID schoolGradeId, UUID schoolClassId);

    // Kiểm tra đã tồn tại Assessment Policy nào liên kết với Rubric Version này đang ở trạng thái PUBLISHED hay chưa
    boolean existsPublishedByRubricVersionId(UUID rubricVersionId);

    // Kiểm tra còn Assessment Policy nào liên kết với Rubric Version này CHƯA ở trạng thái PUBLISHED hay không
    boolean existsNotPublishedByRubricVersionId(UUID rubricVersionId);

    // Danh sách Assessment Policy hệ thống (schoolId IS NULL) đang DRAFT liên kết với Rubric Version này (dùng cho publish hàng loạt)
    List<AssessmentPolicy> findDraftSystemWideByRubricVersionId(UUID rubricVersionId);

    // Danh sách Assessment Policy của một trường học đang DRAFT liên kết với Rubric Version này (dùng cho publish hàng loạt)
    List<AssessmentPolicy> findDraftBySchoolIdAndRubricVersionId(UUID schoolId, UUID rubricVersionId);

    void deleteById(UUID id);

    record CurrentPolicy(UUID rubricVersionId, UUID targetFrameworkBandId) {
    }

    // Chính sách chấm đang hiệu lực cho 1 học sinh (dùng khi bắt đầu phiên luyện tập)
    List<CurrentPolicy> findCurrentPolicyForStudent(UUID studentId);
}
