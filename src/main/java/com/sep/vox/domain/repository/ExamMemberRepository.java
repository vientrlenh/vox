package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamMember;
import com.sep.vox.domain.model.exam.ExamMemberRole;

public interface ExamMemberRepository {
    ExamMember save(ExamMember examMember);
    Optional<ExamMember> findById(UUID id);
    List<ExamMember> findByExamId(UUID examId);
    Optional<ExamMember> findByExamIdAndUserId(UUID examId, UUID userId);
    boolean existsByExamIdAndUserIdAndRole(UUID examId, UUID userId, ExamMemberRole role);
    boolean existsByUserIdAndRoleAndSchoolId(UUID userId, ExamMemberRole role, UUID schoolId);
    boolean existsByExamIdAndRoleExcludingUserId(UUID examId, ExamMemberRole role, UUID excludeUserId);
    boolean existsByRoleAndSchoolIdExcludingUserId(ExamMemberRole role, UUID schoolId, UUID excludeUserId);
    boolean canAttachBlueprint(UUID examId, UUID userId);
    boolean canApproveBlueprintVersion(UUID examId, UUID userId);
    void deleteById(UUID id);
}
