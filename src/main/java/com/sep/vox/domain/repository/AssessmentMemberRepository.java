package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentMember;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;

public interface AssessmentMemberRepository {
    AssessmentMember save(AssessmentMember assessmentMember);
    Optional<AssessmentMember> findById(UUID id);
    List<AssessmentMember> findByAssessmentId(UUID assessmentId);
    List<AssessmentMember> findByAssessmentIdIn(Collection<UUID> assessmentIds);
    Optional<AssessmentMember> findByAssessmentIdAndUserId(UUID assessmentId, UUID userId);
    boolean existsByAssessmentIdAndUserIdAndRole(UUID assessmentId, UUID userId, AssessmentMemberRole role);
    boolean existsByUserIdAndRoleAndSchoolId(UUID userId, AssessmentMemberRole role, UUID schoolId);
    boolean existsByAssessmentIdAndRoleExcludingUserId(UUID assessmentId, AssessmentMemberRole role, UUID excludeUserId);
    boolean existsByRoleAndSchoolIdExcludingUserId(AssessmentMemberRole role, UUID schoolId, UUID excludeUserId);
    /** School admins, or AUTHOR/CHAIR members of the assessment — the people allowed to attach/change the paper framework. */
    boolean canAttachBlueprint(UUID assessmentId, UUID userId);
    boolean canApproveBlueprintVersion(UUID assessmentId, UUID userId);
    void deleteById(UUID id);
    void deleteByAssessmentId(UUID assessmentId);
}
