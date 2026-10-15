package com.sep.vox.domain.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentMember;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;

public interface AssessmentMemberRepository {
    AssessmentMember save(AssessmentMember member);
    Optional<AssessmentMember> findById(UUID id);
    List<AssessmentMember> findByExamId(UUID examId);
    List<AssessmentMember> findByExamIdIn(Collection<UUID> examIds);
    Optional<AssessmentMember> findByExamIdAndUserId(UUID examId, UUID userId);
    boolean existsByExamIdAndUserIdAndRole(UUID examId, UUID userId, AssessmentMemberRole role);
    boolean existsByUserIdAndRoleAndSchoolId(UUID userId, AssessmentMemberRole role, UUID schoolId);
    boolean existsByExamIdAndRoleExcludingUserId(UUID examId, AssessmentMemberRole role, UUID excludeUserId);
    boolean existsByRoleAndSchoolIdExcludingUserId(AssessmentMemberRole role, UUID schoolId, UUID excludeUserId);
    /** Quản trị trường, hoặc thành viên AUTHOR/CHAIR của kỳ thi — người được gắn/đổi khung đề. */
    boolean canAttachBlueprint(UUID examId, UUID userId);
    boolean canApproveBlueprintVersion(UUID examId, UUID userId);
    void deleteById(UUID id);
    void deleteByExamId(UUID examId);
}
