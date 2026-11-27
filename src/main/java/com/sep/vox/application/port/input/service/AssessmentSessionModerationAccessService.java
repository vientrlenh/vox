package com.sep.vox.application.port.input.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class AssessmentSessionModerationAccessService {

    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;

    public UUID getCurrentUserId() {
        return userContextPort.getCurrentAuthenticatedUserId();
    }

    public void authorize(Assessment assessment, AssessmentCandidate candidate) {
        UUID currentUserId = getCurrentUserId();
        if (userContextPort.isSystemAdmin()) {
            return;
        }

        if (isSchoolAdminOfAssessmentSchool(currentUserId, assessment)
                || isMemberWithAccess(assessment, currentUserId)) {
            return;
        }

        if (candidate.getScheduleId() != null
                && assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(candidate.getScheduleId(), currentUserId)) {
            return;
        }

        throw new ForbiddenException("Access denied");
    }

    private boolean isSchoolAdminOfAssessmentSchool(UUID currentUserId, Assessment assessment) {
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        return schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId());
    }

    private boolean isMemberWithAccess(Assessment assessment, UUID currentUserId) {
        return assessmentMemberRepository.findByAssessmentIdAndUserId(assessment.getId(), currentUserId)
            .map(member -> member.getRole() == AssessmentMemberRole.CHAIR
                || member.getRole() == AssessmentMemberRole.AUTHOR
                || member.getRole() == AssessmentMemberRole.REVIEWER)
            .orElse(false);
    }
}
